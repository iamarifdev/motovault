package com.motovault.app.domain.documents

import com.motovault.app.domain.model.DocumentKinds

/** Official source set verified for P05-01 on 2026-11-05. The date is shown with every guide. */
const val RENEWAL_SOURCES_CHECKED_ON = "2026-11-05"
const val RENEWAL_QR_VERIFICATION_URL = "https://bsp.brta.gov.bd/bsp/qr/qrScanner"
const val RENEWAL_FEE_CALCULATOR_URL = "https://bsp.brta.gov.bd/feeCalculator"
const val RENEWAL_LICENCE_SOURCE_URL = "https://brta.gov.bd/pages/static-pages/6922dbe0933eb65569e0d26a"
const val RENEWAL_TAX_SOURCE_URL = "https://brta.gov.bd/pages/static-pages/6922e0ab933eb65569e281ad"
const val RENEWAL_FITNESS_SOURCE_URL = "https://brta.gov.bd/pages/static-pages/6922db91933eb65569e0af12"
const val RENEWAL_TRANSFER_SOURCE_URL = "https://brta.gov.bd/pages/static-pages/6922dc6b933eb65569e10468"
const val RENEWAL_DIGITAL_CERTIFICATE_URL = "https://brta.gov.bd/pages/static-pages/6922dba6933eb65569e0b8fe"

data class RenewalLink(val id: String, val url: String)

/** Stable identity only; labels and details are resolved from localized resources. */
data class RenewalStep(val id: String)

data class RenewalGuide(
    val kind: String,
    val steps: List<RenewalStep>,
    val links: List<RenewalLink>,
    val sourceCheckedOn: String = RENEWAL_SOURCES_CHECKED_ON,
    val feeCalculator: Boolean = true,
    val qrVerification: Boolean = true,
    val lateFineNote: Boolean = false
) {
    val stepIds: Set<String> get() = steps.map { it.id }.toSet()
}

/** Sourced, dated checklist content. No BRTA API or fee table is invented here. */
object RenewalChecklists {
    private val guides = listOf(
        RenewalGuide(
            kind = DocumentKinds.LICENCE,
            steps = listOf(
                RenewalStep("licence_form"), RenewalStep("licence_medical"), RenewalStep("licence_nid"),
                RenewalStep("licence_education"), RenewalStep("licence_fee_receipt"), RenewalStep("licence_photos"),
                RenewalStep("licence_practical_test"), RenewalStep("licence_biometrics")
            ),
            links = listOf(RenewalLink("licence_instructions", RENEWAL_LICENCE_SOURCE_URL)),
            lateFineNote = true
        ),
        RenewalGuide(
            kind = DocumentKinds.TAX,
            steps = listOf(
                RenewalStep("tax_previous_token"), RenewalStep("tax_fee_payment"), RenewalStep("tax_fee_receipt")
            ),
            links = listOf(RenewalLink("tax_instructions", RENEWAL_TAX_SOURCE_URL))
        ),
        RenewalGuide(
            kind = DocumentKinds.FITNESS,
            steps = listOf(
                RenewalStep("fitness_form"), RenewalStep("fitness_appointment"),
                RenewalStep("fitness_previous_certificate"), RenewalStep("fitness_tax_token_copy"),
                RenewalStep("fitness_tin"), RenewalStep("fitness_vehicle_present"), RenewalStep("fitness_fee_receipt")
            ),
            links = listOf(RenewalLink("fitness_instructions", RENEWAL_FITNESS_SOURCE_URL))
        ),
        RenewalGuide(
            kind = DocumentKinds.INSURANCE,
            steps = listOf(
                RenewalStep("insurance_insurer"), RenewalStep("insurance_policy_copy"), RenewalStep("insurance_new_expiry")
            ),
            links = emptyList(),
            feeCalculator = false,
            qrVerification = false
        ),
        RenewalGuide(
            kind = DocumentKinds.REGISTRATION,
            steps = listOf(
                RenewalStep("registration_no_periodic_renewal"),
                RenewalStep("registration_transfer_or_correction"),
                RenewalStep("registration_official_sources")
            ),
            links = listOf(
                RenewalLink("registration_transfer", RENEWAL_TRANSFER_SOURCE_URL),
                RenewalLink("registration_digital_certificate", RENEWAL_DIGITAL_CERTIFICATE_URL)
            ),
            qrVerification = false
        )
    )

    fun of(kind: String?): RenewalGuide? = guides.firstOrNull { it.kind == kind }

    fun linksFor(kind: String?): List<RenewalLink> = of(kind)?.links.orEmpty()
}
