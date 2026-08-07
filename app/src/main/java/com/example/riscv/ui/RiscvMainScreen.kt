package com.example.riscv.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.riscv.viewmodel.AppTab
import com.example.riscv.viewmodel.SimulatorViewModel

fun getFileNameFromUri(context: Context, uri: Uri): String {
    var fileName: String? = null
    if (uri.scheme == "content") {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    if (fileName.isNullOrBlank()) {
        val lastSeg = uri.lastPathSegment?.substringAfterLast('/')
        if (!lastSeg.isNullOrBlank() && !lastSeg.contains(":")) {
            fileName = lastSeg
        }
    }
    if (fileName.isNullOrBlank()) {
        fileName = "program.asm"
    }
    return fileName
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiscvMainScreen(
    viewModel: SimulatorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val code = inputStream.bufferedReader().readText()
                    val fileName = getFileNameFromUri(context, uri)
                    viewModel.openFileInTab(fileName, code)
                    Toast.makeText(context, "Code opened: $fileName", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to open file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/x-asm")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(uiState.sourceCode.toByteArray())
                    val savedName = getFileNameFromUri(context, uri)
                    viewModel.updateActiveFileName(savedName)
                    Toast.makeText(context, "Saved as $savedName", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFFD0BCFF),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "RARS",
                                color = Color(0xFF381E72),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "RISC-V",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                            color = Color(0xFFE6E1E5)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { openFileLauncher.launch(arrayOf("*/*")) }) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Open File", tint = Color(0xFFD0BCFF))
                    }
                    IconButton(onClick = {
                        val activeName = uiState.activeFile?.name ?: "program.asm"
                        saveFileLauncher.launch(activeName)
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Save File", tint = Color(0xFFD0BCFF))
                    }
                    IconButton(onClick = { viewModel.setShowExamplesDialog(true) }) {
                        Icon(Icons.Default.Category, contentDescription = "Sample Programs", tint = Color(0xFFD0BCFF))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1C1B1F),
                    titleContentColor = Color(0xFFE6E1E5)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF1C1B1F),
                contentColor = Color(0xFFD0BCFF)
            ) {
                val navItemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFFD0BCFF),
                    selectedTextColor = Color(0xFFD0BCFF),
                    indicatorColor = Color(0xFF49454F),
                    unselectedIconColor = Color(0xFFCAC4D0),
                    unselectedTextColor = Color(0xFFCAC4D0)
                )

                NavigationBarItem(
                    selected = uiState.currentTab == AppTab.EDITOR,
                    onClick = { viewModel.selectTab(AppTab.EDITOR) },
                    icon = { Icon(Icons.Default.Code, contentDescription = "Editor") },
                    label = { Text("Editor", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                    colors = navItemColors
                )

                NavigationBarItem(
                    selected = uiState.currentTab == AppTab.CONSOLE,
                    onClick = { viewModel.selectTab(AppTab.CONSOLE) },
                    icon = { Icon(Icons.Default.Terminal, contentDescription = "Console") },
                    label = { Text("Console", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                    colors = navItemColors
                )

                NavigationBarItem(
                    selected = uiState.currentTab == AppTab.EXECUTE,
                    onClick = { viewModel.selectTab(AppTab.EXECUTE) },
                    icon = { Icon(Icons.Default.BugReport, contentDescription = "Debug") },
                    label = { Text("Debug", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                    colors = navItemColors
                )

                NavigationBarItem(
                    selected = uiState.currentTab == AppTab.REGISTERS,
                    onClick = { viewModel.selectTab(AppTab.REGISTERS) },
                    icon = { Icon(Icons.Default.FormatListNumbered, contentDescription = "Registers") },
                    label = { Text("Registers", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                    colors = navItemColors
                )

                NavigationBarItem(
                    selected = uiState.currentTab == AppTab.MEMORY,
                    onClick = { viewModel.selectTab(AppTab.MEMORY) },
                    icon = { Icon(Icons.Default.Memory, contentDescription = "Memory") },
                    label = { Text("Memory", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                    colors = navItemColors
                )
            }
        },
        containerColor = Color(0xFF1C1B1F)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.currentTab) {
                AppTab.EDITOR -> EditorTab(
                    uiState = uiState,
                    onCodeChange = viewModel::updateSourceCode,
                    onToggleBreakpoint = viewModel::toggleBreakpoint,
                    onAssemble = viewModel::assembleCode,
                    onRun = viewModel::runCodeAndSwitchToConsole,
                    onOpenExamples = { viewModel.setShowExamplesDialog(true) },
                    onInsertSnippet = { snippet ->
                        val newCode = if (uiState.sourceCode.endsWith("\n")) uiState.sourceCode + snippet else uiState.sourceCode + "\n" + snippet
                        viewModel.updateSourceCode(newCode)
                    },
                    onSelectFileTab = viewModel::selectEditorFile,
                    onAddFileTab = { viewModel.addEditorFile() },
                    onCloseFileTab = viewModel::closeEditorFile,
                    onToggleShortcuts = viewModel::toggleShortcuts
                )

                AppTab.EXECUTE -> ExecuteTab(
                    uiState = uiState,
                    onStepForward = viewModel::stepForward,
                    onStepBack = viewModel::stepBack,
                    onStartRun = viewModel::startAutoRun,
                    onPauseRun = viewModel::pauseAutoRun,
                    onReset = viewModel::resetSimulator,
                    onSpeedChange = viewModel::setRunDelay,
                    onToggleBreakpoint = viewModel::toggleBreakpoint
                )

                AppTab.REGISTERS -> RegistersTab(
                    uiState = uiState,
                    onEditRegister = viewModel::setEditingRegister,
                    onEditFloatRegister = viewModel::setEditingFloatRegister
                )

                AppTab.MEMORY -> MemoryTab(
                    uiState = uiState,
                    memoryEntries = viewModel.getMemoryEntries(),
                    onJumpToAddress = viewModel::setMemoryViewAddress,
                    onEditMemory = viewModel::setEditingMemoryAddress
                )

                AppTab.CONSOLE -> ConsoleTab(
                    uiState = uiState,
                    onSubmitInputInt = viewModel::submitUserInputInt,
                    onSubmitInputFloat = viewModel::submitUserInputFloat,
                    onSubmitInputString = viewModel::submitUserInputString
                )
            }

            // Dialogs
            if (uiState.showExamplesDialog) {
                ExamplesDialog(
                    onDismiss = { viewModel.setShowExamplesDialog(false) },
                    onSelectProgram = { prog -> viewModel.loadSampleProgram(prog) }
                )
            }

            if (uiState.editingRegister != null || uiState.editingFloatRegister != null) {
                RegisterEditDialog(
                    register = uiState.editingRegister,
                    floatRegister = uiState.editingFloatRegister,
                    onDismiss = {
                        viewModel.setEditingRegister(null)
                        viewModel.setEditingFloatRegister(null)
                    },
                    onSaveRegister = viewModel::setRegisterValue,
                    onSaveFloatRegister = viewModel::setFloatRegisterValue
                )
            }

            if (uiState.editingMemoryAddress != null) {
                MemoryEditDialog(
                    address = uiState.editingMemoryAddress,
                    onDismiss = { viewModel.setEditingMemoryAddress(null) },
                    onSaveMemory = viewModel::setMemoryValue
                )
            }
        }
    }
}
