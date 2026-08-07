package com.example.riscv.model

data class Symbol(
    val name: String,
    val address: Int,
    val isData: Boolean = false,
    val lineNumber: Int = -1
)

data class AssemblyError(
    val lineNumber: Int,
    val lineText: String,
    val message: String
)

data class AssemblyResult(
    val isSuccess: Boolean,
    val instructions: List<Instruction>,
    val symbols: Map<String, Symbol>,
    val initialMemory: Memory,
    val errors: List<AssemblyError>,
    val sourceLineToAddressMap: Map<Int, Int> // Line number -> instruction address
)
