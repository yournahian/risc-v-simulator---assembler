package com.example.riscv.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.riscv.assembler.RiscvAssembler
import com.example.riscv.model.*
import com.example.riscv.simulator.ProgramStatus
import com.example.riscv.simulator.RiscvSimulator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppTab {
    EDITOR,
    EXECUTE,
    REGISTERS,
    MEMORY,
    CONSOLE
}

data class EditorFile(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "program.asm",
    val content: String = RiscvSamplePrograms.ALL[0].code
)

data class UiState(
    val currentTab: AppTab = AppTab.EDITOR,
    val editorFiles: List<EditorFile> = listOf(EditorFile(id = "1", name = "program.asm", content = RiscvSamplePrograms.ALL[0].code)),
    val activeFileId: String = "1",
    val showShortcuts: Boolean = true,
    val sourceCode: String = RiscvSamplePrograms.ALL[0].code,
    val breakpoints: Set<Int> = emptySet(),
    val isAssembled: Boolean = false,
    val assemblyResult: AssemblyResult? = null,
    val status: ProgramStatus = ProgramStatus.READY,
    val pc: Int = Memory.TEXT_BASE,
    val registers: Array<Register> = Register.createDefaultRegisters(),
    val floatRegisters: Array<FloatRegister> = FloatRegister.createDefaultFloatRegisters(),
    val stepCount: Long = 0,
    val cycleCount: Long = 0,
    val lastModifiedRegister: Int? = null,
    val lastModifiedFloatRegister: Int? = null,
    val consoleLog: String = "",
    val errorMessage: String? = null,
    val runDelayMs: Long = 100L,
    val canStepBack: Boolean = false,
    val memoryViewAddress: Int = Memory.DATA_BASE,
    val showExamplesDialog: Boolean = false,
    val editingRegister: Register? = null,
    val editingFloatRegister: FloatRegister? = null,
    val editingMemoryAddress: Int? = null
) {
    val activeFile: EditorFile?
        get() = editorFiles.find { it.id == activeFileId } ?: editorFiles.firstOrNull()
}

class SimulatorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val assembler = RiscvAssembler()
    private val simulator = RiscvSimulator()
    private var autoRunJob: Job? = null

    init {
        assembleCode()
    }

    fun runCodeAndSwitchToConsole() {
        assembleCode()
        if (_uiState.value.isAssembled) {
            _uiState.update { it.copy(currentTab = AppTab.CONSOLE) }
            startAutoRun()
        } else {
            _uiState.update { it.copy(currentTab = AppTab.CONSOLE) }
        }
    }

    fun selectTab(tab: AppTab) {
        if (tab == AppTab.EXECUTE && !_uiState.value.isAssembled) {
            assembleCode()
        }
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun updateSourceCode(newCode: String) {
        stopAutoRun()
        _uiState.update { state ->
            val updatedFiles = state.editorFiles.map { file ->
                if (file.id == state.activeFileId) file.copy(content = newCode) else file
            }
            state.copy(
                editorFiles = updatedFiles,
                sourceCode = newCode,
                isAssembled = false
            )
        }
    }

    fun selectEditorFile(id: String) {
        stopAutoRun()
        _uiState.update { state ->
            val targetFile = state.editorFiles.find { it.id == id } ?: return@update state
            state.copy(
                activeFileId = id,
                sourceCode = targetFile.content,
                breakpoints = emptySet(),
                isAssembled = false
            )
        }
    }

    fun addEditorFile(name: String? = null, content: String = "") {
        stopAutoRun()
        _uiState.update { state ->
            val fileName = name ?: "file_${state.editorFiles.size + 1}.asm"
            val newFile = EditorFile(name = fileName, content = content)
            val updatedList = state.editorFiles + newFile
            state.copy(
                editorFiles = updatedList,
                activeFileId = newFile.id,
                sourceCode = content,
                breakpoints = emptySet(),
                isAssembled = false
            )
        }
    }

    fun closeEditorFile(id: String) {
        stopAutoRun()
        _uiState.update { state ->
            val filtered = state.editorFiles.filterNot { it.id == id }
            val updatedList = if (filtered.isEmpty()) {
                listOf(EditorFile(name = "program.asm", content = ""))
            } else {
                filtered
            }
            val newActiveId = if (state.activeFileId == id) {
                updatedList.last().id
            } else {
                state.activeFileId
            }
            val activeContent = updatedList.find { it.id == newActiveId }?.content ?: ""
            state.copy(
                editorFiles = updatedList,
                activeFileId = newActiveId,
                sourceCode = activeContent,
                breakpoints = emptySet(),
                isAssembled = false
            )
        }
    }

    fun updateActiveFileName(newName: String) {
        _uiState.update { state ->
            val updatedFiles = state.editorFiles.map { file ->
                if (file.id == state.activeFileId) file.copy(name = newName) else file
            }
            state.copy(editorFiles = updatedFiles)
        }
    }

    fun openFileInTab(fileName: String, content: String) {
        stopAutoRun()
        _uiState.update { state ->
            val active = state.activeFile
            if (active != null && active.content.isBlank() && active.name.startsWith("program")) {
                val updatedFiles = state.editorFiles.map {
                    if (it.id == active.id) it.copy(name = fileName, content = content) else it
                }
                state.copy(
                    editorFiles = updatedFiles,
                    sourceCode = content,
                    breakpoints = emptySet(),
                    isAssembled = false
                )
            } else {
                val newFile = EditorFile(name = fileName, content = content)
                val updatedList = state.editorFiles + newFile
                state.copy(
                    editorFiles = updatedList,
                    activeFileId = newFile.id,
                    sourceCode = content,
                    breakpoints = emptySet(),
                    isAssembled = false
                )
            }
        }
        assembleCode()
    }

    fun toggleShortcuts() {
        _uiState.update { it.copy(showShortcuts = !it.showShortcuts) }
    }

    fun toggleBreakpoint(lineNumber: Int) {
        _uiState.update { state ->
            val set = state.breakpoints.toMutableSet()
            if (set.contains(lineNumber)) set.remove(lineNumber) else set.add(lineNumber)
            state.copy(breakpoints = set)
        }
    }

    fun loadSampleProgram(program: SampleProgram) {
        stopAutoRun()
        val sampleFileName = program.title.lowercase().replace(Regex("[^a-z0-9_]"), "_").trim('_') + ".asm"
        _uiState.update { state ->
            val active = state.activeFile
            val updatedFiles = state.editorFiles.map { file ->
                if (file.id == state.activeFileId) file.copy(name = sampleFileName, content = program.code) else file
            }
            state.copy(
                editorFiles = updatedFiles,
                sourceCode = program.code,
                breakpoints = emptySet(),
                isAssembled = false,
                showExamplesDialog = false
            )
        }
        assembleCode()
    }

    fun assembleCode() {
        stopAutoRun()
        val code = _uiState.value.sourceCode
        val result = assembler.assemble(code)

        if (result.isSuccess) {
            simulator.loadProgram(result)
            syncSimulatorState(result)
            _uiState.update {
                it.copy(
                    isAssembled = true,
                    assemblyResult = result,
                    errorMessage = null
                )
            }
        } else {
            val errMsgs = result.errors.joinToString("\n") { "Line ${it.lineNumber}: ${it.message}" }
            _uiState.update {
                it.copy(
                    isAssembled = false,
                    assemblyResult = result,
                    errorMessage = errMsgs,
                    consoleLog = "[ASSEMBLY ERROR]\n$errMsgs"
                )
            }
        }
    }

    fun stepForward() {
        if (!_uiState.value.isAssembled) {
            assembleCode()
            if (!_uiState.value.isAssembled) return
        }

        if (_uiState.value.status == ProgramStatus.HALTED || _uiState.value.status == ProgramStatus.ERROR) {
            resetSimulator()
        }

        val breakpoints = _uiState.value.breakpoints
        simulator.executeNextStep(breakpoints)
        syncSimulatorState()
    }

    fun stepBack() {
        if (simulator.canStepBack()) {
            simulator.stepBack()
            syncSimulatorState()
        }
    }

    fun startAutoRun() {
        if (!_uiState.value.isAssembled) {
            assembleCode()
            if (!_uiState.value.isAssembled) return
        }

        if (_uiState.value.status == ProgramStatus.HALTED || _uiState.value.status == ProgramStatus.ERROR) {
            resetSimulator()
        }

        if (autoRunJob?.isActive == true) return

        autoRunJob = viewModelScope.launch {
            while (simulator.status == ProgramStatus.READY || simulator.status == ProgramStatus.RUNNING || simulator.status == ProgramStatus.PAUSED) {
                val breakpoints = _uiState.value.breakpoints
                val stepped = simulator.executeNextStep(breakpoints)
                syncSimulatorState()
                if (!stepped || simulator.status == ProgramStatus.HALTED || simulator.status == ProgramStatus.ERROR || simulator.status.name.startsWith("WAITING_INPUT")) {
                    break
                }
                delay(_uiState.value.runDelayMs)
            }
        }
    }

    fun pauseAutoRun() {
        stopAutoRun()
    }

    private fun stopAutoRun() {
        autoRunJob?.cancel()
        autoRunJob = null
    }

    fun resetSimulator() {
        stopAutoRun()
        simulator.reset()
        syncSimulatorState()
    }

    fun setRunDelay(delayMs: Long) {
        _uiState.update { it.copy(runDelayMs = delayMs) }
    }

    fun setMemoryViewAddress(address: Int) {
        _uiState.update { it.copy(memoryViewAddress = address) }
    }

    fun submitUserInputInt(value: Int) {
        simulator.provideUserInputInt(value)
        syncSimulatorState()
        startAutoRun()
    }

    fun submitUserInputFloat(value: Float) {
        simulator.provideUserInputFloat(value)
        syncSimulatorState()
        startAutoRun()
    }

    fun submitUserInputString(str: String) {
        simulator.provideUserInputString(str)
        syncSimulatorState()
        startAutoRun()
    }

    fun setRegisterValue(id: Int, value: Int) {
        simulator.setRegisterValue(id, value)
        syncSimulatorState()
        _uiState.update { it.copy(editingRegister = null) }
    }

    fun setFloatRegisterValue(id: Int, value: Float) {
        simulator.setFloatRegisterBits(id, value.toBits())
        syncSimulatorState()
        _uiState.update { it.copy(editingFloatRegister = null) }
    }

    fun setMemoryValue(address: Int, value: Int) {
        simulator.memory.writeWord(address, value)
        syncSimulatorState()
        _uiState.update { it.copy(editingMemoryAddress = null) }
    }

    fun setShowExamplesDialog(show: Boolean) {
        _uiState.update { it.copy(showExamplesDialog = show) }
    }

    fun setEditingRegister(register: Register?) {
        _uiState.update { it.copy(editingRegister = register) }
    }

    fun setEditingFloatRegister(register: FloatRegister?) {
        _uiState.update { it.copy(editingFloatRegister = register) }
    }

    fun setEditingMemoryAddress(address: Int?) {
        _uiState.update { it.copy(editingMemoryAddress = address) }
    }

    fun getMemoryEntries(): List<Pair<Int, Int>> {
        return simulator.memory.getMemoryEntriesSorted(_uiState.value.memoryViewAddress, 32)
    }

    private fun syncSimulatorState(assemblyResult: AssemblyResult? = _uiState.value.assemblyResult) {
        _uiState.update {
            it.copy(
                pc = simulator.pc,
                registers = simulator.registers.copyOf(),
                floatRegisters = simulator.floatRegisters.copyOf(),
                status = simulator.status,
                stepCount = simulator.stepCount,
                cycleCount = simulator.cycleCount,
                lastModifiedRegister = simulator.lastModifiedRegister,
                lastModifiedFloatRegister = simulator.lastModifiedFloatRegister,
                consoleLog = simulator.consoleLog.toString(),
                canStepBack = simulator.canStepBack(),
                errorMessage = simulator.errorMessage,
                assemblyResult = assemblyResult
            )
        }
    }
}
