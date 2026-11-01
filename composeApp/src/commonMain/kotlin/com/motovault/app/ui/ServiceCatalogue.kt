package com.motovault.app.ui

/** UI-facing aliases; the catalogue data is owned by domain/model. */
typealias ServiceCatalogueItem = com.motovault.app.domain.model.ServiceCatalogueItem
typealias EngineOilMetadata = com.motovault.app.domain.model.EngineOilMetadata
typealias ServiceLineItem = com.motovault.app.domain.model.ServiceLineItem

object BangladeshServiceCatalogue {
    val items get() = com.motovault.app.domain.model.BangladeshServiceCatalogue.items
    fun find(id: String) = com.motovault.app.domain.model.BangladeshServiceCatalogue.find(id)
    fun itemForScheduleName(name: String) = com.motovault.app.domain.model.BangladeshServiceCatalogue.itemForScheduleName(name)
    fun scheduleNameMatchesItem(name: String, itemId: String) = com.motovault.app.domain.model.BangladeshServiceCatalogue.scheduleNameMatchesItem(name, itemId)
    fun scheduleMatchesItem(ruleCatalogueId: String?, ruleName: String, itemId: String) =
        com.motovault.app.domain.model.BangladeshServiceCatalogue.scheduleMatchesItem(ruleCatalogueId, ruleName, itemId)
}

fun serviceTotalPoisha(lines: List<ServiceLineItem>, labourPoisha: Int?, miscellaneousPoisha: Int?, discountPoisha: Int?): Int? =
    com.motovault.app.domain.model.serviceTotalPoisha(lines, labourPoisha, miscellaneousPoisha, discountPoisha)
