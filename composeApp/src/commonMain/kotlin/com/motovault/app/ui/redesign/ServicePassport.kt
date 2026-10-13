package com.motovault.app.ui.redesign

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PassportIdentity(
    val owner: String, val brand: String, val model: String, val edition: String, val year: Int,
    val registration: String, val chassis: String, val engine: String, val odometerKmBits: String
)

@Serializable
data class PassportService(
    val id: String, val date: String, val title: String, val detail: String,
    val odometerKmBits: String, val costBdtBits: String, val workshop: String, val notes: String,
    val componentId: String?
)

@Serializable
data class ServicePassport(
    val schema: String = "motovault-service-passport-v1",
    val confirmedAt: String, val identity: PassportIdentity, val services: List<PassportService>,
    val declaration: String = PASSPORT_DECLARATION
)

@Serializable
data class PassportBundle(val passport: ServicePassport, val sha256: String)

const val PASSPORT_DECLARATION = "Owner-reviewed records. Not independently certified. The digest proves integrity relative to a known digest, not that a service occurred."
private val passportJson = Json { encodeDefaults = true; explicitNulls = true; ignoreUnknownKeys = false }

// Fixed DTO declaration order, no whitespace, UTF-8, JSON escaping, no Unicode normalization.
// Binary64 hex strings cover the exact saved number, including fractional values, on both platforms.
fun passportNumber(value: Double): String {
    require(value.isFinite() && value >= 0) { "Mileage and costs must be finite and non-negative." }
    return value.toBits().toULong().toString(16).padStart(16, '0')
}
fun passportNumberValue(value: String): Double {
    require(value.matches(Regex("[0-9a-f]{16}"))) { "Invalid passport number." }
    return Double.fromBits(value.toULong(16).toLong()).also { passportNumber(it) }
}
fun canonicalPassport(passport: ServicePassport): String = passportJson.encodeToString(ServicePassport.serializer(), passport)
fun encodePassportBundle(bundle: PassportBundle): String = passportJson.encodeToString(PassportBundle.serializer(), bundle)

fun passportDefaults(state: VaultState): PassportIdentity {
    val ride = requireNotNull(state.ride) { "Set up your motorcycle first." }
    val registration = state.documents.firstOrNull { it.title == "Registration smart card" && it.detailsConfirmed }
    return PassportIdentity(registration?.holder.orEmpty(), ride.brandId, ride.displayName, ride.edition, ride.year,
        ride.registration.ifBlank { registration?.details?.get("registration").orEmpty() },
        registration?.details?.get("chassis").orEmpty(), registration?.details?.get("engine").orEmpty(), passportNumber(ride.odometer))
}

class ExportServicePassportUseCase {
    fun execute(state: VaultState, reviewed: PassportIdentity, ownerConfirmed: Boolean, confirmedAt: String = Clock.System.now().toString()): PassportBundle {
        require(ownerConfirmed) { "Confirm the identity and service history before exporting." }
        requireNotNull(state.ride)
        require(reviewed.owner.isNotBlank() && reviewed.model.isNotBlank()) { "Enter the owner and model." }
        Instant.parse(confirmedAt)
        passportNumberValue(reviewed.odometerKmBits)
        val services = state.logs.filterNot { it.isFuel }.sortedWith(compareBy<RideLog> { LocalDate.parse(it.date) }.thenBy { it.odometer }.thenBy { it.id }).map {
            PassportService(it.id, it.date, it.title, it.detail, passportNumber(it.odometer), passportNumber(it.cost), it.workshop, it.notes, it.componentId)
        }
        val passport = ServicePassport(confirmedAt = confirmedAt, identity = reviewed, services = services)
        return PassportBundle(passport, sha256(canonicalPassport(passport).encodeToByteArray()))
    }
}

data class PassportVerification(val bundle: PassportBundle, val matchesExpected: Boolean)
fun verifyPassportBundle(json: String, expectedHash: String = ""): PassportVerification {
    require(json.encodeToByteArray().size <= 8 * 1024 * 1024) { "Verification file is too large." }
    val bundle = passportJson.decodeFromString(PassportBundle.serializer(), json)
    val passport = bundle.passport
    require(passport.schema == "motovault-service-passport-v1" && passport.declaration == PASSPORT_DECLARATION) { "Unsupported passport format." }
    Instant.parse(passport.confirmedAt)
    passportNumberValue(passport.identity.odometerKmBits)
    passport.services.forEach { LocalDate.parse(it.date); passportNumberValue(it.odometerKmBits); passportNumberValue(it.costBdtBits) }
    require(bundle.sha256.matches(Regex("[0-9a-f]{64}"))) { "Invalid SHA-256 digest." }
    require(sha256(canonicalPassport(passport).encodeToByteArray()) == bundle.sha256) { "Integrity check failed. Covered data changed." }
    val expected = expectedHash.trim().removePrefix("motovault:passport:v1:").lowercase()
    require(expected.isEmpty() || expected.matches(Regex("[0-9a-f]{64}"))) { "Enter a 64-character SHA-256 digest." }
    require(expected.isEmpty() || expected == bundle.sha256) { "This file does not match the independently supplied digest." }
    return PassportVerification(bundle, expected.isNotEmpty())
}

