package com.motovault.app.ui

import com.motovault.app.domain.model.BaselineCertainty
import com.motovault.app.domain.model.ComponentRule
import com.motovault.app.domain.model.FuelDefaults
import com.motovault.app.domain.model.Ride
import com.motovault.app.domain.vault.VaultState
import kotlinx.datetime.LocalDate

fun VaultState.Companion.emptyData(): VaultState = VaultState()


fun VaultState.Companion.seedData(today: LocalDate = today()): VaultState {
        val d = { n: Int -> LocalDate.fromEpochDays(today.toEpochDays() + n).toString() }
        val fuel = { id: String, day: Int, odo: Double, liters: Double, full: Boolean, place: String ->
            val cost = kotlin.math.round((liters * 120.0) / 10.0) * 10.0
            RideLog(
                id = id,
                title = "Fuel fill-up",
                detail = "$liters L · ${if (full) "Full tank" else "Partial fill"}",
                date = d(day),
                odometer = odo,
                kind = "fuel",
                liters = liters,
                fullTank = full,
                cost = cost,
                fuelType = "octane",
                fuelPrice = 120.0,
                locationLabel = place
            )
        }
        val logs = listOf(
            fuel("f7", -11, 12340.0, 4.9, true, "পদ্মা পাম্প, মিরপুর"),
            fuel("f6", -17, 12120.0, 5.2, true, "পদ্মা পাম্প, মিরপুর"),
            fuel("f5", -27, 11890.0, 5.0, true, "যমুনা ফিলিং স্টেশন"),
            fuel("f4", -33, 11700.0, 3.8, false, "যমুনা ফিলিং স্টেশন"),
            fuel("f3", -41, 11500.0, 4.8, true, "পদ্মা পাম্প, মিরপুর"),
            fuel("f2", -52, 11290.0, 4.7, true, "পদ্মা পাম্প, মিরপুর"),
            fuel("f1", -64, 11080.0, 5.0, true, "যমুনা ফিলিং স্টেশন"),
            RideLog(
                id = "s1",
                title = "Service",
                detail = "ইঞ্জিন অয়েল ও ফিল্টার",
                date = d(-45),
                odometer = 11350.0,
                kind = "service",
                workshop = "রহমান মোটরস, মিরপুর ১০",
                serviceLabourPoisha = 20000,
                serviceMiscellaneousPoisha = 0,
                serviceDiscountPoisha = 0,
                serviceTotalPoisha = 135000,
                cost = 1350.0,
                serviceLines = listOf(
                    ServiceLineItem(catalogueId = "engine_oil", customName = "ইঞ্জিন অয়েল", action = "replace", quantity = 1.0, unitPricePoisha = 90000, detail = "10W-40"),
                    ServiceLineItem(catalogueId = "oil_filter", customName = "অয়েল ফিল্টার", action = "replace", quantity = 1.0, unitPricePoisha = 25000)
                ),
                scheduleIds = listOf("r-oil")
            )
        )
        val rules = listOf(
            ComponentRule("r-oil", "primary", "ইঞ্জিন অয়েল ও ফিল্টার", "ইঞ্জিন অয়েল ও ফিল্টার", 3000.0, 180, 11350.0, today.toEpochDays().toLong() - 45),
            ComponentRule("r-chain", "primary", "চেইন পরিষ্কার ও লুব্রিকেন্ট", "চেইন পরিষ্কার ও লুব্রিকেন্ট", 500.0, 30, 11520.0, today.toEpochDays().toLong() - 41),
            ComponentRule("r-air", "primary", "এয়ার ফিল্টার", "এয়ার ফিল্টার", 5000.0, 180, 9900.0, today.toEpochDays().toLong() - 140),
            ComponentRule("r-spark", "primary", "স্পার্ক প্লাগ", "স্পার্ক প্লাগ", 8000.0, 365, 8000.0, today.toEpochDays().toLong() - 200),
            ComponentRule("r-brakes", "primary", "ব্রেক প্যাড", "ব্রেক প্যাড", 10000.0, 365, 7200.0, today.toEpochDays().toLong() - 300)
        ).map { it.copy(baselineCertainty = com.motovault.app.domain.model.BaselineCertainty.CONFIRMED) }
        val docs = listOf(
            VaultDocument("রেজিস্ট্রেশন স্মার্ট কার্ড", "ঢাকা মেট্রো-ল ৬২-৪৫১৮", "মো. রাফিদ হাসান", d(400), "d1", reminder = true, kind = "registration"),
            VaultDocument("ড্রাইভিং লাইসেন্স", "DL-1987-654321", "মো. রাফিদ হাসান", d(24), "d2", reminder = false, kind = "licence"),
            VaultDocument("ট্যাক্স টোকেন", "TT-2025-88120", "মো. রাফিদ হাসান", d(-5), "d3", reminder = false, kind = "tax"),
            VaultDocument("বিমা", "INS-554-2291", "মো. রাফিদ হাসান", d(200), "d4", reminder = true, kind = "insurance")
        )
        return VaultState(
            ride = Ride("yamaha", "Official catalogue", 2023, 12340.0, "yamaha-r15-v4", "R15 V4"),
            rules = rules,
            logs = logs,
            documents = docs,
        lastPrices = FuelDefaults.legacyPrices(),
            lastItemPrices = mapOf(
                "engine_oil" to 900.0,
                "oil_filter" to 250.0,
                "chain_sprocket" to 350.0,
                "air_filter" to 400.0,
                "spark_plug" to 300.0,
                "brake_pad" to 650.0
            ),
            workshops = listOf("রহমান মোটরস, মিরপুর ১০", "স্টার সার্ভিস সেন্টার"),
            places = listOf("পদ্মা পাম্প, মিরপুর", "যমুনা ফিলিং স্টেশন")
        )
    }



