package com.example.riscv.assembler

import com.example.riscv.model.*

class RiscvAssembler {

    fun assemble(sourceCode: String): AssemblyResult {
        val lines = sourceCode.lines()
        val errors = mutableListOf<AssemblyError>()
        val symbols = mutableMapOf<String, Symbol>()
        val initialMemory = Memory()
        val sourceLineToAddressMap = mutableMapOf<Int, Int>()

        var currentSegment = Segment.TEXT
        var currentTextAddr = Memory.TEXT_BASE
        var currentDataAddr = Memory.DATA_BASE

        // Structure to store raw parsed lines before pass 2
        data class PreParsedInstruction(
            val address: Int,
            val lineNumber: Int,
            val originalLine: String,
            val tokens: List<String>,
            val opcode: String
        )

        val preParsedInstructions = mutableListOf<PreParsedInstruction>()

        // ----------------------------------------------------
        // PASS 1: Symbol discovery, pseudo-expansion, data emission
        // ----------------------------------------------------
        for ((index, rawLine) in lines.withIndex()) {
            val lineNumber = index + 1
            // Remove comments and trim
            val commentIdx = rawLine.indexOf('#')
            var line = if (commentIdx >= 0) rawLine.substring(0, commentIdx) else rawLine
            line = line.trim()

            if (line.isEmpty()) continue

            // Check for labels (e.g., "main:" or "loop: addi x1, x1, 1")
            while (true) {
                val colonIdx = line.indexOf(':')
                if (colonIdx >= 0) {
                    val labelName = line.substring(0, colonIdx).trim()
                    if (labelName.isNotEmpty()) {
                        if (symbols.containsKey(labelName)) {
                            errors.add(AssemblyError(lineNumber, rawLine, "Duplicate symbol definition: '$labelName'"))
                        } else {
                            val addr = if (currentSegment == Segment.TEXT) currentTextAddr else currentDataAddr
                            symbols[labelName] = Symbol(labelName, addr, isData = (currentSegment == Segment.DATA), lineNumber = lineNumber)
                        }
                    }
                    line = line.substring(colonIdx + 1).trim()
                } else {
                    break
                }
            }

            if (line.isEmpty()) continue

            // Check directives
            if (line.startsWith(".")) {
                val parts = line.split("\\s+".toRegex(), limit = 2)
                val directive = parts[0].lowercase()
                val args = if (parts.size > 1) parts[1].trim() else ""

                when (directive) {
                    ".text" -> currentSegment = Segment.TEXT
                    ".data" -> currentSegment = Segment.DATA
                    ".globl", ".global" -> { /* Ignore global declarations */ }
                    ".asciiz", ".ascii" -> {
                        if (currentSegment == Segment.DATA) {
                            var str = args
                            if (str.startsWith("\"") && str.endsWith("\"") && str.length >= 2) {
                                str = str.substring(1, str.length - 1)
                            }
                            str = unescapeString(str)
                            val written = initialMemory.writeString(currentDataAddr, str, nullTerminate = (directive == ".asciiz"))
                            currentDataAddr += written
                        }
                    }
                    ".word" -> {
                        if (currentSegment == Segment.DATA) {
                            val nums = args.split(",").map { it.trim() }
                            for (nStr in nums) {
                                if (nStr.isNotEmpty()) {
                                    val value = parseNumberOrSymbolPlaceholder(nStr)
                                    initialMemory.writeWord(currentDataAddr, value)
                                    currentDataAddr += 4
                                }
                            }
                        }
                    }
                    ".half" -> {
                        if (currentSegment == Segment.DATA) {
                            val nums = args.split(",").map { it.trim() }
                            for (nStr in nums) {
                                if (nStr.isNotEmpty()) {
                                    val value = parseNumberOrSymbolPlaceholder(nStr).toShort()
                                    initialMemory.writeHalfword(currentDataAddr, value)
                                    currentDataAddr += 2
                                }
                            }
                        }
                    }
                    ".byte" -> {
                        if (currentSegment == Segment.DATA) {
                            val nums = args.split(",").map { it.trim() }
                            for (nStr in nums) {
                                if (nStr.isNotEmpty()) {
                                    val value = parseNumberOrSymbolPlaceholder(nStr).toByte()
                                    initialMemory.writeByte(currentDataAddr, value)
                                    currentDataAddr += 1
                                }
                            }
                        }
                    }
                    ".space" -> {
                        if (currentSegment == Segment.DATA) {
                            val bytes = args.toIntOrNull() ?: 0
                            currentDataAddr += bytes
                        }
                    }
                    ".float", ".double" -> {
                        if (currentSegment == Segment.DATA) {
                            val nums = args.split(",").map { it.trim() }
                            for (nStr in nums) {
                                if (nStr.isNotEmpty()) {
                                    val fVal = nStr.toFloatOrNull() ?: 0f
                                    val bits = java.lang.Float.floatToIntBits(fVal)
                                    initialMemory.writeWord(currentDataAddr, bits)
                                    currentDataAddr += 4
                                }
                            }
                        }
                    }
                    ".align" -> {
                        val alignPower = args.toIntOrNull() ?: 2
                        val alignment = 1 shl alignPower
                        if (currentSegment == Segment.DATA) {
                            val rem = currentDataAddr % alignment
                            if (rem != 0) currentDataAddr += (alignment - rem)
                        } else {
                            val rem = currentTextAddr % alignment
                            if (rem != 0) currentTextAddr += (alignment - rem)
                        }
                    }
                }
                continue
            }

            // Normal instruction parsing in .text segment
            if (currentSegment == Segment.TEXT) {
                val expandedLines = expandPseudoInstruction(line)
                for (exp in expandedLines) {
                    val tokens = tokenizeInstruction(exp)
                    if (tokens.isNotEmpty()) {
                        val opcode = tokens[0].lowercase()
                        preParsedInstructions.add(
                            PreParsedInstruction(
                                address = currentTextAddr,
                                lineNumber = lineNumber,
                                originalLine = rawLine,
                                tokens = tokens,
                                opcode = opcode
                            )
                        )
                        if (!sourceLineToAddressMap.containsKey(lineNumber)) {
                            sourceLineToAddressMap[lineNumber] = currentTextAddr
                        }
                        currentTextAddr += 4
                    }
                }
            }
        }

        // ----------------------------------------------------
        // PASS 2: Instruction Encoding & Machine Code Generation
        // ----------------------------------------------------
        val instructions = mutableListOf<Instruction>()

        for (pre in preParsedInstructions) {
            try {
                val inst = encodeInstruction(
                    address = pre.address,
                    lineNumber = pre.lineNumber,
                    originalLine = pre.originalLine,
                    tokens = pre.tokens,
                    opcode = pre.opcode,
                    symbols = symbols
                )
                instructions.add(inst)
            } catch (e: Exception) {
                errors.add(AssemblyError(pre.lineNumber, pre.originalLine, e.message ?: "Assembly error"))
            }
        }

        return AssemblyResult(
            isSuccess = errors.isEmpty(),
            instructions = instructions,
            symbols = symbols,
            initialMemory = initialMemory,
            errors = errors,
            sourceLineToAddressMap = sourceLineToAddressMap
        )
    }