// FIPS 180-4 SHA-256. Pure common code so Android/iOS cover the same exact bytes.
@OptIn(ExperimentalUnsignedTypes::class)
fun sha256(bytes: ByteArray): String {
    val constants = uintArrayOf(0x428a2f98u,0x71374491u,0xb5c0fbcfu,0xe9b5dba5u,0x3956c25bu,0x59f111f1u,0x923f82a4u,0xab1c5ed5u,0xd807aa98u,0x12835b01u,0x243185beu,0x550c7dc3u,0x72be5d74u,0x80deb1feu,0x9bdc06a7u,0xc19bf174u,0xe49b69c1u,0xefbe4786u,0x0fc19dc6u,0x240ca1ccu,0x2de92c6fu,0x4a7484aau,0x5cb0a9dcu,0x76f988dau,0x983e5152u,0xa831c66du,0xb00327c8u,0xbf597fc7u,0xc6e00bf3u,0xd5a79147u,0x06ca6351u,0x14292967u,0x27b70a85u,0x2e1b2138u,0x4d2c6dfcu,0x53380d13u,0x650a7354u,0x766a0abbu,0x81c2c92eu,0x92722c85u,0xa2bfe8a1u,0xa81a664bu,0xc24b8b70u,0xc76c51a3u,0xd192e819u,0xd6990624u,0xf40e3585u,0x106aa070u,0x19a4c116u,0x1e376c08u,0x2748774cu,0x34b0bcb5u,0x391c0cb3u,0x4ed8aa4au,0x5b9cca4fu,0x682e6ff3u,0x748f82eeu,0x78a5636fu,0x84c87814u,0x8cc70208u,0x90befffau,0xa4506cebu,0xbef9a3f7u,0xc67178f2u)
    val h = uintArrayOf(0x6a09e667u,0xbb67ae85u,0x3c6ef372u,0xa54ff53au,0x510e527fu,0x9b05688cu,0x1f83d9abu,0x5be0cd19u)
    val padded = ByteArray(((bytes.size + 9 + 63) / 64) * 64)
    bytes.copyInto(padded); padded[bytes.size] = 0x80.toByte()
    val length = bytes.size.toULong() * 8u
    for (i in 0..7) padded[padded.size - 1 - i] = (length shr (8 * i)).toByte()
    fun rotate(v: UInt, n: Int) = (v shr n) or (v shl (32 - n))
    val w = UIntArray(64)
    for (offset in padded.indices step 64) {
        for (i in 0..15) w[i] = (0..3).fold(0u) { v, j -> (v shl 8) or padded[offset + i * 4 + j].toUByte().toUInt() }
        for (i in 16..63) {
            val a = w[i - 15]; val b = w[i - 2]
            w[i] = w[i - 16] + (rotate(a,7) xor rotate(a,18) xor (a shr 3)) + w[i - 7] + (rotate(b,17) xor rotate(b,19) xor (b shr 10))
        }
        var a=h[0]; var b=h[1]; var c=h[2]; var d=h[3]; var e=h[4]; var f=h[5]; var g=h[6]; var v=h[7]
        for (i in 0..63) {
            val t1 = v + (rotate(e,6) xor rotate(e,11) xor rotate(e,25)) + ((e and f) xor (e.inv() and g)) + constants[i] + w[i]
            val t2 = (rotate(a,2) xor rotate(a,13) xor rotate(a,22)) + ((a and b) xor (a and c) xor (b and c))
            v=g; g=f; f=e; e=d+t1; d=c; c=b; b=a; a=t1+t2
        }
        val values = uintArrayOf(a,b,c,d,e,f,g,v)
        for(i in 0..7) h[i] += values[i]
    }
    return h.joinToString("") { it.toString(16).padStart(8,'0') }
}

data class PassportExport(val pdfPath: String, val jsonPath: String)
expect suspend fun renderServicePassport(bundle: PassportBundle): PassportExport
expect suspend fun discardPassportExport(export: PassportExport)

// Shared ordered content. Native text layout wraps every block and carries it across pages.
data class PassportParagraph(val text: String, val heading: Boolean = false)
fun passportParagraphs(bundle: PassportBundle): List<PassportParagraph> = buildList {
    val passport = bundle.passport; val identity = passport.identity
    add(PassportParagraph("MOTOVAULT / SERVICE PASSPORT", true))
    add(PassportParagraph(PASSPORT_DECLARATION))
    add(PassportParagraph("Owner confirmation: ${passport.confirmedAt}"))
    add(PassportParagraph("Vehicle identity", true))
    add(PassportParagraph("Owner: ${identity.owner}\nVehicle: ${identity.brand} ${identity.model}\nEdition: ${identity.edition.ifBlank { "Not supplied" }} / ${identity.year}\nRegistration: ${identity.registration.ifBlank { "Not supplied" }}\nChassis: ${identity.chassis.ifBlank { "Not supplied" }}\nEngine: ${identity.engine.ifBlank { "Not supplied" }}\nCurrent mileage: ${passportNumberValue(identity.odometerKmBits)} km"))
    add(PassportParagraph("Service ledger (${passport.services.size} entries)", true))
    if (passport.services.isEmpty()) add(PassportParagraph("No saved service entries. Schedule baselines are not service records."))
    passport.services.forEachIndexed { index, entry ->
        add(PassportParagraph("${index + 1}. ${entry.date} / ${entry.title}", true))
        add(PassportParagraph("${passportNumberValue(entry.odometerKmBits)} km / BDT ${passportNumberValue(entry.costBdtBits)}\nWorkshop: ${entry.workshop.ifBlank { "Not supplied" }}\n${entry.detail}" + if(entry.notes.isNotBlank()) "\nNotes: ${entry.notes}" else ""))
    }
    add(PassportParagraph("Integrity verification", true))
    add(PassportParagraph("SHA-256: ${bundle.sha256}\nQR text: motovault:passport:v1:${bundle.sha256}\nIn MotoVault History, choose Verify passport and import the paired JSON. Supply a digest received independently to compare identity of the data. The PDF layout itself is not covered by this digest. Fuel and document originals are excluded."))
}
