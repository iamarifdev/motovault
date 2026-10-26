package com.motovault.app

import android.os.Bundle
import android.content.Intent
import com.motovault.app.ui.ReminderNavigation
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import android.view.WindowManager
import com.motovault.app.domain.privacy.currentMainActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentMainActivity = this
        enableEdgeToEdge()
        com.motovault.app.ui.vaultContext = applicationContext
        com.motovault.app.ui.cleanDocumentScratchOnProcessStart()
        intent?.getStringExtra("motovault.reminder")?.let(ReminderNavigation::open)
        setContent {
            App()
        }
    }

    override fun onResume() {
        super.onResume()
        currentMainActivity = this
    }

    override fun onDestroy() {
        if (currentMainActivity == this) currentMainActivity = null
        super.onDestroy()
    }

    fun setSecureDisplay(secure: Boolean) {
        if (secure) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("motovault.reminder")?.let(ReminderNavigation::open)
    }
}
