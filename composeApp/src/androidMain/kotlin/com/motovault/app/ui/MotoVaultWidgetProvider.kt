package com.motovault.app.ui

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.widget.RemoteViews
import com.motovault.app.MainActivity
import com.motovault.app.R
import com.motovault.app.application.widget.WidgetDeepLink
import com.motovault.app.application.widget.WidgetSnapshot
import com.motovault.app.application.widget.WidgetSnapshotProjector
import com.motovault.app.application.widget.WidgetTaskStatus
import com.motovault.app.domain.model.AppLanguage
import com.motovault.app.domain.model.Digits
import com.motovault.app.domain.vault.VaultState
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale
import kotlin.math.roundToLong

private const val WIDGET_PREFS = "motovault_widget"
private const val SNAPSHOT_KEY = "snapshot"
private val widgetJson = Json { ignoreUnknownKeys = true }

class MotoVaultWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val snapshot = readWidgetSnapshot(context)
        appWidgetIds.forEach { widgetId ->
            appWidgetManager.updateAppWidget(widgetId, buildWidgetViews(context, snapshot))
        }
    }
}

internal fun syncAndroidWidgetSnapshot(context: Context, state: VaultState) {
    val now = Clock.System.now()
    val todayEpoch = now.let {
        Clock.System.todayIn(TimeZone.currentSystemDefault()).toEpochDays().toLong()
    }
    val snapshot = WidgetSnapshotProjector.project(state, todayEpoch, now.toEpochMilliseconds())
    context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(SNAPSHOT_KEY, widgetJson.encodeToString(snapshot))
        .apply()
    val manager = AppWidgetManager.getInstance(context)
    val component = ComponentName(context, MotoVaultWidgetProvider::class.java)
    val ids = manager.getAppWidgetIds(component)
    if (ids.isNotEmpty()) {
        val views = buildWidgetViews(context, snapshot)
        manager.updateAppWidget(ids, views)
    }
}

private fun readWidgetSnapshot(context: Context): WidgetSnapshot? {
    val raw = context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE).getString(SNAPSHOT_KEY, null)
        ?: return null
    return runCatching { widgetJson.decodeFromString<WidgetSnapshot>(raw) }.getOrNull()
}

private fun buildWidgetViews(context: Context, snapshot: WidgetSnapshot?): RemoteViews {
    val lang = snapshot?.language ?: AppLanguage.BN
    val localized = localizedWidgetContext(context, lang)
    val views = RemoteViews(context.packageName, R.layout.motovault_widget)

    views.setTextViewText(R.id.widget_btn_fuel, localized.getString(R.string.widget_btn_fuel))
    views.setTextViewText(R.id.widget_btn_km, localized.getString(R.string.widget_btn_km))

    if (snapshot == null || snapshot.taskStatus == WidgetTaskStatus.NONE) {
        views.setTextViewText(R.id.widget_bike_name, localized.getString(R.string.widget_no_bike_title))
        views.setTextViewText(R.id.widget_odometer, "")
        views.setTextViewText(R.id.widget_task_line, localized.getString(R.string.widget_no_bike_body))
    } else if (snapshot.privacyRedacted || snapshot.taskStatus == WidgetTaskStatus.LOCKED) {
        views.setTextViewText(R.id.widget_bike_name, localized.getString(R.string.widget_locked_title))
        views.setTextViewText(R.id.widget_odometer, "")
        views.setTextViewText(R.id.widget_task_line, localized.getString(R.string.widget_locked_body))
    } else {
        val odoFormatted = snapshot.odometerKm?.roundToLong()?.toString()?.let { raw ->
            val digits = if (snapshot.digits == Digits.BN) toBnDigits(raw) else raw
            "$digits ${localized.getString(R.string.widget_km_suffix)}"
        }.orEmpty()
        val itemLabel = resolveWidgetItemLabel(localized, snapshot.taskItemKey)
        val statusRes = when (snapshot.taskStatus) {
            WidgetTaskStatus.OVERDUE -> R.string.widget_status_overdue
            WidgetTaskStatus.SOON -> R.string.widget_status_soon
            WidgetTaskStatus.OK -> R.string.widget_status_ok
            WidgetTaskStatus.UNKNOWN -> R.string.widget_status_unknown
            else -> R.string.widget_status_unknown
        }
        val baseLine = localized.getString(statusRes, itemLabel)
        val taskLine = if (snapshot.isStale(System.currentTimeMillis())) {
            "$baseLine · ${localized.getString(R.string.widget_stale_badge)}"
        } else {
            baseLine
        }
        views.setTextViewText(R.id.widget_bike_name, snapshot.bikeName)
        views.setTextViewText(R.id.widget_odometer, odoFormatted)
        views.setTextViewText(R.id.widget_task_line, taskLine)
    }

    val bikeId = snapshot?.bikeId
    views.setOnClickPendingIntent(R.id.widget_root, widgetPendingIntent(context, WidgetDeepLink("garage", bikeId), 101))
    views.setOnClickPendingIntent(R.id.widget_btn_fuel, widgetPendingIntent(context, WidgetDeepLink("fuel", bikeId), 102))
    views.setOnClickPendingIntent(R.id.widget_btn_km, widgetPendingIntent(context, WidgetDeepLink("odometer", bikeId), 103))
    return views
}

private fun widgetPendingIntent(context: Context, link: WidgetDeepLink, requestCode: Int): PendingIntent {
    val uri = link.toUri()
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri), context, MainActivity::class.java)
        .putExtra("motovault.widget", uri)
        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    return PendingIntent.getActivity(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

private fun localizedWidgetContext(context: Context, language: AppLanguage): Context {
    val config = Configuration(context.resources.configuration)
    config.setLocale(Locale.forLanguageTag(language.code))
    return context.createConfigurationContext(config)
}

private fun resolveWidgetItemLabel(localized: Context, key: String?): String {
    val normalized = key?.lowercase().orEmpty()
    val resId = when {
        normalized.contains("chain") -> R.string.widget_item_chain
        normalized.contains("oil") -> R.string.widget_item_oil
        normalized.contains("air") -> R.string.widget_item_air
        normalized.contains("spark") -> R.string.widget_item_spark
        normalized.contains("brake") -> R.string.widget_item_brake
        normalized.contains("coolant") -> R.string.widget_item_coolant
        else -> R.string.widget_item_service
    }
    return localized.getString(resId)
}

private fun toBnDigits(ascii: String): String {
    val bnDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    return buildString(ascii.length) {
        for (ch in ascii) {
            if (ch in '0'..'9') append(bnDigits[ch - '0']) else append(ch)
        }
    }
}
