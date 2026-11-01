package com.motovault.app.domain.maintenance

import com.motovault.app.domain.model.BaselineCertainty
import com.motovault.app.domain.model.ComponentRule
import com.motovault.app.domain.usecase.LogMaintenanceUseCase
import kotlin.test.*

class MaintenanceProfileTest {
    private val r15 = requireNotNull(MaintenanceProfiles.find("yamaha-r15-v4-2023-bd-155"))

    private fun rule(id: String, action: MaintenanceAction, name: String = id, oneTime: Boolean = false) =
        ComponentRule(id, "primary", name, "test", 500.0, 0, 0.0, 0L,
            action = action, oneTime = oneTime, baselineCertainty = BaselineCertainty.CONFIRMED)

    @Test fun profileRequiresExactModelYearMarketAndCode() {
        assertTrue(r15.matches("yamaha-r15-v4", 2023, "BANGLADESH", "yzf155-a"))
        assertFalse(r15.matches("yamaha-r15-v4", 2022, "BANGLADESH", "YZF155-A"))
        assertFalse(r15.matches("yamaha-r15-v4", 2023, "INDIA", "YZF155-A"))
        assertFalse(r15.matches("yamaha-r15-v4", 2023, "BANGLADESH", "OTHER"))
        assertNull(MaintenanceProfiles.matching("honda-hornet-2-0", 2023, "BANGLADESH", "any"))
    }

    @Test fun everyManualScheduleCarriesSourceAndPage() {
        r15.schedules.forEach { spec ->
            assertTrue(spec.provenance.isManualBacked, spec.key)
            assertTrue(spec.provenance.url.startsWith("https://library.ymcapps.net/"), spec.key)
            assertTrue(spec.provenance.page.isNotBlank(), spec.key)
            assertTrue(spec.provenance.revision.isNotBlank(), spec.key)
        }
    }

    @Test fun runningInSchedulesOnlyApplyBelowFirstService() {
        assertTrue(r15.includesBaseline(null).any { it.phase == SchedulePhase.RUNNING_IN })
        assertTrue(r15.includesBaseline(0.0).any { it.oneTime })
        assertFalse(r15.includesBaseline(1000.0).any { it.phase == SchedulePhase.RUNNING_IN })
        assertTrue(r15.includesBaseline(1000.0).any { it.phase == SchedulePhase.RECURRING })
    }

    @Test fun actionsAreSeparateSchedules() {
        val chain = r15.schedules.filter { it.catalogueId in setOf("chain_lube", "chain_adjust") }
        assertEquals(setOf(MaintenanceAction.LUBRICATE, MaintenanceAction.ADJUST), chain.map { it.action }.toSet())
        assertNotEquals(chain[0].key, chain[1].key)
        assertTrue(r15.schedules.any { it.action == MaintenanceAction.REPLACE })
        assertTrue(r15.schedules.any { it.action == MaintenanceAction.INSPECT })
    }

    @Test fun serviceResetsOnlyTheSelectedAction() {
        val rules = listOf(
            rule("lube", MaintenanceAction.LUBRICATE),
            rule("adjust", MaintenanceAction.ADJUST),
            rule("oil", MaintenanceAction.REPLACE)
        )
        val result = LogMaintenanceUseCase().recordService(rules, setOf("lube"), 1200.0, 100L)
        assertEquals(1, result.resetCount)
        assertEquals(1200.0, result.updatedRules.first { it.id == "lube" }.lastServicedOdometerKm)
        assertEquals(0.0, result.updatedRules.first { it.id == "adjust" }.lastServicedOdometerKm)
        assertEquals(0.0, result.updatedRules.first { it.id == "oil" }.lastServicedOdometerKm)
        assertEquals(3, result.updatedRules.size)
    }

    @Test fun oneTimeRunningInScheduleIsRemovedAfterRealService() {
        val rules = listOf(
            rule("running", MaintenanceAction.REPLACE, oneTime = true),
            rule("oil", MaintenanceAction.REPLACE)
        )
        val result = LogMaintenanceUseCase().recordService(rules, setOf("running"), 1000.0, 100L)
        assertEquals(listOf("oil"), result.updatedRules.map { it.id })
        assertEquals(BaselineCertainty.CONFIRMED, result.updatedRules.single().baselineCertainty)
    }

    @Test fun unselectedCustomRuleKeepsItsBaselineAndInterval() {
        val custom = ComponentRule("custom", "primary", "My chain care", "Owner", 700.0, 14, 50.0, 10L,
            baselineCertainty = BaselineCertainty.OWNER_ESTIMATE)
        val result = LogMaintenanceUseCase().recordService(listOf(custom), setOf("other"), 900.0, 100L)
        assertEquals(custom, result.updatedRules.single())
    }
}
