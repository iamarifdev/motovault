package com.motovault.app.domain.maintenance

/**
 * Small, source-checked profile set for P04. The manual intervals are base
 * intervals; local-condition shortening is an explicit owner edit, never an
 * inferred multiplier. Models without a profile keep the owner-defined fallback.
 */
object MaintenanceProfiles {
    private const val YAMAHA_APPLICABILITY =
        "India-market Yamaha owner's manual for the same model code; Bangladesh catalogue model. Confirm year, variant and local paperwork."

    private fun manual(
        label: String,
        url: String,
        revision: String,
        page: String,
        applicability: String = YAMAHA_APPLICABILITY
    ) = ScheduleProvenance(
        source = ScheduleSource.MANUAL,
        label = label,
        url = url,
        revision = revision,
        page = page,
        applicability = applicability
    )

    private val r15Manual = manual(
        "Yamaha R15 V4 owner's manual", "https://library.ymcapps.net/library/om/contents/pdf/10/BGN-F819V-E0_02.pdf",
        "BGN-F819V-E0", "8-2 to 8-6"
    )
    private val mt15Manual = manual(
        "Yamaha MT-15 V2 owner's manual", "https://library.ymcapps.net/library/om/contents/pdf/10/BXU-F819V-EB_02.pdf",
        "BXU-F819V-EB", "8-2 to 8-5"
    )

    private fun spec(
        key: String,
        name: String,
        action: MaintenanceAction,
        intervalKm: Double,
        intervalDays: Int,
        source: ScheduleProvenance,
        page: String,
        catalogueId: String? = null,
        phase: SchedulePhase = SchedulePhase.RECURRING,
        oneTime: Boolean = false,
        critical: Boolean = true
    ) = MaintenanceScheduleSpec(
        key = key,
        name = name,
        action = action,
        intervalKm = intervalKm,
        intervalDays = intervalDays,
        provenance = source.copy(page = page),
        phase = phase,
        oneTime = oneTime,
        isCritical = critical,
        catalogueId = catalogueId
    )

    private fun runningIn(source: ScheduleProvenance) = listOf(
        spec("running_in_oil", "Engine oil", MaintenanceAction.REPLACE, 1000.0, 0, source, "8-5",
            catalogueId = "engine_oil", phase = SchedulePhase.RUNNING_IN, oneTime = true),
        spec("running_in_filter", "Oil filter element", MaintenanceAction.REPLACE, 1000.0, 0, source, "8-5",
            catalogueId = "oil_filter", phase = SchedulePhase.RUNNING_IN, oneTime = true),
        spec("running_in_inspection", "First service inspection", MaintenanceAction.INSPECT, 1000.0, 0, source, "8-2 to 8-6",
            catalogueId = "first_service", phase = SchedulePhase.RUNNING_IN, oneTime = true, critical = false)
    )

    private fun commonSchedules(source: ScheduleProvenance): List<MaintenanceScheduleSpec> = listOf(
        spec("engine_oil", "Engine oil", MaintenanceAction.REPLACE, 5000.0, 0, source, "8-5", "engine_oil"),
        spec("oil_filter", "Oil filter element", MaintenanceAction.REPLACE, 5000.0, 0, source, "8-5", "oil_filter"),
        spec("air_filter", "Air filter element", MaintenanceAction.REPLACE, 17000.0, 0, source, "8-3", "air_filter"),
        spec("spark_plug", "Spark plug", MaintenanceAction.REPLACE, 17000.0, 0, source, "8-2", "spark_plug"),
        spec("chain_lubricate", "Drive chain lubrication", MaintenanceAction.LUBRICATE, 500.0, 0, source, "8-4", "chain_lube"),
        spec("chain_adjust", "Drive chain adjustment", MaintenanceAction.ADJUST, 500.0, 0, source, "8-4", "chain_adjust"),
        spec("brake_fluid", "Brake fluid", MaintenanceAction.REPLACE, 0.0, 730, source, "8-3", "brake_fluid"),
        spec("coolant", "Coolant", MaintenanceAction.REPLACE, 0.0, 730, source, "8-5", "coolant"),
        spec("brake_hose", "Brake hose", MaintenanceAction.REPLACE, 0.0, 1460, source, "8-3", "brake_hose"),
        spec("fork_oil", "Front fork oil", MaintenanceAction.REPLACE, 13000.0, 0, source, "8-5", "fork_oil"),
        spec("valve_clearance", "Valve clearance", MaintenanceAction.INSPECT, 5000.0, 0, source, "8-2", "valve_clearance"),
        spec("tyres_brakes", "Tyres, brakes and chassis", MaintenanceAction.INSPECT, 5000.0, 0, source, "8-3 to 8-4", "tyre_brake_inspect", critical = false)
    )

