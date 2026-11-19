package com.motovault.app.ui

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.motovault.app.domain.input.NormalizedNumbers
import com.motovault.app.domain.model.BangladeshServiceCatalogue
import com.motovault.app.domain.model.ComponentRule
import com.motovault.app.domain.model.PartWarranty
import com.motovault.app.domain.model.VisitRecordState
import com.motovault.app.domain.workshop.migratedWorkshopContacts
import kotlinx.datetime.LocalDate
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

private const val MAX_SERVICE_ODOMETER_KM = 9_999_999.0
private const val MAX_SUGGESTED_SERVICES = 6

internal data class ServiceRowState(
    val uid: String,
    val catalogueId: String?,
    val name: String,
    val qty: Int = 1,
    val detail: String? = null
)

internal enum class ServiceValidationReason {
    RECORD_MISSING,
    NO_ROWS,
    INVALID_ODOMETER,
    CONFIRM_ODOMETER,
    INVALID_DATE,
    FUTURE_DATE
}

internal data class ServiceTotals(
    val subtotal: Double,
    val labour: Double,
    val misc: Double,
    val discount: Double,
    val total: Double,
    val unknownPriceCount: Int
)

internal fun dueServiceKeys(state: VaultState, today: LocalDate): List<String> {
    val ride = state.ride ?: return emptyList()
    val todayEpoch = today.toEpochDays().toLong()
    return state.rules.mapNotNull { rule ->
        val wear = WearCalculator.wearOf(rule, ride.odometer, todayEpoch)
        if (wear.status == WearStatus.OK || wear.status == WearStatus.UNKNOWN) {
            null
        } else {
            CATALOG_ITEMS.firstOrNull { it.key == rule.catalogueId }?.key
                ?: CATALOG_ITEMS.firstOrNull {
                    BangladeshServiceCatalogue.scheduleNameMatchesItem(rule.name, it.key)
                }?.key
        }
    }.distinct()
}

internal fun suggestedServiceKeys(
    dueKeys: List<String>,
    lastItemPrices: Map<String, Double>,
    pickedKeys: Set<String>
): List<String> {
    val pricedKeys = CATALOG_ITEMS.map { it.key }.filter { lastItemPrices[it] != null }
    val allKeys = CATALOG_ITEMS.map { it.key }
    return (dueKeys + pricedKeys + allKeys)
        .distinct()
        .filterNot { it in pickedKeys }
        .take(MAX_SUGGESTED_SERVICES)
}

internal fun matchingResetRules(
    rows: List<ServiceRowState>,
    rules: List<ComponentRule>
): List<ComponentRule> = rows.mapNotNull { it.catalogueId }
    .flatMap { catId ->
        rules.filter { rule ->
            BangladeshServiceCatalogue.scheduleMatchesItem(rule.catalogueId, rule.name, catId)
        }
    }
    .distinctBy { it.id }

internal fun calculateServiceTotals(
    rows: List<ServiceRowState>,
    values: Map<String, String>,
    existingMiscPoisha: Int
): ServiceTotals {
    var unknown = 0
    val subtotal = rows.sumOf { row ->
        val parsed = NormalizedNumbers.parse(values["p:${row.uid}"].orEmpty())
        if (parsed == null) unknown += 1
        (parsed ?: 0.0) * row.qty
    }
    val labour = NormalizedNumbers.parse(values["labour"].orEmpty()) ?: 0.0
    val misc = existingMiscPoisha / 100.0
    val discount = NormalizedNumbers.parse(values["discount"].orEmpty()) ?: 0.0
    val total = max(0.0, subtotal + labour + misc - discount)
    return ServiceTotals(subtotal, labour, misc, discount, total, unknown)
}

internal fun validateServiceDraft(
    existingId: String?,
    existingFound: Boolean,
    rowCount: Int,
    odoVal: Double?,
    odoTouched: Boolean,
    date: LocalDate?,
    today: LocalDate
): ServiceValidationReason? = when {
    existingId != null && !existingFound -> ServiceValidationReason.RECORD_MISSING
    rowCount == 0 -> ServiceValidationReason.NO_ROWS
    odoVal == null || !odoVal.isFinite() || odoVal <= 0.0 || odoVal > MAX_SERVICE_ODOMETER_KM ->
        ServiceValidationReason.INVALID_ODOMETER
    !odoTouched -> ServiceValidationReason.CONFIRM_ODOMETER
    date == null -> ServiceValidationReason.INVALID_DATE
    date > today -> ServiceValidationReason.FUTURE_DATE
    else -> null
}

internal fun resolveServiceDate(dateKey: EntryDateKey, customDate: String, today: LocalDate): LocalDate? =
    when (dateKey) {
        EntryDateKey.TODAY -> today
        EntryDateKey.YESTERDAY -> LocalDate.fromEpochDays(today.toEpochDays() - 1)
        EntryDateKey.CUSTOM -> runCatching { LocalDate.parse(NormalizedNumbers.ascii(customDate)) }.getOrNull()
    }

