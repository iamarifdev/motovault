package com.motovault.app.ui

import androidx.compose.ui.graphics.Color
import com.motovault.app.domain.model.RideLog

data class BrandWorld(val id: String, val name: String, val accent: Color)

val worlds = listOf(
    BrandWorld("ktm", "KTM", Color(0xFFFF7900)),
    BrandWorld("cfmoto", "CFMOTO", Color(0xFF69D2DB)),
    BrandWorld("yamaha", "Yamaha", Color(0xFF88A8FF)),
    BrandWorld("honda", "Honda", Color(0xFFFF7B84)),
    BrandWorld("suzuki", "Suzuki", Color(0xFF7EBAF5)),
    BrandWorld("bajaj", "Bajaj", Color(0xFF96B9FA)),
    BrandWorld("tvs", "TVS", Color(0xFFF4828B)),
    BrandWorld("hero", "Hero", Color(0xFFFF8983)),
    BrandWorld("enfield", "Royal Enfield", Color(0xFFDBB77C))
)

enum class Tab(val id: String, val icon: String) {
    GARAGE("garage", "fi_home.svg"),
    VAULT("vault", "fi_folder-open.svg"),
    HISTORY("history", "fi_history.svg"),
    ME("me", "fi_user.svg")
}

data class ActiveSheet(
    val name: String,
    val props: Any? = null
)

internal data class FuelEditTarget(val record: RideLog, val revision: Long)
