package com.example.meterdemo

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import com.example.meterdemo.localization.AppLanguageManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.meterdemo.ui.MeterDemoApp
import com.example.meterdemo.viewmodel.MainViewModel

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        AppLanguageManager(this).applyStoredLanguage()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme {
                val viewModel: MainViewModel = viewModel()
                MeterDemoApp(viewModel = viewModel)
            }
        }
    }
}
