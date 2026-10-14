# MotoVault app UI · v2.0

Status: high-detail native app design proposal, ready to map into Compose after visual/font acceptance. It is not a claim that these new data features are implemented in the native app. Created 2026-10-14.

## User direction

Keep the current app's better bike-brand logos and model/photo picker. Preserve the improved overall flow from v1. Design an actual Bangla-first motorcycle ownership app for Bangladesh, including common services, replaceable parts, oil categories, prices, optional automatic fuel/workshop location and easier entry. Offer real Bangla font choices.

## Preserve, improve, add

| Decision | Native evidence / reference |
|---|---|
| Preserve nine manufacturer logos and the complete 78-model/photo catalogue | Onboarding.kt:80-98; BikeCatalogue.kt:8 onward. Converted existing vector paths; copied original photos. |
| Preserve search, model confirmation, manual model entry and attribution | Onboarding.kt:165-197. v2 S02/S26. |
| Preserve shared neutral/brand palette and System/Light/Dark in native | Appearance.kt:18-48,66-87. v2 demonstrates light/dark; native System remains a requirement. |
| Preserve original documents, explicit owner confirmation, focused inspection | DocumentFlow.kt; v1 document flow retained, with cost fields outside inspection. |
| Improve Bangla typography, task priority, errors and correction | v1 audit and v2 screen contracts. |
| Add one service visit with many items, separate labour, prices and workshop | Current RideLog is flat and has one componentId (VaultState.kt:35-39). This requires data behavior, not just restyling. |
| Add fuel type/rate/calculation and structured record location | Existing fuel record has litres/cost; workshop is a string. No structured GPS object in the examined RideLog. |

## Visual system

Bangla-first, calm ownership tool; no decorative flags or ornamental cultural styling. Real motorcycle photos and original manufacturer logos provide identity. Brand cards use matching optical bounds, selected outline/check and accessible label/count. Model cards use a two-column photo grid, search and exact existing names.

Font choice: Anek Bangla, selected by the user for the native app. S34 retains the four-way comparison for visual reference. The common Compose app bundles the Anek Bangla variable TTF and its SIL Open Font License; verify its weight mapping and Bangla glyphs at real Android/iOS sizes.

Prototype roles: screen title 29px/600, section 21px/600, component title 18px, body/action 17px, functional secondary 15px, badge 13px, monetary headline 32px. Comfortable line height around 1.6. Bangla is not condensed, uppercase or widely tracked. Native implementation must test equivalent sp/line-height metrics and bundle the licensed selected font.

Spacing/radii continue v1's 4/8/12/16/24/32 rhythm and 12/16/24 radii. Full-width action minimum 52px, secondary selection minimum 44px. New symbols are consistent local stroke SVGs; native should reuse the existing Glyph/component system or one agreed icon family. Do not ship Unicode gear/wave placeholders as production icons.

Core light/dark neutral, semantic and all nine brand accents inherit the native palette (Appearance.kt:32-48), including the exact KTM dark orange #FF7900. Brand/model selection previews the chosen brand; committed Garage uses the actual bike brand. Brand accents do not imply errors or validity. Cost labels use ৳; display follows language, input accepts both digit scripts. Typography choices are review controls outside the eventual app's settings unless explicitly requested as a user feature.

## Motorcycle ownership and prices

The price contract applies to every monetary context, not arbitrary navigation or nonfinancial states:

- Garage: owner-entered bike purchase price and recorded fuel/service/parts expenses.
- Fuel: petrol/octane, price per litre, litres and total paid.
- Services/parts: quantity, unit price, line total, separate labour, other expense, optional discount and total.
- Item picker: user's last paid price when known, otherwise 'দাম দিন'. Never pretend a missing price is free.
- Oil: type/grade/product and explicit purchase basis, paid quantity and optionally used amount.
- History/detail: actual saved totals with item breakdown and place.
- Documents: optional entered document/renewal expense in private review/detail/list. No financial detail in inspection.

