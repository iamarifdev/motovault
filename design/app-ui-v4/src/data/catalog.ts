import type { DocKind, ItemKey } from '../types'

export interface ModelVariant {
  id: string
  label: string
  name: string
  years: [number, number]
  cc: number
  specs: string[]
  photo?: string
}

export interface Model {
  id: string
  name: string
  cc: number
  photo?: string
  variants: ModelVariant[]
}

export interface Brand {
  id: string
  name: string
  light: string
  dark: string
  logo: string
  models: Model[]
}

const p = (file: string) => `/bikes/${file}`

// Bangladesh road-presence catalogue: covers all popular current & legacy models, versions/editions, CCs & release years.
// Accent pairs are contrast-checked in Appearance.kt (all pass 4.5:1 with button text).
export const BRANDS: Brand[] = [
  {
    id: 'yamaha',
    name: 'Yamaha',
    light: '#315ABE',
    dark: '#88A8FF',
    logo: '/logos/logo_yamaha.svg',
    models: [
      {
        id: 'yamaha-r15',
        name: 'R15',
        cc: 155,
        photo: p('model_yamaha_r15_v4.png'),
        variants: [
          { id: 'yamaha-r15-v4', label: 'V4', name: 'R15 V4', years: [2022, 2026], cc: 155, specs: ['USD ফর্ক', 'Dual ABS', 'VVA · FI', 'Liquid-Cooled'], photo: p('model_yamaha_r15_v4.png') },
          { id: 'yamaha-r15m', label: 'R15M', name: 'R15M', years: [2022, 2026], cc: 155, specs: ['Quickshifter', 'TFT মিটার', 'Dual ABS', 'Liquid-Cooled'], photo: p('model_yamaha_r15m.png') },
          { id: 'yamaha-r15m-monster-edition', label: 'Monster Edition', name: 'R15M Monster Edition', years: [2023, 2026], cc: 155, specs: ['MotoGP লিভারি', 'Quickshifter', 'Dual ABS'], photo: p('model_yamaha_r15m_monster_edition.png') },
          { id: 'yamaha-r15-v3', label: 'V3', name: 'R15 V3', years: [2018, 2022], cc: 155, specs: ['VVA · স্লিপার ক্লাচ', 'Dual ABS / Non-ABS', 'Liquid-Cooled'], photo: p('model_yamaha_r15_v3.png') },
          { id: 'yamaha-r15-v2', label: 'V2', name: 'R15 V2', years: [2013, 2018], cc: 149, specs: ['Twin Halogen হেডলাইট', 'SOHC · FI', 'Dual Disc'], photo: p('model_yamaha_r15_v2.png') },
        ],
      },
      {
        id: 'yamaha-mt15',
        name: 'MT-15',
        cc: 155,
        photo: p('model_yamaha_mt_15_v2.png'),
        variants: [
          { id: 'yamaha-mt-15-v2', label: 'V2', name: 'MT-15 V2', years: [2022, 2026], cc: 155, specs: ['USD ফর্ক', 'Dual ABS', 'Alu সুইংআর্ম', 'Liquid-Cooled'], photo: p('model_yamaha_mt_15_v2.png') },
          { id: 'yamaha-mt-15-v1', label: 'V1', name: 'MT-15 V1', years: [2019, 2022], cc: 155, specs: ['VVA · FI', 'Single ABS', 'Liquid-Cooled'], photo: p('model_yamaha_mt_15_v1.png') },
        ],
      },
      {
        id: 'yamaha-fzs',
        name: 'FZS / FZ-FI',
        cc: 149,
        photo: p('model_yamaha_fzs_v4.png'),
        variants: [
          { id: 'yamaha-fzs-v4', label: 'V4 Deluxe', name: 'FZS V4', years: [2023, 2026], cc: 149, specs: ['Traction Control', 'LED হেডলাইট', 'Single ABS'], photo: p('model_yamaha_fzs_v4.png') },
          { id: 'yamaha-fzs-fi-hybrid', label: 'FI Hybrid', name: 'FZS FI Hybrid', years: [2025, 2026], cc: 149, specs: ['SMG Hybrid', 'TFT · Turn-by-Turn', 'ABS'], photo: p('model_yamaha_fzs_fi_hybrid.png') },
          { id: 'yamaha-fzs-v3', label: 'V3 ABS', name: 'FZS V3', years: [2019, 2024], cc: 149, specs: ['Single-Channel ABS', 'FI', 'Air-Cooled'], photo: p('model_yamaha_fzs_v3.png') },
          { id: 'yamaha-fz-fi-v3-family', label: 'FZ-FI V3', name: 'FZ FI · V3', years: [2019, 2023], cc: 149, specs: ['ম্যাট ফিনিশ', 'FI · Single ABS'], photo: p('model_yamaha_fz_fi_v3_family.png') },
          { id: 'yamaha-fzs-v2', label: 'V2 DD/SD', name: 'FZS V2', years: [2015, 2023], cc: 149, specs: ['টুইন সিট', 'FI · Air-Cooled'], photo: p('model_yamaha_fzs_v2.png') },
        ],
      },
      {
        id: 'yamaha-fz-x',
        name: 'FZ-X',
        cc: 149,
        photo: p('model_yamaha_fz_x.png'),
        variants: [
          { id: 'yamaha-fz-x', label: 'Standard / Chrome', name: 'FZ-X', years: [2022, 2026], cc: 149, specs: ['Neo-Retro', 'Traction Control', 'Single ABS'], photo: p('model_yamaha_fz_x.png') },
        ],
      },
      {
        id: 'yamaha-fazer-fi-v2',
        name: 'Fazer FI V2',
        cc: 149,
        photo: p('model_yamaha_fazer_fi_v2.png'),
        variants: [
          { id: 'yamaha-fazer-fi-v2', label: 'FI V2', name: 'Fazer FI V2', years: [2015, 2024], cc: 149, specs: ['সেমি-ফেয়ার্ড ট্যুরিং', 'FI · Air-Cooled'], photo: p('model_yamaha_fazer_fi_v2.png') },
        ],
      },
      {
        id: 'yamaha-fz-25',
        name: 'FZ-25',
        cc: 249,
        photo: p('model_yamaha_fz_25.png'),
        variants: [
          { id: 'yamaha-fz-25', label: 'Dual ABS', name: 'FZ-25', years: [2018, 2024], cc: 249, specs: ['Oil-Cooled', 'Dual ABS', 'FI'], photo: p('model_yamaha_fz_25.png') },
        ],
      },
      {
        id: 'yamaha-saluto-125',
        name: 'Saluto 125',
        cc: 125,
        photo: p('model_yamaha_saluto_125.png'),
        variants: [
          { id: 'yamaha-saluto-125', label: 'UBS Disc / Armada', name: 'Saluto 125', years: [2016, 2026], cc: 125, specs: ['UBS ব্রেক', 'হালকা ওজন', 'কমিউটার'], photo: p('model_yamaha_saluto_125.png') },
        ],
      },
      {
        id: 'yamaha-scooters',
        name: 'Aerox 155 / RayZR',
        cc: 155,
        photo: p('model_yamaha_aerox_155_abs.png'),
        variants: [
          { id: 'yamaha-aerox-155-abs', label: 'Aerox 155 ABS', name: 'Aerox 155 ABS', years: [2022, 2026], cc: 155, specs: ['Maxi স্কুটার', 'VVA · Liquid-Cooled', 'ABS'], photo: p('model_yamaha_aerox_155_abs.png') },
          { id: 'yamaha-rayzr-125-fi', label: 'RayZR 125 FI', name: 'RayZR 125 FI', years: [2021, 2026], cc: 125, specs: ['FI স্কুটার', 'Stop & Start'], photo: p('model_yamaha_rayzr_125_fi.png') },
        ],
      },
    ],
  },
  {
    id: 'honda',
    name: 'Honda',
    light: '#B42A38',
    dark: '#FF7B84',
    logo: '/logos/logo_honda.png',
    models: [
      {
        id: 'honda-cbr150r',
        name: 'CBR150R',
        cc: 149,
        photo: p('model_honda_cbr150r.png'),
        variants: [
          { id: 'honda-cbr150r', label: 'K45R USD ABS', name: 'CBR150R ABS', years: [2021, 2026], cc: 149, specs: ['USD ফর্ক', 'স্লিপার ক্লাচ', 'Dual ABS'], photo: p('model_honda_cbr150r.png') },
          { id: 'honda-cbr150r-k45g', label: 'K45G / K45N', name: 'CBR150R (Street/Standard)', years: [2016, 2021], cc: 149, specs: ['DOHC · FI', 'টেলিস্কোপিক ফর্ক', 'Dual Keen LED'], photo: p('model_honda_cbr150r_k45g.png') },
        ],
      },
      {
        id: 'honda-hornet',
        name: 'Hornet',
        cc: 184,
        photo: p('model_honda_hornet_2_0.png'),
        variants: [
          { id: 'honda-hornet-2-0', label: 'Hornet 2.0', name: 'Hornet 2.0', years: [2021, 2026], cc: 184, specs: ['USD ফর্ক', 'FI · Single ABS', '১৮৪ cc'], photo: p('model_honda_hornet_2_0.png') },
          { id: 'honda-cb-hornet-160r', label: 'CB Hornet 160R', name: 'CB Hornet 160R', years: [2016, 2022], cc: 163, specs: ['X-Shape Tail LED', 'টেলিস্কোপিক ফর্ক', 'Carb · CBS/ABS'], photo: p('model_honda_cb_hornet_160r.png') },
        ],
      },
      {
        id: 'honda-sp160',
        name: 'SP160',
        cc: 163,
        photo: p('model_honda_sp160.png'),
        variants: [
          { id: 'honda-sp160', label: 'Single / Twin Disc ABS', name: 'SP160', years: [2024, 2026], cc: 163, specs: ['PGM-FI', 'Single ABS', 'ডিজিটাল মিটার'], photo: p('model_honda_sp160.png') },
        ],
      },
      {
        id: 'honda-x-blade',
        name: 'X-Blade',
        cc: 163,
        photo: p('model_honda_x_blade.png'),
        variants: [
          { id: 'honda-x-blade', label: 'ABS Double Disc', name: 'X-Blade ABS', years: [2020, 2026], cc: 163, specs: ['Robo-Face LED', 'Single ABS'], photo: p('model_honda_x_blade.png') },
          { id: 'honda-x-blade-sd', label: 'Single Disc (Carb)', name: 'X-Blade SD', years: [2018, 2022], cc: 163, specs: ['কার্বুরেটর', 'Single Disc'], photo: p('model_honda_x_blade.png') },
        ],
      },
      {
        id: 'honda-sp125',
        name: 'SP 125 / Shine SP',
        cc: 124,
        photo: p('model_honda_sp125.png'),
        variants: [
          { id: 'honda-sp125', label: 'SP 125 (FI)', name: 'SP 125', years: [2023, 2026], cc: 124, specs: ['Silent Start ACG', 'PGM-FI · CBS'], photo: p('model_honda_sp125.png') },
          { id: 'honda-cb-shine-sp', label: 'CB Shine SP', name: 'CB Shine SP 125', years: [2016, 2023], cc: 125, specs: ['৫-স্পিড', 'কার্বুরেটর · CBS'], photo: p('model_honda_cb_shine_sp.png') },
        ],
      },
      {
        id: 'honda-livo',
        name: 'Livo 110',
        cc: 110,
        photo: p('model_honda_livo.png'),
        variants: [
          { id: 'honda-livo', label: 'Disc / Drum', name: 'Livo', years: [2017, 2026], cc: 110, specs: ['HET কমিউটার', 'Disc বা Drum'], photo: p('model_honda_livo.png') },
        ],
      },
      {
        id: 'honda-dream-110',
        name: 'Dream 110',
        cc: 109,
        photo: p('model_honda_dream_110.png'),
        variants: [
          { id: 'honda-dream-110', label: 'Neo / 110', name: 'Dream 110', years: [2016, 2026], cc: 109, specs: ['ড্রাম ব্রেক', 'উচ্চ মাইলেজ'], photo: p('model_honda_dream_110.png') },
        ],
      },
      {
        id: 'honda-shine-100',
        name: 'Shine 100',
        cc: 99,
        photo: p('model_honda_shine_100_dx.png'),
        variants: [
          { id: 'honda-shine-100-dx', label: 'DX', name: 'Shine 100 DX', years: [2025, 2026], cc: 99, specs: ['ডিজিটাল ক্লাস্টার', 'টিউবলেস'], photo: p('model_honda_shine_100_dx.png') },
          { id: 'honda-shine-100', label: 'Standard', name: 'Shine 100', years: [2024, 2026], cc: 99, specs: ['eSP · CBS', 'হালকা কমিউটার'], photo: p('model_honda_shine_100.png') },
        ],
      },
      {
        id: 'honda-nx200',
        name: 'NX200',
        cc: 184,
        photo: p('model_honda_nx200.png'),
        variants: [
          { id: 'honda-nx200', label: 'ADV Tourer', name: 'NX200', years: [2025, 2026], cc: 184, specs: ['USD ফর্ক', 'Dual ABS', 'উইন্ডস্ক্রিন'], photo: p('model_honda_nx200.png') },
        ],
      },
    ],
  },
  {
    id: 'bajaj',
    name: 'Bajaj',
    light: '#355D9E',
    dark: '#96B9FA',
    logo: '/logos/logo_bajaj.svg',
    models: [
      {
        id: 'bajaj-pulsar-150',
        name: 'Pulsar 150',
        cc: 149,
        photo: p('model_bajaj_pulsar_150_twin_disc_abs.png'),
        variants: [
          { id: 'bajaj-pulsar-150-twin-disc-abs', label: 'Twin Disc ABS', name: 'Pulsar 150 Twin Disc ABS', years: [2019, 2026], cc: 149, specs: ['Twin Disc', 'Single ABS', 'Split Seat'], photo: p('model_bajaj_pulsar_150_twin_disc_abs.png') },
          { id: 'bajaj-pulsar-150-sd', label: 'Single Disc (UG4/UG5)', name: 'Pulsar 150 Single Disc', years: [2012, 2024], cc: 149, specs: ['DTS-i কার্ব', 'সিঙ্গেল সিট', 'Rear Drum'], photo: p('model_bajaj_pulsar_150_sd.png') },
        ],
      },
      {
        id: 'bajaj-pulsar-n160',
        name: 'Pulsar N160',
        cc: 165,
        photo: p('model_bajaj_pulsar_n160.png'),
        variants: [
          { id: 'bajaj-pulsar-n160', label: 'Dual ABS', name: 'Pulsar N160', years: [2022, 2026], cc: 165, specs: ['Oil-Cooled', 'Dual-Channel ABS', 'Bi-LED'], photo: p('model_bajaj_pulsar_n160.png') },
          { id: 'bajaj-n160-usd', label: 'USD · Bluetooth', name: 'Pulsar N160 USD', years: [2024, 2026], cc: 165, specs: ['USD ফর্ক', 'রাইড মোড', 'Dual ABS'], photo: p('model_bajaj_pulsar_n160.png') },
        ],
      },
      {
        id: 'bajaj-pulsar-ns',
        name: 'Pulsar NS125 / NS160',
        cc: 124,
        photo: p('model_bajaj_pulsar_ns125.png'),
        variants: [
          { id: 'bajaj-pulsar-ns125', label: 'NS125', name: 'Pulsar NS125', years: [2022, 2026], cc: 124, specs: ['Perimeter Frame', 'CBS · Air-Cooled'], photo: p('model_bajaj_pulsar_ns125.png') },
          { id: 'bajaj-pulsar-ns160', label: 'NS160 FI ABS', name: 'Pulsar NS160', years: [2017, 2025], cc: 160, specs: ['Oil-Cooled', 'Twin Disc · ABS'], photo: p('model_bajaj_pulsar_ns160.png') },
        ],
      },
      {
        id: 'bajaj-pulsar-250',
        name: 'Pulsar N250 / F250',
        cc: 249,
        photo: p('model_bajaj_pulsar_n250.png'),
        variants: [
          { id: 'bajaj-pulsar-n250', label: 'N250 (Naked)', name: 'Pulsar N250', years: [2024, 2026], cc: 249, specs: ['USD ফর্ক', 'Traction Control', 'Dual ABS'], photo: p('model_bajaj_pulsar_n250.png') },
          { id: 'bajaj-pulsar-f250', label: 'F250 (Semi-Faired)', name: 'Pulsar F250', years: [2024, 2026], cc: 249, specs: ['সেমি-ফেয়ার্ড', 'Dual ABS', 'Oil-Cooled'], photo: p('model_bajaj_pulsar_f250.png') },
        ],
      },
      {
        id: 'bajaj-discover',
        name: 'Discover',
        cc: 125,
        photo: p('model_bajaj_discover_125_disc.png'),
        variants: [
          { id: 'bajaj-discover-125-disc', label: '125 Disc', name: 'Discover 125 Disc', years: [2015, 2026], cc: 125, specs: ['DTS-i', 'Front Disc · Anti-Skid'], photo: p('model_bajaj_discover_125_disc.png') },
          { id: 'bajaj-discover-110-disc', label: '110 Disc', name: 'Discover 110 Disc', years: [2018, 2026], cc: 115, specs: ['DTS-i · ৫-গিয়ার', 'DRL'], photo: p('model_bajaj_discover_110_disc.png') },
        ],
      },
      {
        id: 'bajaj-platina',
        name: 'Platina',
        cc: 115,
        photo: p('model_bajaj_platina_110_h.png'),
        variants: [
          { id: 'bajaj-platina-110-h', label: '110 H-Gear', name: 'Platina 110 H-Gear', years: [2019, 2026], cc: 115, specs: ['৫-স্পিড H-Gear', 'Front Disc'], photo: p('model_bajaj_platina_110_h.png') },
          { id: 'bajaj-platina-100-es', label: '100 ES', name: 'Platina 100 ES', years: [2015, 2026], cc: 102, specs: ['Electric Start', 'উচ্চ মাইলেজ'], photo: p('model_bajaj_platina_100_es.png') },
        ],
      },
      {
        id: 'bajaj-ct100-es',
        name: 'CT100 ES',
        cc: 102,
        photo: p('model_bajaj_ct100_es.png'),
        variants: [
          { id: 'bajaj-ct100-es', label: 'ES / Alloy', name: 'CT100 ES', years: [2015, 2026], cc: 102, specs: ['রাফ অ্যান্ড টাফ', 'কমিউটার'], photo: p('model_bajaj_ct100_es.png') },
        ],
      },
    ],
  },
  {
    id: 'tvs',
    name: 'TVS',
    light: '#A52B41',
    dark: '#F4828B',
    logo: '/logos/logo_tvs.svg',
    models: [
      {
        id: 'tvs-apache-160',
        name: 'Apache RTR 160',
        cc: 160,
        photo: p('model_tvs_apache_rtr_160_4v.png'),
        variants: [
          { id: 'tvs-apache-rtr-160-4v', label: '4V FI / SmartXonnect', name: 'Apache RTR 160 4V', years: [2021, 2026], cc: 160, specs: ['Oil-Cooled · ৪-ভালভ', 'Dual/Single ABS', 'RT-Fi'], photo: p('model_tvs_apache_rtr_160_4v.png') },
          { id: 'tvs-apache-160-4v-carb', label: '4V Carburetor', name: 'Apache RTR 160 4V (Carb)', years: [2018, 2023], cc: 160, specs: ['Oil-Cooled', 'কার্বুরেটর · SD/DD'], photo: p('model_tvs_apache_rtr_160_4v.png') },
          { id: 'tvs-apache-rtr-160-2v', label: '2V Refresh', name: 'Apache RTR 160 2V', years: [2012, 2025], cc: 160, specs: ['Air-Cooled', 'ক্লিপ-অন হ্যান্ডেল'], photo: p('model_tvs_apache_rtr_160_2v.png') },
        ],
      },
      {
        id: 'tvs-raider-125',
        name: 'Raider 125',
        cc: 125,
        photo: p('model_tvs_raider_125.png'),
        variants: [
          { id: 'tvs-raider-125', label: 'FI / SmartXonnect', name: 'Raider 125', years: [2022, 2026], cc: 125, specs: ['৩-ভালভ', 'Eco/Power মোড', 'আন্ডার-সিট স্টোরেজ'], photo: p('model_tvs_raider_125.png') },
        ],
      },
      {
        id: 'tvs-stryker-125',
        name: 'Stryker 125',
        cc: 125,
        photo: p('model_tvs_stryker_125.png'),
        variants: [
          { id: 'tvs-stryker-125', label: '125 Disc', name: 'Stryker 125', years: [2016, 2025], cc: 125, specs: ['EcoThrust', 'কমিউটার ডিস্ক'], photo: p('model_tvs_stryker_125.png') },
        ],
      },
      {
        id: 'tvs-metro-plus',
        name: 'Metro Plus 110',
        cc: 110,
        photo: p('model_tvs_metro_plus.png'),
        variants: [
          { id: 'tvs-metro-plus', label: 'Disc / Drum', name: 'Metro Plus 110', years: [2015, 2026], cc: 110, specs: ['USB চার্জার', 'মাইলেজ কমিউটার'], photo: p('model_tvs_metro_plus.png') },
        ],
      },
      {
        id: 'tvs-ntorq-125',
        name: 'Ntorq 125',
        cc: 125,
        photo: p('model_tvs_ntorq_125.png'),
        variants: [
          { id: 'tvs-ntorq-125', label: 'Race XP / XT', name: 'Ntorq 125', years: [2019, 2026], cc: 125, specs: ['স্পোর্ট স্কুটার', 'SmartXonnect'], photo: p('model_tvs_ntorq_125.png') },
        ],
      },
    ],
  },
  {
    id: 'suzuki',
    name: 'Suzuki',
    light: '#246294',
    dark: '#7EBAF5',
    logo: '/logos/logo_suzuki.svg',
    models: [
      {
        id: 'suzuki-gixxer-naked',
        name: 'Gixxer (155)',
        cc: 155,
        photo: p('model_suzuki_gixxer.png'),
        variants: [
          { id: 'suzuki-gixxer', label: 'New Gen FI ABS', name: 'Gixxer', years: [2020, 2026], cc: 155, specs: ['LED হেডলাইট', 'FI · Single ABS'], photo: p('model_suzuki_gixxer.png') },
          { id: 'suzuki-gixxer-monotone', label: 'Monotone', name: 'Gixxer Monotone', years: [2018, 2025], cc: 155, specs: ['Twin Exhaust Tip', 'Carb / FI'], photo: p('model_suzuki_gixxer_monotone.png') },
          { id: 'suzuki-gixxer-classic-matt', label: 'Classic Matt', name: 'Gixxer Classic Matt', years: [2016, 2021], cc: 155, specs: ['ম্যাট বডি', 'কার্বুরেটর · SD/DD'], photo: p('model_suzuki_gixxer_classic_matt.png') },
        ],
      },
      {
        id: 'suzuki-gixxer-sf-group',
        name: 'Gixxer SF',
        cc: 155,
        photo: p('model_suzuki_gixxer_sf.png'),
        variants: [
          { id: 'suzuki-gixxer-sf', label: 'New Gen FI ABS', name: 'Gixxer SF', years: [2020, 2026], cc: 155, specs: ['ফুল ফেয়ার্ড', 'LED · Single ABS'], photo: p('model_suzuki_gixxer_sf.png') },
          { id: 'suzuki-gixxer-sf-250', label: 'SF 250', name: 'Gixxer SF 250', years: [2024, 2026], cc: 249, specs: ['SOCS Oil-Cooled', 'Dual ABS'], photo: p('model_suzuki_gixxer_sf_250.png') },
        ],
      },
      {
        id: 'suzuki-gixxer-250',
        name: 'Gixxer 250',
        cc: 249,
        photo: p('model_suzuki_gixxer_250.png'),
        variants: [
          { id: 'suzuki-gixxer-250', label: '250 Dual ABS', name: 'Gixxer 250', years: [2024, 2026], cc: 249, specs: ['SOCS Oil-Cooled', 'Dual ABS · ৬-স্পিড'], photo: p('model_suzuki_gixxer_250.png') },
        ],
      },
      {
        id: 'suzuki-gsx-r150',
        name: 'GSX-R150',
        cc: 147,
        photo: p('model_suzuki_gsx_r150.png'),
        variants: [
          { id: 'suzuki-gsx-r150', label: 'Keyless Dual ABS', name: 'GSX-R150', years: [2018, 2026], cc: 147, specs: ['DOHC Liquid-Cooled', 'Keyless · Dual ABS'], photo: p('model_suzuki_gsx_r150.png') },
        ],
      },
      {
        id: 'suzuki-gsx-125',
        name: 'GSX 125',
        cc: 125,
        photo: p('model_suzuki_gsx_125.png'),
        variants: [
          { id: 'suzuki-gsx-125', label: '125 Disc', name: 'GSX 125', years: [2021, 2026], cc: 125, specs: ['স্মুথ ইঞ্জিন', 'কমিউটার'], photo: p('model_suzuki_gsx_125.png') },
        ],
      },
      {
        id: 'suzuki-hayate-ep',
        name: 'Hayate EP',
        cc: 113,
        photo: p('model_suzuki_hayate_ep.png'),
        variants: [
          { id: 'suzuki-hayate-ep', label: 'SEP 110', name: 'Hayate EP', years: [2016, 2026], cc: 113, specs: ['SEP মাইলেজ', 'টিউবলেস'], photo: p('model_suzuki_hayate_ep.png') },
        ],
      },
      {
        id: 'suzuki-access-125',
        name: 'Access 125',
        cc: 124,
        photo: p('model_suzuki_access_125.png'),
        variants: [
          { id: 'suzuki-access-125', label: 'SE / FI', name: 'Access 125', years: [2020, 2026], cc: 124, specs: ['রেট্রো স্কুটার', 'SEP ইঞ্জিন'], photo: p('model_suzuki_access_125.png') },
        ],
      },
    ],
  },
  {
    id: 'hero',
    name: 'Hero',
    light: '#B1362C',
    dark: '#FF8983',
    logo: '/logos/logo_hero.svg',
    models: [
      {
        id: 'hero-hunk',
        name: 'Hunk 150',
        cc: 149,
        photo: p('model_hero_hunk_150r.png'),
        variants: [
          { id: 'hero-hunk-150-xtec', label: '150 Xtec', name: 'Hunk 150 Xtec', years: [2024, 2026], cc: 149, specs: ['LED হেডলাইট', 'Bluetooth · ABS'], photo: p('model_hero_hunk_150_xtec.png') },
          { id: 'hero-hunk-150r', label: '150R ABS', name: 'Hunk 150R', years: [2021, 2026], cc: 149, specs: ['Mono-Shock', 'Single ABS', 'SD / DD'], photo: p('model_hero_hunk_150r.png') },
          { id: 'hero-hunk-150', label: 'Classic 150', name: 'Hunk 150', years: [2014, 2022], cc: 149, specs: ['টুইন শক', 'কার্বুরেটর'], photo: p('model_hero_hunk_150.png') },
        ],
      },
      {
        id: 'hero-thriller',
        name: 'Thriller 160R',
        cc: 163,
        photo: p('model_hero_thriller_160r_4v.png'),
        variants: [
          { id: 'hero-thriller-160r-4v', label: '160R 4V (USD)', name: 'Thriller 160R 4V', years: [2024, 2026], cc: 163, specs: ['USD ফর্ক', 'Oil-Cooled · ৪-ভালভ', 'ABS'], photo: p('model_hero_thriller_160r_4v.png') },
          { id: 'hero-thriller-160r', label: '160R 2V', name: 'Thriller 160R', years: [2021, 2024], cc: 163, specs: ['FI · Air-Cooled', 'Single ABS'], photo: p('model_hero_thriller_160r.png') },
        ],
      },
      {
        id: 'hero-ignitor',
        name: 'Ignitor 125',
        cc: 125,
        photo: p('model_hero_ignitor_xtec.png'),
        variants: [
          { id: 'hero-ignitor-xtec', label: 'Xtec', name: 'Ignitor Xtec', years: [2024, 2026], cc: 125, specs: ['H-LED হেডলাইট', 'ডিজিটাল ক্লাস্টার'], photo: p('model_hero_ignitor_xtec.png') },
          { id: 'hero-ignitor-techno', label: 'Techno', name: 'Ignitor Techno', years: [2018, 2024], cc: 125, specs: ['i3S স্টার্ট', 'Front Disc'], photo: p('model_hero_ignitor_techno.png') },
        ],
      },
      {
        id: 'hero-splendor',
        name: 'Splendor+',
        cc: 97,
        photo: p('model_hero_splendor_plus_xtec.png'),
        variants: [
          { id: 'hero-splendor-plus-xtec', label: 'Xtec', name: 'Splendor Plus Xtec', years: [2023, 2026], cc: 97, specs: ['LED DRL', 'ডিজিটাল · Bluetooth'], photo: p('model_hero_splendor_plus_xtec.png') },
          { id: 'hero-splendor-plus-se', label: 'SE', name: 'Splendor Plus SE', years: [2015, 2026], cc: 97, specs: ['i3S · অ্যালয়', 'সেরা মাইলেজ'], photo: p('model_hero_splendor_plus_se.png') },
        ],
      },
      {
        id: 'hero-passion',
        name: 'Passion Xpro',
        cc: 110,
        photo: p('model_hero_passion_xpro_xtec.png'),
        variants: [
          { id: 'hero-passion-xpro-xtec', label: 'Xtec', name: 'Passion Xpro Xtec', years: [2024, 2026], cc: 110, specs: ['প্রজেক্টর LED', 'ডিজিটাল'], photo: p('model_hero_passion_xpro_xtec.png') },
          { id: 'hero-passion-xpro', label: 'Standard', name: 'Passion Xpro', years: [2018, 2023], cc: 110, specs: ['i3S · Disc/Drum'], photo: p('model_hero_passion_xpro.png') },
        ],
      },
      {
        id: 'hero-glamour',
        name: 'Glamour 125',
        cc: 125,
        photo: p('model_hero_glamour.png'),
        variants: [
          { id: 'hero-glamour', label: 'Disc / Drum', name: 'Glamour', years: [2015, 2026], cc: 125, specs: ['i3S · কমিউটার'], photo: p('model_hero_glamour.png') },
        ],
      },
      {
        id: 'hero-hf-deluxe',
        name: 'HF Deluxe',
        cc: 97,
        photo: p('model_hero_hf_deluxe.png'),
        variants: [
          { id: 'hero-hf-deluxe', label: 'All-Black / SE', name: 'HF Deluxe', years: [2016, 2026], cc: 97, specs: ['ইকোনমি কমিউটার'], photo: p('model_hero_hf_deluxe.png') },
        ],
      },
      {
        id: 'hero-scooters',
        name: 'Pleasure / Maestro Edge',
        cc: 110,
        photo: p('model_hero_maestro_edge_xtec.png'),
        variants: [
          { id: 'hero-maestro-edge-xtec', label: 'Maestro Edge Xtec', name: 'Maestro Edge Xtec', years: [2022, 2026], cc: 110, specs: ['FI স্কুটার', 'Bluetooth'], photo: p('model_hero_maestro_edge_xtec.png') },
          { id: 'hero-pleasure', label: 'Pleasure', name: 'Pleasure', years: [2016, 2025], cc: 102, specs: ['হালকা স্কুটার'], photo: p('model_hero_pleasure.png') },
        ],
      },
    ],
  },
  {
    id: 'ktm',
    name: 'KTM',
    light: '#AA4700',
    dark: '#FF7900',
    logo: '/logos/logo_ktm.svg',
    models: [
      {
        id: 'ktm-duke-125',
        name: 'Duke 125',
        cc: 125,
        photo: p('model_ktm_duke_125_2023.png'),
        variants: [
          { id: 'ktm-duke-125-2023', label: 'European / New Gen LED', name: 'Duke 125 (EU LED · 2022–2026)', years: [2022, 2026], cc: 125, specs: ['LED স্প্লিট হেডলাইট', 'TFT/ডিজিটাল ক্লাস্টার', 'WP USD ফর্ক', 'Dual ABS'], photo: p('model_ktm_duke_125_2023.png') },
          { id: 'ktm-duke-125-indian-bs6', label: 'Indian BS6 · Halogen (Orange/White)', name: 'Duke 125 Indian BS6 (2021–2024)', years: [2021, 2024], cc: 125, specs: ['Halogen হেডলাইট', 'ছোট কমলা LCD মিটার', 'আন্ডারবেলি একজস্ট · BS6', 'Bolt-On ট্রেইলিস সাবফ্রেম'], photo: p('model_ktm_duke_125_indian_bs6.png') },
          { id: 'ktm-duke-125-indian', label: 'Indian BS4 / Gen-1 · Halogen (Black)', name: 'Duke 125 Indian Gen-1 (2018–2020)', years: [2018, 2020], cc: 125, specs: ['Halogen হেডলাইট', 'ছোট কমলা LCD মিটার', 'Single ABS', 'Liquid-Cooled'], photo: p('model_ktm_duke_125_indian.png') },
        ],
      },
      {
        id: 'ktm-rc-125',
        name: 'RC 125',
        cc: 125,
        photo: p('model_ktm_rc_125_2022.png'),
        variants: [
          { id: 'ktm-rc-125-2022', label: 'New Gen', name: 'RC 125', years: [2022, 2026], cc: 125, specs: ['সুপারস্পোর্ট', 'WP Apex USD', 'Dual ABS'], photo: p('model_ktm_rc_125_2022.png') },
        ],
      },
    ],
  },
  {
    id: 'cfmoto',
    name: 'CFMOTO',
    light: '#00717A',
    dark: '#69D2DB',
    logo: '/logos/logo_cfmoto.png',
    models: [
      {
        id: 'cfmoto-250nk',
        name: '250NK',
        cc: 249,
        photo: p('model_cfmoto_250nk.png'),
        variants: [
          { id: 'cfmoto-250nk', label: '250NK ABS', name: '250NK', years: [2024, 2026], cc: 249, specs: ['DOHC Liquid-Cooled', 'TFT মিটার', 'Dual ABS'], photo: p('model_cfmoto_250nk.png') },
        ],
      },
      {
        id: 'cfmoto-250sr',
        name: '250SR',
        cc: 249,
        photo: p('model_cfmoto_250sr.png'),
        variants: [
          { id: 'cfmoto-250sr', label: '250SR', name: '250SR', years: [2024, 2026], cc: 249, specs: ['স্লিপার ক্লাচ', 'USD ফর্ক', 'Dual ABS'], photo: p('model_cfmoto_250sr.png') },
        ],
      },
      {
        id: 'cfmoto-300',
        name: '300NK / 300SR',
        cc: 292,
        photo: p('model_cfmoto_300sr.png'),
        variants: [
          { id: 'cfmoto-300sr', label: '300SR (Faired)', name: '300SR', years: [2024, 2026], cc: 292, specs: ['USD ফর্ক', 'TFT কার্ভড ডিসপ্লে', 'Dual ABS'], photo: p('model_cfmoto_300sr.png') },
          { id: 'cfmoto-300nk', label: '300NK (Naked)', name: '300NK', years: [2024, 2026], cc: 292, specs: ['Liquid-Cooled', 'স্লিপার ক্লাচ', 'Dual ABS'], photo: p('model_cfmoto_300nk.png') },
        ],
      },
      {
        id: 'cfmoto-250cl-c',
        name: '250CL-C',
        cc: 249,
        photo: p('model_cfmoto_250cl_c.png'),
        variants: [
          { id: 'cfmoto-250cl-c', label: 'Cruiser Belt Drive', name: '250CL-C', years: [2025, 2026], cc: 249, specs: ['বেল্ট ড্রাইভ', 'Traction Control', 'Dual ABS'], photo: p('model_cfmoto_250cl_c.png') },
        ],
      },
      {
        id: 'cfmoto-230-dual',
        name: '230 Dual',
        cc: 223,
        photo: p('model_cfmoto_230_dual.png'),
        variants: [
          { id: 'cfmoto-230-dual', label: 'Dual-Sport', name: '230 Dual', years: [2025, 2026], cc: 223, specs: ['অফ-রোড + অন-রোড', '২১" ফ্রন্ট হুইল'], photo: p('model_cfmoto_230_dual.png') },
        ],
      },
      {
        id: 'cfmoto-150sc',
        name: '150SC',
        cc: 149,
        photo: p('model_cfmoto_150sc.png'),
        variants: [
          { id: 'cfmoto-150sc', label: 'Maxi Scooter', name: '150SC', years: [2025, 2026], cc: 149, specs: ['Keyless', 'Traction Control', 'Dual ABS'], photo: p('model_cfmoto_150sc.png') },
        ],
      },
    ],
  },
  {
    id: 'enfield',
    name: 'Royal Enfield',
    light: '#805E28',
    dark: '#DBB77C',
    logo: '/logos/logo_enfield.svg',
    models: [
      {
        id: 'enfield-classic',
        name: 'Classic 350',
        cc: 349,
        photo: p('model_royal_enfield_classic_350.png'),
        variants: [
          { id: 'royal-enfield-classic-350', label: 'Signals / Halcyon / Dark', name: 'Classic 350 (Dual ABS)', years: [2024, 2026], cc: 349, specs: ['J-Series FI', 'Dual-Channel ABS', 'টিউবলেস / স্পোক'], photo: p('model_royal_enfield_classic_350.png') },
          { id: 'royal-enfield-classic-350-single', label: 'Redditch Single ABS', name: 'Classic 350 Redditch', years: [2024, 2026], cc: 349, specs: ['J-Series FI', 'Single ABS', 'হ্যালোজেন/LED'], photo: p('model_royal_enfield_classic_350.png') },
        ],
      },
      {
        id: 'enfield-hunter',
        name: 'Hunter 350',
        cc: 349,
        photo: p('model_royal_enfield_hunter_350.png'),
        variants: [
          { id: 'royal-enfield-hunter-350', label: 'Metro Dual ABS', name: 'Hunter 350 Metro', years: [2024, 2026], cc: 349, specs: ['১৭" অ্যালয় হুইল', 'Dual ABS', 'J-Series FI'], photo: p('model_royal_enfield_hunter_350.png') },
          { id: 'royal-enfield-hunter-retro', label: 'Retro Spoke', name: 'Hunter 350 Retro', years: [2024, 2026], cc: 349, specs: ['স্পোক হুইল', 'Single ABS · Rear Drum'], photo: p('model_royal_enfield_hunter_350.png') },
        ],
      },
      {
        id: 'enfield-bullet',
        name: 'Bullet 350',
        cc: 349,
        photo: p('model_royal_enfield_bullet_350.png'),
        variants: [
          { id: 'royal-enfield-bullet-350', label: 'Standard / Black Gold', name: 'Bullet 350', years: [2024, 2026], cc: 349, specs: ['J-Series FI', 'সিঙ্গেল বেঞ্চ সিট', 'ABS'], photo: p('model_royal_enfield_bullet_350.png') },
        ],
      },
      {
        id: 'enfield-meteor',
        name: 'Meteor 350',
        cc: 349,
        photo: p('model_royal_enfield_meteor_350.png'),
        variants: [
          { id: 'royal-enfield-meteor-350', label: 'Fireball / Stellar / Supernova', name: 'Meteor 350', years: [2024, 2026], cc: 349, specs: ['ক্রুজার', 'Tripper Nav', 'Dual ABS'], photo: p('model_royal_enfield_meteor_350.png') },
        ],
      },
    ],
  },
]

