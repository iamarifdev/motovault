package com.motovault.app.domain.documents

import com.motovault.app.application.VaultCoordinator
import com.motovault.app.domain.garage.addBike
import com.motovault.app.domain.garage.allDocumentAssetIds
import com.motovault.app.domain.model.DocumentKinds
import com.motovault.app.domain.model.DocumentPage
import com.motovault.app.domain.model.DocumentRenewal
import com.motovault.app.domain.model.Ride
import com.motovault.app.domain.model.VaultDocument
import com.motovault.app.domain.model.isCardDocument
import com.motovault.app.domain.model.stableKind
import com.motovault.app.domain.model.today
import com.motovault.app.domain.vault.VaultState
import com.motovault.app.domain.vault.deleteDocument
import com.motovault.app.domain.vault.upsertDocumentByKind
import com.motovault.app.ui.DocExpiryStatus
import com.motovault.app.ui.documentExpiryInfo
import com.motovault.app.ui.passportDefaults
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DocumentUpsertTest {
    private fun baseState() = VaultState(
        ride = Ride("yamaha", "R15 V4", 2024, 5000.0, id = "primary")
    )

    @Test
    fun saveDocumentAcceptsBlankIdentityAndCardCheckHandlesCaseVariants() {
        assertTrue(isCardDocument("Registration Smart Card"))
        assertTrue(isCardDocument("Driving License"))
        assertFalse(isCardDocument("Tax token"))

        val saved = baseState().saveDocument(
            VaultDocument(
                title = "Registration smart card",
                number = "",
                holder = "",
                expiry = "2028-05-01",
                kind = DocumentKinds.REGISTRATION
            )
        )
        assertEquals(1, saved.documents.size)
        assertFailsWith<IllegalArgumentException> {
            baseState().saveDocument(VaultDocument("Tax token", "", "", "2026-01-01", issued = "2026-02-01"))
        }
        assertFailsWith<IllegalArgumentException> {
            baseState().saveDocument(
                VaultDocument(
                    "Driving licence", "", "", "2028-01-01",
                    pages = List(3) { DocumentPage("a$it.jpg", "p$it.jpg", "image/jpeg", 100, 100, "Front") }
                )
            )
        }
    }

    @Test
    fun upsertByKindPreservesOriginalsRenewalsIdentityAndMultiBikeScope() {
        val page = DocumentPage("front-1.jpg", "front-1-p.jpg", "image/jpeg", 1200, 800, "Front")
        val renewal = DocumentRenewal(
            documentId = "doc-reg-1",
            kind = DocumentKinds.REGISTRATION,
            previousExpiry = "2026-01-01",
            newExpiry = "2027-01-01",
            renewedOn = "2026-01-02",
            documentRevision = 1
        )
        val initial = baseState().copy(
            documents = listOf(
                VaultDocument(
                    title = "রেজিস্ট্রেশন স্মার্ট কার্ড",
                    number = "DM-11-2233",
                    holder = "Arif",
                    expiry = "2027-01-01",
                    id = "doc-reg-1",
                    pages = listOf(page),
                    originalPdf = "reg-1.pdf",
                    pdfPageCount = 1,
                    details = mapOf("chassis" to "CHS-99"),
                    detailsConfirmed = true,
                    reminder = true,
                    kind = DocumentKinds.REGISTRATION,
                    reminderSnoozedUntil = "2026-12-20T09:00",
                    revision = 2,
                    renewals = listOf(renewal),
                    checkedRenewalSteps = setOf("registration_official_sources"),
                    vehicleId = "primary"
                ),
                VaultDocument(
                    title = "Driving licence",
                    number = "DL-4455",
                    holder = "Arif",
                    expiry = "2027-06-01",
                    id = "doc-lic-1",
                    kind = DocumentKinds.LICENCE,
                    vehicleId = null
                )
            )
        )

        val upserted = initial.upsertDocumentByKind(
            kind = DocumentKinds.REGISTRATION,
            expiry = "2029-01-01",
            reminder = true,
            number = "",
            holder = ""
        )
        val updatedReg = upserted.documents.first { it.id == "doc-reg-1" }
        assertEquals("Registration smart card", updatedReg.title)
        assertEquals("DM-11-2233", updatedReg.number)
        assertEquals("Arif", updatedReg.holder)
        assertEquals("2029-01-01", updatedReg.expiry)
        assertEquals(listOf(page), updatedReg.pages)
        assertEquals("reg-1.pdf", updatedReg.originalPdf)
        assertEquals(listOf(renewal), updatedReg.renewals)
        assertEquals(setOf("registration_official_sources"), updatedReg.checkedRenewalSteps)
        assertEquals(mapOf("chassis" to "CHS-99"), updatedReg.details)
        assertEquals(3, updatedReg.revision)
        assertNull(updatedReg.reminderSnoozedUntil)
        assertTrue(updatedReg.detailsConfirmed)
    }

    @Test
    fun upsertingSecondBikeRegistrationIsolatesScopesAndRoundTripsBackup() {
        val page = DocumentPage("front-1.jpg", "front-1-p.jpg", "image/jpeg", 1200, 800, "Front")
        val initial = baseState().copy(
            documents = listOf(
                VaultDocument(
                    title = "Registration smart card",
                    number = "DM-11-2233",
                    holder = "Arif",
                    expiry = "2027-01-01",
                    id = "doc-reg-1",
                    pages = listOf(page),
                    details = mapOf("chassis" to "CHS-99"),
                    kind = DocumentKinds.REGISTRATION,
                    vehicleId = "primary"
                ),
                VaultDocument(
                    title = "Driving licence",
                    number = "DL-4455",
                    holder = "Arif",
                    expiry = "2027-06-01",
                    id = "doc-lic-1",
                    kind = DocumentKinds.LICENCE,
                    vehicleId = null
                )
            )
        )
        val secondBike = initial.addBike(
            Ride("honda", "SP 125", 2025, 200.0, id = "bike-2"),
            emptyList()
        ).upsertDocumentByKind(
            kind = DocumentKinds.REGISTRATION,
            expiry = "2030-03-01",
            reminder = true,
            number = "DM-99-0001",
            holder = "Arif"
        ).upsertDocumentByKind(
            kind = DocumentKinds.LICENCE,
            expiry = "2031-01-01",
            reminder = true
        )
        assertEquals("doc-lic-1", secondBike.documents.first { it.stableKind() == DocumentKinds.LICENCE }.id)
        val bike2Reg = secondBike.documents.first { it.stableKind() == DocumentKinds.REGISTRATION }
        assertEquals("bike-2", bike2Reg.vehicleId)
        assertTrue(bike2Reg.id != "doc-reg-1")
        val decoded = VaultState.decode(VaultState.encode(secondBike))
        assertNotNull(decoded)
        assertEquals("CHS-99", decoded.garageBikes.single().documents.single().details["chassis"])
        assertEquals(listOf(page), decoded.garageBikes.single().documents.single().pages)
    }

    @Test
    fun passportDefaultsMatchTranslatedRegistrationTitleViaStableKind() {
        val state = baseState().copy(
            documents = listOf(
                VaultDocument(
                    title = "রেজিস্ট্রেশন স্মার্ট কার্ড",
                    number = "DM-LA-1234",
                    holder = "Nadia Rahman",
                    expiry = "2028-01-01",
                    kind = DocumentKinds.REGISTRATION,
                    details = mapOf("chassis" to "CH-1", "engine" to "EN-2"),
                    detailsConfirmed = true
                )
            )
        )
        val defaults = passportDefaults(state)
        assertEquals("Nadia Rahman", defaults.owner)
        assertEquals("CH-1", defaults.chassis)
        assertEquals("EN-2", defaults.engine)
    }

    @Test
    fun deleteAndUndoPreservesReferencedAssetsWhileUndoRemainsAvailable() = runBlocking {
        val page = DocumentPage("doc-orig.jpg", "doc-prev.jpg", "image/jpeg", 800, 500, "Front")
        val base = baseState().copy(
            documents = listOf(
                VaultDocument(
                    title = "Tax token",
                    number = "",
                    holder = "",
                    expiry = "2028-01-01",
                    id = "doc-tax",
                    kind = DocumentKinds.TAX,
                    pages = listOf(page)
                )
            )
        )
        var durable = base
        val cleaned = mutableListOf<Set<String>>()
        val coordinator = VaultCoordinator(
            scope = this,
            initialState = VaultState(),
            restore = { base },
            write = { durable = it },
            assetIds = { it.allDocumentAssetIds() },
            cleanup = { cleaned += it },
            recover = {},
            reconcile = {}
        )
        val deleted = base.deleteDocument("doc-tax")
        coordinator.commit(deleted, 0) {}
        assertTrue(durable.documents.isEmpty())
        assertTrue(cleaned.flatten().isEmpty())

        var undone: Boolean? = null
        coordinator.undo(1) { undone = it }
        assertEquals(true, undone)
        assertEquals(listOf(page), durable.documents.single().pages)
    }

    @Test
    fun expiryInfoClassifiesOverdueSoonOkAndNoneAccurately() {
        val current = today()
        val overdue = VaultDocument("Tax token", "", "", LocalDate.fromEpochDays(current.toEpochDays() - 1).toString())
        val soonCustom = VaultDocument(
            "Tax token", "", "",
            LocalDate.fromEpochDays(current.toEpochDays() + 12).toString(),
            reminderWarningDays = 15
        )
        val okDoc = VaultDocument("Tax token", "", "", LocalDate.fromEpochDays(current.toEpochDays() + 45).toString())
        val noneDoc = VaultDocument("Tax token", "", "", "")

        assertEquals(DocExpiryStatus.OVERDUE, documentExpiryInfo(overdue, leadDays = 30).status)
        assertEquals(DocExpiryStatus.SOON, documentExpiryInfo(soonCustom, leadDays = 7).status)
        assertEquals(DocExpiryStatus.OK, documentExpiryInfo(okDoc, leadDays = 30).status)
        assertEquals(DocExpiryStatus.NONE, documentExpiryInfo(noneDoc, leadDays = 30).status)
    }

    @Test
    fun editingLegacyDocumentWithUnknownKindKeepsItsKindAndTitle() {
        val legacy = VaultDocument(title = "Route permit", number = "RP-1", holder = "", expiry = "2027-01-01", kind = "")
        assertNull(legacy.stableKind())
        val state = baseState().saveDocument(legacy)

        val edited = state.upsertDocumentByKind(
            kind = DocumentKinds.REGISTRATION,
            expiry = "2028-01-01",
            reminder = true,
            number = "RP-2",
            existingId = legacy.id
        ).documents.single()

        assertEquals("Route permit", edited.title)
        assertEquals("", edited.kind)
        assertEquals("2028-01-01", edited.expiry)
        assertEquals("RP-2", edited.number)
    }
}