All reference prices are sample records. They are not current Bangladesh market prices, official fuel tariffs, official fees or product recommendations. A native catalogue needs separate verified pricing if the user later requests market quotes. Missing values remain unknown; zero is explicitly no charge.

Document expenses and purchase cost are separate ownership categories from operating totals. v2 operating overview sums fuel/service records only. Native full-ownership analytics must state categories and avoid double-counting mutable document expense fields as repeated payments.

## Common service / part taxonomy

The selectable starter list has 30 entries, grouped by service, parts, fluids and custom:

- Engine oil, oil filter, air filter, spark plug.
- Front/rear brake pads, drum brake shoes, brake check/adjustment, brake fluid.
- Chain cleaning/lubrication, chain adjustment, chain/sprocket set.
- Front/rear tyres, tube, puncture repair, wheel bearing.
- Battery, clutch plates, clutch cable, throttle cable.
- Coolant, fork oil, fork seals.
- Bike wash, valve clearance check, injector/throttle-body cleaning, carburettor cleaning, general service and custom work/part.

This is an entry taxonomy, not a claim every bike needs every item. A future model-aware filter must distinguish carburettor/EFI, air/liquid cooling, disc/drum brakes, tube/tubeless tyres, scooter gear oil and electric powertrains. Do not recommend incompatible work from a generic list. Users can enter custom work and notes.

Actions remain distinct: inspection, cleaning, adjustment and replacement. Recording an inspection or buying oil must not reset a replacement/service reminder. v2 shows the intended action fields; the native schedule-action mapping needs explicit rules and tests (see APPLY-MAP.md).

## Engine oil contract

Separate facts: mineral / semi-synthetic / fully synthetic; viscosity grade; brand/product; optional JASO/API; purchase basis; quantity bought; unit price; bottle volume; amount used. These are not interchangeable.

Example: buy one 1L bottle for ৳950, add 0.9L to the bike. Cost remains ৳950; used amount is 0.9L. The UI must not silently reduce spending to ৳855. Loose oil can be billed per litre. Grades and standards come from bottle/manual; do not assign suitability or a longer service interval because an oil is synthetic.

If the rider only remembers the total oil expense, support that explicitly without inventing litres or bottle count. Smart text “অয়েল ৯৫০” means total expense ৳950 until the user supplies purchase details. Type/grade can be explicitly marked unknown instead of guessed.

