package com.example.riscv.simulator

import com.example.riscv.model.*
import java.util.ArrayDeque
import kotlin.random.Random

enum class ProgramStatus {
    READY,
    RUNNING,
    PAUSED,
    HALTED,
    ERROR,
    WAITING_INPUT_INT,
    WAITING_INPUT_FLOAT,
    WAITING_INPUT_STRING
}

data class ExecutionStateSnapshot(
    val pc: Int,
    val registers: IntArray,
    val floatRegisters: IntArray,
    val memorySnapshot: Memory,
    val consoleLog: String,
    val stepCount: Long,
    val cycleCount: Long
)

class RiscvSimulator {

    var pc: Int = Memory.TEXT_BASE
        private set

    val registers = Register.createDefaultRegisters()
    val floatRegisters = FloatRegister.createDefaultFloatRegisters()
    var memory = Memory()
        private set

    var status: ProgramStatus = ProgramStatus.READY
        private set

    var stepCount: Long = 0
        private set

    var cycleCount: Long = 0
        private set

    var lastModifiedRegister: Int? = null
        private set

    var lastModifiedFloatRegister: Int? = null
        private set

    val consoleLog = StringBuilder()
    var errorMessage: String? = null
        private set

    private val historyStack = ArrayDeque<ExecutionStateSnapshot>()
    private var instructionsByAddress = mapOf<Int, Instruction>()
    private var heapPointer = Memory.HEAP_BASE
    private var entryPoint = Memory.TEXT_BASE

    fun loadProgram(assemblyResult: AssemblyResult) {
        memory = assemblyResult.initialMemory.copy()
        instructionsByAddress = assemblyResult.instructions.associateBy { it.address }
        val symbols = assemblyResult.symbols
        val entrySymbol = symbols["main"] ?: symbols["start"] ?: symbols["_start"] ?: symbols["__start"]
        entryPoint = entrySymbol?.address 
            ?: assemblyResult.instructions.firstOrNull()?.address 
            ?: Memory.TEXT_BASE
        reset()
    }

    fun reset() {
        pc = entryPoint
        for (i in 0..31) {
            registers[i].value = if (i == 2) Memory.STACK_BASE else 0
            floatRegisters[i].rawBits = 0
        }
        status = if (instructionsByAddress.isNotEmpty()) ProgramStatus.READY else ProgramStatus.HALTED
        stepCount = 0
        cycleCount = 0
        lastModifiedRegister = null
        lastModifiedFloatRegister = null
        consoleLog.clear()
        errorMessage = null
        heapPointer = Memory.HEAP_BASE
        historyStack.clear()
    }

    fun canStepBack(): Boolean = historyStack.isNotEmpty()

    fun stepBack(): Boolean {
        if (historyStack.isEmpty()) return false
        val prev = historyStack.pop()
        pc = prev.pc
        for (i in 0..31) {
            registers[i].value = prev.registers[i]
            floatRegisters[i].rawBits = prev.floatRegisters[i]
        }
        memory = prev.memorySnapshot.copy()
        consoleLog.clear().append(prev.consoleLog)
        stepCount = prev.stepCount
        cycleCount = prev.cycleCount
        status = ProgramStatus.PAUSED
        lastModifiedRegister = null
        lastModifiedFloatRegister = null
        return true
    }

    private fun pushHistory() {
        if (historyStack.size >= 100) {
            historyStack.removeLast()
        }
        val regVals = IntArray(32) { registers[it].value }
        val fRegVals = IntArray(32) { floatRegisters[it].rawBits }
        historyStack.push(
            ExecutionStateSnapshot(
                pc = pc,
                registers = regVals,
                floatRegisters = fRegVals,
                memorySnapshot = memory.copy(),
                consoleLog = consoleLog.toString(),
                stepCount = stepCount,
                cycleCount = cycleCount
            )
        )
    }

    fun setRegisterValue(id: Int, value: Int) {
        if (id in 1..31) {
            registers[id].value = value
            lastModifiedRegister = id
        }
    }

    fun setFloatRegisterBits(id: Int, rawBits: Int) {
        if (id in 0..31) {
            floatRegisters[id].rawBits = rawBits
            lastModifiedFloatRegister = id
        }
    }

