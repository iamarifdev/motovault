package com.motovault.app.domain.garage

import com.motovault.app.domain.model.ComponentRule
import com.motovault.app.domain.model.GarageBikeRecord
import com.motovault.app.domain.model.Ride
import com.motovault.app.domain.model.RideLog
import com.motovault.app.domain.model.VaultDocument
import com.motovault.app.domain.model.assetIds
import com.motovault.app.domain.model.recordId
import com.motovault.app.domain.vault.VaultState

/** Returns the active bike followed by all non-active bikes in the garage. */
fun VaultState.allBikes(): List<Ride> = listOfNotNull(ride) + garageBikes.map { it.ride }

/** Returns only non-archived bikes eligible for active selection, reminders, and mutations. */
fun VaultState.activeBikes(): List<Ride> = allBikes().filterNot { it.archived }

/** Returns all documents across the active bike, owner-scoped licence, and garage bikes. */
fun VaultState.allDocuments(): List<VaultDocument> =
    (documents + garageBikes.flatMap { it.documents }).distinctBy { it.id }

/** Returns all schedule rules across active and non-archived garage bikes for storage/sync tracking. */
fun VaultState.allRules(): List<ComponentRule> =
    (rules + garageBikes.flatMap { it.rules }).distinctBy { it.id }

/** Returns all log records across active and garage bikes for storage/sync tracking. */
fun VaultState.allLogs(): List<RideLog> =
    (logs + garageBikes.flatMap { it.logs }).distinctBy { it.id }

/** Returns every binary asset ID referenced by any bike or owner document. */
fun VaultState.allDocumentAssetIds(): Set<String> =
    allDocuments().flatMap { it.assetIds() }.toSet()

/** Returns owner documents plus bike-scoped documents belonging to non-archived bikes. */
fun VaultState.activeReminderDocuments(): List<VaultDocument> {
    val ownerDocs = allDocuments().filter { it.isOwnerScoped }
    val activeDocs = if (ride?.archived == true) emptyList() else documents.filterNot { it.isOwnerScoped }
    val otherActiveDocs = garageBikes.filterNot { it.ride.archived }.flatMap { it.documents }.filterNot { it.isOwnerScoped }
    return (ownerDocs + activeDocs + otherActiveDocs).distinctBy { it.id }
}

/** Transforms every document across the active bike and all garage bikes cleanly. */
fun VaultState.mapDocumentsAcrossGarage(transform: (VaultDocument) -> VaultDocument): VaultState =
    copy(
        documents = documents.map(transform),
        garageBikes = garageBikes.map { bike -> bike.copy(documents = bike.documents.map(transform)) }
    )

/** Switches active bike to [targetBikeId], isolating bike records while retaining owner-scoped docs. */
fun VaultState.switchToBike(targetBikeId: String): VaultState {
    val current = ride ?: return this
    if (current.id == targetBikeId) return this
    val targetIndex = garageBikes.indexOfFirst { it.ride.id == targetBikeId }
    if (targetIndex < 0) return this
    val target = garageBikes[targetIndex]
    require(!target.ride.archived) { "Restore an archived bike before switching to it." }
    val ownerDocs = documents.filter { it.isOwnerScoped }
    val currentBikeDocs = documents.filterNot { it.isOwnerScoped }.map { it.copy(vehicleId = it.vehicleId ?: current.id) }
    val packedCurrent = GarageBikeRecord(
        ride = current,
        rules = rules.map { it.copy(vehicleId = current.id) },
        logs = logs.map { it.copy(vehicleId = current.id) },
        documents = currentBikeDocs,
        repairCases = repairCases.map { it.copy(vehicleId = current.id) },
        warranties = warranties.map { it.copy(vehicleId = current.id) }
    )
    val remaining = garageBikes.toMutableList().apply { set(targetIndex, packedCurrent) }
    val nextDocs = (ownerDocs + target.documents).distinctBy { it.id }
    return copy(
        ride = target.ride,
        rules = target.rules,
        logs = target.logs,
        documents = nextDocs,
        economy = VaultState.calculateEconomy(target.logs),
        lastFuelOdometer = target.logs.filter { it.isFuel }.maxOfOrNull { it.odometer },
        repairCases = target.repairCases,
        warranties = target.warranties,
        garageBikes = remaining
    )
}

