package com.example

import com.example.riscv.assembler.RiscvAssembler
import com.example.riscv.simulator.ProgramStatus
import com.example.riscv.simulator.RiscvSimulator
import org.junit.Assert.*
import org.junit.Test

class RiscvAssemblerTest {

    @Test
    fun testAssembleAndExecuteArithmetic() {
        val code = """
            .text
            .globl main
            main:
                li t0, 15
                li t1, 27
                add t2, t0, t1
                li a7, 10
                ecall
        """.trimIndent()

        val assembler = RiscvAssembler()
        val result = assembler.assemble(code)

        assertTrue(result.isSuccess)
        assertEquals(4, result.instructions.size)

        val simulator = RiscvSimulator()
        simulator.loadProgram(result)

        // Step 1: li t0, 15
        simulator.executeNextStep()
        assertEquals(15, simulator.registers[5].value) // t0 is x5

        // Step 2: li t1, 27
        simulator.executeNextStep()
        assertEquals(27, simulator.registers[6].value) // t1 is x6

        // Step 3: add t2, t0, t1
        simulator.executeNextStep()
        assertEquals(42, simulator.registers[7].value) // t2 is x7

        // Step 4: ecall exit
        simulator.executeNextStep()
        assertEquals(ProgramStatus.HALTED, simulator.status)
    }

    @Test
    fun testStepBackUndo() {
        val code = """
            .text
            main:
                li t0, 100
                addi t0, t0, 50
        """.trimIndent()

        val assembler = RiscvAssembler()
        val result = assembler.assemble(code)
        val simulator = RiscvSimulator()
        simulator.loadProgram(result)

        simulator.executeNextStep() // t0 = 100
        assertEquals(100, simulator.registers[5].value)

        simulator.executeNextStep() // t0 = 150
        assertEquals(150, simulator.registers[5].value)

        assertTrue(simulator.canStepBack())
        simulator.stepBack() // Undo addi
        assertEquals(100, simulator.registers[5].value)

        simulator.stepBack() // Undo li
        assertEquals(0, simulator.registers[5].value)
    }

    @Test
    fun testUndefinedSymbolReturnsError() {
        val code = """
            .data
            num1: .word 10, 20, 30, 40
            num2: .word 4
            .text
            .globl start
            start:
                la a1, num1
                lw a2, num2
            my_loop:
                lw a0, 0(a1)
                li a7, 1
                ecall
                li a0, 10
                li a7, 11
                ecall
                addi a1, a1, 4
                addi a2, a2, -1
                bnez a2, my_loopl
                li a7, 93
                li a0, 0
                ecall
        """.trimIndent()

        val assembler = RiscvAssembler()
        val result = assembler.assemble(code)

        assertFalse(result.isSuccess)
        assertTrue(result.errors.isNotEmpty())
        assertTrue(result.errors.any { it.message.contains("Symbol 'my_loopl' not found") })
    }

    @Test
    fun testStringColonParsing() {
        val code = """
            .data
            msg: .asciiz "Time: 12:00"
            .text
            main:
                la a0, msg
                li a7, 4
                ecall
        """.trimIndent()

        val assembler = RiscvAssembler()
        val result = assembler.assemble(code)

        assertTrue(result.isSuccess)
        assertEquals("Time: 12:00", result.initialMemory.readString(result.symbols["msg"]!!.address))
    }

    @Test
    fun testDivOverflowDoesNotCrash() {
        val code = """
            .text
            main:
                li t0, -2147483648
                li t1, -1
                div t2, t0, t1
                rem t3, t0, t1
        """.trimIndent()

        val assembler = RiscvAssembler()
        val result = assembler.assemble(code)
        assertTrue(result.isSuccess)

        val simulator = RiscvSimulator()
        simulator.loadProgram(result)
        simulator.executeNextStep() // li t0
        simulator.executeNextStep() // li t1
        simulator.executeNextStep() // div t2, t0, t1
        assertEquals(Int.MIN_VALUE, simulator.registers[7].value) // t2 is x7
        simulator.executeNextStep() // rem t3, t0, t1
        assertEquals(0, simulator.registers[28].value) // t3 is x28
    }

    @Test
    fun testNumericRegisterParsing() {
        val code = """
            .text
            main:
                addi 5, 0, 10
                addi 6, 5, 20
                add 7, 5, 6
        """.trimIndent()

        val assembler = RiscvAssembler()
        val result = assembler.assemble(code)
        assertTrue(result.isSuccess)

        val simulator = RiscvSimulator()
        simulator.loadProgram(result)
        simulator.executeNextStep()
        assertEquals(10, simulator.registers[5].value)
        simulator.executeNextStep()
        assertEquals(20, simulator.registers[6].value)
        simulator.executeNextStep()
        assertEquals(30, simulator.registers[7].value)
    }
}
