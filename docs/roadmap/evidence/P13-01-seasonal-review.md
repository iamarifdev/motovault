# P13-01 Evidence — Seasonal Content, Sources & Help Boundaries

## Reviewed Artifacts
- Created specification: [P13-SEASONAL-HELP-SPEC.md](../P13-SEASONAL-HELP-SPEC.md).

## Source & Boundary Decisions
1. **Bangladesh seasonal care cycles**: Mapped 12 months cleanly into `MONSOON` (`6..10`), `WINTER_DUST` (`11..12, 1..2`), and `HEAT` (`3..5`) with 3 concise offline-first checklist actions each (chain/brakes/electrical moisture, air filter/battery/fog visibility, coolant-oil/tire pressure/brake fluid).
2. **Weather access**: Anchored to the official Bangladesh Meteorological Department URL (`https://live8.bmd.gov.bd/`), bounded to `WEATHER_CACHE_TTL_HOURS = 6`, defaulting to `OFFLINE_ONLY` with zero automatic fetches or background GPS tracking.
3. **Breakdown help boundaries**: Contacts (`BreakdownContact` + `WorkshopContact`) live offline inside encrypted Vault persistence; calling (`tel:`) and sharing require explicit user taps; emergency `999` dialing is labeled honestly without dispatch promises; missing or denied GPS leaves offline bike/issue sharing fully functional.
