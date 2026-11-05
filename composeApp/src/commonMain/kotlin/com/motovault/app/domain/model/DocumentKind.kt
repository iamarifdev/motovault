package com.motovault.app.domain.model

/** Stable persisted document kinds. Display names never become record identity. */
object DocumentKinds {
    const val REGISTRATION = "registration"
    const val LICENCE = "licence"
    const val TAX = "tax"
    const val INSURANCE = "insurance"
    const val FITNESS = "fitness"

    val all = listOf(REGISTRATION, LICENCE, TAX, INSURANCE, FITNESS)

    /** Resolves a stored kind or a legacy title without persisting a translated label. */
    fun fromStored(kind: String, title: String): String? {
        val normalized = kind.trim().lowercase()
        if (normalized in all) return normalized
        legacyKinds[normalized]?.let { return it }
        val label = title.lowercase()
        return when {
            "licence" in label || "license" in label || "লাইসেন্স" in label -> LICENCE
            "tax" in label || "ট্যাক্স" in label -> TAX
            "insurance" in label || "বিমা" in label || "ইন্স্যুরেন্স" in label -> INSURANCE
            "fitness" in label || "ফিটনেস" in label -> FITNESS
            "registration" in label || "রেজিস্ট্রেশন" in label -> REGISTRATION
            else -> null
        }
    }

    private val legacyKinds = mapOf(
        "smart_card" to REGISTRATION,
        "registration certificate" to REGISTRATION,
        "registration smart card" to REGISTRATION,
        "driving license" to LICENCE,
        "licence" to LICENCE,
        "license" to LICENCE,
        "tax token" to TAX,
        "insurance certificate" to INSURANCE,
        "fitness certificate" to FITNESS
    )
}

fun VaultDocument.stableKind(): String? = DocumentKinds.fromStored(kind, title)
