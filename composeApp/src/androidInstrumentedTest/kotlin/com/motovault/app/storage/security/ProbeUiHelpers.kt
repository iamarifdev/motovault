package com.motovault.app.storage.security

import com.motovault.app.storage.backupSnapshot
import com.motovault.app.storage.backupRideDisplayName

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import android.os.SystemClock
import android.content.Intent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.MotionEvent
import java.io.File
import java.security.KeyStore
import java.util.UUID
import com.motovault.app.ui.*
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.runBlocking
import com.motovault.app.domain.backup.*
import com.motovault.app.domain.model.BaselineCertainty
import com.motovault.app.domain.model.ComponentRule
import kotlin.experimental.xor
import androidx.room.Room
import com.motovault.app.storage.VaultDatabase
import com.motovault.app.storage.buildVaultDatabase
import com.motovault.app.storage.BackupRestoreStaging
import com.motovault.app.storage.StagedRestore
import com.motovault.app.storage.RecoverySnapshotStore
import com.motovault.app.storage.CoordinatedRecovery
import com.motovault.app.storage.FuelEntryAdapter
import com.motovault.app.storage.VaultSession
import com.motovault.app.application.fuel.FuelEditRequest
import kotlinx.datetime.LocalDate
import com.motovault.app.application.PreparedVaultChange
import com.motovault.app.application.VaultCoordinator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import java.util.Locale
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.fuel_entry_count
import motovault.composeapp.generated.resources.fuel_title
import motovault.composeapp.generated.resources.history_record_count

internal fun NativeAssetInstrumentation.nodeWithText(text: String): AccessibilityNodeInfo? {
        fun find(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null) return null
            node.refresh()
            if (node.text?.toString() == text || node.contentDescription?.toString() == text) return node
            val indices = if (text == "আনডু") (node.childCount - 1 downTo 0) else (0 until node.childCount)
            for (index in indices) find(node.getChild(index))?.let { return it }
            return null
        }
        return find(uiAutomation.rootInActiveWindow)
    }

internal fun NativeAssetInstrumentation.waitForAnyText(vararg texts: String): AccessibilityNodeInfo {
        repeat(80) {
            for (text in texts) nodeWithText(text)?.let { return it }
            SystemClock.sleep(250)
        }
        val root = uiAutomation.rootInActiveWindow
        if (root?.packageName?.toString() != targetContext.packageName) {
            error("Synthetic UI is not active; foreground package=${root?.packageName}")
        }
        fun describe(node: AccessibilityNodeInfo?, depth: Int): List<String> {
            if (node == null || depth > 20) return emptyList()
            val current = listOfNotNull(node.text?.toString(), node.contentDescription?.toString())
            return current + (0 until node.childCount).flatMap { describe(node.getChild(it), depth + 1) }
        }
        val visible = describe(root, 0)
        error("Synthetic UI label not found: ${texts.joinToString("/")}; root=$root; matching=$visible")
    }

internal fun NativeAssetInstrumentation.waitForText(text: String): AccessibilityNodeInfo {
        repeat(80) {
            nodeWithText(text)?.let { return it }
            SystemClock.sleep(250)
        }
        val root = uiAutomation.rootInActiveWindow
        if (root?.packageName?.toString() != targetContext.packageName) {
            error("Synthetic UI is not active; foreground package=${root?.packageName}")
        }
        fun describe(node: AccessibilityNodeInfo?, depth: Int): List<String> {
            if (node == null || depth > 20) return emptyList()
            val current = listOfNotNull(node.text?.toString(), node.contentDescription?.toString())
            return current + (0 until node.childCount).flatMap { describe(node.getChild(it), depth + 1) }
        }
        val visible = describe(root, 0)
        error("Synthetic UI label not found: $text; root=$root; matching=$visible")
    }

internal fun NativeAssetInstrumentation.clickText(text: String) {
        var node: AccessibilityNodeInfo? = waitForText(text)
        while (node != null && !node.isClickable) node = node.parent
        check(node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) { "Synthetic UI control not clickable: $text" }
    }

