package com.example.riscv.model

enum class InstructionFormat {
    R_TYPE, I_TYPE, S_TYPE, B_TYPE, U_TYPE, J_TYPE, SYSTEM, PSEUDO, UNKNOWN
}

data class Instruction(
    val address: Int,             // Memory address (e.g. 0x00400000)
    val lineNumber: Int,          // Source code line number (1-indexed)
    val originalSource: String,   // Original assembly text
    val formattedText: String,    // Normalized instruction text (e.g. addi x10, x0, 5)
    val machineCode: Int,         // 32-bit machine code
    val format: InstructionFormat,
    val opcode: String,           // e.g., "add", "lw", "beq", "ecall"
    val rd: Int = 0,
    val rs1: Int = 0,
    val rs2: Int = 0,
    val imm: Int = 0,
    val labelTarget: String? = null
) {
    val hexAddress: String
        get() = String.format("0x%08X", address)

    val hexMachineCode: String
        get() = String.format("0x%08X", machineCode)
}
