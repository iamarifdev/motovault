# P05-01 review: sources, applicability and Bangla wording

Status: reviewed 2026-11-05 (Asia/Dhaka). Companion to
[P05-01-research.md](P05-01-research.md).

## What was reviewed

1. Ten official BRTA/BSP pages were opened on 2026-11-05 and their service
   statements, required papers, appointment rules and page dates were compared.
   The fetched copies were hashed in the research report so the same pages can be
   re-read.
2. The applicability of each of the five app document kinds was checked against
   those pages: registration, driving licence, tax token, insurance and fitness.
3. The proposed Bangla and English checklist wording was read together against
   [the Bangla copy style](../../engineering/localization-style.md).
4. The fee and verification rules were checked for the "no invented API, no
   silent upload, no hard-coded fee" constraints.

## Acceptance mapping

| Acceptance criterion | Result | Where recorded |
| --- | --- | --- |
| Record official sources and checked dates | Ten sources with URL, page last-updated date, HTTP status and hash | Research tables S1-S10 |
| Record applicable document categories | Per-kind authority, cycle, appointment and in-app action | Research "Document applicability" and "Checklist content" |
| Safe fallback when a service URL is unavailable | Offline-first checklist with named portal, last-checked date and a caught launch failure; no background retry or third-party substitute | Research "Safe fallback when a service URL is unavailable" |
| Fee estimates separate from confirmed payments | No embedded fee; link to the official calculator; only the owner-entered paid amount is stored on the dated renewal event | Research "Fees: keep estimates separate from confirmed payments" |
| Open the QR-verification and renewal/transfer sources | S1-S10 fetched, including the BSP QR page and the ownership-transfer page | Research tables |
| Review plain Bangla wording | Copy table below, checked against the style guide | This document |

## Bangla copy review

The labels below are the exact copy that P05-03 will add to
`composeResources/values/strings.xml` and `values-bn/strings.xml`. They are
short, action-first and use the loanwords a rider already sees on BRTA's site
(নবায়ন, ফি, রসিদ, ফিটনেস, ট্যাক্স টোকেন, সার্কেল অফিস). "মেয়াদোত্তীর্ণ",
"দস্তাবেজ", "নথি" and "প্রতিলিপি" are not used. Full stops use `।`.

| Purpose | Bangla | English |
| --- | --- | --- |
| Sheet title | নবায়ন | Renewal |
| Subtitle | সরকারি তথ্য দেখে ধাপগুলো সাজানো। | Steps arranged from the official source. |
| Progress | %1$s/%2$s ধাপ হয়েছে | %1$s of %2$s steps done |
| Source line | সোর্স: BRTA · সর্বশেষ দেখা %1$s | Source: BRTA · last checked %1$s |
| Offline note | লিংক না খুললেও ধাপ ও নবায়ন কাজ করবে। | The steps and renewal still work if the link does not open. |
| Link failure | BRTA সাইট খোলা যায়নি। আবার চেষ্টা করুন বা brta.gov.bd-এ দেখুন। | The BRTA site could not open. Try again or check brta.gov.bd. |
| Fee help | ফি বদলাতে পারে। BRTA-র ক্যালকুলেটরে দেখে নিন। যা দিয়েছেন, সেটাই লিখুন। | Fees change. Check BRTA's calculator, then enter what you actually paid. |
| Fee calculator link | BRTA ফি ক্যালকুলেটর | BRTA fee calculator |
| Paid amount | যত টাকা দিয়েছেন | Amount you paid |
| Paid amount hint | টাকা না দিলে খালি রাখুন | Leave empty if you did not pay |
| Receipt | পেমেন্ট রসিদ | Payment receipt |
| Add receipt | রসিদের ছবি যোগ করুন | Add receipt photo |
| Replace receipt | রসিদ বদলান | Replace receipt |
| Remove receipt | রসিদ সরান | Remove receipt |
| Receipt read failure | রসিদ পড়া যায়নি। আবার চেষ্টা করুন। | Could not read the receipt. Try again. |
| New expiry | নতুন মেয়াদ শেষের তারিখ | New expiry date |
| Expiry error | নতুন মেয়াদ আগের মেয়াদের পরে হতে হবে। | The new expiry must be after the current expiry. |
| Save | নবায়ন সেভ করুন | Save renewal |
| Saved toast | %1$s নবায়ন সেভ হয়েছে | %1$s renewal saved |
| Save failure | নবায়ন সেভ হয়নি। আবার চেষ্টা করুন। | The renewal was not saved. Try again. |
| History | নবায়নের ইতিহাস | Renewal history |
| Empty history | এখনো কোনো নবায়ন নেই। | No renewals recorded yet. |
| Previous expiry | আগের মেয়াদ: %1$s | Previous expiry: %1$s |
| View receipt | রসিদ দেখুন | View receipt |
| Back | কাগজে ফিরে যান | Back to document |
| Verify action | BRTA সাইটে কপি যাচাই করুন | Verify the copy on BRTA |
| Verify note | আপনি নিজে ফাইল দিলে তবেই BRTA সাইটে আপলোড হবে। | A file is uploaded only if you choose one on BRTA's site. |
| Source link | BRTA-র নির্দেশনা দেখুন | See BRTA's instructions |

