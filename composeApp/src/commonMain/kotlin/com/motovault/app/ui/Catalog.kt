package com.motovault.app.ui

data class ServiceCatalogItem(
    val key: String,
    val bn: String,
    val short: String,
    val unit: String,
    val ruleSuffix: String?
)

val CATALOG_ITEMS = listOf(
    ServiceCatalogItem("engine_oil", "ইঞ্জিন অয়েল", "অয়েল", "বোতল", "oil"),
    ServiceCatalogItem("oil_filter", "অয়েল ফিল্টার", "ফিল্টার", "টি", "oil"),
    ServiceCatalogItem("air_filter", "এয়ার ফিল্টার", "এয়ার ফিল্টার", "টি", "air_filter"),
    ServiceCatalogItem("spark_plug", "স্পার্ক প্লাগ", "স্পার্ক প্লাগ", "টি", "spark_plug"),
    ServiceCatalogItem("chain_sprocket", "চেইন ও স্প্রকেট", "চেইন", "সেট", "chain"),
    ServiceCatalogItem("brake_pad", "ব্রেক প্যাড", "ব্রেক প্যাড", "সেট", "brake_pads"),
    ServiceCatalogItem("coolant", "কুল্যান্ট", "কুল্যান্ট", "বোতল", "coolant"),
    ServiceCatalogItem("tyre", "টায়ার", "টায়ার", "টি", null),
    ServiceCatalogItem("battery", "ব্যাটারি", "ব্যাটারি", "টি", null),
    ServiceCatalogItem("general_service", "সাধারণ সার্ভিস", "সাধারণ সার্ভিস", "বার", null)
)

fun catalogItemOf(key: String): ServiceCatalogItem? = CATALOG_ITEMS.firstOrNull { it.key == key }

val VISCOSITY_GRADES = listOf("10W-30", "10W-40", "20W-40", "20W-50")

data class DocumentKindInfo(
    val kind: String,
    val title: String,
    val bn: String,
    val card: Boolean
)

val DOCUMENT_KINDS = listOf(
    DocumentKindInfo("registration", "Registration certificate", "রেজিস্ট্রেশন স্মার্ট কার্ড", true),
    DocumentKindInfo("licence", "Driving licence", "ড্রাইভিং লাইসেন্স", true),
    DocumentKindInfo("tax", "Tax token", "ট্যাক্স টোকেন", false),
    DocumentKindInfo("insurance", "Insurance certificate", "বিমা", false),
    DocumentKindInfo("fitness", "Fitness certificate", "ফিটনেস সনদ", false)
)

fun docKindInfo(kind: String): DocumentKindInfo = DOCUMENT_KINDS.firstOrNull { it.kind == kind }
    ?: DOCUMENT_KINDS.firstOrNull { it.title.equals(kind, ignoreCase = true) }
    ?: DocumentKindInfo(kind, kind, kind, false)

fun docNameBn(kindOrTitle: String): String = docKindInfo(kindOrTitle).bn
