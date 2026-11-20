package com.motovault.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.input.NormalizedNumbers
import com.motovault.app.domain.model.ComponentRule
import com.motovault.app.domain.model.VisitRecordState
import kotlinx.datetime.LocalDate
import motovault.composeapp.generated.resources.*

private enum class ServicePickerMode { NONE, PARTS, WORKSHOP }

@Composable
fun ServiceSheet(
    state: VaultState,
    initialItemKey: String? = null,
    initialRows: List<ParsedLine>? = null,
    existing: RideLog? = null,
    onDismiss: () -> Unit,
    onSave: (VaultState, String, () -> Unit) -> Unit,
    initialOdometer: Double? = null
) {
    val ride = state.ride ?: return
    val currentDay = today()
    val estimate = remember(ride, state.logs, currentDay) { WearCalculator.estimateOdometer(ride, state.logs, currentDay) }
    val selectedRule = state.rules.firstOrNull { it.id == initialItemKey }
    val initialCatalogueKey = selectedRule?.let {
        it.catalogueId ?: BangladeshServiceCatalogue.itemForScheduleName(it.name).id
    } ?: initialItemKey
    val catalogLabels = CATALOG_ITEMS.associate { it.key to appStringResource(it.label) }
    val rows = remember {
        mutableStateListOf<ServiceRowState>().apply {
            addAll(ServiceSaveMapper.buildInitialRows(existing, initialRows, initialCatalogueKey, catalogLabels))
        }
    }
    val entry = rememberNumberEntry(
        initialValues = remember { ServiceSaveMapper.buildInitialValues(existing, initialRows, rows, state, estimate, initialOdometer) },
        firstField = if (rows.isNotEmpty()) "p:${rows.first().uid}" else "odo"
    )
    ServiceSheetHost(state, existing, selectedRule, catalogLabels, rows, entry, currentDay, onDismiss, onSave)
}