    fun provideUserInputInt(value: Int) {
        if (status == ProgramStatus.WAITING_INPUT_INT) {
            registers[10].value = value // a0 = x10
            status = ProgramStatus.RUNNING
        }
    }

    fun provideUserInputFloat(value: Float) {
        if (status == ProgramStatus.WAITING_INPUT_FLOAT) {
            floatRegisters[10].rawBits = value.toBits() // fa0 = f10
            status = ProgramStatus.RUNNING
        }
    }

    fun provideUserInputString(str: String) {
        if (status == ProgramStatus.WAITING_INPUT_STRING) {
            val bufAddr = registers[10].value // a0
            val maxLen = registers[11].value // a1
            val truncated = if (maxLen > 1 && str.length >= maxLen) str.substring(0, maxLen - 1) else str
            memory.writeString(bufAddr, truncated, nullTerminate = true)
            status = ProgramStatus.RUNNING
        }
    }

    fun executeNextStep(breakpoints: Set<Int> = emptySet()): Boolean {
        if (status == ProgramStatus.HALTED || status == ProgramStatus.ERROR || status == ProgramStatus.WAITING_INPUT_INT || status == ProgramStatus.WAITING_INPUT_FLOAT || status == ProgramStatus.WAITING_INPUT_STRING) {
            return false
        }

        val inst = instructionsByAddress[pc]
        if (inst == null) {
            status = ProgramStatus.HALTED
            consoleLog.append("\n[Program terminated: Reached end of text segment at 0x${String.format("%08X", pc)}]\n")
            return false
        }

        // Check breakpoint
        if (breakpoints.contains(inst.lineNumber) && stepCount > 0 && status == ProgramStatus.RUNNING) {
            status = ProgramStatus.PAUSED
            consoleLog.append("[Breakpoint hit at line ${inst.lineNumber} (0x${inst.hexAddress})]\n")
            return false
        }

        pushHistory()
        lastModifiedRegister = null
        lastModifiedFloatRegister = null
        val currentPc = pc
        var nextPc = pc + 4

        try {
            executeInstruction(inst) { targetPc ->
                nextPc = targetPc
            }
            pc = nextPc
            stepCount++
            cycleCount++

            // Keep x0 strictly 0
            registers[0].value = 0

            return true
        } catch (e: Exception) {
            status = ProgramStatus.ERROR
            errorMessage = "Execution error at 0x${inst.hexAddress} (Line ${inst.lineNumber}): ${e.message}"
            consoleLog.append("\n[EXECUTION ERROR]: ${e.message}\n")
            return false
        }
    }

