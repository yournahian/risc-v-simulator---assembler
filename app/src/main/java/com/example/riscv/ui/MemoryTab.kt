package com.example.riscv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.riscv.model.Memory
import com.example.riscv.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryTab(
    uiState: UiState,
    memoryEntries: List<Pair<Int, Int>>,
    onJumpToAddress: (Int) -> Unit,
    onEditMemory: (Int) -> Unit
) {
    var customAddressInput by remember { mutableStateOf("") }

    val segmentButtons = listOf(
        ".text" to Memory.TEXT_BASE,
        ".data" to Memory.DATA_BASE,
        "Heap" to Memory.HEAP_BASE,
        "Stack" to 0x7FFFFFE0
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1B1F))
    ) {
        // Jump & Search Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF252329)),
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text("JUMP TO MEMORY SEGMENT", fontSize = 10.sp, color = Color(0xFF938F99), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    segmentButtons.forEach { (label, addr) ->
                        val isSelected = uiState.memoryViewAddress == addr
                        Button(
                            onClick = { onJumpToAddress(addr) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFFD0BCFF) else Color(0xFF1C1B1F),
                                contentColor = if (isSelected) Color(0xFF381E72) else Color(0xFFCAC4D0)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) {
                            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Custom Hex address input
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = customAddressInput,
                        onValueChange = { customAddressInput = it },
                        placeholder = { Text("e.g. 0x10010000", fontSize = 11.sp, color = Color(0xFF938F99)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF938F99), modifier = Modifier.size(14.dp)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF121115),
                            unfocusedContainerColor = Color(0xFF121115),
                            focusedBorderColor = Color(0xFFD0BCFF),
                            unfocusedBorderColor = Color(0xFF49454F),
                            focusedTextColor = Color(0xFFE6E1E5),
                            unfocusedTextColor = Color(0xFFE6E1E5)
                        ),
                        modifier = Modifier.weight(1f).height(36.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val addr = parseHexOrDec(customAddressInput)
                            if (addr != null) onJumpToAddress(addr)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD0BCFF),
                            contentColor = Color(0xFF381E72)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Jump", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Divider(color = Color(0xFF49454F))

        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF252329))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Address", fontSize = 10.sp, color = Color(0xFFCAC4D0), modifier = Modifier.width(95.dp))
            Text("Hex Word", fontSize = 10.sp, color = Color(0xFFCAC4D0), modifier = Modifier.width(95.dp))
            Text("Signed Dec", fontSize = 10.sp, color = Color(0xFFCAC4D0), modifier = Modifier.weight(1f))
            Text("ASCII", fontSize = 10.sp, color = Color(0xFFCAC4D0), modifier = Modifier.width(50.dp))
        }

        Divider(color = Color(0xFF49454F))

        // Memory Words List
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(memoryEntries) { (addr, value) ->
                val hexAddr = String.format("0x%08X", addr)
                val hexVal = String.format("0x%08X", value)
                val decVal = value.toString()

                // 4-byte ASCII chars
                val b0 = (value and 0xFF).toChar()
                val b1 = ((value shr 8) and 0xFF).toChar()
                val b2 = ((value shr 16) and 0xFF).toChar()
                val b3 = ((value shr 24) and 0xFF).toChar()
                val asciiStr = buildString {
                    append(if (b0.code in 32..126) b0 else '.')
                    append(if (b1.code in 32..126) b1 else '.')
                    append(if (b2.code in 32..126) b2 else '.')
                    append(if (b3.code in 32..126) b3 else '.')
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditMemory(addr) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = hexAddr,
                        color = Color(0xFFD0BCFF),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(95.dp)
                    )

                    Text(
                        text = hexVal,
                        color = Color(0xFFEFB8C8),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(95.dp)
                    )

                    Text(
                        text = decVal,
                        color = Color(0xFFE6E1E5),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        modifier = Modifier.width(50.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = asciiStr,
                            color = Color(0xFFD0BCFF),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF938F99), modifier = Modifier.size(12.dp))
                    }
                }

                Divider(color = Color(0xFF252329))
            }
        }
    }
}

private fun parseHexOrDec(input: String): Int? {
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