    private enum class Segment { TEXT, DATA }

    private fun resolveSymbolOrNumber(target: String, symbols: Map<String, Symbol>): Int {
        val s = target.trim()
        val sym = symbols[s]
        if (sym != null) {
            return sym.address
        }
        val num = parseImmediateValue(s)
        if (num != null) {
            return num
        }
        throw IllegalArgumentException("Symbol '$s' not found in symbol table.")
    }

    private fun parseImmediateValue(str: String): Int? {
        val s = str.trim()
        if (s.isEmpty()) return null
        if (s.startsWith("0x") || s.startsWith("0X")) {
            return try { s.substring(2).toLong(16).toInt() } catch (e: Exception) { null }
        }
        if (s.startsWith("0b") || s.startsWith("0B")) {
            return try { s.substring(2).toInt(2) } catch (e: Exception) { null }
        }
        return s.toIntOrNull()
    }

    private fun parseNumberOrSymbolPlaceholder(str: String, symbols: Map<String, Symbol>): Int {
        val s = str.trim()
        val sym = symbols[s]
        if (sym != null) return sym.address
        val num = parseImmediateValue(s)
        if (num != null) return num
        return 0
    }

    private fun tokenizeInstruction(line: String): List<String> {
        val result = mutableListOf<String>()
        val parts = line.split("\\s+".toRegex(), limit = 2)
        if (parts.isEmpty()) return result
        result.add(parts[0].trim())

        if (parts.size > 1) {
            val argsStr = parts[1]
            var current = StringBuilder()
            var inParens = false
            for (ch in argsStr) {
                if (ch == '(' || ch == ')') inParens = !inParens
                if (ch == ',' && !inParens) {
                    val token = current.toString().trim()
                    if (token.isNotEmpty()) result.add(token)
                    current = StringBuilder()
                } else {
                    current.append(ch)
                }
            }
            val lastToken = current.toString().trim()
            if (lastToken.isNotEmpty()) result.add(lastToken)
        }
        return result
    }

