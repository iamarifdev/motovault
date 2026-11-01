package com.motovault.app.domain.backup

import com.motovault.app.domain.maintenance.MaintenanceAction
import com.motovault.app.domain.maintenance.SchedulePhase
import com.motovault.app.domain.maintenance.ScheduleProvenance
import com.motovault.app.domain.maintenance.ScheduleSource
import com.motovault.app.domain.model.BaselineCertainty
import com.motovault.app.domain.model.ComponentRule
import com.motovault.app.storage.backupSnapshot
import com.motovault.app.ui.AppLanguage
import com.motovault.app.ui.Digits
import com.motovault.app.ui.DocumentPage
import com.motovault.app.ui.Ride
import com.motovault.app.ui.RideLog
import com.motovault.app.ui.VaultDocument
import com.motovault.app.ui.VaultState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Synthetic V1 wire payloads; these must not be regenerated from the models under test. */
class BackupPayloadCompatibilityTest {
    private val json = Json { ignoreUnknownKeys = true }

    private val currentV1 = """
        {
          "ride":{"brandId":"honda","edition":"Standard","year":2020,"odometer":8000.0,"modelName":"Synthetic Bike"},
          "logs":[{"title":"Fuel fill-up","detail":"4.0 L","date":"2026-01-01","odometer":8000.0,"id":"log-1","kind":"fuel","liters":4.0}],
          "documents":[{"title":"Synthetic document","number":"SYN-1","holder":"Example","expiry":"2030-01-01","id":"doc-1",
            "pages":[{"originalId":"original.jpg","previewId":"preview.jpg","mimeType":"image/jpeg","width":100,"height":80,"side":"Front","acceptedId":"accepted.jpg"}],"originalPdf":"original.pdf"}],
          "preferences":{"language":"EN","digits":"EN","haptics":false},
          "autoLocation":true
        }
    """.trimIndent()

    private val olderV1 = """
        {"version":1,"ride":{"brandId":"honda","edition":"Standard","year":2020,"odometer":8000.0},
         "rules":[],"logs":[],"documents":[{"title":"Synthetic document","number":"SYN-1","holder":"Example","expiry":"2030-01-01","id":"doc-1"}]}
    """.trimIndent()

    @Test
    fun currentV1PayloadMatchesFrozenWireFieldsAndRoundTrips() {
        val state = VaultState(
            ride = Ride("honda", "Standard", 2020, 8000.0, modelName = "Synthetic Bike"),
            logs = listOf(RideLog("Fuel fill-up", "4.0 L", "2026-01-01", 8000.0, id = "log-1", kind = "fuel", liters = 4.0)),
            documents = listOf(VaultDocument("Synthetic document", "SYN-1", "Example", "2030-01-01", id = "doc-1",
                pages = listOf(DocumentPage("original.jpg", "preview.jpg", "image/jpeg", 100, 80, "Front", acceptedId = "accepted.jpg")),
                originalPdf = "original.pdf")),
            language = AppLanguage.EN, digits = Digits.EN, haptics = false, autoLocation = true
        )
        val snapshot = state.backupSnapshot()
        assertEquals(setOf("original.jpg", "preview.jpg", "accepted.jpg", "original.pdf"), snapshot.assetIds)
        assertEquals(json.parseToJsonElement(currentV1), json.parseToJsonElement(json.encodeToString(snapshot.payload)))
        assertEquals(snapshot.payload, json.decodeFromString<VaultExportPayload>(currentV1))
    }

    @Test
    fun olderV1PayloadKeepsMissingFieldDefaults() {
        val payload = json.decodeFromString<VaultExportPayload>(olderV1)
        assertEquals("doc-1", payload.documents.single().id)
        assertEquals(emptyList(), payload.documents.single().pages)
        assertEquals(BackupLanguage.BN, payload.preferences.language)
        assertEquals("SYSTEM", payload.appearance)
        assertFalse(payload.autoLocation)
    }

    @Test
    fun olderDocumentWithoutIdStillReceivesAnId() {
        val document = json.decodeFromString<BackupDocument>("""{"title":"Synthetic","number":"1","holder":"Example","expiry":"2030-01-01"}""")
        assertTrue(document.id.isNotBlank())
        assertTrue(document.id.contains('-'))
    }

    @Test
    fun olderLogWithoutIdStillReceivesAnIdAndDefaults() {
        val log = json.decodeFromString<BackupLog>("""{"title":"Fuel fill-up","detail":"4 L","date":"2026-01-01","odometer":1000.0}""")
        assertTrue(log.id.isNotBlank())
        assertTrue(log.id.contains('-'))
        assertTrue(log.fullTank)
        assertEquals(emptyList(), log.serviceLines)
    }

    @Test
    fun p04ActionAndProvenanceFieldsRoundTripThroughBackupPayload() {
        val rule = ComponentRule(
            "rule-1", "primary", "Engine oil", "manual reference", 5000.0, 0, 1000.0, 120L,
            baselineCertainty = BaselineCertainty.CONFIRMED,
            action = MaintenanceAction.REPLACE,
            provenance = ScheduleProvenance(ScheduleSource.MANUAL, "Yamaha R15 V4 owner's manual",
                "https://library.ymcapps.net/library/om/contents/pdf/10/BGN-F819V-E0_02.pdf", "BGN-F819V-E0", "8-5",
                "India-market manual; Bangladesh model"),
            phase = SchedulePhase.RUNNING_IN, oneTime = true, catalogueId = "engine_oil"
        )
        val state = VaultState(ride = Ride("yamaha", "BD", 2023, 1000.0, "yamaha-r15-v4", "R15 V4"), rules = listOf(rule))
        val payload = state.backupSnapshot().payload
        val decoded = json.decodeFromString<VaultExportPayload>(json.encodeToString(payload)).rules.single()
        assertEquals(MaintenanceAction.REPLACE, decoded.action)
        assertEquals(ScheduleSource.MANUAL, decoded.provenance.source)
        assertEquals("BGN-F819V-E0", decoded.provenance.revision)
        assertEquals(SchedulePhase.RUNNING_IN, decoded.phase)
        assertTrue(decoded.oneTime)
        assertEquals("engine_oil", decoded.catalogueId)
    }

    @Test
    fun preP04RuleWirePayloadUsesSafeActionDefaults() {
        val rule = json.decodeFromString<ComponentRule>(
            """{"id":"old","vehicleId":"primary","name":"Old rule","specification":"frozen","intervalKm":500.0,"intervalDays":30,"baselineCertainty":"CONFIRMED"}"""
        )
        assertEquals(MaintenanceAction.REPLACE, rule.action)
        assertEquals(ScheduleSource.OWNER, rule.provenance.source)
        assertEquals(SchedulePhase.RECURRING, rule.phase)
        assertFalse(rule.oneTime)
        assertNull(rule.catalogueId)
    }
}