### Checklist steps

| Kind | Step ID | Bangla title / detail | English title / detail |
| --- | --- | --- | --- |
| licence | `licence_form` | আবেদনের ফরম — নির্ধারিত ফরম পূরণ ও সই করুন। | Application form — Fill in and sign the prescribed form. |
| licence | `licence_medical` | মেডিকেল সার্টিফিকেট — রেজিস্টার্ড ডাক্তারের সার্টিফিকেট নিন। | Medical certificate — Get a registered doctor's certificate. |
| licence | `licence_nid` | NID কপি — জাতীয় পরিচয়পত্রের সত্যায়িত কপি নিন। | NID copy — Take an attested copy of your national ID. |
| licence | `licence_education` | শিক্ষার সনদ — শিক্ষাগত যোগ্যতার সনদ নিন। | Education certificate — Take your education certificate. |
| licence | `licence_fee_receipt` | ফি জমার রসিদ — নির্ধারিত ফি জমিয়ে রসিদ নিন। | Fee receipt — Pay the prescribed fee and keep the receipt. |
| licence | `licence_photos` | ছবি — পাসপোর্ট সাইজ ১ কপি ও স্ট্যাম্প সাইজ ১ কপি ছবি নিন। | Photos — Keep one passport-size and one stamp-size photo. |
| licence | `licence_practical_test` | ব্যবহারিক পরীক্ষা — পেশাদার লাইসেন্সে আবার পরীক্ষা দিতে হয়। | Practical test — Professional licences repeat the practical test. |
| licence | `licence_biometrics` | বায়োমেট্রিক — সার্কেল অফিসে ছবি, সই ও আঙুলের ছাপ দিতে হবে। | Biometrics — Give photo, signature and fingerprints at the circle office. |
| tax | `tax_previous_token` | আগের ট্যাক্স টোকেন — আগের টোকেনের মূল কপি নিন। | Previous tax token — Keep the original of the previous token. |
| tax | `tax_fee_payment` | ফি জমা — BRTA-র ব্যাংক বা অনলাইনে ফি দিন। | Pay the fee — Pay at a BRTA bank or online. |
| tax | `tax_fee_receipt` | জমার রসিদ — জমার রসিদ সংরক্ষণ করুন। | Payment receipt — Keep the payment receipt. |
| fitness | `fitness_form` | আবেদনের ফরম — পূরণ করা আবেদনপত্র প্রস্তুত রাখুন। | Application form — Keep the completed application ready. |
| fitness | `fitness_appointment` | অ্যাপয়েন্টমেন্ট — ঢাকা ও চট্টগ্রাম সার্কেলে আগে অনলাইনে সিরিয়াল নিন। | Appointment — Book online first in Dhaka and Chattogram circles. |
| fitness | `fitness_previous_certificate` | আগের ফিটনেস সনদ — আগের সনদের মূল কপি নিন। | Previous fitness certificate — Keep the original certificate. |
| fitness | `fitness_tax_token_copy` | ট্যাক্স টোকেন কপি — হালনাগাদ ট্যাক্স টোকেনের সত্যায়িত কপি নিন। | Tax token copy — Take an attested copy of the updated tax token. |
| fitness | `fitness_tin` | TIN ও আয়কর — TIN কাগজ ও অগ্রিম আয়করের প্রমাণ নিন। | TIN and income tax — Keep TIN papers and advance-tax proof. |
| fitness | `fitness_vehicle_present` | গাড়ি হাজির — সার্কেল অফিসে মোটরযান হাজির করুন। | Present the vehicle — Bring the vehicle to the circle office. |
| fitness | `fitness_fee_receipt` | ফি জমার রসিদ — নির্ধারিত ফি জমিয়ে রসিদ নিন। | Fee receipt — Pay the prescribed fee and keep the receipt. |
| insurance | `insurance_insurer` | বিমা কোম্পানি — নবায়ন BRTA-তে হয় না, বিমা কোম্পানিতে করুন। | Insurance company — Renew with the insurer, not BRTA. |
| insurance | `insurance_policy_copy` | পলিসি কপি — নতুন পলিসি বা সনদের কপি নিন। | Policy copy — Keep the new policy or certificate. |
| insurance | `insurance_new_expiry` | নতুন মেয়াদ — নতুন মেয়াদ শেষের তারিখ লিখুন। | New expiry — Enter the new expiry date. |
| registration | `registration_no_periodic_renewal` | মেয়াদ নেই — রেজিস্ট্রেশন সনদের মেয়াদ শেষ হয় না। | No expiry — The registration certificate does not expire. |
| registration | `registration_transfer_or_correction` | মালিকানা বদল বা সংশোধন — বদল, সংশোধন বা ডুপ্লিকেট লাগলে সার্কেল অফিসে যান। | Transfer or correction — Visit a circle office for a transfer, correction or duplicate. |
| registration | `registration_official_sources` | BRTA-র নিয়ম — প্রয়োজনীয় কাগজপত্র BRTA-র সাইটে দেখুন। | BRTA rules — Check the required papers on BRTA's site. |

