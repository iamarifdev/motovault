package com.motovault.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDate

@Composable
internal fun GaugeDetailSheet(state: VaultState, id: String?, onDismiss: () -> Unit, onOpen: (String, String?) -> Unit) {
    val ride = state.ride ?: return
    val rule = state.confirmedRules().firstOrNull { it.id == id }
    val f = currentFmt
    Sheet(rule?.name ?: "সার্ভিসের ব্যবধান", onDismiss = onDismiss) {
        Text("এটি রেকর্ড করা সার্ভিসের ব্যবধান। যন্ত্রাংশের শারীরিক অবস্থা মাপা হয়নি।", fontSize = 14.sp, color = TextSecondary)
        if (rule == null) Text("এই সময়সূচি আর নেই।", fontSize = 14.sp)
        else {
            val wear = WearCalculator.wearOf(rule, ride.odometer, today().toEpochDays().toLong())
            Panel {
                DocumentNote(if (wear.status == WearStatus.UNKNOWN) "আগের সার্ভিস নিশ্চিত নয়" else "ব্যবধান ${f.n(wear.life.toLong())}% বাকি")
                DocumentCopy(WearCalculator.describeWear(wear, f))
                DocumentNote("হিসাবের ভিত্তি")
                DocumentCopy(if (wear.status == WearStatus.UNKNOWN) "শেষ সার্ভিসের নিশ্চিত তথ্য নেই। সার্ভিসের রেকর্ড দিয়ে নিশ্চিত করুন।"
                    else "${f.km(rule.lastServicedOdometerKm)} · ${f.date(LocalDate.fromEpochDays(rule.lastServicedEpochDays.toInt()))}")
                DocumentNote("সময়সূচির উৎস")
                DocumentCopy(rule.specification.ifBlank { "মালিকের সময়সূচি। ম্যানুয়ালের সঙ্গে মিলিয়ে নিন।" })
                DocumentCopy("${f.km(rule.intervalKm)} বা ${f.days(rule.intervalDays)}, যেটি আগে হয়")
                DocumentNote("শেষ নিশ্চিত কিমি")
                DocumentCopy("${f.km(ride.odometer)} · ${ride.odometerConfirmedDate ?: "তারিখ নিশ্চিত নয়"}")
                DocumentCopy("পুরোনো কিমি হলে দূরত্বের হিসাবও পুরোনো।")
            }
            VaultButton("বর্তমান কিমি নিশ্চিত করুন", onClick = { onOpen("odometer", null) }, full = true)
            VaultButton("সার্ভিস লিখুন", onClick = { onOpen("service", rule.id) }, full = true, kind = ButtonKind.SECONDARY)
            VaultButton("এই সার্ভিসের রিমাইন্ডার", onClick = { onOpen("serviceReminder", rule.id) }, full = true, kind = ButtonKind.SECONDARY)
        }
    }
}
