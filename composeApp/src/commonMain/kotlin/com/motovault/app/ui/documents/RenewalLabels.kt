package com.motovault.app.ui

import androidx.compose.runtime.Composable
import motovault.composeapp.generated.resources.*
import org.jetbrains.compose.resources.StringResource

/** Resource lookup for persisted checklists. Step and link IDs are the only stored identity. */
private data class StepCopy(val title: StringResource, val detail: StringResource)

private fun stepCopy(id: String): StepCopy? = when (id) {
    "licence_form" -> StepCopy(Res.string.renewal_step_licence_form, Res.string.renewal_step_licence_form_detail)
    "licence_medical" -> StepCopy(Res.string.renewal_step_licence_medical, Res.string.renewal_step_licence_medical_detail)
    "licence_nid" -> StepCopy(Res.string.renewal_step_licence_nid, Res.string.renewal_step_licence_nid_detail)
    "licence_education" -> StepCopy(Res.string.renewal_step_licence_education, Res.string.renewal_step_licence_education_detail)
    "licence_fee_receipt" -> StepCopy(Res.string.renewal_step_licence_fee_receipt, Res.string.renewal_step_licence_fee_receipt_detail)
    "licence_photos" -> StepCopy(Res.string.renewal_step_licence_photos, Res.string.renewal_step_licence_photos_detail)
    "licence_practical_test" -> StepCopy(Res.string.renewal_step_licence_practical_test, Res.string.renewal_step_licence_practical_test_detail)
    "licence_biometrics" -> StepCopy(Res.string.renewal_step_licence_biometrics, Res.string.renewal_step_licence_biometrics_detail)
    "tax_previous_token" -> StepCopy(Res.string.renewal_step_tax_previous_token, Res.string.renewal_step_tax_previous_token_detail)
    "tax_fee_payment" -> StepCopy(Res.string.renewal_step_tax_fee_payment, Res.string.renewal_step_tax_fee_payment_detail)
    "tax_fee_receipt" -> StepCopy(Res.string.renewal_step_tax_fee_receipt, Res.string.renewal_step_tax_fee_receipt_detail)
    "fitness_form" -> StepCopy(Res.string.renewal_step_fitness_form, Res.string.renewal_step_fitness_form_detail)
    "fitness_appointment" -> StepCopy(Res.string.renewal_step_fitness_appointment, Res.string.renewal_step_fitness_appointment_detail)
    "fitness_previous_certificate" -> StepCopy(Res.string.renewal_step_fitness_previous_certificate, Res.string.renewal_step_fitness_previous_certificate_detail)
    "fitness_tax_token_copy" -> StepCopy(Res.string.renewal_step_fitness_tax_token_copy, Res.string.renewal_step_fitness_tax_token_copy_detail)
    "fitness_tin" -> StepCopy(Res.string.renewal_step_fitness_tin, Res.string.renewal_step_fitness_tin_detail)
    "fitness_vehicle_present" -> StepCopy(Res.string.renewal_step_fitness_vehicle_present, Res.string.renewal_step_fitness_vehicle_present_detail)
    "fitness_fee_receipt" -> StepCopy(Res.string.renewal_step_fitness_fee_receipt, Res.string.renewal_step_fitness_fee_receipt_detail)
    "insurance_insurer" -> StepCopy(Res.string.renewal_step_insurance_insurer, Res.string.renewal_step_insurance_insurer_detail)
    "insurance_policy_copy" -> StepCopy(Res.string.renewal_step_insurance_policy_copy, Res.string.renewal_step_insurance_policy_copy_detail)
    "insurance_new_expiry" -> StepCopy(Res.string.renewal_step_insurance_new_expiry, Res.string.renewal_step_insurance_new_expiry_detail)
    "registration_no_periodic_renewal" -> StepCopy(Res.string.renewal_step_registration_no_periodic_renewal, Res.string.renewal_step_registration_no_periodic_renewal_detail)
    "registration_transfer_or_correction" -> StepCopy(Res.string.renewal_step_registration_transfer_or_correction, Res.string.renewal_step_registration_transfer_or_correction_detail)
    "registration_official_sources" -> StepCopy(Res.string.renewal_step_registration_official_sources, Res.string.renewal_step_registration_official_sources_detail)
    else -> null
}

@Composable
internal fun renewalStepText(id: String): Pair<String, String>? {
    val copy = stepCopy(id) ?: return null
    return appStringResource(copy.title) to appStringResource(copy.detail)
}

@Composable
internal fun renewalLinkLabel(id: String): String = when (id) {
    "registration_transfer" -> appStringResource(Res.string.renewal_transfer_link)
    "registration_digital_certificate" -> appStringResource(Res.string.renewal_digital_certificate_link)
    else -> appStringResource(Res.string.renewal_instructions)
}
