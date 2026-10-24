package com.motovault.app.ui

import com.motovault.app.domain.model.ComponentRule
import com.motovault.app.domain.model.BaselineCertainty
import kotlinx.datetime.LocalDate

enum class WearStatus {
    OK,
    SOON,
    OVERDUE,
    UNKNOWN
}

data class Wear(
    val rule: ComponentRule,
    val life: Int, // 0..100 remaining percentage
    val used: Double, // >= 1.0 is overdue, >= 0.75 is soon
    val status: WearStatus,
    val remainingKm: Double?,
    val remainingDays: Int?,
    val limit: String // "km" or "days"
)

object WearCalculator {
    fun diffDays(fromEpochDays: Long, toEpochDays: Long): Int = (toEpochDays - fromEpochDays).toInt()

    fun wearOf(rule: ComponentRule, odometer: Double, todayEpochDays: Long): Wear {
        val usedKm = if (rule.intervalKm > 0) (odometer - rule.lastServicedOdometerKm) / rule.intervalKm else 0.0
        val daysElapsed = (todayEpochDays - rule.lastServicedEpochDays).coerceAtLeast(0)
        val usedDays = if (rule.intervalDays > 0) daysElapsed.toDouble() / rule.intervalDays.toDouble() else 0.0
        val used = maxOf(usedKm, usedDays)
        val remainingKm = if (rule.intervalKm > 0) rule.intervalKm - (odometer - rule.lastServicedOdometerKm) else null
        val remainingDays = if (rule.intervalDays > 0) (rule.intervalDays - daysElapsed).toInt() else null

        val life = ((1.0 - used) * 100.0).toInt().coerceIn(0, 100)
        val status = when {
            rule.baselineCertainty != BaselineCertainty.CONFIRMED ||
                !rule.intervalKm.isFinite() || rule.intervalKm < 0 || rule.intervalDays < 0 ||
                !rule.lastServicedOdometerKm.isFinite() || !odometer.isFinite() ||
                rule.lastServicedOdometerKm < 0 || rule.lastServicedOdometerKm > odometer ||
                rule.lastServicedEpochDays < 0 || rule.lastServicedEpochDays > todayEpochDays ||
                (rule.intervalKm <= 0 && rule.intervalDays <= 0) -> WearStatus.UNKNOWN
            used >= 1.0 -> WearStatus.OVERDUE
            used >= 0.75 -> WearStatus.SOON
            else -> WearStatus.OK
        }
        val limit = if (usedKm >= usedDays) "km" else "days"

        return Wear(
            rule = rule,
            life = life,
            used = used,
            status = status,
            remainingKm = remainingKm,
            remainingDays = remainingDays,
            limit = limit
        )
    }

    fun byUrgency(rules: List<ComponentRule>, odometer: Double, todayEpochDays: Long): List<Wear> {
        return rules.map { wearOf(it, odometer, todayEpochDays) }.sortedWith(
            compareByDescending<Wear> { when (it.status) { WearStatus.OVERDUE -> 3; WearStatus.UNKNOWN -> 2; WearStatus.SOON -> 1; WearStatus.OK -> 0 } }
                .thenByDescending { it.used }
        )
    }

    /**
     * Resolves the overdue bug: says how far overdue a part is, or how much is left.
     * The old app printed "0 km / 30 দিন বাকি" under "সময় পেরিয়েছে".
     */
    fun describeWear(w: Wear, f: Fmt): String {
        if (w.status == WearStatus.UNKNOWN) return "আগের সার্ভিস নিশ্চিত নয়"
        if (w.status == WearStatus.OVERDUE) {
            val overdueKm = if (w.remainingKm != null && w.remainingKm < 0) -w.remainingKm else 0.0
            val overdueDays = if (w.remainingDays != null && w.remainingDays < 0) -w.remainingDays else 0
            return if (w.limit == "km" || (overdueKm > 0 && overdueDays == 0)) {
                "${f.km(overdueKm)} পেরিয়েছে"
            } else {
                "${f.days(overdueDays)} পেরিয়েছে"
            }
        }
        val parts = mutableListOf<String>()
        if (w.remainingKm != null) parts.add(f.km(w.remainingKm.coerceAtLeast(0.0)))
        if (w.remainingDays != null) parts.add(f.days(w.remainingDays.coerceAtLeast(0)))
        return parts.joinToString(" বা ") + " বাকি"
    }

    fun statusLabel(s: WearStatus): String = when (s) {
        WearStatus.OVERDUE -> "সার্ভিস দরকার"
        WearStatus.SOON -> "শিগগির"
        WearStatus.OK -> "সময় বাকি"
        WearStatus.UNKNOWN -> "তথ্য নিশ্চিত করুন"
    }

    fun economySeries(logs: List<RideLog>): List<Double> {
        val fuels = logs.filter { it.isFuel }.sortedBy { it.odometer }
        var baseOdo: Double? = null
        var litres = 0.0
        val out = mutableListOf<Double>()
        for (l in fuels) {
            val lLitres = l.fuelLiters ?: 0.0
            if (baseOdo != null) litres += lLitres
            if (l.fullTank) {
                if (baseOdo != null && litres > 0) {
                    out.add((l.odometer - baseOdo) / litres)
                }
                baseOdo = l.odometer
                litres = 0.0
            }
        }
        return out
    }

    fun economyOf(logs: List<RideLog>): Double? {
        val fuels = logs.filter { it.isFuel }.sortedBy { it.odometer }
        var baseOdo: Double? = null
        var litres = 0.0
        var km = 0.0
        var totalLitres = 0.0
        for (l in fuels) {
            val lLitres = l.fuelLiters ?: 0.0
            if (baseOdo != null) litres += lLitres
            if (l.fullTank) {
                if (baseOdo != null && litres > 0) {
                    km += (l.odometer - baseOdo)
                    totalLitres += litres
                }
                baseOdo = l.odometer
                litres = 0.0
            }
        }
        return if (totalLitres > 0) km / totalLitres else null
    }

    fun estimateOdometer(ride: Ride, logs: List<RideLog>, today: LocalDate): Double {
        val fuels = logs.filter { it.isFuel }.sortedBy { it.odometer }
        val last = fuels.lastOrNull()
        val first = if (fuels.size > 1) fuels[maxOf(0, fuels.size - 6)] else null
        var perDay = 25.0
        if (last != null && first != null && last.date != first.date) {
            val daysBetween = runCatching {
                (LocalDate.parse(last.date).toEpochDays() - LocalDate.parse(first.date).toEpochDays()).coerceAtLeast(1)
            }.getOrDefault(1)
            perDay = maxOf(5.0, (last.odometer - first.odometer) / daysBetween)
        }
        val lastDate = runCatching { LocalDate.parse(ride.odometerConfirmedDate ?: "") }.getOrNull() ?: return ride.odometer
        val since = (today.toEpochDays() - lastDate.toEpochDays()).coerceAtLeast(0)
        val raw = ride.odometer + perDay * since
        return (kotlin.math.round(raw / 5.0) * 5.0).coerceAtLeast(ride.odometer)
    }
}