@Composable
private fun ServiceSheetHost(
    state: VaultState,
    existing: RideLog?,
    selectedRule: ComponentRule?,
    catalogLabels: Map<String, String>,
    rows: SnapshotStateList<ServiceRowState>,
    entry: NumberEntryState,
    currentDay: LocalDate,
    onDismiss: () -> Unit,
    onSave: (VaultState, String, () -> Unit) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val haptic = currentHaptic
    val lastService = remember(state.logs, existing?.id) {
        state.logs.firstOrNull { it.kind == "service" && !it.isFuel && it.id != existing?.id }
    }
    var dateKey by remember { mutableStateOf(initialServiceDateKey(existing, currentDay)) }
    var customDate by remember { mutableStateOf(existing?.date ?: currentDay.toString()) }
    var odoTouched by remember { mutableStateOf(existing != null) }
    var workshop by remember { mutableStateOf(existing?.workshop.orEmpty()) }
    var workshopPhone by remember { mutableStateOf(initialServiceWorkshopPhone(state, existing)) }
    var visitState by remember { mutableStateOf(initialServiceVisitState(existing)) }
    var caseTitle by remember { mutableStateOf(initialServiceRepairCaseTitle(state, existing)) }
    var warrantyMonths by remember { mutableStateOf(inferWarrantyMonths(existing, state.warranties)) }
    var smartOpen by remember { mutableStateOf(false) }
    val capture = LocalOdometerCapture.current ?: rememberOdometerCaptureState()
    var pickerMode by remember { mutableStateOf(ServicePickerMode.NONE) }
    var moreOpen by remember {
        mutableStateOf(
            existing != null && (visitState.isOpenQuote ||
                workshopPhone.isNotBlank() || caseTitle.isNotBlank() || warrantyMonths > 0)
        )
    }
    var extrasOpen by remember { mutableStateOf(false) }
    var keysOpen by remember { mutableStateOf(false) }
    var openGradeUid by remember { mutableStateOf<String?>(null) }
    var customSeq by remember { mutableStateOf(0) }
    var saveIssue by remember { mutableStateOf<String?>(null) }

    val dueList = remember(state, currentDay) { dueServiceKeys(state, currentDay) }
    val dueSet = remember(dueList) { dueList.toSet() }
    val pickedCatalogueKeys = remember(rows.toList()) { rows.mapNotNull { it.catalogueId }.toSet() }
    val focusKeypadField: (String) -> Unit = { field -> focusManager.clearFocus(); entry.setActive(field); keysOpen = true }

    if (pickerMode == ServicePickerMode.PARTS) {
        ServicePartPickerSheet(pickedCatalogueKeys, state.lastItemPrices, dueSet, { keys, customName ->
            val newUid = applyServicePartSelection(rows, entry, state, keys, customName, customSeq, catalogLabels)
            if (newUid != null) { customSeq += 1; focusKeypadField("p:$newUid") }
            pickerMode = ServicePickerMode.NONE
        }) { pickerMode = ServicePickerMode.NONE }
        return
    }
    if (pickerMode == ServicePickerMode.WORKSHOP) {
        ServiceWorkshopPickerSheet(workshop, state.workshops, { workshop = it; pickerMode = ServicePickerMode.NONE }) {
            pickerMode = ServicePickerMode.NONE
        }
        return
    }

    val resolvedDate = resolveServiceDate(dateKey, customDate, currentDay)
    val odoVal = NormalizedNumbers.parse(entry.values["odo"].orEmpty())
    val miscPoisha = existing?.serviceMiscellaneousPoisha ?: 0
    val totals = calculateServiceTotals(rows, entry.values, miscPoisha)
    val resets = matchingResetRules(rows, state.rules)
    val reason = validateServiceDraft(existing?.id, existing != null, rows.size, odoVal, odoTouched, resolvedDate, currentDay)
    val baselineOrderMsg = appStringResource(Res.string.service_baseline_order)
    val saveErrorMsg = appStringResource(Res.string.service_save_error)
    val toastMsg = serviceSavedToastMessage(existing != null, visitState, totals.total, resets.size)

    Sheet(
        title = appStringResource(if (existing == null) Res.string.service_title else Res.string.service_edit_title),
        onDismiss = onDismiss,
        actions = {
            ServiceHeaderActions(
                cameraOpen = capture.open,
                smartOpen = smartOpen,
                hasLastService = lastService != null,
                onToggleCamera = {
                    smartOpen = false
                    keysOpen = false
                    if (capture.open) capture.close() else capture.start()
                },
                onToggleSmart = {
                    capture.close()
                    keysOpen = false
                    smartOpen = !smartOpen
                }
            ) {
                lastService?.let { applyRepeatedLastService(rows, entry, it, catalogLabels); keysOpen = false; haptic.perform(HapticKind.SUCCESS) }
            }
        },
        footer = {
            ServiceSaveFooter(totals, visitState, reason, keysOpen, { keysOpen = false }, { key ->
                if (entry.activeField == "odo") odoTouched = true
                entry.press(key)
            }) {
                val saved = runCatching {
                    ServiceSaveMapper.save(
                        state, existing, selectedRule?.id, rows.toList(), entry.values, requireNotNull(resolvedDate),
                        requireNotNull(odoVal), workshop, workshopPhone, visitState, caseTitle, warrantyMonths, currentDay
                    )
                }.getOrElse { error ->
                    saveIssue = if (error.message == "service_baseline_invalid") baselineOrderMsg else saveErrorMsg
                    haptic.perform(HapticKind.ERROR)
                    return@ServiceSaveFooter
                }
                haptic.perform(HapticKind.SUCCESS)
                onSave(saved, toastMsg) {}
            }
        }
    ) {
        AnimatedVisibility(visible = capture.open) {
            ServiceBillCameraCard(capture, catalogLabels) { parsedLines ->
                applyParsedServiceLines(rows, entry, state, parsedLines, catalogLabels)
                capture.close()
                haptic.perform(HapticKind.SUCCESS)
            }
        }
        ServiceSmartInputPanel(smartOpen) { parsedLines ->
            applyParsedServiceLines(rows, entry, state, parsedLines, catalogLabels)
            smartOpen = false
            haptic.perform(HapticKind.SUCCESS)
        }
        ServiceOdometerBlock(
            entry.values["odo"].orEmpty(), dateKey, resolvedDate, customDate, odoTouched,
            keysOpen && entry.activeField == "odo", { focusKeypadField("odo") },
            { odoTouched = true }, { dateKey = it }, { customDate = it }
        )
        if (rows.isEmpty()) {
            ServiceSuggestions(
                suggestedServiceKeys(dueList, state.lastItemPrices, pickedCatalogueKeys), dueSet, state.lastItemPrices,
                { key -> focusKeypadField("p:${addServiceSuggestionRow(rows, entry, state, key, catalogLabels[key] ?: key)}") },
                { keysOpen = false; pickerMode = ServicePickerMode.PARTS }
            )
        } else {
            val showExtras = extrasOpen || (existing?.serviceLabourPoisha ?: 0) > 0 ||
                (existing?.serviceDiscountPoisha ?: 0) > 0 || miscPoisha > 0
            ServiceBillCard(
                rows, entry, keysOpen, showExtras, miscPoisha, openGradeUid, focusKeypadField, { openGradeUid = it },
                { uid, grade -> updateServiceRow(rows, uid) { it.copy(detail = grade) } },
                { uid, delta -> updateServiceRow(rows, uid) { it.copy(qty = (it.qty + delta).coerceIn(1, 20)) } },
                { uid -> rows.removeAll { it.uid == uid } },
                { keysOpen = false; pickerMode = ServicePickerMode.PARTS },
                { extrasOpen = true; focusKeypadField("labour") }
            )
        }
        ServiceMetaAndWorkshop(
            resets, visitState, workshop, moreOpen, workshopPhone, caseTitle, warrantyMonths, saveIssue,
            { keysOpen = false; pickerMode = ServicePickerMode.WORKSHOP }, { keysOpen = false; moreOpen = !moreOpen },
            { keysOpen = false },
            { visitState = visitState.toggledQuote() },
            { workshopPhone = it }, { caseTitle = it }, { warrantyMonths = it }
        )
    }
}

