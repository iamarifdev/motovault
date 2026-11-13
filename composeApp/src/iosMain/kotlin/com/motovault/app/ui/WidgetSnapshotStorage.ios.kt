@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.motovault.app.ui

import com.motovault.app.application.widget.WidgetSnapshotProjector
import com.motovault.app.domain.vault.VaultState
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDefaults
import platform.Foundation.create
import platform.Foundation.writeToURL

interface IosWidgetBridge {
    fun reloadTimelines()
}

object IosWidgetBridgeRegistry {
    var bridge: IosWidgetBridge? = null
}

private const val SNAPSHOT_DEFAULTS_KEY = "motovault.widget.snapshot"
private val iosWidgetJson = Json { ignoreUnknownKeys = true }

internal fun syncIosWidgetSnapshot(state: VaultState) {
    val now = Clock.System.now()
    val todayEpoch = Clock.System.todayIn(TimeZone.currentSystemDefault()).toEpochDays().toLong()
    val snapshot = WidgetSnapshotProjector.project(state, todayEpoch, now.toEpochMilliseconds())
    val encoded = iosWidgetJson.encodeToString(snapshot)
    NSUserDefaults.standardUserDefaults.setObject(encoded, forKey = SNAPSHOT_DEFAULTS_KEY)

    val groupId = NSBundle.mainBundle.objectForInfoDictionaryKey("CaptureAppGroup") as? String
    if (!groupId.isNullOrBlank()) {
        val root = NSFileManager.defaultManager.containerURLForSecurityApplicationGroupIdentifier(groupId)
        val fileUrl = root?.URLByAppendingPathComponent("widget-snapshot.json")
        if (fileUrl != null) {
            @Suppress("CAST_NEVER_SUCCEEDS")
            (encoded as NSString).writeToURL(
                url = fileUrl,
                atomically = true,
                encoding = NSUTF8StringEncoding,
                error = null
            )
        }
    }
    IosWidgetBridgeRegistry.bridge?.reloadTimelines()
}
