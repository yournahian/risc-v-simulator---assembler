package com.example.riscv.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.riscv.model.FloatRegister
import com.example.riscv.model.Register

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterEditDialog(
    register: Register?,
    floatRegister: FloatRegister?,
    onDismiss: () -> Unit,
    onSaveRegister: (Int, Int) -> Unit,
    onSaveFloatRegister: (Int, Float) -> Unit
) {
    if (register == null && floatRegister == null) return

    val title = if (register != null) "Edit Register ${register.name} (${register.abiName})" else "Edit Float Register ${floatRegister?.name} (${floatRegister?.abiName})"
    var valueInput by remember {
        mutableStateOf(
            if (register != null) register.hexValue else String.format("%.6f", floatRegister?.floatValue ?: 0f)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF252329),
        titleContentColor = Color(0xFFE6E1E5),
        title = {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        },
        text = {
            Column {
                Text(
                    text = "Enter value (hex e.g. 0x0000000A or decimal e.g. 10):",
                    fontSize = 12.sp,
                    color = Color(0xFF938F99)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = valueInput,
                    onValueChange = { valueInput = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF121115),
                        unfocusedContainerColor = Color(0xFF121115),
                        focusedBorderColor = Color(0xFFD0BCFF),
                        unfocusedBorderColor = Color(0xFF49454F),
                        focusedTextColor = Color(0xFFE6E1E5),
                        unfocusedTextColor = Color(0xFFE6E1E5)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val txt = valueInput.trim()
                    if (register != null) {
                        val intVal = parseHexOrDecInt(txt)
                        if (intVal != null) onSaveRegister(register.id, intVal)
                    } else if (floatRegister != null) {
                        val fltVal = txt.toFloatOrNull() ?: 0f
                        onSaveFloatRegister(floatRegister.id, fltVal)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD0BCFF),
                    contentColor = Color(0xFF381E72)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFFCAC4D0))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryEditDialog(
    address: Int?,
    onDismiss: () -> Unit,
    onSaveMemory: (Int, Int) -> Unit
) {
    if (address == null) return

    val hexAddr = String.format("0x%08X", address)
    var valueInput by remember { mutableStateOf("0x00000000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF252329),
        titleContentColor = Color(0xFFE6E1E5),
        title = {
            Text("Edit Memory Word at $hexAddr", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        },
        text = {
            Column {
                Text("Enter 32-bit word value (hex or decimal):", fontSize = 12.sp, color = Color(0xFF938F99))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = valueInput,
                    onValueChange = { valueInput = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF121115),
                        unfocusedContainerColor = Color(0xFF121115),
                        focusedBorderColor = Color(0xFFD0BCFF),
                        unfocusedBorderColor = Color(0xFF49454F),
                        focusedTextColor = Color(0xFFE6E1E5),
                        unfocusedTextColor = Color(0xFFE6E1E5)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val intVal = parseHexOrDecInt(valueInput.trim())
                    if (intVal != null) onSaveMemory(address, intVal)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD0BCFF),
                    contentColor = Color(0xFF381E72)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFFCAC4D0))
            }
        }
    )
}

private fun parseHexOrDecInt(input: String): Int? {
    val s = input.trim()
    if (s.isEmpty()) return null
    return try {
        if (s.startsWith("0x") || s.startsWith("0X")) {
            s.substring(2).toLong(16).toInt()
        } else {
            s.toInt()
        }
    } catch (e: Exception) {
        null
    }
}