suspend fun VaultState.Companion.restore(): VaultState = loadVault()?.let { decode(it) } ?: seedData()

fun VaultState.Companion.create(
        ride: Ride,
        baseline: Double? = ride.odometer,
        baselineDate: LocalDate = today(),
        baselineCertainty: com.motovault.app.domain.model.BaselineCertainty? = null
    ): VaultState {
        FactoryProfiles.validate(ride)
        require(ride.odometer.isFinite() && ride.odometer in 0.0..9999999.0)
        require(baseline == null || (baseline.isFinite() && baseline in 0.0..ride.odometer)) {
            "Last service mileage cannot exceed current mileage."
        }
        require(baselineDate <= today()) { "Baseline date cannot be in the future." }
        require(ride.modelId == null || BikeCatalogue.find(ride.modelId)?.brandId == ride.brandId) { "Choose a model from the selected brand." }
        val certainty = baselineCertainty ?: if (baseline == null) {
            com.motovault.app.domain.model.BaselineCertainty.UNKNOWN
        } else {
            com.motovault.app.domain.model.BaselineCertainty.CONFIRMED
        }
        val examples = listOf(
            Triple("Chain lubrication", 500.0, 30),
            Triple("Engine oil", 3000.0, 180),
            Triple("Air filter", 5000.0, 180)
        )
        return VaultState(ride = ride, rules = FactoryProfiles.find(ride.factoryProfileId)?.rules(baseline, baselineDate, certainty) ?: examples.map { (name, km, days) ->
            ComponentRule(recordId(), "primary", name, "Owner-defined example. Confirm against your manual.", km, days,
                baseline ?: 0.0, if (baseline == null) 0L else baselineDate.toEpochDays().toLong(),
                baselineCertainty = certainty,
                action = when (name) {
                    "Chain lubrication" -> com.motovault.app.domain.maintenance.MaintenanceAction.LUBRICATE
                    else -> com.motovault.app.domain.maintenance.MaintenanceAction.REPLACE
                },
                provenance = com.motovault.app.domain.maintenance.ScheduleProvenance(
                    source = com.motovault.app.domain.maintenance.ScheduleSource.OWNER,
                    label = "Owner-defined example"
                )
            )
        })
    }


