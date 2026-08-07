package com.example.riscv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.riscv.simulator.ProgramStatus
import com.example.riscv.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsoleTab(
    uiState: UiState,
    onSubmitInputInt: (Int) -> Unit,
    onSubmitInputFloat: (Float) -> Unit,
    onSubmitInputString: (String) -> Unit
) {
    var userInputText by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    val isWaitingInput = uiState.status.name.startsWith("WAITING_INPUT")

    LaunchedEffect(uiState.consoleLog) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1B1F))
    ) {
        // Top Toolbar
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF252329)),
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("CONSOLE / TERMINAL", fontSize = 11.sp, color = Color(0xFF938F99), fontWeight = FontWeight.Bold)
                    if (isWaitingInput) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = Color(0xFFEFB8C8),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                "WAITING INPUT",
                                color = Color(0xFF381E72),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { clipboardManager.setText(AnnotatedString(uiState.consoleLog)) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Log", tint = Color(0xFFCAC4D0), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Divider(color = Color(0xFF49454F))

        // Terminal Log Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF121115))
                .padding(12.dp)
                .verticalScroll(scrollState)
        ) {
            if (uiState.consoleLog.isEmpty()) {
                Text(
                    text = "Console is empty. Run a program with ecall prints to view output.",
                    color = Color(0xFF938F99),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            } else {
                Text(
                    text = uiState.consoleLog,
                    color = Color(0xFFD0BCFF),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 20.sp
                )
            }
        }

        // User Input Bar if program requires input
        if (isWaitingInput) {
            Surface(
                color = Color(0xFF252329),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val promptMsg = when (uiState.status) {
                        ProgramStatus.WAITING_INPUT_INT -> "Enter Integer: "
                        ProgramStatus.WAITING_INPUT_FLOAT -> "Enter Float: "
                        ProgramStatus.WAITING_INPUT_STRING -> "Enter String: "
                        else -> "Enter Input: "
                    }

                    Text(
                        text = promptMsg,
                        color = Color(0xFFEFB8C8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    OutlinedTextField(
                        value = userInputText,
                        onValueChange = { userInputText = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF121115),
                            unfocusedContainerColor = Color(0xFF121115),
                            focusedBorderColor = Color(0xFFD0BCFF),
                            unfocusedBorderColor = Color(0xFF49454F),
                            focusedTextColor = Color(0xFFE6E1E5),
                            unfocusedTextColor = Color(0xFFE6E1E5)
                        ),
                        modifier = Modifier.weight(1f).height(40.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Button(
                        onClick = {
                            val txt = userInputText
                            userInputText = ""
                            when (uiState.status) {
                                ProgramStatus.WAITING_INPUT_INT -> onSubmitInputInt(txt.toIntOrNull() ?: 0)
                                ProgramStatus.WAITING_INPUT_FLOAT -> onSubmitInputFloat(txt.toFloatOrNull() ?: 0f)
                                ProgramStatus.WAITING_INPUT_STRING -> onSubmitInputString(txt)
                                else -> {}
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD0BCFF),
                            contentColor = Color(0xFF381E72)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
