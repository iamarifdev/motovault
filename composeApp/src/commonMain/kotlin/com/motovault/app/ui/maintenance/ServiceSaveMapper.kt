package com.motovault.app.ui

import com.motovault.app.domain.input.NormalizedNumbers
import com.motovault.app.domain.model.BangladeshServiceCatalogue
import com.motovault.app.domain.model.VisitRecordState
import com.motovault.app.domain.workshop.recordWorkshopVisit
import com.motovault.app.domain.workshop.updateServiceVisit
import kotlinx.datetime.LocalDate
import kotlin.math.max
import kotlin.math.round
import kotlin.math.roundToInt

internal object ServiceSaveMapper {
    fun buildInitialRows(
        existing: RideLog?,
        initialRows: List<ParsedLine>?,
        initialCatalogueKey: String?,
        catalogLabels: Map<String, String>
    ): List<ServiceRowState> {
        if (existing != null) {
            if (existing.serviceLines.isEmpty()) {
                val label = existing.detail.ifBlank { existing.title }
                return listOf(ServiceRowState("legacy-0", null, label, 1))
            }
            val counts = existing.serviceLines.mapNotNull { it.catalogueId }.groupingBy { it }.eachCount()
            return existing.serviceLines.mapIndexed { index, line ->
                val catId = line.catalogueId
                val uid = when {
                    catId != null && (counts[catId] ?: 0) == 1 -> catId
                    catId != null -> "$catId-e$index"
                    else -> "custom-e$index"
                }
                val label = catId?.let { catalogLabels[it] } ?: line.displayName
                ServiceRowState(uid, catId, label, line.quantity.roundToInt().coerceIn(1, 20), line.detail)
            }
        }
        if (!initialRows.isNullOrEmpty()) {
            return initialRows.mapIndexed { index, parsed ->
                val catId = parsed.item
                val label = catalogLabels[catId] ?: catId
                ServiceRowState("$catId-$index", catId, label, parsed.qty.roundToInt().coerceIn(1, 20))
            }
        }
        if (initialCatalogueKey != null) {
            val known = BangladeshServiceCatalogue.find(initialCatalogueKey) != null ||
                CATALOG_ITEMS.any { it.key == initialCatalogueKey }
            val catId = if (known) initialCatalogueKey else null
            val label = catalogLabels[initialCatalogueKey] ?: initialCatalogueKey
            return listOf(ServiceRowState(catId ?: "custom-0", catId, label, 1))
        }
        return emptyList()
    }

    fun buildInitialValues(
        existing: RideLog?,
        initialRows: List<ParsedLine>?,
        rows: List<ServiceRowState>,
        state: VaultState,
        estimate: Double,
        initialOdometer: Double?
    ): Map<String, String> = buildMap {
        put("labour", existing?.serviceLabourPoisha?.takeIf { it > 0 }?.let { formatServiceNumber(it / 100.0) }.orEmpty())
        put("discount", existing?.serviceDiscountPoisha?.takeIf { it > 0 }?.let { formatServiceNumber(it / 100.0) }.orEmpty())
        put(
            "odo",
            existing?.odometer?.let(::formatServiceNumber)
                ?: initialOdometer?.let(::formatServiceNumber)
                ?: round(estimate).toLong().toString()
        )
        rows.forEachIndexed { index, row ->
            val price = when {
                existing != null && existing.serviceLines.isNotEmpty() ->
                    existing.serviceLines.getOrNull(index)?.unitPricePoisha?.let { it / 100.0 }
                existing != null && index == 0 && existing.cost > 0.0 -> existing.cost
                else -> initialRows?.getOrNull(index)?.price ?: row.catalogueId?.let { state.lastItemPrices[it] }
            }
            put("p:${row.uid}", price?.let(::formatServiceNumber).orEmpty())
        }
    }

    fun save(
        state: VaultState,
        existing: RideLog?,
        selectedRuleId: String?,
        rows: List<ServiceRowState>,
        values: Map<String, String>,
        date: LocalDate,
        odoVal: Double,
        workshop: String,
        workshopPhone: String,
        visitState: VisitRecordState,
        caseTitle: String,
        warrantyMonths: Int,
        today: LocalDate
    ): VaultState {
        val lines = rows.map { row ->
            val unitPrice = NormalizedNumbers.parse(values["p:${row.uid}"].orEmpty())
            val catId = row.catalogueId
            ServiceLineItem(
                catalogueId = catId,
                customName = if (catId == null) row.name else "",
                action = catId?.let { BangladeshServiceCatalogue.find(it)?.action } ?: "service",
                quantity = row.qty.toDouble(),
                unitPricePoisha = unitPrice?.let { round(it * 100.0).toInt() },
                detail = row.detail
            )
        }
        val labourPoisha = NormalizedNumbers.parse(values["labour"].orEmpty())?.let { round(it * 100.0).toInt() } ?: 0
        val miscPoisha = existing?.serviceMiscellaneousPoisha ?: 0
        val discountPoisha = NormalizedNumbers.parse(values["discount"].orEmpty())?.let { round(it * 100.0).toInt() } ?: 0
        val pricedLinesTotal = lines.sumOf { (it.lineTotalPoisha ?: 0).toLong() }
        val beforeDiscount = (pricedLinesTotal + labourPoisha + miscPoisha).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val boundedDiscount = discountPoisha.coerceIn(0, beforeDiscount)
        val totalPoisha = max(0L, beforeDiscount.toLong() - boundedDiscount)
        val allLinesPriced = lines.all { it.lineTotalPoisha != null }
        val log = RideLog(
            title = rows.firstOrNull()?.name ?: "",
            detail = rows.joinToString(", ") { it.name },
            date = date.toString(),
            odometer = odoVal,
            kind = "service",
            serviceLines = lines,
            serviceLabourPoisha = labourPoisha,
            serviceMiscellaneousPoisha = miscPoisha,
            serviceDiscountPoisha = boundedDiscount,
            serviceTotalPoisha = if (allLinesPriced) totalPoisha.toInt() else null,
            cost = totalPoisha / 100.0,
            workshop = workshop.trim(),
            notes = existing?.notes.orEmpty()
        )
        if (existing != null) {
            return state.updateServiceVisit(
                existingId = existing.id,
                updatedLog = log,
                visitState = visitState,
                caseTitle = caseTitle,
                warrantyMonths = warrantyMonths,
                workshopPhone = workshopPhone,
                saveQuickServiceFn = VaultState::saveQuickService,
                today = today
            )
        }
        val recorded = state.recordWorkshopVisit(
            log = log,
            selectedRuleId = selectedRuleId,
            visitState = visitState,
            caseTitle = caseTitle,
            warrantyMonths = warrantyMonths,
            workshopPhone = workshopPhone,
            saveQuickServiceFn = VaultState::saveQuickService
        )
        val mergedPrices = recorded.lastItemPrices.toMutableMap()
        lines.forEach { line ->
            val catId = line.catalogueId
            val pricePoisha = line.unitPricePoisha
            if (catId != null && pricePoisha != null) {
                mergedPrices[catId] = pricePoisha / 100.0
            }
        }
        return recorded.copy(lastItemPrices = mergedPrices)
    }
}
