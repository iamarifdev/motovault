@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)
package com.motovault.app

import com.motovault.app.ui.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlin.native.Platform

/** Explicit debug host hook. Builds an isolated export and never reads or writes the owner's vault. */
fun generateDebugPassportFixture() {
    check(Platform.isDebugBinary) { "Synthetic runtime fixtures are disabled in release builds." }
    MainScope().launch {
        val identity = PassportIdentity("SYNTHETIC ONLY / আরিফ 😀", "ktm", "Duke 125 TEST", "Unicode fixture", 2023,
            "TEST-NOT-A-CREDENTIAL", "", "", passportNumber(70000.0))
        val services = (1..70).map { index ->
            PassportService("debug-$index", "2026-01-${((index - 1) % 28 + 1).toString().padStart(2, '0')}",
                "TEST Service $index / তেল বদল 😀", "Synthetic maintenance record only", passportNumber(index * 1000.0),
                passportNumber(1500.0), "TEST Workshop / ঢাকা", if (index == 35) (1..100).joinToString("\n") { "Long note $it / বাংলা লেখা 😀 / SYNTHETIC ONLY" } else "Note $index", null)
        }.sortedWith(compareBy<PassportService> { it.date }.thenBy { passportNumberValue(it.odometerKmBits) }.thenBy { it.id })
        val passport = ServicePassport(confirmedAt = "2026-10-13T00:00:00Z", identity = identity, services = services)
        val bundle = PassportBundle(passport, sha256(canonicalPassport(passport).encodeToByteArray()))
        val encoded = encodePassportBundle(bundle)
        verifyPassportBundle(encoded, bundle.sha256)
        val wrongDigest = "0".repeat(64)
        check(runCatching { verifyPassportBundle(encoded, wrongDigest) }.isFailure) {
            "Native verifier accepted a wrong independent digest."
        }
        println("MOTOVAULT_PASSPORT_VERIFIER valid=true wrongIndependentDigestRejected=true")
        val export = renderServicePassport(bundle)
        println("MOTOVAULT_PASSPORT_FIXTURE pdf=${export.pdfPath} json=${export.jsonPath} entries=${services.size} hash=${bundle.sha256}")
    }
}
