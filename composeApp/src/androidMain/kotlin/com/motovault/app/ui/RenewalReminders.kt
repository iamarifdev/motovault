@file:JvmName("AndroidRenewalRemindersKt")
package com.motovault.app.ui

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.motovault.app.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

private val reminderLock = Mutex()
private val reminderScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
private const val CHANNEL = "document_renewals"
private val preferences get() = vaultContext.getSharedPreferences("renewal_reminders", Context.MODE_PRIVATE)
private fun plans() = runCatching { Json.decodeFromString<List<RenewalReminder>>(preferences.getString("plans", "[]")!!) }.getOrDefault(emptyList())
private fun consumed() = preferences.getStringSet("consumed", emptySet())!!.toSet()
private fun permitted(): Boolean {
    val manager = vaultContext.getSystemService(NotificationManager::class.java)
    return (Build.VERSION.SDK_INT < 33 || vaultContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) && manager.areNotificationsEnabled() &&
        (Build.VERSION.SDK_INT < 26 || manager.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE)
}
private fun enabled() = preferences.getBoolean("enabled", false)
private fun intent(key: String) = Intent(vaultContext, RenewalAlarmReceiver::class.java).setAction("com.motovault.RENEWAL").setData(Uri.parse("motovault://renewal/" + Uri.encode(key)))
private fun pending(key: String) = PendingIntent.getBroadcast(vaultContext, 0, intent(key), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
private fun cancel(plan: RenewalReminder) {
    vaultContext.getSystemService(AlarmManager::class.java).cancel(pending(plan.key))
    vaultContext.getSystemService(NotificationManager::class.java).cancel(plan.key, 0)
}
private fun channel() {
    if (Build.VERSION.SDK_INT >= 26) vaultContext.getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL, "Document renewals", NotificationManager.IMPORTANCE_DEFAULT).apply { description = "Document warnings and expiry-day reminders" })
}

internal fun renewalNotification(context: Context, plan: RenewalReminder, target: ReminderTarget): Notification {
    channel()
    val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java)
        .setData(Uri.parse("motovault://reminder/" + Uri.encode(plan.key)))
        .putExtra("motovault.reminder", reminderPayload(plan))
        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(context, CHANNEL) else Notification.Builder(context)
    return builder.setSmallIcon(com.motovault.app.R.drawable.motovault_icon).setContentTitle(reminderHeading(plan))
        .setContentText(reminderMessage(plan, target))
        .setContentIntent(open).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE).build()
}

internal actual suspend fun reconcileRenewalReminders(documents: List<VaultDocument>) = withContext(Dispatchers.IO) {
    reminderLock.withLock {
        val state = VaultState.restore()
        channel()
        val old = plans()
        val next = if (enabled() && permitted()) reminderPlan(state, Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()), old, consumed()) else emptyList()
        old.filter { previous -> next.none { it.key == previous.key } }.forEach(::cancel)
        // Persist the allow-list before scheduling so old broadcasts cannot deliver after edit/delete.
        check(preferences.edit().putString("plans", Json.encodeToString(next)).commit())
        next.forEach { plan ->
            val due = plan.localDateTime().toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
            vaultContext.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, maxOf(due, System.currentTimeMillis() + 1000), pending(plan.key))
        }
    }
}

/** Receivers initialize application storage even when no Activity has ever been created in this process. */
class RenewalApplication : Application() {
    override fun onCreate() { super.onCreate(); vaultContext = applicationContext }
}

class RenewalAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        vaultContext = context.applicationContext
        val result = goAsync()
        reminderScope.launch {
            try {
                val state = VaultState.restore()
                if (intent.action != "com.motovault.RENEWAL") {
                    reconcileRenewalReminders(state.documents)
                    return@launch
                }
                reminderLock.withLock {
                    val key = intent.data?.lastPathSegment ?: return@withLock
                    val plan = plans().firstOrNull { it.key == key } ?: return@withLock
                    val committed = VaultState.restore()
                    val target = reminderTarget(committed, plan) ?: return@withLock
                    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    if (!enabled() || !permitted() || key in consumed() || reminderPlan(committed, now, listOf(plan), emptySet()).none { it.key == key }) return@withLock
                    // Wall-clock movement must not make an old alarm deliver before local 09:00.
                    val due = plan.localDateTime().toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                    if (due > System.currentTimeMillis()) {
                        vaultContext.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, due, pending(key))
                        return@withLock
                    }
                    val notification = renewalNotification(context, plan, target)
                    // At-most-once delivery across process death. A crash between this commit and notify can miss an alert.
                    check(preferences.edit().putStringSet("consumed", consumed() + key).commit())
                    context.getSystemService(NotificationManager::class.java).notify(key, 0, notification)
                }
            } catch (_: Exception) { /* Leave saved owner records intact; retry on the next reconciliation. */ }
            finally { result.finish() }
        }
    }
}

@Composable internal actual fun rememberReminderAccess(): ReminderAccess {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var refresh by remember { mutableIntStateOf(0) }
    var failed by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(true) }
    val owner = LocalLifecycleOwner.current
    fun reconcile() { scope.launch {
        busy = true
        failed = runCatching { reconcileRenewalReminders(VaultState.restore().documents) }.isFailure
        busy = false
        refresh++
    } }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> preferences.edit().putBoolean("enabled", granted).apply(); reconcile() }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) reconcile() }
        owner.lifecycle.addObserver(observer)
        reconcile()
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val optedIn = remember(refresh) { enabled() }
    val allowed = remember(refresh) { permitted() }
    return ReminderAccess(optedIn, allowed, busy, failed,
        request = {
            if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            else { preferences.edit().putBoolean("enabled", true).apply(); reconcile() }
        },
        disable = { preferences.edit().putBoolean("enabled", false).apply(); reconcile() },
        refresh = ::reconcile,
        settings = { context.startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)) },
        plans = remember(refresh) { plans().filterNot { it.key in consumed() } }
    )
}