@Composable
private fun serviceSavedToastMessage(isEdit: Boolean, visitState: VisitRecordState, total: Double, resetCount: Int): String {
    val f = currentFmt
    return when {
        isEdit -> appStringResource(Res.string.service_edit_saved)
        visitState == VisitRecordState.PAID && resetCount > 0 ->
            appStringResource(Res.string.service_added_with_resets, f.money(total), f.d(resetCount.toString()))
        else -> appStringResource(Res.string.service_added, f.money(total))
    }
}

@Composable
private fun ServiceHeaderActions(
    cameraOpen: Boolean,
    smartOpen: Boolean,
    hasLastService: Boolean,
    onToggleCamera: () -> Unit,
    onToggleSmart: () -> Unit,
    onRepeatLast: () -> Unit
) {
    VaultIconButton(
        icon = "fi_camera.svg",
        label = appStringResource(Res.string.service_camera_action),
        tone = if (cameraOpen) IconTone.TINT else IconTone.PLAIN,
        onClick = onToggleCamera
    )
    VaultIconButton(
        icon = "fi_mic.svg",
        label = appStringResource(Res.string.service_mic_action),
        tone = if (smartOpen) IconTone.TINT else IconTone.PLAIN,
        onClick = onToggleSmart
    )
    if (hasLastService) {
        VaultIconButton(icon = "fi_copy.svg", label = appStringResource(Res.string.service_like_previous), onClick = onRepeatLast)
    }
}

@Composable
private fun ServiceOdometerBlock(
    odoText: String,
    dateKey: EntryDateKey,
    resolvedDate: LocalDate?,
    customDate: String,
    odoTouched: Boolean,
    active: Boolean,
    onFocusOdo: () -> Unit,
    onConfirmOdo: () -> Unit,
    onSelectDateKey: (EntryDateKey) -> Unit,
    onCustomDateChange: (String) -> Unit
) {
    val f = currentFmt
    val dateLabel = when (dateKey) {
        EntryDateKey.TODAY -> appStringResource(Res.string.date_today)
        EntryDateKey.YESTERDAY -> appStringResource(Res.string.date_yesterday)
        EntryDateKey.CUSTOM -> resolvedDate?.let(f::date) ?: appStringResource(Res.string.cost_date)
    }
    val kmText = appStringResource(Res.string.fuel_distance_value, f.editableNumber(odoText))
    OdoRow(
        label = appStringResource(if (odoTouched) Res.string.service_odo_date_label else Res.string.service_odo_date_estimated_label),
        valueText = appStringResource(Res.string.service_odo_date_value, kmText, dateLabel),
        active = active,
        isError = false,
        onClick = onFocusOdo,
        trailing = if (!odoTouched) {
            { ConfirmPill(text = appStringResource(Res.string.odo_confirm), icon = "fi_check.svg", tone = PillTone.CONFIRM_PENDING, onClick = onConfirmOdo) }
        } else null
    )
    AnimatedVisibility(visible = active) {
        ChipLine(contentDescription = appStringResource(Res.string.cost_date)) {
            DateChips(dateKey = dateKey, customDate = customDate, onSelectKey = onSelectDateKey, onCustomDateChange = onCustomDateChange)
        }
    }
}