    private fun executeInstruction(inst: Instruction, setNextPc: (Int) -> Unit) {
        val rd = inst.rd
        val rs1 = inst.rs1
        val rs2 = inst.rs2
        val imm = inst.imm

        fun getReg(idx: Int) = registers[idx].value
        fun setReg(idx: Int, valInt: Int) {
            if (idx != 0) {
                registers[idx].value = valInt
                lastModifiedRegister = idx
            }
        }

        fun getFReg(idx: Int) = Float.fromBits(floatRegisters[idx].rawBits)
        fun setFReg(idx: Int, valFloat: Float) {
            floatRegisters[idx].rawBits = valFloat.toBits()
            lastModifiedFloatRegister = idx
        }

        when (inst.opcode) {
            // Arithmetic & Logical R-Type
            "add" -> setReg(rd, getReg(rs1) + getReg(rs2))
            "sub" -> setReg(rd, getReg(rs1) - getReg(rs2))
            "sll" -> setReg(rd, getReg(rs1) shl (getReg(rs2) and 0x1F))
            "slt" -> setReg(rd, if (getReg(rs1) < getReg(rs2)) 1 else 0)
            "sltu" -> setReg(rd, if ((getReg(rs1).toLong() and 0xFFFFFFFFL) < (getReg(rs2).toLong() and 0xFFFFFFFFL)) 1 else 0)
            "xor" -> setReg(rd, getReg(rs1) xor getReg(rs2))
            "srl" -> setReg(rd, getReg(rs1) ushr (getReg(rs2) and 0x1F))
            "sra" -> setReg(rd, getReg(rs1) shr (getReg(rs2) and 0x1F))
            "or" -> setReg(rd, getReg(rs1) or getReg(rs2))
            "and" -> setReg(rd, getReg(rs1) and getReg(rs2))
            "mul" -> setReg(rd, getReg(rs1) * getReg(rs2))
            "mulh" -> setReg(rd, ((getReg(rs1).toLong() * getReg(rs2).toLong()) shr 32).toInt())
            "mulhu" -> setReg(rd, (((getReg(rs1).toLong() and 0xFFFFFFFFL) * (getReg(rs2).toLong() and 0xFFFFFFFFL)) ushr 32).toInt())
            "div" -> {
                val n = getReg(rs1)
                val d = getReg(rs2)
                val res = when {
                    d == 0 -> -1
                    n == Int.MIN_VALUE && d == -1 -> Int.MIN_VALUE
                    else -> n / d
                }
                setReg(rd, res)
            }
            "divu" -> {
                val d = (getReg(rs2).toLong() and 0xFFFFFFFFL)
                setReg(rd, if (d != 0L) ((getReg(rs1).toLong() and 0xFFFFFFFFL) / d).toInt() else -1)
            }
            "rem" -> {
                val n = getReg(rs1)
                val d = getReg(rs2)
                val res = when {
                    d == 0 -> n
                    n == Int.MIN_VALUE && d == -1 -> 0
                    else -> n % d
                }
                setReg(rd, res)
            }
            "remu" -> {
                val d = (getReg(rs2).toLong() and 0xFFFFFFFFL)
                setReg(rd, if (d != 0L) ((getReg(rs1).toLong() and 0xFFFFFFFFL) % d).toInt() else getReg(rs1))
            }

            // Immediate Arith
            "addi" -> setReg(rd, getReg(rs1) + imm)
            "slti" -> setReg(rd, if (getReg(rs1) < imm) 1 else 0)
            "sltiu" -> setReg(rd, if ((getReg(rs1).toLong() and 0xFFFFFFFFL) < (imm.toLong() and 0xFFFFFFFFL)) 1 else 0)
            "xori" -> setReg(rd, getReg(rs1) xor imm)
            "ori" -> setReg(rd, getReg(rs1) or imm)
            "andi" -> setReg(rd, getReg(rs1) and imm)
            "slli" -> setReg(rd, getReg(rs1) shl (imm and 0x1F))
            "srli" -> setReg(rd, getReg(rs1) ushr (imm and 0x1F))
            "srai" -> setReg(rd, getReg(rs1) shr (imm and 0x1F))

            // Memory Loads
            "lb" -> setReg(rd, memory.readByte(getReg(rs1) + imm).toInt())
            "lbu" -> setReg(rd, memory.readByte(getReg(rs1) + imm).toInt() and 0xFF)
            "lh" -> setReg(rd, memory.readHalfword(getReg(rs1) + imm).toInt())
            "lhu" -> setReg(rd, memory.readHalfword(getReg(rs1) + imm).toInt() and 0xFFFF)
            "lw" -> setReg(rd, memory.readWord(getReg(rs1) + imm))

            // Memory Stores
            "sb" -> memory.writeByte(getReg(rs1) + imm, getReg(rs2).toByte())
            "sh" -> memory.writeHalfword(getReg(rs1) + imm, getReg(rs2).toShort())
            "sw" -> memory.writeWord(getReg(rs1) + imm, getReg(rs2))

            // Control flow
            "beq" -> if (getReg(rs1) == getReg(rs2)) setNextPc(pc + imm)
            "bne" -> if (getReg(rs1) != getReg(rs2)) setNextPc(pc + imm)
            "blt" -> if (getReg(rs1) < getReg(rs2)) setNextPc(pc + imm)
            "bge" -> if (getReg(rs1) >= getReg(rs2)) setNextPc(pc + imm)
            "bltu" -> if ((getReg(rs1).toLong() and 0xFFFFFFFFL) < (getReg(rs2).toLong() and 0xFFFFFFFFL)) setNextPc(pc + imm)
            "bgeu" -> if ((getReg(rs1).toLong() and 0xFFFFFFFFL) >= (getReg(rs2).toLong() and 0xFFFFFFFFL)) setNextPc(pc + imm)

            "jal" -> {
                setReg(rd, pc + 4)
                setNextPc(pc + imm)
            }
            "jalr" -> {
                setReg(rd, pc + 4)
                setNextPc((getReg(rs1) + imm) and -2)
            }
            "lui" -> setReg(rd, imm shl 12)
            "auipc" -> setReg(rd, pc + (imm shl 12))

            // Pseudos
            "li" -> setReg(rd, imm)
            "la" -> setReg(rd, imm)

            // Float
            "flw" -> setFReg(rd, Float.fromBits(memory.readWord(getReg(rs1) + imm)))
            "fsw" -> memory.writeWord(getReg(rs1) + imm, floatRegisters[rs2].rawBits)
            "fadd.s" -> setFReg(rd, getFReg(rs1) + getFReg(rs2))
            "fsub.s" -> setFReg(rd, getFReg(rs1) - getFReg(rs2))
            "fmul.s" -> setFReg(rd, getFReg(rs1) * getFReg(rs2))
            "fdiv.s" -> setFReg(rd, getFReg(rs1) / getFReg(rs2))
            "fmv.s" -> setFReg(rd, getFReg(rs1))

            // System calls
            "ecall" -> handleEcall()
            "ebreak" -> {
                status = ProgramStatus.PAUSED
                consoleLog.append("[ebreak instruction encountered at 0x${String.format("%08X", pc)}]\n")
            }
        }
    }