Every step ID is prefixed with its document kind. The IDs are the only persisted
checklist identity; labels and details are resolved from resources, and the
licence and fitness "form" steps therefore keep distinct wording.

## Findings and decisions

- **Registration is not a periodic renewal.** The app must not imply that a
  registration smart card expires and needs yearly renewal. The guide explains
  the real services and still lets the owner record a reissued card.
- **Insurance is not a BRTA service.** The guide sends the owner to the insurer
  and only records the new expiry and premium; no BRTA step is invented.
- **Fees are not embedded.** Two official pages disagree (4152 vs 4212; 2427 vs
  2487), and the fee PDF is an unreadable 2014 scan. The renewal event stores the
  amount actually paid.
- **Late-renewal wording is a warning, not a computed fee.** The 15-day / BDT 518
  per year rule is shown as a "check before paying" note with the source date.
  The app does not calculate a total.
- **QR verification is owner-initiated.** The BSP page verifies e-tax token,
  e-licence and e-fitness. MotoVault only opens the page; it does not upload,
  prefill or fetch anything.
- **Fallback keeps the task usable.** A dead link never blocks the checklist,
  the fee entry, the receipt or the renewal save.

## Limits

- This is an implementer review of public pages, not a BRTA confirmation and not
  an independent native-speaker sign-off. P08-05 keeps the Android/iOS runtime
  language-switch evidence open.
- The pages can change without notice; the source dates are shown in the UI so an
  owner can judge freshness.
- No screenshot of the BSP QR page is attached; the review is based on the page
  text fetched on 2026-11-05 and stored in the research hashes.
