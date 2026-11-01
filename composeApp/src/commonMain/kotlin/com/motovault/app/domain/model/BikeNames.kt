package com.motovault.app.domain.model

/** Pure legacy-name fallback; presentation adds catalogue photos and notes. */
fun legacyBikeName(brandId: String): String? = when (brandId) {
    "ktm" -> "Duke 125 · 2023"
    "cfmoto" -> "250NK"
    "yamaha" -> "R15 V4"
    "honda" -> "CBR150R"
    "suzuki" -> "Gixxer SF"
    "bajaj" -> "Pulsar N160"
    "tvs" -> "Apache RTR 160 4V"
    "hero" -> "Hunk 150R"
    "enfield" -> "Classic 350"
    else -> null
}