    private fun expandPseudoInstruction(line: String): List<String> {
        val tokens = tokenizeInstruction(line)
        if (tokens.isEmpty()) return listOf(line)

        val op = tokens[0].lowercase()

        return when (op) {
            "nop" -> listOf("addi x0, x0, 0")
            "mv" -> if (tokens.size >= 3) listOf("addi ${tokens[1]}, ${tokens[2]}, 0") else listOf(line)
            "not" -> if (tokens.size >= 3) listOf("xori ${tokens[1]}, ${tokens[2]}, -1") else listOf(line)
            "neg" -> if (tokens.size >= 3) listOf("sub ${tokens[1]}, x0, ${tokens[2]}") else listOf(line)
            "j" -> if (tokens.size >= 2) listOf("jal x0, ${tokens[1]}") else listOf(line)
            "jr" -> if (tokens.size >= 2) listOf("jalr x0, ${tokens[1]}, 0") else listOf(line)
            "ret" -> listOf("jalr x0, x1, 0")
            "call" -> if (tokens.size >= 2) listOf("jal x1, ${tokens[1]}") else listOf(line)
            "beqz" -> if (tokens.size >= 3) listOf("beq ${tokens[1]}, x0, ${tokens[2]}") else listOf(line)
            "bnez" -> if (tokens.size >= 3) listOf("bne ${tokens[1]}, x0, ${tokens[2]}") else listOf(line)
            "blez" -> if (tokens.size >= 3) listOf("bge x0, ${tokens[1]}, ${tokens[2]}") else listOf(line)
            "bgtz" -> if (tokens.size >= 3) listOf("blt x0, ${tokens[1]}, ${tokens[2]}") else listOf(line)
            "bltz" -> if (tokens.size >= 3) listOf("blt ${tokens[1]}, x0, ${tokens[2]}") else listOf(line)
            "bgez" -> if (tokens.size >= 3) listOf("bge ${tokens[1]}, x0, ${tokens[2]}") else listOf(line)
            "ble" -> if (tokens.size >= 4) listOf("bge ${tokens[2]}, ${tokens[1]}, ${tokens[3]}") else if (tokens.size == 3) listOf("bge x0, ${tokens[1]}, ${tokens[2]}") else listOf(line)
            "bgt" -> if (tokens.size >= 4) listOf("blt ${tokens[2]}, ${tokens[1]}, ${tokens[3]}") else if (tokens.size == 3) listOf("blt x0, ${tokens[1]}, ${tokens[2]}") else listOf(line)
            "bleu" -> if (tokens.size >= 4) listOf("bgeu ${tokens[2]}, ${tokens[1]}, ${tokens[3]}") else if (tokens.size == 3) listOf("bgeu x0, ${tokens[1]}, ${tokens[2]}") else listOf(line)
            "bgtu" -> if (tokens.size >= 4) listOf("bltu ${tokens[2]}, ${tokens[1]}, ${tokens[3]}") else if (tokens.size == 3) listOf("bltu x0, ${tokens[1]}, ${tokens[2]}") else listOf(line)
            "seqz" -> if (tokens.size >= 3) listOf("sltiu ${tokens[1]}, ${tokens[2]}, 1") else listOf(line)
            "snez" -> if (tokens.size >= 3) listOf("sltu ${tokens[1]}, x0, ${tokens[2]}") else listOf(line)
            "sltz" -> if (tokens.size >= 3) listOf("slt ${tokens[1]}, ${tokens[2]}, x0") else listOf(line)
            "sgtz" -> if (tokens.size >= 3) listOf("slt ${tokens[1]}, x0, ${tokens[2]}") else listOf(line)
            "fneg.s" -> if (tokens.size >= 3) listOf("fsub.s ${tokens[1]}, f0, ${tokens[2]}") else listOf(line)
            "fabs.s" -> if (tokens.size >= 3) listOf("fmul.s ${tokens[1]}, ${tokens[2]}, ${tokens[2]}") else listOf(line)
            "la" -> {
                if (tokens.size >= 3) {
                    listOf(line)
                } else listOf(line)
            }
            "li" -> {
                if (tokens.size >= 3) {
                    val immVal = parseImmediateValue(tokens[2])
                    if (immVal != null && (immVal < -2048 || immVal > 2047)) {
                        val upper = (immVal + 0x800) ushr 12
                        val lower = immVal - (upper shl 12)
                        listOf("lui ${tokens[1]}, $upper", "addi ${tokens[1]}, ${tokens[1]}, $lower")
                    } else listOf(line)
                } else listOf(line)
            }
            "lw", "lh", "lb", "lbu", "lhu" -> {
                if (tokens.size >= 3 && !tokens[2].contains("(")) {
                    if (Register.parseRegisterName(tokens[2]) == null) {
                        val rd = tokens[1]
                        val label = tokens[2]
                        return listOf("la $rd, $label", "$op $rd, 0($rd)")
                    }
                }
                listOf(line)
            }
            "flw" -> {
                if (tokens.size >= 3 && !tokens[2].contains("(")) {
                    if (Register.parseRegisterName(tokens[2]) == null) {
                        val rd = tokens[1]
                        val label = tokens[2]
                        return listOf("la t6, $label", "flw $rd, 0(t6)")
                    }
                }
                listOf(line)
            }
            "sw", "sh", "sb", "fsw" -> {
                if (tokens.size >= 3 && !tokens[2].contains("(")) {
                    if (Register.parseRegisterName(tokens[2]) == null) {
                        val rs = tokens[1]
                        val label = tokens[2]
                        return listOf("la t6, $label", "$op $rs, 0(t6)")
                    }
                }
                listOf(line)
            }
            else -> listOf(line)
        }
    }

