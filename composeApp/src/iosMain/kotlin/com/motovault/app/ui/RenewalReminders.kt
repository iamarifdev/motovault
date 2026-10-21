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
private suspend fun permitted(): Boolean = suspendCancellableCoroutine { continuation ->
    center.getNotificationSettingsWithCompletionHandler { settings -> continuation.resume(settings?.authorizationStatus == UNAuthorizationStatusAuthorized || settings?.authorizationStatus == UNAuthorizationStatusProvisional) }
}
private suspend fun pending(): Set<String> = suspendCancellableCoroutine { continuation ->
    center.getPendingNotificationRequestsWithCompletionHandler { requests -> continuation.resume(requests?.filterIsInstance<UNNotificationRequest>()?.map { it.identifier }?.toSet() ?: emptySet()) }
}
private suspend fun delivered(): Set<String> = suspendCancellableCoroutine { continuation ->
    center.getDeliveredNotificationsWithCompletionHandler { notifications ->
        continuation.resume(notifications?.filterIsInstance<UNNotification>()?.map { it.request.identifier }?.filter { it.startsWith("renewal:") }?.toSet() ?: emptySet())
    }
}
private suspend fun add(plan: RenewalReminder, title: String, now: LocalDateTime) {
    val content = UNMutableNotificationContent().apply {
        setTitle("Document renewal")
        setBody(if (plan.kind == "expiry") "$title expires today." else "$title expires on ${plan.expiry}.")
        setSound(UNNotificationSound.defaultSound())
    }
    val date = LocalDate.parse(plan.date)
    val trigger: UNNotificationTrigger = if (plan.localDateTime() <= now) UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(1.0, false)
    else UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(NSDateComponents().apply {
        year = date.year.toLong(); month = date.monthNumber.toLong(); day = date.dayOfMonth.toLong(); hour = 9; minute = 0; second = 0
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
        val currentDocuments = VaultState.restore().documents
        val storage = renewalJournalStorage()
        var journal = storage.read() ?: RenewalJournal(plans(), consumed())
        val old = journal.requests
        val pending = pending()
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        // Accepted requests which elapsed and left the OS queue must never be reposted on relaunch.
        journal = journal.recover(now, pending, delivered())
        storage.write(journal)
        val next = if (enabled() && permitted()) renewalPlan(currentDocuments, now, old, journal.consumed) else emptyList()
        val obsolete = (pending.filter { it.startsWith("renewal:") } + old.map { it.key }).filter { key -> next.none { it.key == key } }
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
                    try { add(plan, currentDocuments.first { it.id == plan.documentId }.title, now) }
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

@Composable internal actual fun RenewalReminderControl() {
    val scope = rememberCoroutineScope()
    var active by remember { mutableStateOf(false) }
    var allowed by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    val owner = LocalLifecycleOwner.current
    fun refresh() { scope.launch {
        allowed = permitted(); active = enabled() && allowed
        failed = runCatching { reconcileRenewalReminders(VaultState.restore().documents) }.isFailure
    } }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh() }
        owner.lifecycle.addObserver(observer); refresh()
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    Panel {
        DocumentNote("নবায়নের রিমাইন্ডার")
        DocumentCopy("মেয়াদ শেষের ৬০ দিন আগে এবং মেয়াদ শেষের দিন সকাল ৯টার দিকে জানানো হবে। ৬০ দিনের কম মেয়াদ থাকা নতুন কাগজ যোগ করলে পরের সকাল ৯টায় একবার জানানো হবে।")
        DocumentSecondary(if (active) "রিমাইন্ডার বন্ধ করুন" else "রিমাইন্ডার চালু করুন", onClick = {
            if (active) { defaults.setBool(false, forKey = "renewal.enabled"); refresh() }
            else scope.launch {
                val granted = suspendCancellableCoroutine<Boolean> { continuation -> center.requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { granted, _ -> continuation.resume(granted) } }
                defaults.setBool(granted, forKey = "renewal.enabled"); refresh()
            }
        })
        if (!allowed) {
            DocumentNote("ডিভাইসের সেটিংসে নোটিফিকেশন বন্ধ আছে। অ্যাপের ভেতরে নবায়নের সতর্কতা দেখা যাবে।")
            DocumentLink("নোটিফিকেশন সেটিংস খুলুন") { NSURL.URLWithString(UIApplicationOpenSettingsURLString)?.let { UIApplication.sharedApplication.openURL(it, emptyMap<Any?, Any?>(), null) } }
        }
        if (failed) DocumentNote("রিমাইন্ডার আপডেট করা যায়নি। কাগজপত্র সুরক্ষিত আছে। আবার চেষ্টা করুন।")
    }
}
