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
}