    private fun parseRegister(str: String): Int {
        return Register.parseRegisterName(str)
            ?: throw IllegalArgumentException("Unknown register name: '$str'")
    }

    private fun parseFloatRegister(str: String): Int {
        return FloatRegister.parseFloatRegisterName(str)
            ?: throw IllegalArgumentException("Unknown float register name: '$str'")
    }

    private fun parseMemoryOffsetRegister(str: String, symbols: Map<String, Symbol>): Pair<Int, Int> {
        val idxOpen = str.indexOf('(')
        val idxClose = str.indexOf(')')
        if (idxOpen >= 0 && idxClose > idxOpen) {
            val offsetStr = str.substring(0, idxOpen).trim()
            val regStr = str.substring(idxOpen + 1, idxClose).trim()
            val offset = if (offsetStr.isNotEmpty()) resolveSymbolOrNumber(offsetStr, symbols) else 0
            val reg = parseRegister(regStr)
            return Pair(offset, reg)
        }
        return Pair(0, parseRegister(str))
    }

    private fun encodeInstruction(
        address: Int,
        lineNumber: Int,
        originalLine: String,
        tokens: List<String>,
        opcode: String,
        symbols: Map<String, Symbol>
    ): Instruction {
        val formattedText = tokens.joinToString(" ")

        return when (opcode) {
            // R-Type
            "add", "sub", "sll", "slt", "sltu", "xor", "srl", "sra", "or", "and", "mul", "mulh", "mulhu", "div", "divu", "rem", "remu" -> {
                val rd = parseRegister(tokens[1])
                val rs1 = parseRegister(tokens[2])
                val rs2 = parseRegister(tokens[3])
                val (funct3, funct7) = when (opcode) {
                    "add" -> Pair(0, 0x00)
                    "sub" -> Pair(0, 0x20)
                    "sll" -> Pair(1, 0x00)
                    "slt" -> Pair(2, 0x00)
                    "sltu" -> Pair(3, 0x00)
                    "xor" -> Pair(4, 0x00)
                    "srl" -> Pair(5, 0x00)
                    "sra" -> Pair(5, 0x20)
                    "or" -> Pair(6, 0x00)
                    "and" -> Pair(7, 0x00)
                    "mul" -> Pair(0, 0x01)
                    "mulh" -> Pair(1, 0x01)
                    "mulhu" -> Pair(3, 0x01)
                    "div" -> Pair(4, 0x01)
                    "divu" -> Pair(5, 0x01)
                    "rem" -> Pair(6, 0x01)
                    "remu" -> Pair(7, 0x01)
                    else -> Pair(0, 0)
                }
                val code = (funct7 shl 25) or (rs2 shl 20) or (rs1 shl 15) or (funct3 shl 12) or (rd shl 7) or 0x33
                Instruction(address, lineNumber, originalLine, formattedText, code, InstructionFormat.R_TYPE, opcode, rd, rs1, rs2)
            }

            // I-Type Arith / Logic
            "addi", "slti", "sltiu", "xori", "ori", "andi", "slli", "srli", "srai" -> {
                val rd = parseRegister(tokens[1])
                val rs1 = parseRegister(tokens[2])
                val imm = resolveSymbolOrNumber(tokens[3], symbols)
                val funct3 = when (opcode) {
                    "addi" -> 0
                    "slli" -> 1
                    "slti" -> 2
                    "sltiu" -> 3
                    "xori" -> 4
                    "srli", "srai" -> 5
                    "ori" -> 6
                    "andi" -> 7
                    else -> 0
                }
                val imm12 = if (opcode == "srai") (0x20 shl 5) or (imm and 0x1F) else (imm and 0xFFF)
                val code = (imm12 shl 20) or (rs1 shl 15) or (funct3 shl 12) or (rd shl 7) or 0x13
                Instruction(address, lineNumber, originalLine, formattedText, code, InstructionFormat.I_TYPE, opcode, rd, rs1, imm = imm)
            }

            // Loads: lb, lh, lw, lbu, lhu
            "lb", "lh", "lw", "lbu", "lhu" -> {
                val rd = parseRegister(tokens[1])
                val (offset, rs1) = parseMemoryOffsetRegister(tokens[2], symbols)
                val funct3 = when (opcode) {
                    "lb" -> 0
                    "lh" -> 1
                    "lw" -> 2
                    "lbu" -> 4
                    "lhu" -> 5
                    else -> 2
                }
                val code = ((offset and 0xFFF) shl 20) or (rs1 shl 15) or (funct3 shl 12) or (rd shl 7) or 0x03
                Instruction(address, lineNumber, originalLine, formattedText, code, InstructionFormat.I_TYPE, opcode, rd, rs1, imm = offset)
            }

            // Stores: sb, sh, sw
            "sb", "sh", "sw" -> {
                val rs2 = parseRegister(tokens[1])
                val (offset, rs1) = parseMemoryOffsetRegister(tokens[2], symbols)
                val funct3 = when (opcode) {
                    "sb" -> 0
                    "sh" -> 1
                    "sw" -> 2
                    else -> 2
                }
                val imm11_5 = (offset and 0xFE0) shr 5
                val imm4_0 = offset and 0x01F
                val code = (imm11_5 shl 25) or (rs2 shl 20) or (rs1 shl 15) or (funct3 shl 12) or (imm4_0 shl 7) or 0x23
                Instruction(address, lineNumber, originalLine, formattedText, code, InstructionFormat.S_TYPE, opcode, rs1 = rs1, rs2 = rs2, imm = offset)
            }

            // Branches: beq, bne, blt, bge, bltu, bgeu
            "beq", "bne", "blt", "bge", "bltu", "bgeu" -> {
                val rs1 = parseRegister(tokens[1])
                val rs2 = parseRegister(tokens[2])
                val target = tokens[3]
                val targetAddr = resolveSymbolOrNumber(target, symbols)
                val offset = targetAddr - address

                val funct3 = when (opcode) {
                    "beq" -> 0
                    "bne" -> 1
                    "blt" -> 4
                    "bge" -> 5
                    "bltu" -> 6
                    "bgeu" -> 7
                    else -> 0
                }
                val imm12 = (offset shr 12) and 0x1
                val imm10_5 = (offset shr 5) and 0x3F
                val imm4_1 = (offset shr 1) and 0xF
                val imm11 = (offset shr 11) and 0x1
                val code = (imm12 shl 31) or (imm10_5 shl 25) or (rs2 shl 20) or (rs1 shl 15) or (funct3 shl 12) or (imm4_1 shl 8) or (imm11 shl 7) or 0x63
                Instruction(address, lineNumber, originalLine, formattedText, code, InstructionFormat.B_TYPE, opcode, rs1 = rs1, rs2 = rs2, imm = offset, labelTarget = target)
            }

            // JAL
            "jal" -> {
                val rd = if (tokens.size >= 3) parseRegister(tokens[1]) else 1 // ra
                val target = if (tokens.size >= 3) tokens[2] else tokens[1]
                val targetAddr = resolveSymbolOrNumber(target, symbols)
                val offset = targetAddr - address

                val imm20 = (offset shr 20) and 0x1
                val imm10_1 = (offset shr 1) and 0x3FF
                val imm11 = (offset shr 11) and 0x1
                val imm19_12 = (offset shr 12) and 0xFF
                val code = (imm20 shl 31) or (imm10_1 shl 21) or (imm11 shl 20) or (imm19_12 shl 12) or (rd shl 7) or 0x6F
                Instruction(address, lineNumber, originalLine, formattedText, code, InstructionFormat.J_TYPE, opcode, rd = rd, imm = offset, labelTarget = target)
            }

            // JALR
            "jalr" -> {
                val rd = parseRegister(tokens[1])
                val rs1: Int
                val offset: Int
                if (tokens.size >= 4) {
                    rs1 = parseRegister(tokens[2])
                    offset = resolveSymbolOrNumber(tokens[3], symbols)
                } else {
                    val (off, r) = parseMemoryOffsetRegister(tokens[2], symbols)
                    rs1 = r
                    offset = off
                }
                val code = ((offset and 0xFFF) shl 20) or (rs1 shl 15) or (0 shl 12) or (rd shl 7) or 0x67
                Instruction(address, lineNumber, originalLine, formattedText, code, InstructionFormat.I_TYPE, opcode, rd = rd, rs1 = rs1, imm = offset)
            }

            // LUI
            "lui" -> {
                val rd = parseRegister(tokens[1])
                val imm = resolveSymbolOrNumber(tokens[2], symbols)
                val code = ((imm and 0xFFFFF) shl 12) or (rd shl 7) or 0x37
                Instruction(address, lineNumber, originalLine, formattedText, code, InstructionFormat.U_TYPE, opcode, rd = rd, imm = imm)
            }

            // AUIPC
            "auipc" -> {
                val rd = parseRegister(tokens[1])
                val imm = resolveSymbolOrNumber(tokens[2], symbols)
                val code = ((imm and 0xFFFFF) shl 12) or (rd shl 7) or 0x17
                Instruction(address, lineNumber, originalLine, formattedText, code, InstructionFormat.U_TYPE, opcode, rd = rd, imm = imm)
            }

            // LI (Pseudo)
            "li" -> {
                val rd = parseRegister(tokens[1])
                val imm = resolveSymbolOrNumber(tokens[2], symbols)
                val code = ((imm and 0xFFF) shl 20) or (0 shl 15) or (0 shl 12) or (rd shl 7) or 0x13
                Instruction(address, lineNumber, originalLine, formattedText, code, InstructionFormat.PSEUDO, opcode, rd = rd, imm = imm)
            }

            // LA (Pseudo)
            "la" -> {
                val rd = parseRegister(tokens[1])
                val target = tokens[2]
                val targetAddr = resolveSymbolOrNumber(target, symbols)
                val code = ((targetAddr and 0xFFF) shl 20) or (0 shl 15) or (0 shl 12) or (rd shl 7) or 0x13
                Instruction(address, lineNumber, originalLine, formattedText, code, InstructionFormat.PSEUDO, opcode, rd = rd, imm = targetAddr, labelTarget = target)
            }

            // Floating point: flw, fsw, fadd.s, fsub.s, fmul.s, fdiv.s, fmv.s
            "flw" -> {
                val rd = parseFloatRegister(tokens[1])
                val (offset, rs1) = parseMemoryOffsetRegister(tokens[2], symbols)
                val code = ((offset and 0xFFF) shl 20) or (rs1 shl 15) or (2 shl 12) or (rd shl 7) or 0x07
                Instruction(address, lineNumber, originalLine, formattedText, code, InstructionFormat.I_TYPE, opcode, rd = rd, rs1 = rs1, imm = offset)
            }
            "fsw" -> {
                val rs2 = parseFloatRegister(tokens[1])
                val (offset, rs1) = parseMemoryOffsetRegister(tokens[2], symbols)
                val imm11_5 = (offset and 0xFE0) shr 5
                val imm4_0 = offset and 0x01F
                val code = (imm11_5 shl 25) or (rs2 shl 20) or (rs1 shl 15) or (2 shl 12) or (imm4_0 shl 7) or 0x27
                Instruction(address, lineNumber, originalLine, formattedText, code, InstructionFormat.S_TYPE, opcode, rs1 = rs1, rs2 = rs2, imm = offset)
            }
            "fadd.s", "fsub.s", "fmul.s", "fdiv.s" -> {
                val rd = parseFloatRegister(tokens[1])
                val rs1 = parseFloatRegister(tokens[2])
                val rs2 = parseFloatRegister(tokens[3])
                val funct7 = when (opcode) {
                    "fadd.s" -> 0x00
                    "fsub.s" -> 0x04
                    "fmul.s" -> 0x08
                    "fdiv.s" -> 0x0C
                    else -> 0x00
                }
                val code = (funct7 shl 25) or (rs2 shl 20) or (rs1 shl 15) or (0 shl 12) or (rd shl 7) or 0x53
                Instruction(address, lineNumber, originalLine, formattedText, code, InstructionFormat.R_TYPE, opcode, rd = rd, rs1 = rs1, rs2 = rs2)
            }
            "fmv.s" -> {
                val rd = parseFloatRegister(tokens[1])
                val rs1 = parseFloatRegister(tokens[2])
                val code = (0x00 shl 25) or (0 shl 20) or (rs1 shl 15) or (0 shl 12) or (rd shl 7) or 0x53
                Instruction(address, lineNumber, originalLine, formattedText, code, InstructionFormat.R_TYPE, opcode, rd = rd, rs1 = rs1)
            }

            // System calls & Breaks
            "ecall" -> Instruction(address, lineNumber, originalLine, formattedText, 0x00000073, InstructionFormat.SYSTEM, "ecall")
            "ebreak" -> Instruction(address, lineNumber, originalLine, formattedText, 0x00100073, InstructionFormat.SYSTEM, "ebreak")

            else -> throw IllegalArgumentException("Unsupported or unknown opcode: '$opcode'")
        }
    }
}