export const brandOf = (id: string) => BRANDS.find((b) => b.id === id) ?? BRANDS[0]

export function modelOf(id: string): ModelVariant | undefined {
  for (const b of BRANDS) {
    for (const m of b.models) {
      const v = m.variants.find((x) => x.id === id)
      if (v) return v
      if (m.id === id) return m.variants[0]
    }
  }
  return undefined
}

export function familyOfModel(id: string): Model | undefined {
  for (const b of BRANDS) {
    for (const m of b.models) {
      if (m.id === id || m.variants.some((v) => v.id === id)) return m
    }
  }
  return undefined
}

export interface ServiceItem { key: ItemKey; bn: string; short: string; unit: string; ruleName?: string }

export const ITEMS: ServiceItem[] = [
  { key: 'engine_oil', bn: 'ইঞ্জিন অয়েল (মবিল)', short: 'মবিল / অয়েল', unit: 'বোতল', ruleName: 'oil' },
  { key: 'oil_filter', bn: 'অয়েল ফিল্টার', short: 'ফিল্টার', unit: 'টি', ruleName: 'oil' },
  { key: 'air_filter', bn: 'এয়ার ফিল্টার', short: 'এয়ার ফিল্টার', unit: 'টি', ruleName: 'air' },
  { key: 'spark_plug', bn: 'স্পার্ক প্লাগ', short: 'স্পার্ক প্লাগ', unit: 'টি', ruleName: 'spark' },
  { key: 'chain_sprocket', bn: 'চেইন ও স্প্রকেট', short: 'চেইন', unit: 'সেট', ruleName: 'chain' },
  { key: 'brake_pad', bn: 'ব্রেক প্যাড', short: 'ব্রেক প্যাড', unit: 'সেট', ruleName: 'brakes' },
  { key: 'coolant', bn: 'কুল্যান্ট', short: 'কুল্যান্ট', unit: 'বোতল' },
  { key: 'tyre', bn: 'টায়ার', short: 'টায়ার', unit: 'টি' },
  { key: 'battery', bn: 'ব্যাটারি', short: 'ব্যাটারি', unit: 'টি' },
  { key: 'general_service', bn: 'সাধারণ সার্ভিস', short: 'সাধারণ সার্ভিস', unit: 'বার' },
]
export const itemOf = (key: string) => ITEMS.find((i) => i.key === key)

export const VISCOSITY = ['10W-30', '10W-40', '20W-40', '20W-50']

export const DOC_KINDS: { kind: DocKind; bn: string; card: boolean }[] = [
  { kind: 'registration', bn: 'রেজিস্ট্রেশন স্মার্ট কার্ড', card: true },
  { kind: 'licence', bn: 'ড্রাইভিং লাইসেন্স', card: true },
  { kind: 'tax', bn: 'ট্যাক্স টোকেন', card: false },
  { kind: 'insurance', bn: 'বিমা', card: false },
  { kind: 'fitness', bn: 'ফিটনেস সার্টিফিকেট', card: false },
]
export const docName = (k: DocKind) => DOC_KINDS.find((d) => d.kind === k)?.bn ?? k

export const FUEL_LABEL = { octane: 'অকটেন', petrol: 'পেট্রোল' } as const