@Composable
private fun ServiceMetaAndWorkshop(
    resets: List<ComponentRule>,
    visitState: VisitRecordState,
    workshop: String,
    moreOpen: Boolean,
    workshopPhone: String,
    caseTitle: String,
    warrantyMonths: Int,
    saveIssue: String?,
    onOpenWorkshop: () -> Unit,
    onToggleMore: () -> Unit,
    onHideKeypad: () -> Unit,
    onToggleQuote: () -> Unit,
    onWorkshopPhoneChange: (String) -> Unit,
    onCaseTitleChange: (String) -> Unit,
    onWarrantyMonthsChange: (Int) -> Unit
) {
    if (resets.isNotEmpty() && visitState == VisitRecordState.PAID) {
        val names = resets.map { componentShortLabel(it) }.distinct().joinToString(", ")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            V3Icon(path = "feather_icon/fi_tool.svg", description = null, modifier = Modifier.size(14.dp), tint = HealthyColor)
            Text(text = appStringResource(Res.string.service_reset_note, names), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = HealthyColor)
        }
    }
    PickerField(
        text = workshop.ifBlank { appStringResource(Res.string.service_workshop_placeholder) },
        isSet = workshop.isNotBlank(),
        leadingIcon = "fi_tool.svg",
        onClick = onOpenWorkshop
    )
    ServiceMoreSection(
        moreOpen, visitState, workshopPhone, caseTitle, warrantyMonths, onToggleMore,
        onHideKeypad, onToggleQuote, onWorkshopPhoneChange, onCaseTitleChange, onWarrantyMonthsChange
    )
    saveIssue?.let { issue -> Text(text = issue, color = ErrorColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun ServiceSaveFooter(
    totals: ServiceTotals,
    visitState: VisitRecordState,
    reason: ServiceValidationReason?,
    keysOpen: Boolean,
    onHideKeys: () -> Unit,
    onPressKey: (String) -> Unit,
    onSaveClick: () -> Unit
) {
    val f = currentFmt
    val haptic = currentHaptic
    val unknownNote = if (totals.unknownPriceCount > 0) {
        appStringResource(Res.string.service_unknown_prices, f.d(totals.unknownPriceCount.toString()))
    } else ""
    val reasonText = serviceValidationText(reason)
    val saveLabel = if (visitState.isOpenQuote) appStringResource(Res.string.service_quote_save)
    else appStringResource(Res.string.service_save)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = appStringResource(Res.string.service_total) + unknownNote, fontSize = 14.sp, color = TextSecondary)
        Text(text = f.money(totals.total), fontSize = 21.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
    VaultButton(
        text = reasonText ?: saveLabel,
        icon = "fi_tool.svg",
        full = true,
        enabled = reason == null,
        onClick = { if (reason != null) haptic.perform(HapticKind.ERROR) else onSaveClick() }
    )
    AnimatedVisibility(visible = keysOpen) {
        Column {
            Box(Modifier.fillMaxWidth().height(26.dp).clickable(role = Role.Button, onClick = onHideKeys), contentAlignment = Alignment.Center) {
                V3Icon(path = "feather_icon/fi_chevron-down.svg", description = appStringResource(Res.string.service_hide), modifier = Modifier.size(20.dp), tint = TextTertiary)
            }
            VaultKeypad(onKey = onPressKey)
        }
    }
}

@Composable
private fun serviceValidationText(reason: ServiceValidationReason?): String? = when (reason) {
    ServiceValidationReason.RECORD_MISSING -> appStringResource(Res.string.service_record_missing)
    ServiceValidationReason.NO_ROWS -> appStringResource(Res.string.service_error_no_rows)
    ServiceValidationReason.INVALID_ODOMETER -> appStringResource(Res.string.service_error_km)
    ServiceValidationReason.CONFIRM_ODOMETER -> appStringResource(Res.string.fuel_confirm_odometer)
    ServiceValidationReason.INVALID_DATE -> appStringResource(Res.string.fuel_invalid_date)
    ServiceValidationReason.FUTURE_DATE -> appStringResource(Res.string.fuel_future_date)
    null -> null
}
