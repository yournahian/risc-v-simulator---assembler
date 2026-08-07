package com.example.riscv.model

data class Register(
    val id: Int,             // 0..31
    val name: String,         // x0..x31
    val abiName: String,      // zero, ra, sp, gp, tp, t0-t6, s0-s11, a0-a7
    val description: String,  // Usage description
    var value: Int = 0,       // 32-bit integer value
    val isModified: Boolean = false
) {
    val hexValue: String
        get() = String.format("0x%08X", value)

    val unsignedValue: Long
        get() = value.toLong() and 0xFFFFFFFFL

    val asciiChar: String
        get() = if (value in 32..126) "${value.toChar()}" else "."

    companion object {
        val ABI_NAMES = arrayOf(
            "zero", "ra", "sp", "gp", "tp", "t0", "t1", "t2",
            "s0/fp", "s1", "a0", "a1", "a2", "a3", "a4", "a5",
            "a6", "a7", "s2", "s3", "s4", "s5", "s6", "s7",
            "s8", "s9", "s10", "s11", "t3", "t4", "t5", "t6"
        )

        val DESCRIPTIONS = arrayOf(
            "Hard-wired zero",
            "Return address",
            "Stack pointer",
            "Global pointer",
            "Thread pointer",
            "Temporary 0",
            "Temporary 1",
            "Temporary 2",
            "Saved register 0 / Frame pointer",
            "Saved register 1",
            "Function argument 0 / Return value 0",
            "Function argument 1 / Return value 1",
            "Function argument 2",
            "Function argument 3",
            "Function argument 4",
            "Function argument 5",
            "Function argument 6",
            "Function argument 7 / Environment Call ID",
            "Saved register 2",
            "Saved register 3",
            "Saved register 4",
            "Saved register 5",
            "Saved register 6",
            "Saved register 7",
            "Saved register 8",
            "Saved register 9",
            "Saved register 10",
            "Saved register 11",
            "Temporary 3",
            "Temporary 4",
            "Temporary 5",
            "Temporary 6"
        )

        fun createDefaultRegisters(): Array<Register> {
            return Array(32) { i ->
                Register(
                    id = i,
                    name = "x$i",
                    abiName = ABI_NAMES[i],
                    description = DESCRIPTIONS[i],
                    value = if (i == 2) 0x7FFFFFF0 else 0 // sp starts near top of stack
                )
            }
        }

        fun parseRegisterName(str: String): Int? {
            val s = str.lowercase().trim()
            if (s.startsWith("x")) {
                val num = s.substring(1).toIntOrNull()
                if (num != null && num in 0..31) return num
            }
            val rawNum = s.toIntOrNull()
            if (rawNum != null && rawNum in 0..31) return rawNum

            val index = ABI_NAMES.indexOfFirst { abi ->
                abi.equals(s, ignoreCase = true) || abi.split("/").contains(s)
            }
            return if (index >= 0) index else null
        }
    }
}

data class FloatRegister(
    val id: Int,             // 0..31
    val name: String,         // f0..f31
    val abiName: String,      // ft0-ft7, fs0-fs1, fa0-fa7, fs2-fs11, ft8-ft11
    var rawBits: Int = 0,     // Bit pattern
    val isModified: Boolean = false
) {
    val floatValue: Float
        get() = Float.fromBits(rawBits)

    val hexValue: String
        get() = String.format("0x%08X", rawBits)

    companion object {
        val ABI_NAMES = arrayOf(
            "ft0", "ft1", "ft2", "ft3", "ft4", "ft5", "ft6", "ft7",
            "fs0", "fs1", "fa0", "fa1", "fa2", "fa3", "fa4", "fa5",
            "fa6", "fa7", "fs2", "fs3", "fs4", "fs5", "fs6", "fs7",
            "fs8", "fs9", "fs10", "fs11", "ft8", "ft9", "ft10", "ft11"
        )

        fun createDefaultFloatRegisters(): Array<FloatRegister> {
            return Array(32) { i ->
                FloatRegister(
                    id = i,
                    name = "f$i",
                    abiName = ABI_NAMES[i],
                    rawBits = 0
                )
            }
        }

        fun parseFloatRegisterName(str: String): Int? {
            val s = str.lowercase().trim()
            if (s.startsWith("f")) {
                val num = s.substring(1).toIntOrNull()
                if (num != null && num in 0..31) return num
            }
            val index = ABI_NAMES.indexOfFirst { it.equals(s, ignoreCase = true) }
            return if (index >= 0) index else null
        }
    }
}
