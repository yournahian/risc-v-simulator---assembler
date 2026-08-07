package com.example.riscv.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.riscv.model.FloatRegister
import com.example.riscv.model.Register

object SyntaxTheme {
    val DirectiveColor = Color(0xFFEFB8C8) // Soft Rose
    val InstructionColor = Color(0xFFD0BCFF) // Soft Lavender
    val RegisterColor = Color(0xFFD0BCFF) // Lavender
    val FloatRegisterColor = Color(0xFFEFB8C8) // Rose
    val LabelColor = Color(0xFFEFB8C8) // Soft Rose
    val CommentColor = Color(0xFF938F99) // Muted Purple-Gray
    val StringColor = Color(0xFFA8C7FA) // Muted Soft Blue
    val NumberColor = Color(0xFFCCC2DC) // Soft Violet-Gray
    val DefaultTextColor = Color(0xFFE6E1E5) // Off-white
}

object SyntaxHighlighter {

    private val DIRECTIVES = setOf(
        ".text", ".data", ".globl", ".global", ".word", ".half", ".byte",
        ".space", ".asciiz", ".ascii", ".align", ".section", ".type"
    )

    private val OPCODES = setOf(
        "add", "sub", "sll", "slt", "sltu", "xor", "srl", "sra", "or", "and", "mul", "div", "rem",
        "addi", "slti", "sltiu", "xori", "ori", "andi", "slli", "srli", "srai",
        "lb", "lh", "lw", "lbu", "lhu", "sb", "sh", "sw",
        "beq", "bne", "blt", "bge", "bltu", "bgeu",
        "jal", "jalr", "lui", "auipc", "ecall", "ebreak",
        "li", "la", "mv", "nop", "not", "neg", "j", "jr", "ret", "call",
        "beqz", "bnez", "blez", "bgtz", "bltz", "bgez",
        "flw", "fsw", "fadd.s", "fsub.s", "fmul.s", "fdiv.s", "fmv.s"
    )

    fun highlight(code: String): AnnotatedString {
        return buildAnnotatedString {
            append(code)
            val text = code

            // Highlight Comments (# ...)
            var idx = 0
            while (idx < text.length) {
                val lineStart = idx
                var lineEnd = text.indexOf('\n', lineStart)
                if (lineEnd == -1) lineEnd = text.length

                val commentIdx = text.indexOf('#', lineStart)
                if (commentIdx in lineStart until lineEnd) {
                    addStyle(
                        SpanStyle(color = SyntaxTheme.CommentColor, fontWeight = FontWeight.Normal),
                        commentIdx,
                        lineEnd
                    )
                }

                idx = lineEnd + 1
            }

            // Highlight Strings ("...")
            var stringStart = text.indexOf('"')
            while (stringStart != -1) {
                val stringEnd = text.indexOf('"', stringStart + 1)
                if (stringEnd != -1) {
                    addStyle(
                        SpanStyle(color = SyntaxTheme.StringColor),
                        stringStart,
                        stringEnd + 1
                    )
                    stringStart = text.indexOf('"', stringEnd + 1)
                } else {
                    break
                }
            }

            // Tokenize for directives, opcodes, registers, numbers, labels
            val tokenRegex = Regex("\\.[a-zA-Z0-9_]+|[a-zA-Z0-9_.]+:?|-?0x[0-9a-fA-F]+|-?[0-9]+")
            for (match in tokenRegex.findAll(text)) {
                val token = match.value
                val start = match.range.first
                val end = match.range.last + 1

                // Skip if inside comment or string
                val isInsideComment = isIndexInComment(text, start)
                val isInsideString = isIndexInString(text, start)
                if (isInsideComment || isInsideString) continue

                val lowerToken = token.lowercase()

                when {
                    token.endsWith(":") -> {
                        addStyle(SpanStyle(color = SyntaxTheme.LabelColor, fontWeight = FontWeight.Bold), start, end)
                    }
                    DIRECTIVES.contains(lowerToken) -> {
                        addStyle(SpanStyle(color = SyntaxTheme.DirectiveColor, fontWeight = FontWeight.Bold), start, end)
                    }
                    OPCODES.contains(lowerToken) -> {
                        addStyle(SpanStyle(color = SyntaxTheme.InstructionColor, fontWeight = FontWeight.Bold), start, end)
                    }
                    Register.parseRegisterName(lowerToken) != null -> {
                        addStyle(SpanStyle(color = SyntaxTheme.RegisterColor, fontWeight = FontWeight.SemiBold), start, end)
                    }
                    FloatRegister.parseFloatRegisterName(lowerToken) != null -> {
                        addStyle(SpanStyle(color = SyntaxTheme.FloatRegisterColor, fontWeight = FontWeight.SemiBold), start, end)
                    }
                    lowerToken.startsWith("0x") || lowerToken.toIntOrNull() != null -> {
                        addStyle(SpanStyle(color = SyntaxTheme.NumberColor), start, end)
                    }
                }
            }
        }
    }

    private fun isIndexInComment(text: String, index: Int): Boolean {
        val lastNewline = text.lastIndexOf('\n', index)
        val lineStart = if (lastNewline == -1) 0 else lastNewline + 1
        val commentPos = text.indexOf('#', lineStart)
        return commentPos != -1 && commentPos <= index
    }

    private fun isIndexInString(text: String, index: Int): Boolean {
        var count = 0
        for (i in 0 until index) {
            if (text[i] == '"' && (i == 0 || text[i - 1] != '\\')) {
                count++
            }
        }
        return count % 2 == 1
    }
}
