package com.motovault.app

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import com.motovault.app.ui.*

/** Debug-only native-delivery fixture. Never opens or changes the owner database or reminder preferences. */
class RenewalFixtureActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = getSharedPreferences("renewal_debug_fixture", MODE_PRIVATE)
        val old = preferences.getString("key", null)
        if (intent.getStringExtra("mode") == "replay") {
            old?.let { sendBroadcast(Intent(this, RenewalFixtureReceiver::class.java)
                .setData(Uri.parse("motovault://debug-renewal/" + Uri.encode(it)))) }
            finish()
            return
        }
        val alarms = getSystemService(AlarmManager::class.java)
        old?.let { alarms.cancel(fixturePending(this, it)); getSystemService(NotificationManager::class.java).cancel(it, 0) }
        if (intent.getStringExtra("mode") == "cancel") { preferences.edit().clear().commit(); finish(); return }
        val key = "renewal:debug-fixture:${System.currentTimeMillis()}:expiry"
        preferences.edit().putString("key", key).putBoolean("consumed", false).putLong("deliveryCount", 0).commit()
        val delay = intent.getLongExtra("delay_ms", 2000).coerceIn(1000, 600000)
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delay, fixturePending(this, key))
        finish()
    }
}

private fun fixturePending(context: Context, key: String) = PendingIntent.getBroadcast(context, 0,
    Intent(context, RenewalFixtureReceiver::class.java).setData(Uri.parse("motovault://debug-renewal/" + Uri.encode(key))), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

class RenewalFixtureReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val preferences = context.getSharedPreferences("renewal_debug_fixture", Context.MODE_PRIVATE)
        val key = intent.data?.lastPathSegment ?: return
        if (key != preferences.getString("key", null) || preferences.getBoolean("consumed", false)) return
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (!manager.areNotificationsEnabled()) return
        val date = today().toString()
        val document = VaultDocument("SAMPLE TEST ONLY tax token", "SYNTHETIC", "Synthetic fixture", date, id = "debug-fixture")
        val plan = RenewalReminder(key, document.id, date, date, "expiry")
        preferences.edit().putBoolean("consumed", true).putLong("deliveryCount", preferences.getLong("deliveryCount", 0) + 1).commit()
        manager.notify(key, 0, renewalNotification(context, plan, document))
    }
}
