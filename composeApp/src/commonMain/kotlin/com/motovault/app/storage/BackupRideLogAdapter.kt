package com.motovault.app.storage

import com.motovault.app.domain.backup.*
import com.motovault.app.ui.*

internal fun Ride.toBackupRide() = BackupRide(
    brandId, edition, year, odometer, modelId, modelName, registration,
    BackupRideMarket.valueOf(market.name), BackupRideEmission.valueOf(emission.name),
    modelCode, factoryProfileId, odometerConfirmedDate
)

internal fun BackupRide.toRide() = Ride(
    brandId, edition, year, odometer, modelId, modelName, registration,
    RideMarket.valueOf(market.name), RideEmission.valueOf(emission.name),
    modelCode, factoryProfileId, odometerConfirmedDate
)

internal fun backupRideDisplayName(ride: BackupRide): String = ride.toRide().displayName

private fun EngineOilMetadata.toBackupOil() = BackupOilMetadata(
    baseType, saeGrade, apiStandard, jasoStandard, productName, purchaseBasis, packVolumeMl, boughtQuantity, usedQuantityMl
)
private fun BackupOilMetadata.toOil() = EngineOilMetadata(
    baseType, saeGrade, apiStandard, jasoStandard, productName, purchaseBasis, packVolumeMl, boughtQuantity, usedQuantityMl
)
private fun ServiceLineItem.toBackupLine() = BackupServiceLine(
    catalogueId, customName, action, quantity, unitBn, unitPricePoisha, scheduleId, resetsScheduleBaseline, oil?.toBackupOil(), detail
)
private fun BackupServiceLine.toServiceLine() = ServiceLineItem(
    catalogueId, customName, action, quantity, unitBn, unitPricePoisha, scheduleId, resetsScheduleBaseline, oil?.toOil(), detail
)

internal fun RideLog.toBackupLog() = BackupLog(
    title, detail, date, odometer, id, kind, liters, fullTank, cost, workshop, notes, componentId,
    serviceLines.map { it.toBackupLine() }, serviceLabourPoisha, serviceMiscellaneousPoisha,
    serviceDiscountPoisha, serviceTotalPoisha, scheduleIds,
    location?.let { BackupLocation(it.latitude, it.longitude, it.accuracyMeters, it.capturedAtEpochMillis, it.source) },
    locationLabel, receiptBase64, fuelType, fuelPrice
)

internal fun BackupLog.toRideLog() = RideLog(
    title, detail, date, odometer, id, kind, liters, fullTank, cost, workshop, notes, componentId,
    serviceLines.map { it.toServiceLine() }, serviceLabourPoisha, serviceMiscellaneousPoisha,
    serviceDiscountPoisha, serviceTotalPoisha, scheduleIds,
    location?.let { GeoCapture(it.latitude, it.longitude, it.accuracyMeters, it.capturedAtEpochMillis, it.source) },
    locationLabel, receiptBase64, fuelType, fuelPrice
)
