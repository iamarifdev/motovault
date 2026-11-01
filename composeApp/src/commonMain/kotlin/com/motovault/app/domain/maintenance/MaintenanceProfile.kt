package com.motovault.app.domain.maintenance

/**
 * One source-backed schedule owned by a model profile. The interval and action
 * are kept together so a service visit can reset only the action it performed.
 */
data class MaintenanceScheduleSpec(
    val key: String,
    val name: String,
    val action: MaintenanceAction,
    val intervalKm: Double,
    val intervalDays: Int,
    val provenance: ScheduleProvenance,
    val phase: SchedulePhase = SchedulePhase.RECURRING,
    val oneTime: Boolean = false,
    val isCritical: Boolean = true,
    val catalogueId: String? = null
) {
    init {
        require(key.isNotBlank()) { "Schedule key is required." }
        require(name.isNotBlank()) { "Schedule name is required." }
        require(intervalKm.isFinite() && intervalKm >= 0.0) { "Distance interval must be finite." }
        require(intervalDays >= 0) { "Time interval must not be negative." }
        require(intervalKm > 0.0 || intervalDays > 0) { "A schedule needs a distance or time interval." }
        require(!oneTime || phase == SchedulePhase.RUNNING_IN) { "Only a running-in schedule can be one-time." }
    }
}

data class MaintenanceProfile(
    val id: String,
    val modelId: String,
    val modelName: String,
    val year: Int,
    val marketCode: String,
    val modelCodes: Set<String>,
    val manualTitle: String,
    val manualUrl: String,
    val applicability: String,
    val schedules: List<MaintenanceScheduleSpec>
) {
    fun matches(modelId: String?, year: Int?, marketCode: String, modelCode: String): Boolean =
        this.modelId == modelId && this.year == year && this.marketCode == marketCode &&
            modelCode.trim().uppercase() in modelCodes

    fun includesBaseline(baselineKm: Double?): List<MaintenanceScheduleSpec> = schedules.filter { spec ->
        spec.phase == SchedulePhase.RECURRING || baselineKm == null || baselineKm < RUNNING_IN_LIMIT_KM
    }

    companion object {
        const val RUNNING_IN_LIMIT_KM = 1000.0
    }
}
