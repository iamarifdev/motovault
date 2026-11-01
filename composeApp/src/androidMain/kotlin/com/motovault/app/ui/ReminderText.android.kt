package com.motovault.app.ui

import android.content.res.Configuration
import com.motovault.app.R
import java.util.Locale
import java.text.DateFormatSymbols

internal actual fun localizedMonthName(language: AppLanguage, monthIndex: Int): String =
    DateFormatSymbols.getInstance(Locale.forLanguageTag(language.code)).months[monthIndex]

internal fun reminderContext(language: AppLanguage): android.content.Context {
    val configuration = Configuration(vaultContext.resources.configuration)
    configuration.setLocale(Locale.forLanguageTag(language.code))
    return vaultContext.createConfigurationContext(configuration)
}

internal actual fun reminderHeading(plan: RenewalReminder, target: ReminderTarget): String =
    reminderContext(target.language).getString(if (plan.targetType == "service") R.string.reminder_service_title else R.string.reminder_document_title)

internal actual fun reminderMessage(plan: RenewalReminder, target: ReminderTarget): String {
    val context = reminderContext(target.language)
    return if (target.type == "service") context.getString(R.string.reminder_service_message, target.title)
    else context.getString(R.string.reminder_document_message, target.title, reminderDate(plan, target))
}
