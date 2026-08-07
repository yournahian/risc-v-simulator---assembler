package com.example.riscv.model

class Memory {
    // Sparse byte storage: Address -> Byte
    private val memoryMap = HashMap<Int, Byte>()

    fun clear() {
        memoryMap.clear()
    }

    fun readByte(address: Int): Byte {
        return memoryMap[address] ?: 0
    }

    fun writeByte(address: Int, value: Byte) {
        if (value == 0.toByte()) {
            memoryMap.remove(address)
        } else {
            memoryMap[address] = value
        }
    }

    fun readHalfword(address: Int): Short {
        val b0 = readByte(address).toInt() and 0xFF
        val b1 = readByte(address + 1).toInt() and 0xFF
        return ((b1 shl 8) or b0).toShort()
    }

    fun writeHalfword(address: Int, value: Short) {
        writeByte(address, (value.toInt() and 0xFF).toByte())
        writeByte(address + 1, ((value.toInt() shr 8) and 0xFF).toByte())
    }

    fun readWord(address: Int): Int {
        val b0 = readByte(address).toInt() and 0xFF
        val b1 = readByte(address + 1).toInt() and 0xFF
        val b2 = readByte(address + 2).toInt() and 0xFF
        val b3 = readByte(address + 3).toInt() and 0xFF
        return (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0
    }

    fun writeWord(address: Int, value: Int) {
        writeByte(address, (value and 0xFF).toByte())
        writeByte(address + 1, ((value shr 8) and 0xFF).toByte())
        writeByte(address + 2, ((value shr 16) and 0xFF).toByte())
        writeByte(address + 3, ((value shr 24) and 0xFF).toByte())
    }

    fun readString(address: Int, maxLen: Int = 1000): String {
        val sb = StringBuilder()
        var curr = address
        var count = 0
        while (count < maxLen) {
            val b = readByte(curr)
            if (b == 0.toByte()) break
            sb.append((b.toInt() and 0xFF).toChar())
            curr++
            count++
        }
        return sb.toString()
    }

    fun writeString(address: Int, str: String, nullTerminate: Boolean = true): Int {
        var curr = address
        for (ch in str) {
            writeByte(curr++, ch.code.toByte())
        }
        if (nullTerminate) {
            writeByte(curr++, 0)
        }
        return curr - address
    }

    fun copy(): Memory {
        val copyMem = Memory()
        copyMem.memoryMap.putAll(this.memoryMap)
        return copyMem
    }

    fun getMemoryEntriesSorted(startAddress: Int, numWords: Int): List<Pair<Int, Int>> {
        val list = ArrayList<Pair<Int, Int>>()
        for (i in 0 until numWords) {
            val addr = startAddress + i * 4
            list.add(Pair(addr, readWord(addr)))
        }
        return list
    }

    companion object {
        const val TEXT_BASE = 0x00400000
        const val DATA_BASE = 0x10010000
        const val HEAP_BASE = 0x10040000
        const val STACK_BASE = 0x7FFFFFF0
    }
}
