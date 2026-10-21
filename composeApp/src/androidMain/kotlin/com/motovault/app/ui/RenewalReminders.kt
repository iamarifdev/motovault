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
    if (Build.VERSION.SDK_INT >= 26) vaultContext.getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL, "Document renewals", NotificationManager.IMPORTANCE_DEFAULT).apply { description = "60-day warnings and expiry-day reminders" })
}

internal fun renewalNotification(context: Context, plan: RenewalReminder, document: VaultDocument): Notification {
    channel()
    val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(context, CHANNEL) else Notification.Builder(context)
    return builder.setSmallIcon(com.motovault.app.R.drawable.motovault_icon).setContentTitle("Document renewal")
        .setContentText(if (plan.kind == "expiry") "${document.title} expires today." else "${document.title} expires on ${document.expiry}.")
        .setContentIntent(open).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE).build()
}

internal actual suspend fun reconcileRenewalReminders(documents: List<VaultDocument>) = withContext(Dispatchers.IO) {
    reminderLock.withLock {
        val currentDocuments = VaultState.restore().documents
        channel()
        val old = plans()
        val next = if (enabled() && permitted()) renewalPlan(currentDocuments, Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()), old, consumed()) else emptyList()
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
                    val document = VaultState.restore().documents.firstOrNull { it.id == plan.documentId && it.expiry == plan.expiry } ?: return@withLock
                    if (!enabled() || !permitted() || key in consumed() || LocalDate.parse(plan.expiry) < today()) return@withLock
                    // Wall-clock movement must not make an old alarm deliver before local 09:00.
                    val due = plan.localDateTime().toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                    if (due > System.currentTimeMillis()) {
                        vaultContext.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, due, pending(key))
                        return@withLock
                    }
                    val notification = renewalNotification(context, plan, document)
                    // At-most-once delivery across process death. A crash between this commit and notify can miss an alert.
                    check(preferences.edit().putStringSet("consumed", consumed() + key).commit())
                    context.getSystemService(NotificationManager::class.java).notify(key, 0, notification)
                }
            } catch (_: Exception) { /* Leave saved owner records intact; retry on the next reconciliation. */ }
            finally { result.finish() }
        }
    }
}

@Composable internal actual fun RenewalReminderControl() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var refresh by remember { mutableIntStateOf(0) }
    var failed by remember { mutableStateOf(false) }
    val owner = LocalLifecycleOwner.current
    fun reconcile() { scope.launch { failed = runCatching { reconcileRenewalReminders(VaultState.restore().documents) }.isFailure; refresh++ } }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> preferences.edit().putBoolean("enabled", granted).apply(); reconcile() }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) reconcile() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val active = remember(refresh) { enabled() && permitted() }
    Panel {
        DocumentNote("নবায়নের রিমাইন্ডার")
        DocumentCopy("মেয়াদ শেষের ৬০ দিন আগে এবং মেয়াদ শেষের দিন সকাল ৯টার দিকে জানানো হবে। ৬০ দিনের কম মেয়াদ থাকা নতুন কাগজ যোগ করলে পরের সকাল ৯টায় একবার জানানো হবে।")
        DocumentSecondary(if (active) "রিমাইন্ডার বন্ধ করুন" else "রিমাইন্ডার চালু করুন", onClick = {
            if (active) { preferences.edit().putBoolean("enabled", false).apply(); reconcile() }
            else if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            else { preferences.edit().putBoolean("enabled", true).apply(); reconcile() }
        })
        if (!permitted()) {
            DocumentNote("ডিভাইসের সেটিংসে নোটিফিকেশন বন্ধ আছে। অ্যাপের ভেতরে নবায়নের সতর্কতা দেখা যাবে।")
            DocumentLink("নোটিফিকেশন সেটিংস খুলুন") { context.startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)) }
        }
        if (failed) DocumentNote("রিমাইন্ডার আপডেট করা যায়নি। কাগজপত্র সুরক্ষিত আছে। আবার চেষ্টা করুন।")
    }
}
