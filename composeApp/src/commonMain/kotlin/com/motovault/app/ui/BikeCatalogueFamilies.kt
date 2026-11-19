package com.motovault.app.ui

import motovault.composeapp.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource

data class BikeVariant(
    val modelId: String,
    val name: String,
    val label: String,
    val photo: DrawableResource,
    val cc: Int,
    val yearStart: Int,
    val yearEnd: Int
)

data class BikeFamily(
    val id: String,
    val brandId: String,
    val name: String,
    val cc: Int,
    val heroModelId: String,
    val variants: List<BikeVariant>
) {
    val leadModel: BikeModel?
        get() {
            val lead = variants.firstOrNull { it.modelId == heroModelId } ?: variants.firstOrNull() ?: return null
            return BikeModel(lead.modelId, brandId, lead.name, lead.photo)
        }
}

private fun v(id: String, name: String, label: String, photo: DrawableResource, cc: Int, y1: Int, y2: Int) =
    BikeVariant(id, name, label, photo, cc, y1, y2)

private val BIKE_FAMILIES: List<BikeFamily> = listOf(
    BikeFamily("yamaha-r15", "yamaha", "R15", 155, "yamaha-r15-v4", listOf(
        v("yamaha-r15-v4", "R15 V4", "V4", Res.drawable.model_yamaha_r15_v4, 155, 2022, 2026),
        v("yamaha-r15m", "R15M", "R15M", Res.drawable.model_yamaha_r15m, 155, 2022, 2026),
        v("yamaha-r15m-monster-edition", "R15M Monster Edition", "Monster Edition", Res.drawable.model_yamaha_r15m_monster_edition, 155, 2023, 2026),
        v("yamaha-r15-v3", "R15 V3", "V3", Res.drawable.model_yamaha_r15_v3, 155, 2018, 2022),
        v("yamaha-r15-v2", "R15 V2", "V2", Res.drawable.model_yamaha_r15_v2, 149, 2013, 2018)
    )),
    BikeFamily("yamaha-mt15", "yamaha", "MT-15", 155, "yamaha-mt-15-v2", listOf(
        v("yamaha-mt-15-v2", "MT-15 V2", "V2", Res.drawable.model_yamaha_mt_15_v2, 155, 2022, 2026),
        v("yamaha-mt-15-v1", "MT-15 V1", "V1", Res.drawable.model_yamaha_mt_15_v1, 155, 2019, 2022)
    )),
    BikeFamily("yamaha-fzs", "yamaha", "FZS / FZ-FI", 149, "yamaha-fzs-v4", listOf(
        v("yamaha-fzs-v4", "FZS V4", "V4 Deluxe", Res.drawable.model_yamaha_fzs_v4, 149, 2023, 2026),
        v("yamaha-fzs-fi-hybrid", "FZS FI Hybrid", "FI Hybrid", Res.drawable.model_yamaha_fzs_fi_hybrid, 149, 2025, 2026),
        v("yamaha-fzs-v3", "FZS V3", "V3 ABS", Res.drawable.model_yamaha_fzs_v3, 149, 2019, 2024),
        v("yamaha-fz-fi-v3-family", "FZ FI · V3", "FZ-FI V3", Res.drawable.model_yamaha_fz_fi_v3_family, 149, 2019, 2023),
        v("yamaha-fzs-v2", "FZS V2", "V2 DD/SD", Res.drawable.model_yamaha_fzs_v2, 149, 2015, 2023)
    )),
    BikeFamily("yamaha-fz-x", "yamaha", "FZ-X", 149, "yamaha-fz-x", listOf(
        v("yamaha-fz-x", "FZ-X", "Standard / Chrome", Res.drawable.model_yamaha_fz_x, 149, 2022, 2026)
    )),
    BikeFamily("yamaha-fazer-fi-v2", "yamaha", "Fazer FI V2", 149, "yamaha-fazer-fi-v2", listOf(
        v("yamaha-fazer-fi-v2", "Fazer FI V2", "FI V2", Res.drawable.model_yamaha_fazer_fi_v2, 149, 2015, 2024)
    )),
    BikeFamily("yamaha-fz-25", "yamaha", "FZ-25", 249, "yamaha-fz-25", listOf(
        v("yamaha-fz-25", "FZ-25", "Dual ABS", Res.drawable.model_yamaha_fz_25, 249, 2018, 2024)
    )),
    BikeFamily("yamaha-saluto-125", "yamaha", "Saluto 125", 125, "yamaha-saluto-125", listOf(
        v("yamaha-saluto-125", "Saluto 125", "UBS Disc / Armada", Res.drawable.model_yamaha_saluto_125, 125, 2016, 2026)
    )),
    BikeFamily("yamaha-scooters", "yamaha", "Aerox 155 / RayZR", 155, "yamaha-aerox-155-abs", listOf(
        v("yamaha-aerox-155-abs", "Aerox 155 ABS", "Aerox 155 ABS", Res.drawable.model_yamaha_aerox_155_abs, 155, 2022, 2026),
        v("yamaha-rayzr-125-fi", "RayZR 125 FI", "RayZR 125 FI", Res.drawable.model_yamaha_rayzr_125_fi, 125, 2021, 2026)
    )),
    BikeFamily("honda-cbr150r", "honda", "CBR150R", 149, "honda-cbr150r", listOf(
        v("honda-cbr150r", "CBR150R ABS", "K45R USD ABS", Res.drawable.model_honda_cbr150r, 149, 2021, 2026),
        v("honda-cbr150r-k45g", "CBR150R (Street/Standard)", "K45G / K45N", Res.drawable.model_honda_cbr150r_k45g, 149, 2016, 2021)
    )),
    BikeFamily("honda-hornet", "honda", "Hornet", 184, "honda-hornet-2-0", listOf(
        v("honda-hornet-2-0", "Hornet 2.0", "Hornet 2.0", Res.drawable.model_honda_hornet_2_0, 184, 2021, 2026),
        v("honda-cb-hornet-160r", "CB Hornet 160R", "CB Hornet 160R", Res.drawable.model_honda_cb_hornet_160r, 163, 2016, 2022)
    )),
    BikeFamily("honda-sp160", "honda", "SP160", 163, "honda-sp160", listOf(
        v("honda-sp160", "SP160", "Single / Twin Disc ABS", Res.drawable.model_honda_sp160, 163, 2024, 2026)
    )),
    BikeFamily("honda-x-blade", "honda", "X-Blade", 163, "honda-x-blade", listOf(
        v("honda-x-blade", "X-Blade ABS", "ABS Double Disc", Res.drawable.model_honda_x_blade, 163, 2020, 2026),
        v("honda-x-blade-sd", "X-Blade SD", "Single Disc (Carb)", Res.drawable.model_honda_x_blade, 163, 2018, 2022)
    )),
    BikeFamily("honda-sp125", "honda", "SP 125 / Shine SP", 124, "honda-sp125", listOf(
        v("honda-sp125", "SP 125", "SP 125 (FI)", Res.drawable.model_honda_sp125, 124, 2023, 2026),
        v("honda-cb-shine-sp", "CB Shine SP 125", "CB Shine SP", Res.drawable.model_honda_cb_shine_sp, 125, 2016, 2023)
    )),
    BikeFamily("honda-livo", "honda", "Livo 110", 110, "honda-livo", listOf(
        v("honda-livo", "Livo", "Disc / Drum", Res.drawable.model_honda_livo, 110, 2017, 2026)
    )),
    BikeFamily("honda-dream-110", "honda", "Dream 110", 109, "honda-dream-110", listOf(
        v("honda-dream-110", "Dream 110", "Neo / 110", Res.drawable.model_honda_dream_110, 109, 2016, 2026)
    )),
    BikeFamily("honda-shine-100", "honda", "Shine 100", 99, "honda-shine-100-dx", listOf(
        v("honda-shine-100-dx", "Shine 100 DX", "DX", Res.drawable.model_honda_shine_100_dx, 99, 2025, 2026),
        v("honda-shine-100", "Shine 100", "Standard", Res.drawable.model_honda_shine_100, 99, 2024, 2026)
    )),
    BikeFamily("honda-nx200", "honda", "NX200", 184, "honda-nx200", listOf(
        v("honda-nx200", "NX200", "ADV Tourer", Res.drawable.model_honda_nx200, 184, 2025, 2026)
    )),
    BikeFamily("bajaj-pulsar-150", "bajaj", "Pulsar 150", 149, "bajaj-pulsar-150-twin-disc-abs", listOf(
        v("bajaj-pulsar-150-twin-disc-abs", "Pulsar 150 Twin Disc ABS", "Twin Disc ABS", Res.drawable.model_bajaj_pulsar_150_twin_disc_abs, 149, 2019, 2026),
        v("bajaj-pulsar-150-sd", "Pulsar 150 Single Disc", "Single Disc (UG4/UG5)", Res.drawable.model_bajaj_pulsar_150_sd, 149, 2012, 2024)
    )),
    BikeFamily("bajaj-pulsar-n160", "bajaj", "Pulsar N160", 165, "bajaj-pulsar-n160", listOf(
        v("bajaj-pulsar-n160", "Pulsar N160", "Dual ABS", Res.drawable.model_bajaj_pulsar_n160, 165, 2022, 2026),
        v("bajaj-n160-usd", "Pulsar N160 USD", "USD · Bluetooth", Res.drawable.model_bajaj_pulsar_n160, 165, 2024, 2026)
    )),
    BikeFamily("bajaj-pulsar-ns", "bajaj", "Pulsar NS125 / NS160", 124, "bajaj-pulsar-ns125", listOf(
        v("bajaj-pulsar-ns125", "Pulsar NS125", "NS125", Res.drawable.model_bajaj_pulsar_ns125, 124, 2022, 2026),
        v("bajaj-pulsar-ns160", "Pulsar NS160", "NS160 FI ABS", Res.drawable.model_bajaj_pulsar_ns160, 160, 2017, 2025)
    )),
    BikeFamily("bajaj-pulsar-250", "bajaj", "Pulsar N250 / F250", 249, "bajaj-pulsar-n250", listOf(
        v("bajaj-pulsar-n250", "Pulsar N250", "N250 (Naked)", Res.drawable.model_bajaj_pulsar_n250, 249, 2024, 2026),
        v("bajaj-pulsar-f250", "Pulsar F250", "F250 (Semi-Faired)", Res.drawable.model_bajaj_pulsar_f250, 249, 2024, 2026)
    )),
    BikeFamily("bajaj-discover", "bajaj", "Discover", 125, "bajaj-discover-125-disc", listOf(
        v("bajaj-discover-125-disc", "Discover 125 Disc", "125 Disc", Res.drawable.model_bajaj_discover_125_disc, 125, 2015, 2026),
        v("bajaj-discover-110-disc", "Discover 110 Disc", "110 Disc", Res.drawable.model_bajaj_discover_110_disc, 115, 2018, 2026)
    )),
    BikeFamily("bajaj-platina", "bajaj", "Platina", 115, "bajaj-platina-110-h", listOf(
        v("bajaj-platina-110-h", "Platina 110 H-Gear", "110 H-Gear", Res.drawable.model_bajaj_platina_110_h, 115, 2019, 2026),
        v("bajaj-platina-100-es", "Platina 100 ES", "100 ES", Res.drawable.model_bajaj_platina_100_es, 102, 2015, 2026)
    )),
    BikeFamily("bajaj-ct100-es", "bajaj", "CT100 ES", 102, "bajaj-ct100-es", listOf(
        v("bajaj-ct100-es", "CT100 ES", "ES / Alloy", Res.drawable.model_bajaj_ct100_es, 102, 2015, 2026)
    )),
    BikeFamily("tvs-apache-160", "tvs", "Apache RTR 160", 160, "tvs-apache-rtr-160-4v", listOf(
        v("tvs-apache-rtr-160-4v", "Apache RTR 160 4V", "4V FI / SmartXonnect", Res.drawable.model_tvs_apache_rtr_160_4v, 160, 2021, 2026),
        v("tvs-apache-160-4v-carb", "Apache RTR 160 4V (Carb)", "4V Carburetor", Res.drawable.model_tvs_apache_rtr_160_4v, 160, 2018, 2023),
        v("tvs-apache-rtr-160-2v", "Apache RTR 160 2V", "2V Refresh", Res.drawable.model_tvs_apache_rtr_160_2v, 160, 2012, 2025)
    )),
    BikeFamily("tvs-raider-125", "tvs", "Raider 125", 125, "tvs-raider-125", listOf(
        v("tvs-raider-125", "Raider 125", "FI / SmartXonnect", Res.drawable.model_tvs_raider_125, 125, 2022, 2026)
    )),
    BikeFamily("tvs-stryker-125", "tvs", "Stryker 125", 125, "tvs-stryker-125", listOf(
        v("tvs-stryker-125", "Stryker 125", "125 Disc", Res.drawable.model_tvs_stryker_125, 125, 2016, 2025)
    )),
    BikeFamily("tvs-metro-plus", "tvs", "Metro Plus 110", 110, "tvs-metro-plus", listOf(
        v("tvs-metro-plus", "Metro Plus 110", "Disc / Drum", Res.drawable.model_tvs_metro_plus, 110, 2015, 2026)
    )),
    BikeFamily("tvs-ntorq-125", "tvs", "Ntorq 125", 125, "tvs-ntorq-125", listOf(
        v("tvs-ntorq-125", "Ntorq 125", "Race XP / XT", Res.drawable.model_tvs_ntorq_125, 125, 2019, 2026)
    )),
    BikeFamily("suzuki-gixxer-naked", "suzuki", "Gixxer (155)", 155, "suzuki-gixxer", listOf(
        v("suzuki-gixxer", "Gixxer", "New Gen FI ABS", Res.drawable.model_suzuki_gixxer, 155, 2020, 2026),
        v("suzuki-gixxer-monotone", "Gixxer Monotone", "Monotone", Res.drawable.model_suzuki_gixxer_monotone, 155, 2018, 2025),
        v("suzuki-gixxer-classic-matt", "Gixxer Classic Matt", "Classic Matt", Res.drawable.model_suzuki_gixxer_classic_matt, 155, 2016, 2021)
    )),
    BikeFamily("suzuki-gixxer-sf-group", "suzuki", "Gixxer SF", 155, "suzuki-gixxer-sf", listOf(
        v("suzuki-gixxer-sf", "Gixxer SF", "New Gen FI ABS", Res.drawable.model_suzuki_gixxer_sf, 155, 2020, 2026),
        v("suzuki-gixxer-sf-250", "Gixxer SF 250", "SF 250", Res.drawable.model_suzuki_gixxer_sf_250, 249, 2024, 2026)
    )),
    BikeFamily("suzuki-gixxer-250", "suzuki", "Gixxer 250", 249, "suzuki-gixxer-250", listOf(
        v("suzuki-gixxer-250", "Gixxer 250", "250 Dual ABS", Res.drawable.model_suzuki_gixxer_250, 249, 2024, 2026)
    )),
    BikeFamily("suzuki-gsx-r150", "suzuki", "GSX-R150", 147, "suzuki-gsx-r150", listOf(
        v("suzuki-gsx-r150", "GSX-R150", "Keyless Dual ABS", Res.drawable.model_suzuki_gsx_r150, 147, 2018, 2026)
    )),
    BikeFamily("suzuki-gsx-125", "suzuki", "GSX 125", 125, "suzuki-gsx-125", listOf(
        v("suzuki-gsx-125", "GSX 125", "125 Disc", Res.drawable.model_suzuki_gsx_125, 125, 2021, 2026)
    )),
    BikeFamily("suzuki-hayate-ep", "suzuki", "Hayate EP", 113, "suzuki-hayate-ep", listOf(
        v("suzuki-hayate-ep", "Hayate EP", "SEP 110", Res.drawable.model_suzuki_hayate_ep, 113, 2016, 2026)
    )),
    BikeFamily("suzuki-access-125", "suzuki", "Access 125", 124, "suzuki-access-125", listOf(
        v("suzuki-access-125", "Access 125", "SE / FI", Res.drawable.model_suzuki_access_125, 124, 2020, 2026)
    )),
    BikeFamily("hero-hunk", "hero", "Hunk 150", 149, "hero-hunk-150r", listOf(
        v("hero-hunk-150-xtec", "Hunk 150 Xtec", "150 Xtec", Res.drawable.model_hero_hunk_150_xtec, 149, 2024, 2026),
        v("hero-hunk-150r", "Hunk 150R", "150R ABS", Res.drawable.model_hero_hunk_150r, 149, 2021, 2026),
        v("hero-hunk-150", "Hunk 150", "Classic 150", Res.drawable.model_hero_hunk_150, 149, 2014, 2022)
    )),
    BikeFamily("hero-thriller", "hero", "Thriller 160R", 163, "hero-thriller-160r-4v", listOf(
        v("hero-thriller-160r-4v", "Thriller 160R 4V", "160R 4V (USD)", Res.drawable.model_hero_thriller_160r_4v, 163, 2024, 2026),
        v("hero-thriller-160r", "Thriller 160R", "160R 2V", Res.drawable.model_hero_thriller_160r, 163, 2021, 2024)
    )),
    BikeFamily("hero-ignitor", "hero", "Ignitor 125", 125, "hero-ignitor-xtec", listOf(
        v("hero-ignitor-xtec", "Ignitor Xtec", "Xtec", Res.drawable.model_hero_ignitor_xtec, 125, 2024, 2026),
        v("hero-ignitor-techno", "Ignitor Techno", "Techno", Res.drawable.model_hero_ignitor_techno, 125, 2018, 2024)
    )),
    BikeFamily("hero-splendor", "hero", "Splendor+", 97, "hero-splendor-plus-xtec", listOf(
        v("hero-splendor-plus-xtec", "Splendor Plus Xtec", "Xtec", Res.drawable.model_hero_splendor_plus_xtec, 97, 2023, 2026),
        v("hero-splendor-plus-se", "Splendor Plus SE", "SE", Res.drawable.model_hero_splendor_plus_se, 97, 2015, 2026)
    )),
    BikeFamily("hero-passion", "hero", "Passion Xpro", 110, "hero-passion-xpro-xtec", listOf(
        v("hero-passion-xpro-xtec", "Passion Xpro Xtec", "Xtec", Res.drawable.model_hero_passion_xpro_xtec, 110, 2024, 2026),
        v("hero-passion-xpro", "Passion Xpro", "Standard", Res.drawable.model_hero_passion_xpro, 110, 2018, 2023)
    )),
    BikeFamily("hero-glamour", "hero", "Glamour 125", 125, "hero-glamour", listOf(
        v("hero-glamour", "Glamour", "Disc / Drum", Res.drawable.model_hero_glamour, 125, 2015, 2026)
    )),
    BikeFamily("hero-hf-deluxe", "hero", "HF Deluxe", 97, "hero-hf-deluxe", listOf(
        v("hero-hf-deluxe", "HF Deluxe", "All-Black / SE", Res.drawable.model_hero_hf_deluxe, 97, 2016, 2026)
    )),
    BikeFamily("hero-scooters", "hero", "Pleasure / Maestro Edge", 110, "hero-maestro-edge-xtec", listOf(
        v("hero-maestro-edge-xtec", "Maestro Edge Xtec", "Maestro Edge Xtec", Res.drawable.model_hero_maestro_edge_xtec, 110, 2022, 2026),
        v("hero-pleasure", "Pleasure", "Pleasure", Res.drawable.model_hero_pleasure, 102, 2016, 2025)
    )),
    BikeFamily("ktm-duke-125", "ktm", "Duke 125", 125, "ktm-duke-125-2023", listOf(
        v("ktm-duke-125-2023", "Duke 125 (EU LED · 2022–2026)", "European / New Gen LED", Res.drawable.model_ktm_duke_125_2023, 125, 2022, 2026),
        v("ktm-duke-125-indian-bs6", "Duke 125 Indian BS6 (2021–2024)", "Indian BS6 · Halogen (Orange/White)", Res.drawable.model_ktm_duke_125_indian_bs6, 125, 2021, 2024),
        v("ktm-duke-125-indian", "Duke 125 Indian Gen-1 (2018–2020)", "Indian BS4 / Gen-1 · Halogen (Black)", Res.drawable.model_ktm_duke_125_indian, 125, 2018, 2020)
    )),
    BikeFamily("ktm-rc-125", "ktm", "RC 125", 125, "ktm-rc-125-2022", listOf(
        v("ktm-rc-125-2022", "RC 125", "New Gen", Res.drawable.model_ktm_rc_125_2022, 125, 2022, 2026)
    )),
    BikeFamily("cfmoto-250nk", "cfmoto", "250NK", 249, "cfmoto-250nk", listOf(
        v("cfmoto-250nk", "250NK", "250NK ABS", Res.drawable.model_cfmoto_250nk, 249, 2024, 2026)
    )),
    BikeFamily("cfmoto-250sr", "cfmoto", "250SR", 249, "cfmoto-250sr", listOf(
        v("cfmoto-250sr", "250SR", "250SR", Res.drawable.model_cfmoto_250sr, 249, 2024, 2026)
    )),
    BikeFamily("cfmoto-300", "cfmoto", "300NK / 300SR", 292, "cfmoto-300sr", listOf(
        v("cfmoto-300sr", "300SR", "300SR (Faired)", Res.drawable.model_cfmoto_300sr, 292, 2024, 2026),
        v("cfmoto-300nk", "300NK", "300NK (Naked)", Res.drawable.model_cfmoto_300nk, 292, 2024, 2026)
    )),
    BikeFamily("cfmoto-250cl-c", "cfmoto", "250CL-C", 249, "cfmoto-250cl-c", listOf(
        v("cfmoto-250cl-c", "250CL-C", "Cruiser Belt Drive", Res.drawable.model_cfmoto_250cl_c, 249, 2025, 2026)
    )),
    BikeFamily("cfmoto-230-dual", "cfmoto", "230 Dual", 223, "cfmoto-230-dual", listOf(
        v("cfmoto-230-dual", "230 Dual", "Dual-Sport", Res.drawable.model_cfmoto_230_dual, 223, 2025, 2026)
    )),
    BikeFamily("cfmoto-150sc", "cfmoto", "150SC", 149, "cfmoto-150sc", listOf(
        v("cfmoto-150sc", "150SC", "Maxi Scooter", Res.drawable.model_cfmoto_150sc, 149, 2025, 2026)
    )),
    BikeFamily("enfield-classic", "enfield", "Classic 350", 349, "royal-enfield-classic-350", listOf(
        v("royal-enfield-classic-350", "Classic 350 (Dual ABS)", "Signals / Halcyon / Dark", Res.drawable.model_royal_enfield_classic_350, 349, 2024, 2026),
        v("royal-enfield-classic-350-single", "Classic 350 Redditch", "Redditch Single ABS", Res.drawable.model_royal_enfield_classic_350, 349, 2024, 2026)
    )),
    BikeFamily("enfield-hunter", "enfield", "Hunter 350", 349, "royal-enfield-hunter-350", listOf(
        v("royal-enfield-hunter-350", "Hunter 350 Metro", "Metro Dual ABS", Res.drawable.model_royal_enfield_hunter_350, 349, 2024, 2026),
        v("royal-enfield-hunter-retro", "Hunter 350 Retro", "Retro Spoke", Res.drawable.model_royal_enfield_hunter_350, 349, 2024, 2026)
    )),
    BikeFamily("enfield-bullet", "enfield", "Bullet 350", 349, "royal-enfield-bullet-350", listOf(
        v("royal-enfield-bullet-350", "Bullet 350", "Standard / Black Gold", Res.drawable.model_royal_enfield_bullet_350, 349, 2024, 2026)
    )),
    BikeFamily("enfield-meteor", "enfield", "Meteor 350", 349, "royal-enfield-meteor-350", listOf(
        v("royal-enfield-meteor-350", "Meteor 350", "Fireball / Stellar / Supernova", Res.drawable.model_royal_enfield_meteor_350, 349, 2024, 2026)
    ))
)

object BikeCatalogueFamilies {
    val all: List<BikeFamily> get() = BIKE_FAMILIES

    fun forBrand(brandId: String): List<BikeFamily> =
        BIKE_FAMILIES.filter { it.brandId == brandId }

    fun search(brandId: String, query: String): List<BikeFamily> {
        val needle = query.trim()
        if (needle.isEmpty()) return forBrand(brandId)
        return forBrand(brandId).filter { family ->
            family.name.contains(needle, ignoreCase = true) ||
                family.variants.any {
                    it.label.contains(needle, ignoreCase = true) ||
                        it.name.contains(needle, ignoreCase = true)
                }
        }
    }

    fun familyForVariant(modelId: String?): BikeFamily? =
        if (modelId == null) null else BIKE_FAMILIES.firstOrNull { f -> f.variants.any { it.modelId == modelId } }

    fun variantOf(modelId: String?): BikeVariant? =
        if (modelId == null) null else BIKE_FAMILIES.firstNotNullOfOrNull { f -> f.variants.firstOrNull { it.modelId == modelId } }
}
