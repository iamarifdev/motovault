package com.motovault.app.storage.security

import android.content.Intent
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.room.Room
import com.motovault.app.storage.VaultDatabase
import com.motovault.app.storage.buildVaultDatabase
import java.io.File
import kotlinx.coroutines.runBlocking

internal fun NativeAssetInstrumentation.checkCostRuntime(): String {
    if (costPriceOnly) return checkCostPriceRuntime()
    val root = File(targetContext.filesDir, "synthetic-ledger-${System.nanoTime()}").apply { mkdirs() }
    val storage = try {
        runBlocking { verifyLedgerStorageProbe { buildVaultDatabase(Room.databaseBuilder<VaultDatabase>(targetContext, File(root, "ledger.db").absolutePath)) } }
    } finally { root.deleteRecursively() }
    val activity = startActivitySync(Intent().setClassName(targetContext.packageName, "com.motovault.app.CostFixtureActivity")
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    try {
        waitForText("রেকর্ডের খরচ")
        waitForText("৳২,২৫০.৫০")
        costShot("p06-android-bn-bn")
        clickText("English")
        waitForText("Recorded costs")
        waitForText("৳২,২৫০.৫০")
        costShot("p06-android-en-bn")
        clickText("Digits")
        waitForText("৳2,250.50")
        costShot("p06-android-en-en")
        clickText("বাংলা")
        waitForText("রেকর্ডের খরচ")
        waitForText("৳2,250.50")
        costShot("p06-android-bn-en")
        clickText("ডিসপ্লে")
        costShot("p06-android-dark-large")
        clickText("ডিসপ্লে")
        costScrollTo("খরচ যোগ করুন")
        clickText("খরচ যোগ করুন")
        waitForText("সেভ করুন")
        clickText("1"); clickText("2"); clickText("."); clickText("5")
        costShot("p06-android-expense")
        clickText("সেভ করুন")
        waitForText("৳2,263.00")
        costShot("p06-android-saved")
        clickText("আনডু")
        waitForText("৳2,250.50")
        clickText("দামের রিভিউ")
        waitForText("প্রতি লিটারের দাম মিলিয়ে কনফার্ম করুন")
        costShot("p06-android-price-bn")
        clickText("বন্ধ করুন")
        clickText("English")
        clickText("Price review")
        waitForText("Check and confirm the price per litre")
        costShot("p06-android-price-en")
        clickText("Close")
    } finally { runOnMainSync { activity.finish() } }
    checkCostFailure()
    return "P06 Android: $storage; UI save/Undo and four language/digit combinations"
}

private fun NativeAssetInstrumentation.checkCostFailure() {
    val activity = startActivitySync(Intent().setClassName(targetContext.packageName, "com.motovault.app.CostFixtureActivity")
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("failSave", true))
    try {
        costScrollTo("খরচ যোগ করুন"); clickText("খরচ যোগ করুন")
        clickText("১"); clickText("সেভ করুন")
        waitForText("সেভ হয়নি। লেখা আছে, আবার চেষ্টা করুন।")
        waitForText("টাকা: ১")
        costShot("p06-android-save-failure")
        clickText("বন্ধ করুন")
        waitForText("৳২,২৫০.৫০")
    } finally { runOnMainSync { activity.finish() } }
}

private fun NativeAssetInstrumentation.costScrollTo(text: String) {
    repeat(8) {
        if (nodeWithText(text) != null) return
        fun scrollables(node: android.view.accessibility.AccessibilityNodeInfo?): List<android.view.accessibility.AccessibilityNodeInfo> =
            if (node == null) emptyList() else (0 until node.childCount).flatMap { scrollables(node.getChild(it)) } + if (node.isScrollable) listOf(node) else emptyList()
        scrollables(uiAutomation.rootInActiveWindow).maxByOrNull { node -> android.graphics.Rect().also { node.getBoundsInScreen(it) }.height() }
            ?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
        SystemClock.sleep(200)
    }
    waitForText(text)
}

private fun NativeAssetInstrumentation.costShot(name: String) {
    SystemClock.sleep(300)
    uiAutomation.takeScreenshot()?.let { bitmap ->
        File(targetContext.cacheDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}

private fun NativeAssetInstrumentation.checkCostPriceRuntime(): String {
    val activity = startActivitySync(Intent().setClassName(targetContext.packageName, "com.motovault.app.CostFixtureActivity")
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    try {
        waitForText("৳২,২৫০.৫০")
        costShot("p06-android-bn-bn-final")
        clickText("English"); clickText("Price review")
        waitForText("Check and confirm the price per litre")
        clickText("Check and confirm the price per litre")
        waitForText("I confirmed this price")
        costShot("p06-android-price-en-final")
        clickText("Close"); clickText("বাংলা"); clickText("দামের রিভিউ")
        clickText("প্রতি লিটারের দাম মিলিয়ে কনফার্ম করুন")
        waitForText("দাম কনফার্ম করেছি")
        costShot("p06-android-price-bn-final")
    } finally { runOnMainSync { activity.finish() } }
    return "P06 labeled price confirmation controls in English and Bangla"
}
