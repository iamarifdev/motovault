package com.motovault.app.ui

// Display labels are separate from the English document keys stored on disk.
internal fun documentDisplayName(value: String): String = when (value) {
    "Registration smart card" -> "রেজিস্ট্রেশন স্মার্ট কার্ড"
    "Driving licence" -> "ড্রাইভিং লাইসেন্স"
    "Tax token" -> "ট্যাক্স টোকেন"
    "Insurance" -> "বিমা"
    "Fitness certificate" -> "ফিটনেস সনদ"
    else -> value
}

internal fun documentSideName(value: String): String = when {
    value == "Front" -> "সামনের দিক"
    value == "Back" -> "পেছনের দিক"
    value.startsWith("Page ") -> "পৃষ্ঠা ${value.removePrefix("Page ")}"
    else -> value
}
