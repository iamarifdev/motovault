package com.motovault.app.ui

import platform.Foundation.NSBundle
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale

internal actual fun localizedMonthName(language: AppLanguage, monthIndex: Int): String =
    NSDateFormatter().apply { locale = NSLocale(localeIdentifier = language.code) }.monthSymbols[monthIndex] as String

private fun reminderResource(target: ReminderTarget, key: String): String {
    val path = NSBundle.mainBundle.pathForResource(target.language.code, ofType = "lproj")
        ?: NSBundle.mainBundle.pathForResource(AppLanguage.EN.code, ofType = "lproj")
    val bundle = path?.let { NSBundle.bundleWithPath(it) } ?: NSBundle.mainBundle
    return bundle.localizedStringForKey(key, value = null, table = null)
}

internal actual fun reminderHeading(plan: RenewalReminder, target: ReminderTarget): String =
    reminderResource(target, if (plan.targetType == "service") "reminder_service_title" else "reminder_document_title")

internal actual fun reminderMessage(plan: RenewalReminder, target: ReminderTarget): String {
    val key = if (target.type == "service") "reminder_service_message" else "reminder_document_message"
    val template = reminderResource(target, key)
    val dated = if (target.type == "service") template else template.replace("%2\$s", reminderDate(plan, target))
    return dated.replace("%1\$s", target.title)
}
