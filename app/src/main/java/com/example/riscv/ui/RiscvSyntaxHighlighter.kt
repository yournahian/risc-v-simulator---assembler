package com.example.riscv.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class RiscvSyntaxVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            highlightRiscvCode(text.text),
            OffsetMapping.Identity
        )
    }
}

private val COMMENT_COLOR = Color(0xFF8B949E)
private val DIRECTIVE_COLOR = Color(0xFFD0BCFF)
private val INSTRUCTION_COLOR = Color(0xFF79C0FF)
private val REGISTER_COLOR = Color(0xFFFFA657)
private val LABEL_COLOR = Color(0xFFE5C07B)
private val NUMBER_COLOR = Color(0xFFA5D6A7)
private val STRING_COLOR = Color(0xFFA5D6A7)

private val REGISTERS_REGEX = Regex(
    "\\b(x[0-9]|x[12][0-9]|x3[01]|zero|ra|sp|gp|tp|t[0-6]|s[0-9]|s10|s11|fp|a[0-7]|f[0-9]|f[12][0-9]|f3[01]|ft[0-9]|ft10|ft11|fs[0-9]|fs10|fs11|fa[0-7])\\b",
    RegexOption.IGNORE_CASE
)

private val INSTRUCTIONS_REGEX = Regex(
    "\\b(add|addi|sub|lui|auipc|jal|jalr|beq|bne|blt|bge|bltu|bgeu|lb|lh|lw|lbu|lhu|sb|sh|sw|slt|slti|sltiu|sltu|xori|ori|andi|slli|srli|srai|xor|or|and|sll|srl|sra|ecall|ebreak|nop|mv|li|la|j|jr|ret|call|bnez|beqz|bgtz|bltz|bgez|blez|bgt|ble|bgtu|bleu|seqz|snez|sltz|sgtz|flw|fsw|fadd\\.s|fsub\\.s|fmul\\.s|fdiv\\.s|fsqrt\\.s|fmadd\\.s|fmsub\\.s|feq\\.s|flt\\.s|fle\\.s|fcvt\\.w\\.s|fcvt\\.s\\.w)\\b",
    RegexOption.IGNORE_CASE
)

private val DIRECTIVE_REGEX = Regex("\\.[a-zA-Z0-9_]+")
private val LABEL_REGEX = Regex("\\b[a-zA-Z_][a-zA-Z0-9_]*:")
private val NUMBER_REGEX = Regex("\\b0x[0-9a-fA-F]+\\b|\\b-?[0-9]+\\b")
private val STRING_REGEX = Regex("\"[^\"]*\"")

fun highlightRiscvCode(code: String): AnnotatedString {
    val builder = AnnotatedString.Builder(code)
    var lineStart = 0

    val lines = code.split("\n")
    for (line in lines) {
        val lineEnd = lineStart + line.length

        // Check for comments (# or //)
        var commentIndex = -1
        var inQuotes = false
        for (i in line.indices) {
            val char = line[i]
            if (char == '"') {
                inQuotes = !inQuotes
            } else if (!inQuotes) {
                if (char == '#') {
                    commentIndex = i
                    break
                } else if (char == '/' && i + 1 < line.length && line[i + 1] == '/') {
                    commentIndex = i
                    break
                }
            }
        }

        val codeLength = if (commentIndex != -1) commentIndex else line.length
        val codePart = line.substring(0, codeLength)

        // Highlight Directive
        DIRECTIVE_REGEX.findAll(codePart).forEach { match ->
            builder.addStyle(
                SpanStyle(color = DIRECTIVE_COLOR, fontWeight = FontWeight.SemiBold),
                lineStart + match.range.first,
                lineStart + match.range.last + 1
            )
        }

        // Highlight Label
        LABEL_REGEX.findAll(codePart).forEach { match ->
            builder.addStyle(
                SpanStyle(color = LABEL_COLOR, fontWeight = FontWeight.Bold),
                lineStart + match.range.first,
                lineStart + match.range.last + 1
            )
        }

        // Highlight Instructions
        INSTRUCTIONS_REGEX.findAll(codePart).forEach { match ->
            builder.addStyle(
                SpanStyle(color = INSTRUCTION_COLOR, fontWeight = FontWeight.Medium),
                lineStart + match.range.first,
                lineStart + match.range.last + 1
            )
        }

        // Highlight Registers
        REGISTERS_REGEX.findAll(codePart).forEach { match ->
            builder.addStyle(
                SpanStyle(color = REGISTER_COLOR, fontWeight = FontWeight.Medium),
                lineStart + match.range.first,
                lineStart + match.range.last + 1
            )
        }

        // Highlight Numbers
        NUMBER_REGEX.findAll(codePart).forEach { match ->
            builder.addStyle(
                SpanStyle(color = NUMBER_COLOR),
                lineStart + match.range.first,
                lineStart + match.range.last + 1
            )
        }

        // Highlight Strings
        STRING_REGEX.findAll(codePart).forEach { match ->
            builder.addStyle(
                SpanStyle(color = STRING_COLOR),
                lineStart + match.range.first,
                lineStart + match.range.last + 1
            )
        }

        // Highlight Comment if present
        if (commentIndex != -1) {
            builder.addStyle(
                SpanStyle(color = COMMENT_COLOR, fontStyle = FontStyle.Italic),
                lineStart + commentIndex,
                lineEnd
            )
        }

        lineStart = lineEnd + 1 // +1 for the '\n'
    }

    return builder.toAnnotatedString()
}
