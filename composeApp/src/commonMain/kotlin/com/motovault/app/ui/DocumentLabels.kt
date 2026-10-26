package com.motovault.app.ui

import androidx.compose.runtime.Composable
import motovault.composeapp.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Maps document title or identifier to its type-safe StringResource.
 * Follows Kotlin Multiplatform best practice: strings are stored in XML bundles
 * (values/strings.xml and values-bn/strings.xml) rather than hardcoded in functions.
 */
internal fun documentResourceOf(value: String): StringResource? = when (value) {
    "Registration smart card", "smart_card", "রেজিস্ট্রেশন স্মার্ট কার্ড" -> Res.string.doc_smart_card
    "Registration certificate", "registration", "রেজিস্ট্রেশন সার্টিফিকেট" -> Res.string.doc_registration
    "Driving licence", "Driving license", "licence", "license", "ড্রাইভিং লাইসেন্স" -> Res.string.doc_driving_license
    "Tax token", "tax", "ট্যাক্স টোকেন" -> Res.string.doc_tax_token
    "Insurance", "Insurance certificate", "insurance", "ইন্স্যুরেন্স / বিমা", "বিমা" -> Res.string.doc_insurance
    "Fitness certificate", "fitness", "ফিটনেস সার্টিফিকেট", "ফিটনেস সনদ" -> Res.string.doc_fitness
    else -> null
}

/**
 * Idiomatic Compose Multiplatform resolution: resolves from strings.xml or values-bn/strings.xml
 * based on the active locale.
 */
@Composable
internal fun documentDisplayName(value: String, lang: AppLanguage? = null): String {
    val res = documentResourceOf(value)
    return if (res != null) stringResource(res) else value
}

@Composable
internal fun documentSideName(value: String, lang: AppLanguage? = null): String = when {
    value == "Front" -> stringResource(Res.string.doc_side_front)
    value == "Back" -> stringResource(Res.string.doc_side_back)
    value.startsWith("Page ") -> stringResource(Res.string.doc_side_page, value.removePrefix("Page "))
    else -> value
}
