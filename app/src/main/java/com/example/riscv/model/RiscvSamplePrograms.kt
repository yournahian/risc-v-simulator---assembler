package com.example.riscv.model

data class SampleProgram(
    val title: String,
    val description: String,
    val category: String,
    val code: String
)

object RiscvSamplePrograms {
    val ALL = listOf(
        SampleProgram(
            title = "Hello World & Syscalls",
            description = "Basic string printing and interactive user input using ecall system calls.",
            category = "Basics",
            code = """# RISC-V Hello World and Interactive Input
.data
    hello_msg:  .asciiz "Hello, World from RISC-V Simulator!\n"
    prompt_msg: .asciiz "Enter an integer to double: "
    result_msg: .asciiz "The doubled result is: "
    newline:    .asciiz "\n"

.text
.globl main
main:
    # Print hello string (ecall 4)
    la a0, hello_msg
    li a7, 4
    ecall

    # Print prompt string
    la a0, prompt_msg
    li a7, 4
    ecall

    # Read integer from console (ecall 5)
    li a7, 5
    ecall
    mv t0, a0        # Store user input in t0

    # Multiply by 2 (add t0, t0, t0)
    add t1, t0, t0

    # Print result message
    la a0, result_msg
    li a7, 4
    ecall

    # Print doubled integer (ecall 1)
    mv a0, t1
    li a7, 1
    ecall

    # Print newline
    la a0, newline
    li a7, 4
    ecall

    # Exit program (ecall 10)
    li a7, 10
    ecall
"""
        ),
        SampleProgram(
            title = "Fibonacci Generator",
            description = "Calculates N terms of the Fibonacci sequence and stores them into memory.",
            category = "Algorithms",
            code = """# RISC-V Fibonacci Sequence Generator
.data
    title: .asciiz "--- Fibonacci Generator ---\n"
    prompt: .asciiz "How many terms? (N): "
    space: .asciiz " "
    newline: .asciiz "\n"
    array: .space 80    # Array for up to 20 integers

.text
.globl main
main:
    # Print Title
    la a0, title
    li a7, 4
    ecall

    # Default count N = 10 if input is 0 or unprovided
    li t0, 10           # N = 10
    la t1, array        # Base pointer to array

    # First two terms: F[0] = 0, F[1] = 1
    li t2, 0            # F[n-2]
    li t3, 1            # F[n-1]

    sw t2, 0(t1)        # Store array[0]
    sw t3, 4(t1)        # Store array[1]

    # Loop index counter i = 2
    li t4, 2

fib_loop:
    bge t4, t0, fib_print_start

    add t5, t2, t3     # F[i] = F[i-2] + F[i-1]
    
    # Calculate offset 4 * i
    slli t6, t4, 2
    add t6, t1, t6
    sw t5, 0(t6)        # Store in array

    # Shift terms
    mv t2, t3
    mv t3, t5

    addi t4, t4, 1      # i++
    j fib_loop

fib_print_start:
    li t4, 0            # Index for printing

fib_print_loop:
    bge t4, t0, fib_done

    slli t6, t4, 2
    add t6, t1, t6
    lw a0, 0(t6)

    # Print number
    li a7, 1
    ecall

    # Print space
    la a0, space
    li a7, 4
    ecall

    addi t4, t4, 1
    j fib_print_loop

fib_done:
    la a0, newline
    li a7, 4
    ecall

    li a7, 10
    ecall
"""
        ),
        SampleProgram(
            title = "Recursive Factorial",
            description = "Demonstrates stack frames (sp, ra), function calling convention, and recursion.",
            category = "Recursion",
            code = """# RISC-V Recursive Factorial Calculator
.data
    prompt: .asciiz "Calculating Factorial(6):\n"
    res_str: .asciiz "6! = "
    newline: .asciiz "\n"

.text
.globl main
main:
    la a0, prompt
    li a7, 4
    ecall

    li a0, 6            # Calculate 6!
    call fact           # Call factorial function

    mv t0, a0           # Save result

    la a0, res_str
    li a7, 4
    ecall

    mv a0, t0           # Print result
    li a7, 1
    ecall

    la a0, newline
    li a7, 4
    ecall

    li a7, 10
    ecall

# int fact(int n)
fact:
    # Allocate stack frame (8 bytes)
    addi sp, sp, -8
    sw ra, 4(sp)
    sw a0, 0(sp)

    # Base case: if n <= 1, return 1
    li t0, 1
    ble a0, t0, fact_base

    # Recursive step: fact(n - 1)
    addi a0, a0, -1
    call fact

    # a0 now contains fact(n - 1)
    lw t1, 0(sp)        # Restore original n
    mul a0, a0, t1      # a0 = n * fact(n - 1)
    j fact_return

fact_base:
    li a0, 1

fact_return:
    lw ra, 4(sp)        # Restore ra
    addi sp, sp, 8      # Deallocate stack
    ret
"""
        ),
        SampleProgram(
            title = "Selection Sort",
            description = "In-place array sorting algorithm operating on data segment memory.",
            category = "Algorithms",
            code = """# RISC-V Selection Sort on Memory Array
.data
    arr:    .word 42, 12, 88, 3, 27, 65, 9, 51
    size:   .word 8
    msg1:   .asciiz "Sorted Array: "
    space:  .asciiz " "
    newline:.asciiz "\n"

.text
.globl main
main:
    la s0, arr          # Array base pointer
    lw s1, size         # Size N = 8
    li t0, 0            # i = 0

outer_loop:
    addi t1, s1, -1
    bge t0, t1, sort_done  # if i >= N - 1, done

    mv t2, t0           # min_idx = i
    addi t3, t0, 1      # j = i + 1

inner_loop:
    bge t3, s1, inner_end  # if j >= N, end inner loop

    # Load arr[j]
    slli t4, t3, 2
    add t4, s0, t4
    lw t5, 0(t4)

    # Load arr[min_idx]
    slli t6, t2, 2
    add t6, s0, t6
    lw a0, 0(t6)

    # Compare arr[j] < arr[min_idx]
    bge t5, a0, no_new_min
    mv t2, t3           # min_idx = j

no_new_min:
    addi t3, t3, 1      # j++
    j inner_loop

inner_end:
    # Swap arr[i] and arr[min_idx]
    slli t4, t0, 2
    add t4, s0, t4
    lw t5, 0(t4)        # arr[i]

    slli t6, t2, 2
    add t6, s0, t6
    lw a0, 0(t6)        # arr[min_idx]

    sw a0, 0(t4)        # arr[i] = arr[min_idx]
    sw t5, 0(t6)        # arr[min_idx] = arr[i]

    addi t0, t0, 1      # i++
    j outer_loop

sort_done:
    la a0, msg1
    li a7, 4
    ecall

    li t0, 0            # Index for print

print_loop:
    bge t0, s1, print_done
    slli t4, t0, 2
    add t4, s0, t4
    lw a0, 0(t4)

    li a7, 1
    ecall

    la a0, space
    li a7, 4
    ecall

    addi t0, t0, 1
    j print_loop

print_done:
    la a0, newline
    li a7, 4
    ecall

    li a7, 10
    ecall
"""
        ),
        SampleProgram(
            title = "Floating Point Operations (RV32F)",
            description = "Floating point load, store, add, multiply, and printing using f0-f31.",
            category = "Floating Point",
            code = """# RISC-V RV32F Floating Point Example
.data
    val1: .float 3.14159
    val2: .float 2.71828
    msg1: .asciiz "PI = "
    msg2: .asciiz "\nE = "
    msg3: .asciiz "\nPI * E = "
    newline: .asciiz "\n"

.text
.globl main
main:
    # Load float values into f1 and f2
    la t0, val1
    flw f1, 0(t0)

    la t0, val2
    flw f2, 0(t0)

    # Print msg1
    la a0, msg1
    li a7, 4
    ecall

    # Print f1 (ecall 2 prints float in fa0)
    # Move f1 to fa0 (f10 is fa0)
    fmv.s fa0, f1
    li a7, 2
    ecall

    # Print msg2
    la a0, msg2
    li a7, 4
    ecall

    # Print f2
    fmv.s fa0, f2
    li a7, 2
    ecall

    # Multiply f3 = f1 * f2
    fmul.s f3, f1, f2

    # Print msg3
    la a0, msg3
    li a7, 4
    ecall

    # Print f3
    fmv.s fa0, f3
    li a7, 2
    ecall

    la a0, newline
    li a7, 4
    ecall

    li a7, 10
    ecall
"""
        )
    )
}
