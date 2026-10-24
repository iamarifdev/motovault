package com.motovault.app.ui

import kotlinx.serialization.Serializable

/** Stable keys are persisted. Bangla labels are the primary UI text. */
@Serializable
data class ServiceCatalogueItem(
    val id: String,
    val labelBn: String,
    val labelEn: String,
    val unitBn: String = "টি",
    val action: String = "replace"
)

object BangladeshServiceCatalogue {
    val items = listOf(
        ServiceCatalogueItem("engine_oil", "ইঞ্জিন অয়েল", "Engine oil", "লিটার", "replace"),
        ServiceCatalogueItem("oil_filter", "অয়েল ফিল্টার", "Oil filter", action = "replace"),
        ServiceCatalogueItem("air_filter", "এয়ার ফিল্টার", "Air filter", action = "replace"),
        ServiceCatalogueItem("spark_plug", "স্পার্ক প্লাগ", "Spark plug", action = "replace"),
        ServiceCatalogueItem("chain_sprocket", "চেইন ও স্প্রকেট", "Chain & sprocket", action = "replace"),
        ServiceCatalogueItem("brake_pad", "ব্রেক প্যাড / শু", "Brake pad / shoe", action = "replace"),
        ServiceCatalogueItem("brake_fluid", "ব্রেক ফ্লুইড", "Brake fluid", "মিলি", "replace"),
        ServiceCatalogueItem("coolant", "কুল্যান্ট", "Coolant", "মিলি", "replace"),
        ServiceCatalogueItem("clutch_cable", "ক্লাচ কেবল", "Clutch cable", action = "replace"),
        ServiceCatalogueItem("tyre", "টায়ার", "Tyre", action = "replace"),
        ServiceCatalogueItem("battery", "ব্যাটারি", "Battery", action = "replace"),
        ServiceCatalogueItem("general_service", "সাধারণ সার্ভিস", "General service", "বার", "service"),
        ServiceCatalogueItem("inspection", "পরীক্ষা", "Inspection", "বার", "inspect"),
        ServiceCatalogueItem("cleaning", "পরিষ্কার", "Cleaning", "বার", "clean"),
        ServiceCatalogueItem("adjustment", "অ্যাডজাস্টমেন্ট", "Adjustment", "বার", "adjust")
    )
    fun find(id: String) = items.firstOrNull { it.id == id }

    fun itemForScheduleName(name: String): ServiceCatalogueItem {
        val normalized = name.lowercase().replace("\u09af\u09bc", "\u09df")
        val id = when {
            "chain" in normalized || "চেইন" in normalized -> "chain_sprocket"
            "oil filter" in normalized || "অয়েল ফিল্টার" in normalized -> "oil_filter"
            "brake fluid" in normalized || "ব্রেক ফ্লুইড" in normalized -> "brake_fluid"
            "oil" in normalized || "অয়েল" in normalized || "তেল" in normalized -> "engine_oil"
            "air filter" in normalized || "এয়ার ফিল্টার" in normalized -> "air_filter"
            "spark" in normalized || "স্পার্ক" in normalized -> "spark_plug"
            "brake" in normalized || "ব্রেক" in normalized -> "brake_pad"
            "coolant" in normalized || "কুল্যান্ট" in normalized -> "coolant"
            "clutch" in normalized || "ক্লাচ" in normalized -> "clutch_cable"
            "tyre" in normalized || "tire" in normalized || "টায়ার" in normalized -> "tyre"
            "battery" in normalized || "ব্যাটারি" in normalized -> "battery"
            else -> "general_service"
        }
        return requireNotNull(find(id))
    }

    fun scheduleNameMatchesItem(name: String, itemId: String): Boolean =
        itemId != "general_service" && itemForScheduleName(name).id == itemId
}

@Serializable
data class EngineOilMetadata(
    val baseType: String = "", // mineral, semi_synthetic, full_synthetic
    val saeGrade: String = "",
    val apiStandard: String = "",
    val jasoStandard: String = "",
    val productName: String = "",
    val purchaseBasis: String = "", // bottle or loose
    val packVolumeMl: Int? = null,
    val boughtQuantity: Double? = null,
    val usedQuantityMl: Int? = null
) {
    fun isValid(): Boolean = baseType in setOf("mineral", "semi_synthetic", "full_synthetic") &&
        purchaseBasis in setOf("bottle", "loose") &&
        (packVolumeMl == null || packVolumeMl > 0) &&
        (boughtQuantity == null || (boughtQuantity.isFinite() && boughtQuantity > 0)) &&
        (usedQuantityMl == null || usedQuantityMl > 0) &&
        (purchaseBasis != "bottle" || packVolumeMl == null || boughtQuantity == null || usedQuantityMl == null ||
            usedQuantityMl.toDouble() <= packVolumeMl.toDouble() * boughtQuantity)
}

/** Prices are integer poisha. A null amount means unknown; zero means explicitly free. */
@Serializable
data class ServiceLineItem(
    val catalogueId: String? = null,
    val customName: String = "",
    val action: String = "replace",
    val quantity: Double = 1.0,
    val unitBn: String = "টি",
    val unitPricePoisha: Int? = null,
    val scheduleId: String? = null,
    val resetsScheduleBaseline: Boolean = false,
    val oil: EngineOilMetadata? = null,
    val detail: String? = null
) {
    val displayNameBn: String get() = BangladeshServiceCatalogue.find(catalogueId.orEmpty())?.labelBn ?: customName
    val lineTotalPoisha: Int? get() {
        val price = unitPricePoisha ?: return null
        if (!quantity.isFinite() || quantity <= 0) return null
        val amount = price.toLong() * quantity
        if (amount > Int.MAX_VALUE || amount < 0) return null
        return amount.toDouble().toLong().let { whole ->
            val fraction = amount - whole
            (whole + if (fraction >= 0.5) 1 else 0).takeIf { it <= Int.MAX_VALUE }?.toInt()
        }
    }
}

fun serviceTotalPoisha(
    lines: List<ServiceLineItem>,
    labourPoisha: Int?,
    miscellaneousPoisha: Int?,
    discountPoisha: Int?
): Int? {
    if (lines.isEmpty() || labourPoisha == null || miscellaneousPoisha == null || discountPoisha == null) return null
    val values = lines.map { it.lineTotalPoisha ?: return null } + listOf(labourPoisha, miscellaneousPoisha)
    if (values.any { it < 0 } || discountPoisha < 0) return null
    val total = values.sumOf { it.toLong() } - discountPoisha
    return total.takeIf { it in 0..Int.MAX_VALUE.toLong() }?.toInt()
}
