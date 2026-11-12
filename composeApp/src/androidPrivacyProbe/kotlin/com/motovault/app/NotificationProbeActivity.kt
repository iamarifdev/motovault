package com.motovault.app

import android.app.NotificationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.motovault.app.ui.AppLanguage
import com.motovault.app.ui.Digits
import com.motovault.app.ui.ReminderTarget
import com.motovault.app.ui.RenewalReminder
import com.motovault.app.ui.renewalNotification
import com.motovault.app.ui.today
import com.motovault.app.ui.vaultContext
import java.util.UUID

/** Posts the production renewal notification once so native delivery/channel behaviour can be observed. */
class NotificationProbeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        check(packageName.endsWith(".privacyprobe"))
        vaultContext = applicationContext
        val language = if (intent.getStringExtra("language") == AppLanguage.EN.code) AppLanguage.EN else AppLanguage.BN
        val digits = if (intent.getStringExtra("digits") == "en") Digits.EN else Digits.BN
        val expiry = today().toString()
        val plan = RenewalReminder(
            key = "probe:" + UUID.randomUUID(),
            documentId = "probe-document",
            expiry = expiry,
            date = expiry,
            kind = "expiry",
            targetType = "document"
        )
        val target = ReminderTarget(
            type = "document",
            id = "probe-document",
            title = "Synthetic document",
            digits = digits,
            language = language
        )
        getSystemService(NotificationManager::class.java)
            .notify("motovault-notification-probe", 1, renewalNotification(this, plan, target))
        finish()
    }
}
