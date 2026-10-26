package com.motovault.app.ui

// Display labels are separate from the English document keys stored on disk.
internal fun documentDisplayName(value: String, lang: AppLanguage = AppLanguage.BN): String = when (value) {
    "Registration smart card" -> if (lang == AppLanguage.EN) "Registration Smart Card" else "রেজিস্ট্রেশন স্মার্ট কার্ড"
    "Driving licence" -> if (lang == AppLanguage.EN) "Driving License" else "ড্রাইভিং লাইসেন্স"
    "Tax token" -> if (lang == AppLanguage.EN) "Tax Token" else "ট্যাক্স টোকেন"
    "Insurance" -> if (lang == AppLanguage.EN) "Insurance" else "ইন্স্যুরেন্স / বিমা"
    "Fitness certificate" -> if (lang == AppLanguage.EN) "Fitness Certificate" else "ফিটনেস সার্টিফিকেট"
    else -> value
}

internal fun documentSideName(value: String, lang: AppLanguage = AppLanguage.BN): String = when {
    value == "Front" -> if (lang == AppLanguage.EN) "Front side" else "সামনের দিক"
    value == "Back" -> if (lang == AppLanguage.EN) "Back side" else "পেছনের দিক"
    value.startsWith("Page ") -> if (lang == AppLanguage.EN) "Page ${value.removePrefix("Page ")}" else "পৃষ্ঠা ${value.removePrefix("Page ")}"
    else -> value
}
