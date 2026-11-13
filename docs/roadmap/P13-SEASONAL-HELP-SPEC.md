# P13 Seasonal Care & Breakdown Help Specification

## 1. Bangladesh Riding Seasons & Offline Checklist Scope
All seasonal guidance is stored locally offline in shared domain logic and EN/BN resource strings, adapted for Bangladesh riding conditions without displacing the primary Next Action hero card or requiring connectivity:

| Season | Enum | Calendar Months | Checklist Item IDs & Guidance |
| --- | --- | --- | --- |
| Monsoon | `MONSOON` | June–October (`6..10`) | `monsoon_chain` (clean and lubricate wet chain after rain rides), `monsoon_brakes` (dry/check brake pads & discs and tire tread drainage), `monsoon_electrical` (inspect exposed connectors & waterproof document pouch) |
| Winter & Dust | `WINTER_DUST` | November–February (`11..12, 1..2`) | `winter_air_filter` (clean air filter dust buildup), `winter_battery` (check cold-start battery charge and terminals after 30–60s warm-up), `winter_visibility` (wipe headlight/visor dust before fog or smog rides) |
| Summer Heat | `HEAT` | March–May (`3..5`) | `heat_coolant_oil` (check coolant level and engine oil viscosity before peak heat), `heat_tire_pressure` (check cold tire pressure to prevent heat over-inflation), `heat_brake_fluid` (inspect brake fluid and shaded parking to limit fuel vapor loss) |

Dismissal is scoped per `${bikeId ?: "default"}:${season.name}:$year` and can be re-enabled anytime from the same card area on Home.

## 2. Official Weather Source & Bounded Access
- **Official source**: Bangladesh Meteorological Department (BMD) at `https://live8.bmd.gov.bd/`.
- **Default region**: `"BD-DAC"` (Dhaka, Bangladesh).
- **Cache TTL**: `WEATHER_CACHE_TTL_HOURS = 6`.
- **Cache states**:
  - `OFFLINE_ONLY` (default when `updatedAtEpochMillis <= 0L`): offline seasonal checklists remain active; no automatic network calls or background weather polling occur.
  - `FRESH`: cached timestamp is within `<= 6` hours.
  - `EXPIRED`: cached timestamp is older than `6` hours, displaying clear stale guidance and the explicit BMD official link action.
- **Location boundary**: No background location polling is requested for weather; region display uses an explicit non-PII region code (`"BD-DAC"`).

## 3. Breakdown Contacts & Deliberate Location Sharing Boundaries
- **Offline availability**: Owner-saved breakdown contacts (`BreakdownContact`) and existing saved workshop contacts (`WorkshopContact`) persist locally in `VaultState` and backup preferences so they remain readable with zero network access.
- **Help boundary disclosure**: MotoVault is a personal log and communication helper, not a 24/7 roadside dispatch or medical/police responder. The breakdown sheet states this clearly and surfaces `999` (`NATIONAL_EMERGENCY_NUMBER`) explicitly as an external public emergency dialer action.
- **Deliberate location capture & share**:
  - One-shot GPS capture runs only on explicit user button press via `LocationCaptureButton`.
  - When captured, the sheet shows coordinates, accuracy (`±m`), and capture time inside the editable share message.
  - Permission denial, timeout, or missing GPS never blocks calling saved contacts or sharing bike/odometer/landmark notes offline.
