# P04-01 initial Bangladesh model and manual research

Status: researched; provisional pilot set. This records the source basis for the
P04 maintenance profiles. No physical device or owner data was used.

## Decision

Start with two Yamaha models that already exist in the Bangladesh catalogue and
the current product fixtures:

| Model | Year | Model code | Manual | Why included |
| --- | --- | --- | --- | --- |
| Yamaha R15 V4 | 2023 | YZF155-A | BGN-F819V-E0 | The app seed, design review and prior simulator fixtures use this bike. |
| Yamaha MT-15 V2 | 2027 | MTN155-A | BXU-F819V-EB | Official Bangladesh catalogue model; same 155 cc platform, useful second profile. |

The set is deliberately small. It is **provisional pilot-owner evidence**: the
workspace contains no structured interview transcripts, so the R15 V4 choice is
based on the owner-facing seed and design review, and MT-15 V2 is the closest
same-platform catalogue model. P08-03 must confirm or replace the set with
representative-owner evidence before release. This limitation is not hidden in
the implementation.

Official Bangladesh catalogue:
[R15 V4](https://www.yamahabd.com/products/yamaha-r15-v4/) and
[MT-15 V2](https://www.yamahabd.com/products/yamaha-mt-15-v2/).

## Source manuals

Both manuals are English-language India-market Yamaha owner's manuals for the
same model codes sold in Bangladesh. They are not Bangladesh-specific prints.
The Bangladesh distributor catalogue confirms model availability; the owner must
confirm the exact year, variant and market code against the local paperwork.

| Manual | Source URL | Manual number | Pages used |
| --- | --- | --- | --- |
| R15 V4 2023 | <https://library.ymcapps.net/library/om/contents/pdf/10/BGN-F819V-E0_02.pdf> | BGN-F819V-E0 | 8-2 to 8-6 |
| MT-15 V2 2027 | <https://library.ymcapps.net/library/om/contents/pdf/10/BXU-F819V-EB_02.pdf> | BXU-F819V-EB | 8-2 to 8-5 |

The Yamaha Owner's Manual Library resolves these through its India product
(`baseCode=6C97`). The PDFs were read locally to verify the charts; the PDFs are
not committed because they are manufacturer documents.

## R15 V4 schedule basis

Source: BGN-F819V-E0, pages 8-2 to 8-6. The manual says the chart is a general
guide under normal conditions and intervals may need to be shortened for weather,
terrain or use.

| App action | Manual item | Interval | Page | Notes |
| --- | --- | --- | --- | --- |
| Replace | Engine oil | 5,000 km recurring; first at 1,000 km | 8-5, chart columns | Chart shows change at 1,000 / 5,000 / 9,000 / 13,000 / 17,000 / 21,000 km. |
| Replace | Oil filter element | 5,000 km recurring; first at 1,000 km | 8-5 | Same chart row sequence as engine oil. |
| Replace | Air filter element | 17,000 km | 8-3 | Paper element must be replaced, not cleaned with compressed air; replace sooner in wet/dusty use. |
| Replace | Spark plug | 17,000 km | 8-2 | Check/clean at earlier services; replace at 17,000 km. |
| Lubricate | Drive chain | 500 km and after washing/rain/wet use | 8-4 | O-ring chain lubricant. |
| Adjust | Drive chain slack/alignment | 500 km and after washing/rain/wet use | 8-4 | Same manual row as chain inspection. |
| Replace | Brake fluid | 2 years | 8-3 | Also change after brake hydraulic disassembly. |
| Replace | Coolant | 2 years | 8-5 | Check level at every listed service. |
| Replace | Brake hose | 4 years | 8-3 | Replace earlier if cracked or damaged. |
| Replace | Front fork oil | 13,000 km | 8-5 | Dealer item in the manual. |
| Inspect | Valve clearance | 1,000 km, then every 5,000 km | 8-2 | Dealer adjustment if needed. |
| Inspect | Tyres, brakes and chassis | Every listed service (5,000 km) | 8-3 to 8-4 | Condition-based replacement is not given a fixed interval. |

Running-in: the 1,000 km engine-oil and filter change is a one-time first
service. It is modelled separately from the recurring 5,000 km schedule.

## MT-15 V2 schedule basis

Source: BXU-F819V-EB, pages 8-2 to 8-5. The same recurring structure applies.

| App action | Manual item | Interval | Page |
| --- | --- | --- | --- |
| Replace | Engine oil | 5,000 km recurring; first at 1,000 km | 8-5 |
| Replace | Oil filter element | 5,000 km recurring; first at 1,000 km | 8-5 |
| Replace | Air filter element | 17,000 km | 8-3 |
| Replace | Spark plug | 17,000 km | 8-2 |
| Lubricate | Drive chain | 500 km and after wet/dusty use | 8-4 |
| Adjust | Drive chain slack/alignment | 500 km and after wet/dusty use | 8-4 |
| Replace | Brake fluid | 2 years | 8-3 |
| Replace | Coolant | 2 years | 8-5 |
| Replace | Brake hose | 4 years | 8-3 |
| Replace | Front fork oil | 13,000 km | 8-5 |
| Inspect | Valve clearance | 1,000 km, then every 5,000 km | 8-2 |
| Inspect | Tyres, brakes and chassis | Every listed service (5,000 km) | 8-3 to 8-4 |

## Mapping rules

- Distance-only manual intervals use `intervalDays = 0`; no time interval is
  invented.
- Calendar-only intervals use `intervalKm = 0`.
- The app shows the manual number, page and applicability in the schedule
  details sheet.
- Local-condition shortening is an explicit owner edit. The app does not apply
  an undocumented multiplier or copy a European/Indian interval by model name.
- A model without a verified profile keeps the owner-defined/unknown fallback.
  The catalogue never silently becomes a manual-backed schedule.

## Review

- The two manuals are official manufacturer sources and the page references
  match the downloaded PDFs.
- The Bangladesh catalogue confirms both models but does not provide a local
  owner's manual. That applicability gap is recorded in every profile.
- Pilot-owner confirmation is still required at P08-03. The provisional status
  is recorded rather than presenting the set as representative research.