internal fun NativeAssetInstrumentation.clickAnyText(vararg texts: String) {
        val label = waitForAnyText(*texts)
        var node: AccessibilityNodeInfo? = label
        // Compose may expose the toast label as a non-clickable text node whose
        // parent performs the action only through ACTION_CLICK, so try each level.
        while (node != null) {
            if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return
            node = node.parent
        }
        // Last resort for visible Compose controls without an exposed click action.
        val bounds = Rect()
        label.getBoundsInScreen(bounds)
        if (bounds.width() > 0 && bounds.height() > 0) {
            val now = SystemClock.uptimeMillis()
            val x = bounds.exactCenterX()
            val y = bounds.exactCenterY()
            val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0)
            val up = MotionEvent.obtain(now, now + 40, MotionEvent.ACTION_UP, x, y, 0)
            if (uiAutomation.injectInputEvent(down, true) && uiAutomation.injectInputEvent(up, true)) {
                down.recycle(); up.recycle()
                SystemClock.sleep(300)
                return
            }
            down.recycle(); up.recycle()
        }
        error("Synthetic UI control not clickable: ${texts.joinToString("/")}")
    }

internal fun NativeAssetInstrumentation.tryClickAnyText(timeoutMs: Long, vararg texts: String): Boolean {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            for (text in texts) {
                val found = nodeWithText(text) ?: continue
                var node: AccessibilityNodeInfo? = found
                while (node != null) {
                    if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
                    node = node.parent
                }
                val bounds = Rect()
                found.getBoundsInScreen(bounds)
                if (bounds.width() > 0 && bounds.height() > 0) {
                    val now = SystemClock.uptimeMillis()
                    val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, bounds.exactCenterX(), bounds.exactCenterY(), 0)
                    val up = MotionEvent.obtain(now, now + 40, MotionEvent.ACTION_UP, bounds.exactCenterX(), bounds.exactCenterY(), 0)
                    val sent = uiAutomation.injectInputEvent(down, true) && uiAutomation.injectInputEvent(up, true)
                    down.recycle(); up.recycle()
                    if (sent) { SystemClock.sleep(300); return true }
                }
            }
            SystemClock.sleep(150)
        }
        return false
    }

internal fun NativeAssetInstrumentation.setEditable(description: String, value: String) {
        fun find(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null) return null
            if (node.isEditable && node.contentDescription?.toString()?.startsWith(description) == true) return node
            for (index in 0 until node.childCount) find(node.getChild(index))?.let { return it }
            return null
        }
        var target: AccessibilityNodeInfo? = null
        for (attempt in 0 until 80) {
            target = find(uiAutomation.rootInActiveWindow)
            if (target != null) break
            SystemClock.sleep(250)
        }
        val field = requireNotNull(target) { "Synthetic password field not found: $description" }
        field.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.id)
        val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value) }
        check(field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args))
        SystemClock.sleep(500)
    }

internal fun NativeAssetInstrumentation.scrollSheetForward() {
        fun scrollables(node: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> {
            if (node == null) return emptyList()
            return (0 until node.childCount).flatMap { scrollables(node.getChild(it)) } + if (node.isScrollable) listOf(node) else emptyList()
        }
        val scrollable = scrollables(uiAutomation.rootInActiveWindow).firstOrNull() ?: error("Synthetic sheet is not scrollable")
        check(scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD))
        SystemClock.sleep(500)
    }

internal fun NativeAssetInstrumentation.clickLastText(text: String) {
        fun collect(node: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> {
            if (node == null) return emptyList()
            node.refresh()
            val found = if (node.text?.toString() == text || node.contentDescription?.toString() == text) listOf(node) else emptyList()
            return found + (0 until node.childCount).flatMap { collect(node.getChild(it)) }
        }
        fun lastClickable(): AccessibilityNodeInfo? = collect(uiAutomation.rootInActiveWindow).asReversed().firstNotNullOfOrNull { found ->
            var node: AccessibilityNodeInfo? = found
            while (node != null && !node.isClickable) node = node.parent
            node
        }
        val initial = requireNotNull(lastClickable()) { "Synthetic control not clickable: $text" }
        initial.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.id)
        SystemClock.sleep(500)
        check(requireNotNull(lastClickable()).performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }


internal fun NativeAssetInstrumentation.expectFailure(block: () -> Unit) {
        var failed = false
        try { block() } catch (_: Exception) { failed = true }
        check(failed) { "Expected failure was accepted" }
    }
