package com.motovault.app.ui

import motovault.composeapp.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource

data class BikeModel(val id: String, val brandId: String, val name: String, val photo: DrawableResource, val note: String, val source: String)

object BikeCatalogue {
    val models = listOf(
        BikeModel("ktm-duke-125-2023", "ktm", "Duke 125 · 2023", Res.drawable.model_ktm_duke_125_2023, "Official global reference · generation / regional equipment may differ", "https://press.ktm.com/news-ktm-updates-its-2023-street-range-with-fresh-new-colorways?id=168537&l=international&menueid=3050"),
        BikeModel("ktm-rc-125-2022", "ktm", "RC 125 · 2022", Res.drawable.model_ktm_rc_125_2022, "Official Japan reference · generation / regional equipment may differ", "https://press.ktm.com/news-2022-2023-ktm-125-dukektm-rc-125?id=215699&l=japan&menueid=7004"),
        BikeModel("yamaha-r15-v3", "yamaha", "R15 V3", Res.drawable.model_yamaha_r15_v3, "Bangladesh official catalogue", "https://www.yamahabd.com/products/"),
        BikeModel("yamaha-r15-v4", "yamaha", "R15 V4", Res.drawable.model_yamaha_r15_v4, "Official India studio reference · exact year / trim may differ", "https://www.yamaha-motor-india.com/yamaha-r15v4.html"),
        BikeModel("yamaha-r15m", "yamaha", "R15M", Res.drawable.model_yamaha_r15m, "Official India studio reference · exact year / trim may differ", "https://www.yamaha-motor-india.com/yamaha-r15m.html"),
        BikeModel("yamaha-r15m-monster-edition", "yamaha", "R15M Monster Edition", Res.drawable.model_yamaha_r15m_monster_edition, "Bangladesh official catalogue", "https://www.yamahabd.com/products/"),
        BikeModel("yamaha-mt-15-v2", "yamaha", "MT-15 V2", Res.drawable.model_yamaha_mt_15_v2, "Official India studio reference · exact year / trim may differ", "https://www.yamaha-motor-india.com/yamaha-mt-15-v2.html"),
        BikeModel("yamaha-mt-15-v1", "yamaha", "MT-15 V1", Res.drawable.model_yamaha_mt_15_v1, "Bangladesh official catalogue", "https://www.yamahabd.com/products/"),
        BikeModel("yamaha-fzs-v2", "yamaha", "FZS V2", Res.drawable.model_yamaha_fzs_v2, "Bangladesh official catalogue", "https://www.yamahabd.com/products/"),
        BikeModel("yamaha-fzs-v4", "yamaha", "FZS V4", Res.drawable.model_yamaha_fzs_v4, "Bangladesh official catalogue", "https://www.yamahabd.com/products/"),
        BikeModel("yamaha-fz-x", "yamaha", "FZ-X", Res.drawable.model_yamaha_fz_x, "Bangladesh official catalogue", "https://www.yamahabd.com/products/"),
        BikeModel("yamaha-fazer-fi-v2", "yamaha", "Fazer FI V2", Res.drawable.model_yamaha_fazer_fi_v2, "Bangladesh official catalogue", "https://www.yamahabd.com/products/"),
        BikeModel("yamaha-fz-25", "yamaha", "FZ-25", Res.drawable.model_yamaha_fz_25, "Bangladesh official catalogue", "https://www.yamahabd.com/products/"),
        BikeModel("yamaha-saluto-125", "yamaha", "Saluto 125", Res.drawable.model_yamaha_saluto_125, "Bangladesh official catalogue", "https://www.yamahabd.com/products/"),
        BikeModel("yamaha-fzs-fi-hybrid", "yamaha", "FZS FI Hybrid", Res.drawable.model_yamaha_fzs_fi_hybrid, "Bangladesh official catalogue", "https://www.yamahabd.com/products/"),
        BikeModel("yamaha-aerox-155-abs", "yamaha", "Aerox 155 ABS", Res.drawable.model_yamaha_aerox_155_abs, "Bangladesh official catalogue", "https://www.yamahabd.com/products/"),
        BikeModel("yamaha-rayzr-125-fi", "yamaha", "RayZR 125 FI", Res.drawable.model_yamaha_rayzr_125_fi, "Bangladesh official catalogue", "https://www.yamahabd.com/products/"),
        BikeModel("yamaha-fz-fi-v3-family", "yamaha", "FZ FI · V3 family", Res.drawable.model_yamaha_fz_fi_v3_family, "Official India reference · confirm exact year and trim", "https://www.yamaha-motor-india.com/yamaha-fz-fi.html"),
        BikeModel("honda-cbr150r", "honda", "CBR150R", Res.drawable.model_honda_cbr150r, "Bangladesh official catalogue", "https://www.bdhonda.com/products"),
        BikeModel("honda-hornet-2-0", "honda", "Hornet 2.0", Res.drawable.model_honda_hornet_2_0, "Bangladesh official product-page studio image", "https://www.bdhonda.com/products/hornet20"),
        BikeModel("honda-x-blade", "honda", "X-Blade", Res.drawable.model_honda_x_blade, "Bangladesh official catalogue", "https://www.bdhonda.com/products"),
        BikeModel("honda-sp160", "honda", "SP160", Res.drawable.model_honda_sp160, "Bangladesh official product-page studio image", "https://www.bdhonda.com/products/sp160"),
        BikeModel("honda-sp125", "honda", "SP125", Res.drawable.model_honda_sp125, "Bangladesh official product-page studio image", "https://www.bdhonda.com/products/sp125"),
        BikeModel("honda-livo", "honda", "Livo", Res.drawable.model_honda_livo, "Bangladesh official catalogue", "https://www.bdhonda.com/products"),
        BikeModel("honda-dream-110", "honda", "Dream 110", Res.drawable.model_honda_dream_110, "Bangladesh official catalogue", "https://www.bdhonda.com/products"),
        BikeModel("honda-shine-100", "honda", "Shine 100", Res.drawable.model_honda_shine_100, "Bangladesh official catalogue", "https://www.bdhonda.com/products"),
        BikeModel("honda-shine-100-dx", "honda", "Shine 100 DX", Res.drawable.model_honda_shine_100_dx, "Bangladesh official catalogue", "https://www.bdhonda.com/products"),
        BikeModel("honda-nx200", "honda", "NX200", Res.drawable.model_honda_nx200, "Bangladesh official catalogue", "https://www.bdhonda.com/products"),
        BikeModel("suzuki-gixxer", "suzuki", "Gixxer", Res.drawable.model_suzuki_gixxer, "Bangladesh official product-page studio image", "https://suzuki.com.bd/product/gixxer?variant=24bh2582g3"),
        BikeModel("suzuki-gixxer-sf", "suzuki", "Gixxer SF", Res.drawable.model_suzuki_gixxer_sf, "Bangladesh official product-page studio image", "https://suzuki.com.bd/product/gixxer-sf?variant=3drqohr5q"),
        BikeModel("suzuki-gsx-r150", "suzuki", "GSX-R150", Res.drawable.model_suzuki_gsx_r150, "Bangladesh official product-page studio image", "https://suzuki.com.bd/product/gsx-r150?variant=62e1pmbm9"),
        BikeModel("suzuki-gixxer-monotone", "suzuki", "Gixxer Monotone", Res.drawable.model_suzuki_gixxer_monotone, "Bangladesh official product-page studio image", "https://suzuki.com.bd/product/gixxer-monotone?variant=1i2o1otvm"),
        BikeModel("suzuki-gixxer-classic-matt", "suzuki", "Gixxer Classic Matt", Res.drawable.model_suzuki_gixxer_classic_matt, "Bangladesh official product-page studio image", "https://suzuki.com.bd/product/gixxer-classic-matt?variant=wq0pokir"),
        BikeModel("suzuki-gixxer-250", "suzuki", "Gixxer 250", Res.drawable.model_suzuki_gixxer_250, "Bangladesh official product-page studio image", "https://suzuki.com.bd/product/gixxer-250?variant=3fkhs7d9f"),
        BikeModel("suzuki-gixxer-sf-250", "suzuki", "Gixxer SF 250", Res.drawable.model_suzuki_gixxer_sf_250, "Bangladesh official product-page studio image", "https://suzuki.com.bd/product/gixxer-sf-250?variant=87k31m99j"),
        BikeModel("suzuki-gsx-125", "suzuki", "GSX 125", Res.drawable.model_suzuki_gsx_125, "Bangladesh official product-page studio image", "https://suzuki.com.bd/product/gsx-125?variant=wq0n74wu"),
        BikeModel("suzuki-hayate-ep", "suzuki", "Hayate EP", Res.drawable.model_suzuki_hayate_ep, "Bangladesh official product-page studio image", "https://suzuki.com.bd/product/hayate-ep?variant=qq67d14h"),
        BikeModel("suzuki-access-125", "suzuki", "Access 125", Res.drawable.model_suzuki_access_125, "Bangladesh official product-page studio image", "https://suzuki.com.bd/product/access-125?variant=20h2eoaue"),
        BikeModel("bajaj-pulsar-150-twin-disc-abs", "bajaj", "Pulsar 150 Twin Disc ABS", Res.drawable.model_bajaj_pulsar_150_twin_disc_abs, "Bangladesh official catalogue", "https://www.bajajauto.com/en-bd/bikes"),
        BikeModel("bajaj-pulsar-n160", "bajaj", "Pulsar N160", Res.drawable.model_bajaj_pulsar_n160, "Bangladesh official catalogue", "https://www.bajajauto.com/en-bd/bikes"),
        BikeModel("bajaj-pulsar-n250", "bajaj", "Pulsar N250", Res.drawable.model_bajaj_pulsar_n250, "Bangladesh official catalogue", "https://www.bajajauto.com/en-bd/bikes"),
        BikeModel("bajaj-pulsar-f250", "bajaj", "Pulsar F250", Res.drawable.model_bajaj_pulsar_f250, "Bangladesh official catalogue", "https://www.bajajauto.com/en-bd/bikes"),
        BikeModel("bajaj-pulsar-ns125", "bajaj", "Pulsar NS125", Res.drawable.model_bajaj_pulsar_ns125, "Bangladesh official catalogue", "https://www.bajajauto.com/en-bd/bikes"),
        BikeModel("bajaj-discover-125-disc", "bajaj", "Discover 125 Disc", Res.drawable.model_bajaj_discover_125_disc, "Bangladesh official catalogue", "https://www.bajajauto.com/en-bd/bikes"),
        BikeModel("bajaj-discover-110-disc", "bajaj", "Discover 110 Disc", Res.drawable.model_bajaj_discover_110_disc, "Bangladesh official catalogue", "https://www.bajajauto.com/en-bd/bikes"),
        BikeModel("bajaj-platina-110-h", "bajaj", "Platina 110 H", Res.drawable.model_bajaj_platina_110_h, "Bangladesh official catalogue", "https://www.bajajauto.com/en-bd/bikes"),
        BikeModel("bajaj-platina-100-es", "bajaj", "Platina 100 ES", Res.drawable.model_bajaj_platina_100_es, "Bangladesh official catalogue", "https://www.bajajauto.com/en-bd/bikes"),
        BikeModel("bajaj-ct100-es", "bajaj", "CT100 ES", Res.drawable.model_bajaj_ct100_es, "Bangladesh official catalogue", "https://www.bajajauto.com/en-bd/bikes"),
        BikeModel("hero-hunk-150r", "hero", "Hunk 150R", Res.drawable.model_hero_hunk_150r, "Bangladesh official catalogue", "https://www.heromotocorp.com/en-bd.html"),
        BikeModel("hero-hunk-150-xtec", "hero", "Hunk 150 Xtec", Res.drawable.model_hero_hunk_150_xtec, "Bangladesh official catalogue", "https://www.heromotocorp.com/en-bd.html"),
        BikeModel("hero-thriller-160r", "hero", "Thriller 160R", Res.drawable.model_hero_thriller_160r, "Bangladesh official catalogue", "https://www.heromotocorp.com/en-bd.html"),
        BikeModel("hero-thriller-160r-4v", "hero", "Thriller 160R 4V", Res.drawable.model_hero_thriller_160r_4v, "Bangladesh official catalogue", "https://www.heromotocorp.com/en-bd.html"),
        BikeModel("hero-ignitor-techno", "hero", "Ignitor Techno", Res.drawable.model_hero_ignitor_techno, "Bangladesh official catalogue", "https://www.heromotocorp.com/en-bd.html"),
        BikeModel("hero-ignitor-xtec", "hero", "Ignitor Xtec", Res.drawable.model_hero_ignitor_xtec, "Bangladesh official catalogue", "https://www.heromotocorp.com/en-bd.html"),
        BikeModel("hero-glamour", "hero", "Glamour", Res.drawable.model_hero_glamour, "Bangladesh official catalogue", "https://www.heromotocorp.com/en-bd.html"),
        BikeModel("hero-splendor-plus-se", "hero", "Splendor Plus SE", Res.drawable.model_hero_splendor_plus_se, "Bangladesh official catalogue", "https://www.heromotocorp.com/en-bd.html"),
        BikeModel("hero-splendor-plus-xtec", "hero", "Splendor Plus Xtec", Res.drawable.model_hero_splendor_plus_xtec, "Bangladesh official catalogue", "https://www.heromotocorp.com/en-bd.html"),
        BikeModel("hero-hf-deluxe", "hero", "HF Deluxe", Res.drawable.model_hero_hf_deluxe, "Bangladesh official catalogue", "https://www.heromotocorp.com/en-bd.html"),
        BikeModel("hero-passion-xpro", "hero", "Passion Xpro", Res.drawable.model_hero_passion_xpro, "Bangladesh official catalogue", "https://www.heromotocorp.com/en-bd.html"),
        BikeModel("hero-passion-xpro-xtec", "hero", "Passion Xpro Xtec", Res.drawable.model_hero_passion_xpro_xtec, "Bangladesh official catalogue", "https://www.heromotocorp.com/en-bd.html"),
        BikeModel("hero-pleasure", "hero", "Pleasure", Res.drawable.model_hero_pleasure, "Bangladesh official catalogue", "https://www.heromotocorp.com/en-bd.html"),
        BikeModel("hero-maestro-edge-xtec", "hero", "Maestro Edge Xtec", Res.drawable.model_hero_maestro_edge_xtec, "Bangladesh official catalogue", "https://www.heromotocorp.com/en-bd.html"),
        BikeModel("hero-hunk-150", "hero", "Hunk 150", Res.drawable.model_hero_hunk_150, "Bangladesh official catalogue", "https://www.heromotocorp.com/en-bd.html"),
        BikeModel("tvs-apache-rtr-160-4v", "tvs", "Apache RTR 160 4V", Res.drawable.model_tvs_apache_rtr_160_4v, "Bangladesh official catalogue", "https://bangladesh.tvsmotor.com/en/p/our-products/rtr-160-4v-bd"),
        BikeModel("tvs-apache-rtr-160-2v", "tvs", "Apache RTR 160 2V", Res.drawable.model_tvs_apache_rtr_160_2v, "Bangladesh official catalogue", "https://bangladesh.tvsmotor.com/en/p/our-products/apache-rtr-160-2v-refresh-bd"),
        BikeModel("tvs-raider-125", "tvs", "Raider 125", Res.drawable.model_tvs_raider_125, "Bangladesh official catalogue", "https://bangladesh.tvsmotor.com/en/p/our-products/tvs-raider-bd"),
        BikeModel("cfmoto-250nk", "cfmoto", "250NK", Res.drawable.model_cfmoto_250nk, "Official Philippines reference · regional colour / trim may differ", "https://www.cfmotoph.com/motorcycle/250nk"),
        BikeModel("cfmoto-250sr", "cfmoto", "250SR", Res.drawable.model_cfmoto_250sr, "Official Philippines reference · regional colour / trim may differ", "https://www.cfmotoph.com/motorcycle/250sr"),
        BikeModel("cfmoto-300sr", "cfmoto", "300SR", Res.drawable.model_cfmoto_300sr, "Bangladesh official catalogue", "https://www.cfmotobd.com/product/300-sr"),
        BikeModel("cfmoto-300nk", "cfmoto", "300NK", Res.drawable.model_cfmoto_300nk, "Bangladesh official product-page studio image", "https://www.cfmotobd.com/product/300nk"),
        BikeModel("cfmoto-250cl-c", "cfmoto", "250CL-C", Res.drawable.model_cfmoto_250cl_c, "Bangladesh official product-page studio image", "https://www.cfmotobd.com/product/250cl-c"),
        BikeModel("cfmoto-230-dual", "cfmoto", "230 Dual", Res.drawable.model_cfmoto_230_dual, "Bangladesh official product-page studio image", "https://www.cfmotobd.com/product/230dual"),
        BikeModel("cfmoto-150sc", "cfmoto", "150SC", Res.drawable.model_cfmoto_150sc, "Bangladesh official product-page studio image", "https://www.cfmotobd.com/product/150sc"),
        BikeModel("royal-enfield-classic-350", "enfield", "Classic 350", Res.drawable.model_royal_enfield_classic_350, "Bangladesh official catalogue", "https://www.royalenfield.com/bd/en/motorcycles/classic-350/"),
        BikeModel("royal-enfield-hunter-350", "enfield", "Hunter 350", Res.drawable.model_royal_enfield_hunter_350, "Bangladesh official catalogue", "https://www.royalenfield.com/bd/en/motorcycles/hunter-350/"),
        BikeModel("royal-enfield-bullet-350", "enfield", "Bullet 350", Res.drawable.model_royal_enfield_bullet_350, "Bangladesh official catalogue", "https://www.royalenfield.com/bd/en/motorcycles/bullet-350/"),
        BikeModel("yamaha-fzs-v3", "yamaha", "FZS V3", Res.drawable.model_yamaha_fzs_v3, "Third-party Bangladesh reference · exact generation confirmed by source", "https://riderbd.com/bikes/yamaha-fzs-v3"),
        BikeModel("royal-enfield-meteor-350", "enfield", "Meteor 350", Res.drawable.model_royal_enfield_meteor_350, "Regional distributor reference · exact year / trim may differ", "https://royalenfieldhn.com/motos/meteor-350/")
    )
    fun forBrand(brandId: String) = models.filter { it.brandId == brandId }
    fun search(brandId: String, query: String) = forBrand(brandId).filter { it.name.contains(query.trim(), ignoreCase = true) }
    fun find(id: String?) = models.firstOrNull { it.id == id }
    fun legacyModel(brandId: String): BikeModel? {
        val name = when (brandId) {
            "ktm" -> "Duke 125 · 2023"
            "cfmoto" -> "250NK"
            "yamaha" -> "R15 V4"
            "honda" -> "CBR150R"
            "suzuki" -> "Gixxer SF"
            "bajaj" -> "Pulsar N160"
            "tvs" -> "Apache RTR 160 4V"
            "hero" -> "Hunk 150R"
            else -> "Classic 350"
        }
        return forBrand(brandId).firstOrNull { it.name == name }
    }
}

