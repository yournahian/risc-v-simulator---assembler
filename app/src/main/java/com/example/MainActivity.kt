package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.riscv.ui.RiscvMainScreen
import com.example.riscv.ui.getFileNameFromUri
import com.example.riscv.viewmodel.SimulatorViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

  private val viewModel: SimulatorViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    handleIncomingIntent(intent)

    setContent {
      MyApplicationTheme(darkTheme = true) {
        RiscvMainScreen(viewModel = viewModel)
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleIncomingIntent(intent)
  }

  private fun handleIncomingIntent(intent: Intent?) {
    val uri: Uri? = intent?.data
    if (uri != null) {
      try {
        contentResolver.openInputStream(uri)?.use { inputStream ->
          val content = inputStream.bufferedReader().readText()
          val name = getFileNameFromUri(this, uri)
          viewModel.openFileInTab(name, content)
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }
}