/** Adds [newRide] with [initialRules] to the garage and makes it the active bike without dropping prior history. */
fun VaultState.addBike(newRide: Ride, initialRules: List<ComponentRule>): VaultState {
    val current = ride
    val existingIds = allBikes().map { it.id }.toSet()
    val candidateId = newRide.id.takeIf { it.isNotBlank() && it !in existingIds && (it != "primary" || current == null) }
        ?: "bike_${recordId()}"
    val ownerDocs = documents.filter { it.isOwnerScoped }
    val packedCurrent = current?.let {
        GarageBikeRecord(
            ride = it,
            rules = rules.map { rule -> rule.copy(vehicleId = it.id) },
            logs = logs.map { log -> log.copy(vehicleId = it.id) },
            documents = documents.filterNot { doc -> doc.isOwnerScoped }.map { doc -> doc.copy(vehicleId = doc.vehicleId ?: it.id) },
            repairCases = repairCases.map { rc -> rc.copy(vehicleId = it.id) },
            warranties = warranties.map { w -> w.copy(vehicleId = it.id) }
        )
    }
    val nextGarage = listOfNotNull(packedCurrent) + garageBikes
    val createdRide = newRide.copy(id = candidateId, archived = false)
    val scopedRules = initialRules.map { it.copy(vehicleId = candidateId) }
    return copy(
        ride = createdRide,
        rules = scopedRules,
        logs = emptyList(),
        documents = ownerDocs,
        economy = null,
        lastFuelOdometer = null,
        repairCases = emptyList(),
        warranties = emptyList(),
        garageBikes = nextGarage
    )
}

/** Archives or restores [bikeId]. Archiving the active bike promotes the next active bike automatically. */
fun VaultState.setBikeArchived(bikeId: String, archived: Boolean): VaultState {
    val current = ride ?: return this
    if (archived) {
        val currentBike = allBikes().firstOrNull { it.id == bikeId } ?: return this
        if (currentBike.archived) return this
        require(activeBikes().size > 1) { "Keep at least one active bike in your garage." }
        if (current.id == bikeId) {
            val replacement = garageBikes.first { !it.ride.archived }.ride.id
            val switched = switchToBike(replacement)
            return switched.copy(
                garageBikes = switched.garageBikes.map {
                    if (it.ride.id == bikeId) it.copy(ride = it.ride.copy(archived = true)) else it
                }
            )
        }
        return copy(
            garageBikes = garageBikes.map {
                if (it.ride.id == bikeId) it.copy(ride = it.ride.copy(archived = true)) else it
            }
        )
    }
    if (current.id == bikeId) return copy(ride = current.copy(archived = false))
    return copy(
        garageBikes = garageBikes.map {
            if (it.ride.id == bikeId) it.copy(ride = it.ride.copy(archived = false)) else it
        }
    )
}

/** Applies a bike-scoped [transform] to [targetBikeId] (or active bike if null) and rejects archived targets. */
fun VaultState.mutateBike(targetBikeId: String?, transform: (VaultState) -> VaultState): VaultState {
    val active = requireNotNull(ride) { "Add a bike first." }
    if (targetBikeId == null || targetBikeId == active.id) {
        require(!active.archived) { "Archived bike records cannot be modified until restored." }
        return transform(this)
    }
    val index = garageBikes.indexOfFirst { it.ride.id == targetBikeId }
    require(index >= 0) { "Target bike was not found in your garage." }
    val target = garageBikes[index]
    require(!target.ride.archived) { "Archived bike records cannot be modified until restored." }
    val ownerDocs = documents.filter { it.isOwnerScoped }
    val scoped = copy(
        ride = target.ride,
        rules = target.rules,
        logs = target.logs,
        documents = (ownerDocs + target.documents).distinctBy { it.id },
        economy = VaultState.calculateEconomy(target.logs),
        lastFuelOdometer = target.logs.filter { it.isFuel }.maxOfOrNull { it.odometer },
        repairCases = target.repairCases,
        warranties = target.warranties,
        garageBikes = emptyList()
    )
    val mutated = transform(scoped)
    val updatedTarget = GarageBikeRecord(
        ride = requireNotNull(mutated.ride),
        rules = mutated.rules,
        logs = mutated.logs,
        documents = mutated.documents.filterNot { it.isOwnerScoped },
        repairCases = mutated.repairCases,
        warranties = mutated.warranties
    )
    val updatedOwnerDocs = mutated.documents.filter { it.isOwnerScoped }
    val activeBikeDocs = documents.filterNot { it.isOwnerScoped }
    val updatedGarage = garageBikes.toMutableList().apply { set(index, updatedTarget) }
    return copy(
        documents = (updatedOwnerDocs + activeBikeDocs).distinctBy { it.id },
        garageBikes = updatedGarage,
        lastPrices = mutated.lastPrices,
        lastItemPrices = mutated.lastItemPrices,
        workshops = mutated.workshops,
        workshopContacts = mutated.workshopContacts,
        places = mutated.places,
        ledger = mutated.ledger,
        captureReceipts = mutated.captureReceipts
    )
}

/** Completes bike onboarding either as the initial bike or by adding another bike to an existing garage. */
fun VaultState.completeBikeOnboarding(created: VaultState): VaultState {
    if (this.ride == null) {
        return created.copy(
            documents = documents,
            appearance = appearance,
            language = language,
            digits = digits,
            haptics = haptics,
            reduceMotion = reduceMotion,
            textScale = textScale
        )
    }
    return addBike(requireNotNull(created.ride), created.rules)
}
