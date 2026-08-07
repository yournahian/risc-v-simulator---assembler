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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.riscv.model.FloatRegister
import com.example.riscv.model.Register
import com.example.riscv.viewmodel.UiState

enum class ValueFormat {
    HEX,
    SIGNED_DEC,
    UNSIGNED_DEC,
    ASCII
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistersTab(
    uiState: UiState,
    onEditRegister: (Register) -> Unit,
    onEditFloatRegister: (FloatRegister) -> Unit
) {
    var selectedRegisterType by remember { mutableStateOf(0) } // 0 = Integer (x0-x31), 1 = Float (f0-f31)
    var selectedFormat by remember { mutableStateOf(ValueFormat.HEX) }
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1B1F))
    ) {
        // Controls Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF252329)),
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Register Type Segment (Int vs Float)
                TabRow(
                    selectedTabIndex = selectedRegisterType,
                    containerColor = Color(0xFF1C1B1F),
                    contentColor = Color(0xFFD0BCFF),
                    divider = {},
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .height(36.dp)
                ) {
                    Tab(
                        selected = selectedRegisterType == 0,
                        onClick = { selectedRegisterType = 0 }
                    ) {
                        Text("Integer Registers (x0-x31)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Tab(
                        selected = selectedRegisterType == 1,
                        onClick = { selectedRegisterType = 1 }
                    ) {
                        Text("Float Registers (f0-f31)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Format Switcher & Search
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Format buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ValueFormat.values().forEach { fmt ->
                            FilterChip(
                                selected = selectedFormat == fmt,
                                onClick = { selectedFormat = fmt },
                                label = { Text(fmt.name, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFD0BCFF),
                                    selectedLabelColor = Color(0xFF381E72),
                                    containerColor = Color(0xFF1C1B1F),
                                    labelColor = Color(0xFFCAC4D0)
                                ),
                                border = null,
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }

                    // Search box
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Filter...", fontSize = 11.sp, color = Color(0xFF938F99)) },
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
                        modifier = Modifier
                            .width(110.dp)
                            .height(36.dp)
                    )
                }
            }
        }

        Divider(color = Color(0xFF49454F))

        // Registers List
        if (selectedRegisterType == 0) {
            // Integer Registers
            val filteredRegisters = remember(uiState.registers, searchQuery) {
                uiState.registers.filter { reg ->
                    searchQuery.isEmpty() ||
                            reg.name.contains(searchQuery, ignoreCase = true) ||
                            reg.abiName.contains(searchQuery, ignoreCase = true) ||
                            reg.description.contains(searchQuery, ignoreCase = true)
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredRegisters) { reg ->
                    val isModified = uiState.lastModifiedRegister == reg.id
                    val formattedVal = when (selectedFormat) {
                        ValueFormat.HEX -> reg.hexValue
                        ValueFormat.SIGNED_DEC -> reg.value.toString()
                        ValueFormat.UNSIGNED_DEC -> reg.unsignedValue.toString()
                        ValueFormat.ASCII -> "'${reg.asciiChar}'"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isModified) Color(0xFF381E72) else Color.Transparent)
                            .clickable { onEditRegister(reg) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Register Name Badge
                            Surface(
                                color = if (isModified) Color(0xFFD0BCFF) else Color(0xFF49454F),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = reg.name,
                                    color = if (isModified) Color(0xFF381E72) else Color(0xFFE6E1E5),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = reg.abiName,
                                        color = Color(0xFFD0BCFF),
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isModified) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = Color(0xFFEFB8C8),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                "MODIFIED",
                                                color = Color(0xFF381E72),
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = reg.description,
                                    color = Color(0xFF938F99),
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formattedVal,
                                color = if (isModified) Color(0xFFD0BCFF) else Color(0xFFE6E1E5),
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF938F99), modifier = Modifier.size(14.dp))
                        }
                    }

                    Divider(color = Color(0xFF252329))
                }
            }
        } else {
            // Float Registers
            val filteredFloatRegisters = remember(uiState.floatRegisters, searchQuery) {
                uiState.floatRegisters.filter { reg ->
                    searchQuery.isEmpty() ||
                            reg.name.contains(searchQuery, ignoreCase = true) ||
                            reg.abiName.contains(searchQuery, ignoreCase = true)
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredFloatRegisters) { reg ->
                    val isModified = uiState.lastModifiedFloatRegister == reg.id
                    val formattedVal = when (selectedFormat) {
                        ValueFormat.HEX -> reg.hexValue
                        else -> String.format("%.6f", reg.floatValue)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isModified) Color(0xFF381E72) else Color.Transparent)
                            .clickable { onEditFloatRegister(reg) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = if (isModified) Color(0xFFD0BCFF) else Color(0xFF49454F),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = reg.name,
                                    color = if (isModified) Color(0xFF381E72) else Color(0xFFE6E1E5),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = reg.abiName,
                                color = Color(0xFFEFB8C8),
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formattedVal,
                                color = if (isModified) Color(0xFFD0BCFF) else Color(0xFFE6E1E5),
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF938F99), modifier = Modifier.size(14.dp))
                        }
                    }

                    Divider(color = Color(0xFF252329))
                }
            }
        }
    }
}