internal fun inferWarrantyMonths(existing: RideLog?, warranties: List<PartWarranty>): Int {
    val log = existing ?: return 0
    val warranty = warranties.firstOrNull { it.visitLogId == log.id }
        ?: log.warrantyIds.firstNotNullOfOrNull { wid -> warranties.firstOrNull { it.id == wid } }
        ?: return 0
    val start = runCatching { LocalDate.parse(warranty.startDate) }.getOrNull() ?: return 0
    val expiry = runCatching { LocalDate.parse(warranty.expiryDate) }.getOrNull() ?: return 0
    val days = expiry.toEpochDays() - start.toEpochDays()
    return when {
        days >= 330 -> 12
        days >= 150 -> 6
        else -> 0
    }
}

internal fun formatServiceNumber(value: Double): String {
    if (!value.isFinite()) return ""
    return if (value % 1.0 == 0.0 && abs(value) <= 1e15) value.toLong().toString() else value.toString()
}

internal fun initialServiceDateKey(existing: RideLog?, today: LocalDate): EntryDateKey {
    if (existing == null || existing.date == today.toString()) return EntryDateKey.TODAY
    val yesterday = LocalDate.fromEpochDays(today.toEpochDays() - 1).toString()
    return if (existing.date == yesterday) EntryDateKey.YESTERDAY else EntryDateKey.CUSTOM
}

internal fun initialServiceVisitState(existing: RideLog?): VisitRecordState =
    if (existing?.visitState == VisitRecordState.QUOTATION.code ||
        existing?.visitState == VisitRecordState.APPROVED.code
    ) VisitRecordState.QUOTATION else VisitRecordState.PAID

internal fun initialServiceWorkshopPhone(state: VaultState, existing: RideLog?): String =
    existing?.workshopId?.let { id -> state.migratedWorkshopContacts().firstOrNull { it.id == id }?.phone }.orEmpty()

internal fun initialServiceRepairCaseTitle(state: VaultState, existing: RideLog?): String =
    existing?.repairCaseId?.let { id -> state.repairCases.firstOrNull { it.id == id }?.title }.orEmpty()

internal fun updateServiceRow(
    rows: SnapshotStateList<ServiceRowState>,
    uid: String,
    transform: (ServiceRowState) -> ServiceRowState
) {
    val index = rows.indexOfFirst { it.uid == uid }
    if (index >= 0) rows[index] = transform(rows[index])
}

internal fun addServiceSuggestionRow(
    rows: SnapshotStateList<ServiceRowState>,
    entry: NumberEntryState,
    state: VaultState,
    key: String,
    label: String
): String {
    val existingRow = rows.firstOrNull { it.catalogueId == key }
    if (existingRow != null) return existingRow.uid
    entry.set("p:$key", state.lastItemPrices[key]?.let(::formatServiceNumber).orEmpty())
    rows.add(ServiceRowState(key, key, label, 1))
    return key
}

internal fun applyServicePartSelection(
    rows: SnapshotStateList<ServiceRowState>,
    entry: NumberEntryState,
    state: VaultState,
    pickedKeys: Set<String>,
    customName: String?,
    customSeq: Int,
    catalogLabels: Map<String, String>
): String? {
    val retained = rows.filter { it.catalogueId == null || it.catalogueId in pickedKeys }.toMutableList()
    pickedKeys.filter { key -> retained.none { it.catalogueId == key } }.forEach { key ->
        entry.set("p:$key", state.lastItemPrices[key]?.let(::formatServiceNumber).orEmpty())
        retained.add(ServiceRowState(key, key, catalogLabels[key] ?: key, 1))
    }
    var newCustomUid: String? = null
    if (!customName.isNullOrBlank()) {
        val uid = "custom-$customSeq"
        entry.set("p:$uid", "")
        retained.add(ServiceRowState(uid, null, customName, 1))
        newCustomUid = uid
    }
    rows.clear()
    rows.addAll(retained)
    return newCustomUid
}

internal fun applyRepeatedLastService(
    rows: SnapshotStateList<ServiceRowState>,
    entry: NumberEntryState,
    lastService: RideLog,
    catalogLabels: Map<String, String>
) {
    rows.clear()
    lastService.serviceLines.forEachIndexed { index, line ->
        val catId = line.catalogueId
        val uid = "${catId ?: "custom"}-r$index"
        val label = catId?.let { catalogLabels[it] } ?: line.displayName
        rows.add(ServiceRowState(uid, catId, label, line.quantity.roundToInt().coerceIn(1, 20), line.detail))
        entry.set("p:$uid", line.unitPricePoisha?.let { formatServiceNumber(it / 100.0) }.orEmpty())
    }
}

internal fun applyParsedServiceLines(
    rows: SnapshotStateList<ServiceRowState>,
    entry: NumberEntryState,
    state: VaultState,
    parsedLines: List<ParsedLine>,
    catalogLabels: Map<String, String>
) {
    val base = rows.size
    parsedLines.forEachIndexed { index, parsed ->
        val catId = parsed.item
        val uid = "$catId-p${base + index}"
        rows.add(ServiceRowState(uid, catId, catalogLabels[catId] ?: catId, parsed.qty.roundToInt().coerceIn(1, 20)))
        val price = parsed.price ?: state.lastItemPrices[catId]
        entry.set("p:$uid", price?.let(::formatServiceNumber).orEmpty())
    }
}
