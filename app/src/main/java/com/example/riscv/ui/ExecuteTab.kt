package com.example.riscv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.riscv.simulator.ProgramStatus
import com.example.riscv.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExecuteTab(
    uiState: UiState,
    onStepForward: () -> Unit,
    onStepBack: () -> Unit,
    onStartRun: () -> Unit,
    onPauseRun: () -> Unit,
    onReset: () -> Unit,
    onSpeedChange: (Long) -> Unit,
    onToggleBreakpoint: (Int) -> Unit
) {
    val isRunning = uiState.status == ProgramStatus.RUNNING

    val currentInstruction = remember(uiState.pc, uiState.assemblyResult) {
        uiState.assemblyResult?.instructions?.firstOrNull { it.address == uiState.pc }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1B1F))
    ) {
        // Top Debugger Controls Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF252329)),
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Control Action Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Reset
                        IconButton(
                            onClick = onReset,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF49454F))
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = "Reset", tint = Color(0xFFCAC4D0))
                        }

                        // Step Back (Undo)
                        IconButton(
                            onClick = onStepBack,
                            enabled = uiState.canStepBack && !isRunning,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (uiState.canStepBack && !isRunning) Color(0xFF49454F) else Color(0xFF1C1B1F))
                        ) {
                            Icon(
                                Icons.Default.Undo,
                                contentDescription = "Step Back",
                                tint = if (uiState.canStepBack && !isRunning) Color(0xFFD0BCFF) else Color(0xFF938F99)
                            )
                        }

                        // Step Forward
                        Button(
                            onClick = onStepForward,
                            enabled = !isRunning && (uiState.assemblyResult?.instructions?.isNotEmpty() == true),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD0BCFF),
                                contentColor = Color(0xFF381E72)
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Step", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Step", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        // Run / Pause
                        Button(
                            onClick = { if (isRunning) onPauseRun() else onStartRun() },
                            enabled = uiState.assemblyResult?.instructions?.isNotEmpty() == true,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRunning) Color(0xFFEFB8C8) else Color(0xFFD0BCFF),
                                contentColor = Color(0xFF381E72)
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(
                                if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isRunning) "Pause" else "Run",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isRunning) "Pause" else "Run", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    // Program Status Badge
                    val statusBg = when (uiState.status) {
                        ProgramStatus.RUNNING -> Color(0xFF381E72)
                        ProgramStatus.PAUSED -> Color(0xFF49454F)
                        ProgramStatus.HALTED -> Color(0xFF252329)
                        ProgramStatus.ERROR -> Color(0xFFB3261E)
                        else -> Color(0xFF381E72)
                    }

                    val statusText = when (uiState.status) {
                        ProgramStatus.RUNNING -> Color(0xFFD0BCFF)
                        ProgramStatus.PAUSED -> Color(0xFFEFB8C8)
                        ProgramStatus.HALTED -> Color(0xFFCAC4D0)
                        ProgramStatus.ERROR -> Color(0xFFF2B8B5)
                        else -> Color(0xFFD0BCFF)
                    }

                    Surface(
                        color = statusBg,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = uiState.status.name,
                            color = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Execution Speed Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Speed (Delay):", fontSize = 11.sp, color = Color(0xFFCAC4D0))
                    Spacer(modifier = Modifier.width(8.dp))
                    Slider(
                        value = uiState.runDelayMs.toFloat(),
                        onValueChange = { onSpeedChange(it.toLong()) },
                        valueRange = 10f..500f,
                        modifier = Modifier.weight(1f).height(24.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFD0BCFF),
                            activeTrackColor = Color(0xFFD0BCFF),
                            inactiveTrackColor = Color(0xFF49454F)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${uiState.runDelayMs} ms", fontSize = 11.sp, color = Color(0xFFE6E1E5), fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Statistics Banner (PC, Steps, Cycles, Active Instruction)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF121115))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("PROGRAM COUNTER", fontSize = 9.sp, color = Color(0xFF938F99), fontWeight = FontWeight.Bold)
                        Text(
                            text = String.format("0x%08X", uiState.pc),
                            color = Color(0xFFD0BCFF),
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("STEPS", fontSize = 9.sp, color = Color(0xFF938F99), fontWeight = FontWeight.Bold)
                        Text(
                            text = uiState.stepCount.toString(),
                            color = Color(0xFFE6E1E5),
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("CURRENT INSTRUCTION", fontSize = 9.sp, color = Color(0xFF938F99), fontWeight = FontWeight.Bold)
                        Text(
                            text = currentInstruction?.formattedText ?: "None",
                            color = Color(0xFFEFB8C8),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Divider(color = Color(0xFF49454F))

        // Instructions Listing & Machine Code Table
        val instructions = uiState.assemblyResult?.instructions ?: emptyList()
        val listState = rememberLazyListState()

        if (instructions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CodeOff, contentDescription = null, tint = Color(0xFF49454F), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No assembled program loaded", color = Color(0xFFCAC4D0), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Go to Editor tab and click 'Assemble'", color = Color(0xFF938F99), fontSize = 12.sp)
                }
            }
        } else {
            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF252329))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Bk", fontSize = 10.sp, color = Color(0xFFCAC4D0), modifier = Modifier.width(20.dp))
                Text("Address", fontSize = 10.sp, color = Color(0xFFCAC4D0), modifier = Modifier.width(85.dp))
                Text("Code (Hex)", fontSize = 10.sp, color = Color(0xFFCAC4D0), modifier = Modifier.width(85.dp))
                Text("Instruction", fontSize = 10.sp, color = Color(0xFFCAC4D0), modifier = Modifier.weight(1f))
                Text("Line", fontSize = 10.sp, color = Color(0xFFCAC4D0), modifier = Modifier.width(35.dp))
            }

            Divider(color = Color(0xFF49454F))

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(instructions) { inst ->
                    val isCurrentPc = inst.address == uiState.pc
                    val isBreakpoint = uiState.breakpoints.contains(inst.lineNumber)

                    val rowBg = when {
                        isCurrentPc -> Color(0xFF49454F)
                        isBreakpoint -> Color(0xFFEFB8C8).copy(alpha = 0.2f)
                        else -> Color.Transparent
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(rowBg)
                            .clickable { onToggleBreakpoint(inst.lineNumber) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Breakpoint Dot
                        Box(
                            modifier = Modifier
                                .width(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isBreakpoint) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEFB8C8))
                                )
                            }
                        }

                        // Address
                        Text(
                            text = inst.hexAddress,
                            color = Color(0xFFD0BCFF),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(85.dp)
                        )

                        // Machine Code (Hex)
                        Text(
                            text = inst.hexMachineCode,
                            color = Color(0xFFEFB8C8),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(85.dp)
                        )

                        // Instruction Text
                        Text(
                            text = inst.formattedText,
                            color = if (isCurrentPc) Color(0xFFD0BCFF) else Color(0xFFE6E1E5),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isCurrentPc) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )

                        // Source Line #
                        Text(
                            text = "L${inst.lineNumber}",
                            color = Color(0xFF938F99),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(35.dp)
                        )
                    }

                    Divider(color = Color(0xFF252329))
                }
            }
        }
    }
}
