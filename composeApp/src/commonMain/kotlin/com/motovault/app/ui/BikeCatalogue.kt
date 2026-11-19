package com.motovault.app.ui

import org.jetbrains.compose.resources.DrawableResource

data class BikeModel(
    val id: String,
    val brandId: String,
    val name: String,
    val photo: DrawableResource,
    val note: String = "",
    val source: String = ""
)

object BikeCatalogue {
    val models: List<BikeModel> by lazy {
        BikeCatalogueFamilies.all.flatMap { family ->
            family.variants.map { variant ->
                BikeModel(variant.modelId, family.brandId, variant.name, variant.photo)
            }
        }
    }

    fun forBrand(brandId: String) = models.filter { it.brandId == brandId }
    fun search(brandId: String, query: String) = forBrand(brandId).filter { it.name.contains(query.trim(), ignoreCase = true) }
    fun find(id: String?) = models.firstOrNull { it.id == id }
    fun legacyModel(brandId: String): BikeModel? {
        val id = when (brandId) {
            "ktm" -> "ktm-duke-125-2023"
            "cfmoto" -> "cfmoto-250nk"
            "yamaha" -> "yamaha-r15-v4"
            "honda" -> "honda-cbr150r"
            "suzuki" -> "suzuki-gixxer-sf"
            "bajaj" -> "bajaj-pulsar-n160"
            "tvs" -> "tvs-apache-rtr-160-4v"
            "hero" -> "hero-hunk-150r"
            else -> "royal-enfield-classic-350"
        }
        return forBrand(brandId).firstOrNull { it.id == id }
    }
}