    private fun handleEcall() {
        var syscallNum = registers[17].value // a7
        if (syscallNum == 0) {
            syscallNum = registers[10].value // fallback to a0 if a7 is 0 (RARS/MARS legacy support)
        }
        when (syscallNum) {
            1 -> { // Print Int (a0)
                consoleLog.append(registers[10].value.toString())
            }
            2 -> { // Print Float (fa0)
                consoleLog.append(Float.fromBits(floatRegisters[10].rawBits).toString())
            }
            4 -> { // Print String (address in a0)
                val str = memory.readString(registers[10].value)
                consoleLog.append(str)
            }
            5 -> { // Read Int
                status = ProgramStatus.WAITING_INPUT_INT
            }
            6 -> { // Read Float
                status = ProgramStatus.WAITING_INPUT_FLOAT
            }
            8 -> { // Read String
                status = ProgramStatus.WAITING_INPUT_STRING
            }
            9 -> { // sbrk (a0 = amount)
                val bytes = registers[10].value
                registers[10].value = heapPointer
                heapPointer += bytes
            }
            10, 93 -> { // Exit / Exit0
                status = ProgramStatus.HALTED
                consoleLog.append("\n[Program finished with exit code 0]\n")
            }
            57 -> { // Exit2 (a0 = exit code)
                val code = registers[10].value
                status = ProgramStatus.HALTED
                consoleLog.append("\n[Program finished with exit code $code]\n")
            }
            11 -> { // Print Char
                consoleLog.append((registers[10].value and 0xFF).toChar())
            }
            12 -> { // Read Char (returns char in a0)
                status = ProgramStatus.WAITING_INPUT_INT
            }
            34 -> { // Print Hex
                consoleLog.append(String.format("0x%08X", registers[10].value))
            }
            35 -> { // Print Binary
                consoleLog.append(Integer.toBinaryString(registers[10].value))
            }
            36 -> { // Print Unsigned Int
                consoleLog.append((registers[10].value.toLong() and 0xFFFFFFFFL).toString())
            }
            41 -> { // Rand Int
                registers[10].value = Random.nextInt()
            }
            42 -> { // Rand Int Range (a0 = upper bound)
                val bound = registers[10].value
                registers[10].value = if (bound > 0) Random.nextInt(bound) else 0
            }
            30 -> { // Time
                val ms = System.currentTimeMillis()
                registers[10].value = ms.toInt()
                registers[11].value = (ms ushr 32).toInt()
            }
            else -> {
                consoleLog.append("[Unknown ecall service code: $syscallNum]\n")
            }
        }
    }
}
