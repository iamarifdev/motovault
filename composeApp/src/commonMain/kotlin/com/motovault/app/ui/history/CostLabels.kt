package com.motovault.app.ui

import androidx.compose.runtime.Composable
import com.motovault.app.domain.ledger.CostCategory
import com.motovault.app.domain.ledger.CostPeriod
import motovault.composeapp.generated.resources.*

@Composable
internal fun costCategoryLabel(category: CostCategory?): String = appStringResource(when (category) {
    null -> Res.string.cost_all_categories
    CostCategory.FUEL -> Res.string.cost_fuel
    CostCategory.SERVICE -> Res.string.cost_service
    CostCategory.RENEWAL -> Res.string.cost_renewal
    CostCategory.PARKING -> Res.string.cost_parking
    CostCategory.TOLL -> Res.string.cost_toll
    CostCategory.CUSTOM -> Res.string.cost_custom
})

@Composable
internal fun costPeriodLabel(period: CostPeriod): String = appStringResource(when (period) {
    CostPeriod.RECENT -> Res.string.cost_recent
    CostPeriod.MONTH -> Res.string.cost_month
    CostPeriod.ALL -> Res.string.cost_all_time
})

@Composable
internal fun costMoney(poisha: Long): String = appStringResource(Res.string.cost_money_value,
    currentFmt.n(poisha / com.motovault.app.domain.ledger.POISHA_PER_TAKA),
    currentFmt.d((poisha % com.motovault.app.domain.ledger.POISHA_PER_TAKA).toString().padStart(2, '0')))
