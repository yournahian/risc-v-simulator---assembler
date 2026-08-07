package com.example.riscv.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.riscv.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorTab(
    uiState: UiState,
    onCodeChange: (String) -> Unit,
    onToggleBreakpoint: (Int) -> Unit,
    onAssemble: () -> Unit,
    onRun: () -> Unit,
    onOpenExamples: () -> Unit,
    onInsertSnippet: (String) -> Unit,
    onSelectFileTab: (String) -> Unit,
    onAddFileTab: () -> Unit,
    onCloseFileTab: (String) -> Unit,
    onToggleShortcuts: () -> Unit
) {
    val lines = remember(uiState.sourceCode) { uiState.sourceCode.lines() }
    val highlightedText = remember(uiState.sourceCode) { SyntaxHighlighter.highlight(uiState.sourceCode) }

    val activeLine = remember(uiState.pc, uiState.assemblyResult) {
        uiState.assemblyResult?.instructions?.firstOrNull { it.address == uiState.pc }?.lineNumber
    }

    val snippets = listOf(
        "li a7, 4" to "# Print String\nli a7, 4\nla a0, msg\necall\n",
        "li a7, 1" to "# Print Int\nli a7, 1\nmv a0, t0\necall\n",
        "ecall read" to "# Read Int\nli a7, 5\necall\nmv t0, a0\n",
        "lw/sw" to "lw t0, 0(s0)\nsw t0, 4(s0)\n",
        "loop" to "li t0, 0\nli t1, 10\nloop:\nbge t0, t1, done\naddi t0, t0, 1\nj loop\ndone:\n",
        "exit" to "li a7, 10\necall\n"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1B1F))
    ) {
        // Toolbar with Action Buttons & Toggleable Shortcuts
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF252329)),
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onRun,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD0BCFF),
                                contentColor = Color(0xFF381E72)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Run", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Run", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        OutlinedButton(
                            onClick = onAssemble,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD0BCFF)),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF49454F))),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Icon(Icons.Default.Build, contentDescription = "Assemble", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Assemble", fontSize = 13.sp)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Toggle Shortcut Insert Toolbar
                        IconButton(
                            onClick = onToggleShortcuts,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.showShortcuts) Icons.Default.KeyboardHide else Icons.Default.Keyboard,
                                contentDescription = "Toggle Shortcuts",
                                tint = if (uiState.showShortcuts) Color(0xFFD0BCFF) else Color(0xFF938F99),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Status pill
                        Surface(
                            color = if (uiState.isAssembled) Color(0xFF381E72) else Color(0xFF49454F),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (uiState.isAssembled) "Ready to Run" else "Not Assembled",
                                color = if (uiState.isAssembled) Color(0xFFD0BCFF) else Color(0xFFCAC4D0),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Quick Snippet insert row (toggleable)
                AnimatedVisibility(
                    visible = uiState.showShortcuts,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                Text(
                                    "Insert: ",
                                    color = Color(0xFF938F99),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            items(snippets) { (label, code) ->
                                AssistChip(
                                    onClick = { onInsertSnippet(code) },
                                    label = { Text(label, fontSize = 11.sp, color = Color(0xFFE6E1E5)) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF1C1B1F)),
                                    border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = Color(0xFF49454F)),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Multi-File Tabs Bar (placed below insert shortcuts section)
        Surface(
            color = Color(0xFF141316),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                uiState.editorFiles.forEach { file ->
                    val isActive = file.id == uiState.activeFileId
                    Surface(
                        color = if (isActive) Color(0xFF252329) else Color.Transparent,
                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clickable { onSelectFileTab(file.id) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                tint = if (isActive) Color(0xFFD0BCFF) else Color(0xFF938F99),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = file.name,
                                color = if (isActive) Color(0xFFE6E1E5) else Color(0xFF938F99),
                                fontSize = 12.sp,
                                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                            )
                            if (uiState.editorFiles.size > 1) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close tab",
                                    tint = Color(0xFF938F99),
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .clickable { onCloseFileTab(file.id) }
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onAddFileTab,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "New Tab",
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Error Banner if assembly failed
        if (uiState.errorMessage != null && !uiState.isAssembled) {
            Surface(
                color = Color(0xFFB3261E).copy(alpha = 0.25f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Error, contentDescription = "Error", tint = Color(0xFFF2B8B5), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = uiState.errorMessage,
                        color = Color(0xFFF2B8B5),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Code Editor & Line Numbers
        Row(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(2.dp)
        ) {
            // Line Numbers & Breakpoint Indicators Column
            val listState = rememberLazyListState()
            
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .width(44.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF1C1B1F))
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(lines) { idx, _ ->
                    val lineNum = idx + 1
                    val isBreakpoint = uiState.breakpoints.contains(lineNum)
                    val isActive = activeLine == lineNum

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(22.dp)
                            .padding(horizontal = 4.dp)
                            .clickable { onToggleBreakpoint(lineNum) }
                    ) {
                        // Red/Pink dot for breakpoint
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isBreakpoint) Color(0xFFEFB8C8) else Color.Transparent)
                        )

                        Text(
                            text = lineNum.toString(),
                            color = if (isActive) Color(0xFFD0BCFF) else Color(0xFF49454F),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Divider(color = Color(0xFF49454F), modifier = Modifier.fillMaxHeight().width(1.dp))

            // Code Editor Box
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF121115))
                    .padding(8.dp)
            ) {
                BasicTextField(
                    value = uiState.sourceCode,
                    onValueChange = onCodeChange,
                    cursorBrush = SolidColor(Color.White),
                    visualTransformation = remember { RiscvSyntaxVisualTransformation() },
                    textStyle = TextStyle(
                        color = Color(0xFFE6E1E5),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 22.sp
                    ),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
