package com.motovault.app.domain.model

import kotlinx.serialization.Serializable

/** Stable keys are persisted. Bangla labels are the primary UI text. */
@Serializable
data class ServiceCatalogueItem(
    val id: String,
    val action: String = "replace"
)

object BangladeshServiceCatalogue {
    val items = listOf(
        ServiceCatalogueItem("engine_oil"),
        ServiceCatalogueItem("oil_filter"),
        ServiceCatalogueItem("air_filter"),
        ServiceCatalogueItem("spark_plug"),
        ServiceCatalogueItem("chain_sprocket"),
        ServiceCatalogueItem("brake_pad"),
        ServiceCatalogueItem("brake_fluid"),
        ServiceCatalogueItem("coolant"),
        ServiceCatalogueItem("clutch_cable"),
        ServiceCatalogueItem("tyre"),
        ServiceCatalogueItem("battery"),
        ServiceCatalogueItem("chain_lube", "lubricate"),
        ServiceCatalogueItem("chain_adjust", "adjust"),
        ServiceCatalogueItem("brake_hose"),
        ServiceCatalogueItem("fork_oil"),
        ServiceCatalogueItem("valve_clearance", "inspect"),
        ServiceCatalogueItem("tyre_brake_inspect", "inspect"),
        ServiceCatalogueItem("first_service", "inspect"),
        ServiceCatalogueItem("general_service", "service"),
        ServiceCatalogueItem("inspection", "inspect"),
        ServiceCatalogueItem("cleaning", "clean"),
        ServiceCatalogueItem("adjustment", "adjust")
    )
    fun find(id: String) = items.firstOrNull { it.id == id }

    fun itemForScheduleName(name: String): ServiceCatalogueItem {
        val normalized = name.lowercase().replace("\u09af\u09bc", "\u09df")
        val id = when {
            "chain lubrication" in normalized || "চেইন লুব্রিকেন্ট" in normalized -> "chain_lube"
            "chain adjustment" in normalized || "চেইন অ্যাডজাস্ট" in normalized -> "chain_adjust"
            "brake hose" in normalized || "ব্রেক হোজ" in normalized -> "brake_hose"
            "fork oil" in normalized || "ফর্ক অয়েল" in normalized -> "fork_oil"
            "valve clearance" in normalized || "ভালভ ক্লিয়ারেন্স" in normalized -> "valve_clearance"
            "tyres, brakes" in normalized || "tyre and brake" in normalized -> "tyre_brake_inspect"
            "first service" in normalized || "প্রথম সার্ভিস" in normalized -> "first_service"
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

    /** Prefer the persisted catalogue link; fall back to the legacy name heuristic. */
    fun scheduleMatchesItem(ruleCatalogueId: String?, ruleName: String, itemId: String): Boolean =
        ruleCatalogueId?.let { it == itemId } ?: scheduleNameMatchesItem(ruleName, itemId)
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
    val unitBn: String = "",
    val unitPricePoisha: Int? = null,
    val scheduleId: String? = null,
    val resetsScheduleBaseline: Boolean = false,
    val oil: EngineOilMetadata? = null,
    val detail: String? = null
) {
    val displayName: String get() = customName.ifBlank { catalogueId.orEmpty() }
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