    private val ktmManual = manual(
        "KTM 125 DUKE 2023 owner's manual", "https://www.ktmshop.se/bike-manuals/23_3214785_en_OM.pdf",
        "3214785en", "section 10.2",
        "Europe-market manual; model codes F4003W1/F4003W2/F4003W5/F4003W6/F4022W3/F4022W4/F4022W5/F4022W6."
    )

    val profiles = listOf(
        MaintenanceProfile(
            id = "yamaha-r15-v4-2023-bd-155",
            modelId = "yamaha-r15-v4",
            modelName = "Yamaha R15 V4",
            year = 2023,
            marketCode = "BANGLADESH",
            modelCodes = setOf("YZF155-A"),
            manualTitle = "Yamaha R15 V4 owner's manual, BGN-F819V-E0",
            manualUrl = r15Manual.url,
            applicability = r15Manual.applicability,
            schedules = runningIn(r15Manual) + commonSchedules(r15Manual)
        ),
        MaintenanceProfile(
            id = "yamaha-mt-15-v2-2027-bd-155",
            modelId = "yamaha-mt-15-v2",
            modelName = "Yamaha MT-15 V2",
            year = 2027,
            marketCode = "BANGLADESH",
            modelCodes = setOf("MTN155-A"),
            manualTitle = "Yamaha MT-15 V2 owner's manual, BXU-F819V-EB",
            manualUrl = mt15Manual.url,
            applicability = mt15Manual.applicability,
            schedules = runningIn(mt15Manual) + commonSchedules(mt15Manual)
        ),
        MaintenanceProfile(
            id = "ktm-125-duke-2023-eu-3214785",
            modelId = "ktm-duke-125-2023",
            modelName = "KTM 125 DUKE",
            year = 2023,
            marketCode = "EUROPE",
            modelCodes = setOf("F4003W1", "F4003W2", "F4003W5", "F4003W6", "F4022W3", "F4022W4", "F4022W5", "F4022W6"),
            manualTitle = "KTM 125 DUKE 2023 manual 3214785en",
            manualUrl = ktmManual.url,
            applicability = ktmManual.applicability,
            schedules = listOf(
                spec("engine_oil", "Engine oil", MaintenanceAction.REPLACE, 7500.0, 365, ktmManual, "10.2", "engine_oil"),
                spec("chain_care", "Drive chain care", MaintenanceAction.LUBRICATE, 500.0, 30, ktmManual, "10.2", "chain_sprocket", critical = false)
            )
        )
    )

    fun find(id: String?): MaintenanceProfile? = profiles.firstOrNull { it.id == id }

    fun available(modelId: String?, year: Int?, marketCode: String): List<MaintenanceProfile> =
        profiles.filter { it.modelId == modelId && it.year == year && it.marketCode == marketCode }

    fun matching(modelId: String?, year: Int?, marketCode: String, modelCode: String): MaintenanceProfile? =
        available(modelId, year, marketCode).firstOrNull { modelCode.trim().uppercase() in it.modelCodes }
}