Source support: [Castrol oil categories](https://www.castrol.com/en_gb/united-kingdom/home/products/two-wheelers/motorcycles.html) distinguishes mineral/part-synthetic/full-synthetic; [Castrol 4-stroke guidance](https://www.castrol.com/en_gb/united-kingdom/home/products/two-wheelers/motorcycles/4-cycle-engine-oil.html) ties selection to manufacturer specifications. These support the data separation, not the sample product/price or a universal interval.

## Service bill flow

S06 → quick template or S27 picker → S28 oil / S29 item → S06 → S32 place if wanted → S30 review → S31 record.

One visit can include oil, filter and chain care. Items have independent unit prices. Labour means separate labour not already included in a priced work item; explain this to prevent double counting. A complete bill requires all item prices and an explicit labour value (0 if none). Discount cannot exceed subtotal. Preserve the draft between internal screens and on failed saves.

Quick templates add only structure. Repeat-last copies the previous items and prices only after an explicit action and tells the user to check today's values. No silent recurring transaction or false current-price guarantee.

Service correction operates on the entire saved visit and all affected schedules atomically. One multi-item bill must not become several duplicate costs in history. Historical corrections must not replace a newer baseline simply because they were edited last.

## Fuel flow

S05 supports petrol/octane and any-two-of-three entry: quantity, price per litre, total. Explicitly select the calculated field, keep every value editable and check agreement before save. Previous user rate is a suggestion, not an official current rate. Full-tank/partial-fill status stays separate from expense arithmetic.

The prototype calculates using browser numbers. Native monetary storage should use integer poisha or an agreed decimal representation, with deterministic rounding. Display precision must not imply fuel measuring accuracy. Preserve partial fills and the existing economy calculation contract.

## GPS and maps

Setting defaults off. When the user enables automatic location and OS foreground permission is available, entering fuel/service can request a one-shot fix and attach it to the draft. Never start background tracking. If denied, disabled, timed out or offline, save still works and manual place entry remains available.

Store optional position, accuracy, capture time, source and user-confirmed station/workshop name. GPS supplies position; it does not verify which nearby business was used. Reverse geocoding may suggest an area, not silently choose a workshop. Cached names and prior places can be explicit suggestions.

Show map/approximate accuracy when useful, allow change/remove, and let the user continue without location. Retain coordinates offline even if place lookup is unavailable. Sharing summaries excludes exact coordinates by default; user-visible inclusion controls are required before optional sharing.

The reference uses fixed public sample coordinates near Tejgaon and an OpenStreetMap embed. It never requests the device's real GPS. A native map provider, attribution, offline behavior, permission explanation and geocoder policy must be chosen before integration.

## Smart/easy input

Three practical mechanisms now represented:

1. Quick service templates and repeat-last items/prices.
2. Fuel arithmetic and previous-rate suggestion.
3. S33 short text such as “অয়েল ৯৫০, ফিল্টার ২৫০, মজুরি ৩০০, ১২৬০০ কিমি” → editable itemized draft.

Parser supports limited Bangla/English aliases, valid grouped amounts and unmatched text. Ambiguous or unrecognized segments must be visible, not silently discarded or guessed. Item details (oil type/grade/bottle basis) still require confirmation before native save. Applying the parsed result replaces the current draft only with clear consequence/confirmation. Never auto-save an inferred bill.

Receipt attachment is available in the reference; automatic receipt OCR, barcode product matching and speech entry are future integration options. Do not show them as working or upload receipts to an external AI service without explicit user choice and a defined data policy.

## Screen contracts

S01-S25 retain the earlier flow IDs. Revised: S02 logo brand selection; S03 simple native setup; S04 richer ownership dashboard; S05 calculated fuel entry; S06 multi-item service; S09 itemized history; S12 document fee; S13 private expense detail; S16 optional auto-location and design-only font choice.

| ID | Route | Contract |
|---|---|---|
| S26 | #models | Full native catalogue, photos, search, selection, attribution, manual fallback. |
| S27 | #servicePicker | Select common work, fluids, parts and custom entries; show previous paid price or missing-price state. |
| S28 | #oil | Type, grade, product, standards, purchase basis, purchased/used amount and actual price. |
| S29 | #item | Action, brand/model, quantity, unit price and line total. |
| S30 | #billReview | Review all items, total, labour and workshop before one save. |
| S31 | #record | Itemized ownership event, place and direct correction. |
| S32 | #location | Auto-location preference, optional map/name, manual/no-location paths. |
| S33 | #smart | Text → disclosed matches/unmatched → editable draft. |
| S34 | #fonts | Four real font comparisons and whole-UI preview; final user choice pending. |
| S35 | #purchase | Owner-entered bike purchase amount; unknown supported. |

## Source and verification boundaries

index.html + styles.css/ui-v2.css + app.js/ui-v2.js are the editable visual/interaction reference, not native source to paste wholesale. catalogue.js is generated from current native resources by build-catalogue.py. See QA.md for exact executed checks. Real camera, GPS, OCR, notifications, PDF verification, cloud and native data migration are not implemented here.

Font sources: [Anek Bangla](https://fonts.google.com/specimen/Anek+Bangla), [Hind Siliguri](https://fonts.google.com/specimen/Hind+Siliguri), [Noto Sans Bengali](https://fonts.google.com/specimen/Noto+Sans+Bengali), [Baloo Da 2](https://fonts.google.com/specimen/Baloo+Da+2). The browser reference loads fonts over the network. The native app bundles Anek Bangla and its license; verify glyphs at real device sizes.
