@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.motovault.app.ui

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.*
import kotlinx.serialization.json.Json
import platform.Foundation.*
import platform.UIKit.*
import platform.UserNotifications.*
import kotlin.coroutines.resume

private val center get() = UNUserNotificationCenter.currentNotificationCenter()
private val defaults get() = NSUserDefaults.standardUserDefaults
private val reminderLock = Mutex()
private fun enabled() = defaults.boolForKey("renewal.enabled")
private fun plans() = runCatching { Json.decodeFromString<List<RenewalReminder>>(defaults.stringForKey("renewal.plans") ?: "[]") }.getOrDefault(emptyList())
private fun consumed() = (defaults.stringArrayForKey("renewal.consumed")?.filterIsInstance<String>() ?: emptyList()).toSet()
private fun notificationSettingsUrl(): String {
    val version = UIDevice.currentDevice.systemVersion.split('.').map { it.toIntOrNull() ?: 0 }
    val major = version.getOrElse(0) { 0 }
    val minor = version.getOrElse(1) { 0 }
    return if (major > 15 || major == 15 && minor >= 4) UIApplicationOpenNotificationSettingsURLString else UIApplicationOpenSettingsURLString
}
private suspend fun permitted(): Boolean = suspendCancellableCoroutine { continuation ->
    center.getNotificationSettingsWithCompletionHandler { settings -> continuation.resume(settings?.authorizationStatus == UNAuthorizationStatusAuthorized || settings?.authorizationStatus == UNAuthorizationStatusProvisional) }
}
private suspend fun pending(): Set<String> = suspendCancellableCoroutine { continuation ->
    center.getPendingNotificationRequestsWithCompletionHandler { requests -> continuation.resume(requests?.filterIsInstance<UNNotificationRequest>()?.map { it.identifier }?.toSet() ?: emptySet()) }
}
private suspend fun delivered(): Set<String> = suspendCancellableCoroutine { continuation ->
    center.getDeliveredNotificationsWithCompletionHandler { notifications ->
        continuation.resume(notifications?.filterIsInstance<UNNotification>()?.map { it.request.identifier }?.filter { it.startsWith("renewal:") || it.startsWith("service:") }?.toSet() ?: emptySet())
    }
}
private suspend fun add(plan: RenewalReminder, target: ReminderTarget, now: LocalDateTime) {
    val content = UNMutableNotificationContent().apply {
        setTitle(reminderHeading(plan))
        setBody(reminderMessage(plan, target))
        setUserInfo(mapOf("motovault.reminder" to reminderPayload(plan)))
        setSound(UNNotificationSound.defaultSound())
    }
    val date = LocalDate.parse(plan.date)
    val trigger: UNNotificationTrigger = if (plan.localDateTime() <= now) UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(1.0, false)
    else UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(NSDateComponents().apply {
        year = date.year.toLong(); month = date.monthNumber.toLong(); day = date.dayOfMonth.toLong()
        hour = (plan.minuteOfDay / 60).toLong(); minute = (plan.minuteOfDay % 60).toLong(); second = 0
        // No fixed timezone: calendar notifications follow local device time.
    }, false)
    suspendCancellableCoroutine<Unit> { continuation ->
        center.addNotificationRequest(UNNotificationRequest.requestWithIdentifier(plan.key, content, trigger)) { error ->
            if (error == null) continuation.resume(Unit) else continuation.resumeWith(Result.failure(IllegalStateException(error.localizedDescription)))
        }
    }
}

internal actual suspend fun reconcileRenewalReminders(documents: List<VaultDocument>) {
    reminderLock.withLock {
        val state = VaultState.restore()
        val storage = renewalJournalStorage()
        var journal = storage.read() ?: RenewalJournal(plans(), consumed())
        val old = journal.requests
        val pending = pending()
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        // Accepted requests which elapsed and left the OS queue must never be reposted on relaunch.
        journal = journal.recover(now, pending, delivered())
        storage.write(journal)
        val next = if (enabled() && permitted()) reminderPlan(state, now, old, journal.consumed) else emptyList()
        val obsolete = (pending.filter { it.startsWith("renewal:") || it.startsWith("service:") } + old.map { it.key }).filter { key -> next.none { it.key == key } }
        center.removePendingNotificationRequestsWithIdentifiers(obsolete)
        center.removeDeliveredNotificationsWithIdentifiers(obsolete)
        journal = journal.retain(next.map { it.key }.toSet())
        storage.write(journal)
        var failure: Exception? = null
        next.forEach { plan ->
            try {
                // Keep elapsed pending requests, including a same-day catch-up, without resetting their delivery.
                if (!(plan.key in pending && plan.localDateTime() <= now)) {
                    // The journal commit must finish before the OS can accept an immediate request.
                    journal = journal.reserve(plan)
                    storage.write(journal)
                    try { add(plan, requireNotNull(reminderTarget(state, plan)), now) }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (error: Exception) {
                        journal = journal.rejected(plan.key)
                        storage.write(journal)
                        throw error
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { failure = error }
        }
        failure?.let { throw it }
    }
}

@Composable internal actual fun rememberReminderAccess(): ReminderAccess {
    val scope = rememberCoroutineScope()
    var active by remember { mutableStateOf(false) }
    var allowed by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(true) }
    var optedIn by remember { mutableStateOf(enabled()) }
    var scheduled by remember { mutableStateOf<List<RenewalReminder>>(emptyList()) }
    val owner = LocalLifecycleOwner.current
    fun refresh() { scope.launch {
        busy = true
        allowed = permitted(); active = enabled() && allowed
        optedIn = enabled()
        failed = runCatching {
            reconcileRenewalReminders(VaultState.restore().documents)
            val queued = pending()
            scheduled = renewalJournalStorage().read()?.requests.orEmpty().filter { it.key in queued }
        }.isFailure
        busy = false
    } }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh() }
        owner.lifecycle.addObserver(observer); refresh()
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return ReminderAccess(optedIn, allowed, busy, failed,
        request = {
            scope.launch {
                val granted = suspendCancellableCoroutine<Boolean> { continuation -> center.requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { granted, _ -> continuation.resume(granted) } }
                defaults.setBool(granted, forKey = "renewal.enabled"); refresh()
            }
        },
        disable = { defaults.setBool(false, forKey = "renewal.enabled"); refresh() },
        refresh = ::refresh,
        settings = { NSURL.URLWithString(notificationSettingsUrl())?.let { UIApplication.sharedApplication.openURL(it, emptyMap<Any?, Any?>(), null) } },
        plans = scheduled
    )
}
