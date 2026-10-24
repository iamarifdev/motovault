package com.motovault.app

import android.os.Bundle
import android.content.Intent
import com.motovault.app.ui.ReminderNavigation
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.motovault.app.ui.vaultContext = applicationContext
        intent?.getStringExtra("motovault.reminder")?.let(ReminderNavigation::open)
        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("motovault.reminder")?.let(ReminderNavigation::open)
    }
}
