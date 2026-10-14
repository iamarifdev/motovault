package com.motovault.app.ui

import kotlin.test.*

class ServicePassportTest {
    private val identity = PassportIdentity("আরিফ", "Honda", "CB150R", "ABS", 2024, "ঢাকা-১২", "C1", "E1", passportNumber(5000.25))
    private val confirmation = "2026-10-12T00:00:00Z"
    private val earlier = RideLog("Engine oil", "Service recorded", "2026-09-01", 4000.5, "a", "service", cost = 1234.56, workshop = "ঢাকা গ্যারেজ", notes = "Filter \"checked\"\nOK", componentId = "oil")
    private val later = earlier.copy(id = "b", date = "2026-10-01", odometer = 5000.0)
    private val state = VaultState(ride = Ride("honda", "ABS", 2024, 5000.25), logs = listOf(later, earlier, RideLog("Fuel fill-up", "5 L", "2026-10-02", 5100.0, "fuel", "fuel", liters = 5.0, cost = 500.0)))
    private fun export(state: VaultState = this.state) = ExportServicePassportUseCase().execute(state, identity, true, confirmation)

    @Test fun shaMatchesPublishedVectorsAcrossBlockBoundaries() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", sha256(byteArrayOf()))
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", sha256("abc".encodeToByteArray()))
        assertEquals("248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1", sha256("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq".encodeToByteArray()))
    }

    @Test fun chronologicalOrderIsStableAndFuelAndOriginalsAreExcluded() {
        val bundle = export()
        assertEquals(listOf("a","b"), bundle.passport.services.map { it.id })
        assertEquals(bundle, export(state.copy(logs = state.logs.reversed(), documents = listOf(VaultDocument("Tax token","SECRET","Owner","", originalPdf = "private.pdf")))))
        assertFalse(canonicalPassport(bundle.passport).contains("SECRET"))
        assertFalse(canonicalPassport(bundle.passport).contains("Fuel fill-up"))
    }

    @Test fun everyCoveredServiceFieldChangesDigest() {
        val original = export().sha256
        val changes = listOf(earlier.copy(id="changed"), earlier.copy(date="2026-09-02"), earlier.copy(title="Changed"), earlier.copy(detail="Changed"), earlier.copy(odometer=4000.5000000001), earlier.copy(cost=1234.560000001), earlier.copy(workshop="Changed"), earlier.copy(notes="Changed"), earlier.copy(componentId=null))
        for(change in changes) assertNotEquals(original, export(state.copy(logs = listOf(later,change))).sha256)
    }

    @Test fun verificationRejectsTamperingAndDifferentIndependentDigest() {
        val bundle = export(); val json = encodePassportBundle(bundle)
        assertEquals(bundle, verifyPassportBundle(json).bundle)
        assertFalse(verifyPassportBundle(json).matchesExpected)
        assertTrue(verifyPassportBundle(json,bundle.sha256).matchesExpected)
        assertTrue(verifyPassportBundle(json,"motovault:passport:v2:${bundle.sha256}").matchesExpected)
        assertTrue(verifyPassportBundle(json,"motovault:passport:v1:${bundle.sha256}").matchesExpected)
        assertFailsWith<IllegalArgumentException> { verifyPassportBundle(json,"motovault:passport:v3:${bundle.sha256}") }
        assertFailsWith<IllegalArgumentException> { verifyPassportBundle(json.replace("CB150R","CB160R")) }
        assertFailsWith<IllegalArgumentException> { verifyPassportBundle(json,"0".repeat(64)) }
    }

    @Test fun ownerConfirmationAndValidNumericValuesAreRequired() {
        assertFailsWith<IllegalArgumentException> { ExportServicePassportUseCase().execute(state,identity,false,confirmation) }
        assertFailsWith<IllegalArgumentException> { export(state.copy(logs=listOf(earlier.copy(cost=Double.NaN)))) }
        assertFailsWith<IllegalArgumentException> { passportNumberValue("7ff0000000000000") }
    }

    @Test fun defaultsNeverUseUnconfirmedExtractionOrLicenceIdentity() {
        val document = VaultDocument("Registration smart card","N","Confirmed owner","", details=mapOf("chassis" to "C"), detailsConfirmed=true)
        assertEquals("", passportDefaults(state.copy(documents=listOf(document.copy(detailsConfirmed=false)))).owner)
        assertEquals("", passportDefaults(state.copy(documents=listOf(document.copy(title="Driving licence")))).chassis)
        assertEquals("C", passportDefaults(state.copy(documents=listOf(document))).chassis)
    }

    @Test fun unicodeQuotesNewlinesAndExactNumericBitsRoundTrip() {
        val bundle = export()
        assertEquals(bundle, verifyPassportBundle(encodePassportBundle(bundle)).bundle)
        assertTrue(canonicalPassport(bundle.passport).contains("আরিফ"))
        assertTrue(canonicalPassport(bundle.passport).contains("\\\"checked\\\"\\nOK"))
        assertEquals(earlier.cost.toBits(),passportNumberValue(bundle.passport.services.first().costBdtBits).toBits())
        assertEquals("3ff0000000000000",passportNumber(1.0))
        // Independently generated with Python json.dumps(ensure_ascii=False,separators=(',',':')) + hashlib.
        assertEquals("81ba0bf7580232c5dd42d82b45679c45f365de36b09f813deef37915c1475107", export(state.copy(logs=emptyList())).sha256)
    }

    @Test fun schemaAndUnknownFieldsCannotSilentlyChangeVerificationCoverage() {
        val json = encodePassportBundle(export())
        assertFails { verifyPassportBundle(json.replace("motovault-service-passport-v2","motovault-service-passport-v1")) }
        assertFails { verifyPassportBundle(json.replace("\"passport\":{", "\"passport\":{\"extra\":\"uncovered\",")) }
    }

    @Test fun legacyV1PassportsRemainVerifiable() {
        val current = export().passport
        val legacy = current.copy(schema = "motovault-service-passport-v1")
        val bundle = PassportBundle(legacy, sha256(canonicalPassport(legacy).encodeToByteArray()))
        assertFalse(encodePassportBundle(bundle).contains("\"items\""))
        assertEquals(bundle, verifyPassportBundle(encodePassportBundle(bundle)).bundle)
        val emptyLegacy = export(state.copy(logs = emptyList())).passport.copy(schema = "motovault-service-passport-v1")
        assertEquals("23e3b64ce02580cc0dee5ba1a6c886200b211df3041f1dfe4cff1dbce6ff8317", sha256(canonicalPassport(emptyLegacy).encodeToByteArray()))
    }

    @Test fun identityAndConfirmationAreAlsoCovered() {
        val bundle = export()
        val changes = listOf(identity.copy(owner="Other"), identity.copy(brand="Other"), identity.copy(model="Other"), identity.copy(edition="Other"), identity.copy(year=2025), identity.copy(registration="Other"), identity.copy(chassis="Other"), identity.copy(engine="Other"), identity.copy(odometerKmBits=passportNumber(5000.25000001)))
        changes.forEach { changed -> assertNotEquals(bundle.sha256, ExportServicePassportUseCase().execute(state,changed,true,confirmation).sha256) }
        assertNotEquals(bundle.sha256, ExportServicePassportUseCase().execute(state,identity,true,"2026-10-12T00:00:01Z").sha256)
    }
}
