# P05-01 official renewal sources, applicability and checklist research

Status: research complete on 2026-11-05 (Asia/Dhaka). No production code was
required for this task.

## Method

Pages were fetched over HTTPS from the official BRTA portals with `curl` on
2026-11-05. For each page the HTTP status, the portal's own "last updated"
metadata and a SHA-256 prefix of the fetched HTML are recorded so the reading can
be repeated. The fee schedule PDF was also downloaded; it is a scanned 2014
document and has no extractable text, so no fee is taken from it.

No BRTA API is assumed to exist. Nothing in this research authorises uploading an
owner file, identity number or document to a third party.

## Official sources checked

| ID | Source | URL | Page last updated | HTTP | SHA-256 (16) |
| --- | --- | --- | --- | --- | --- |
| S1 | BRTA home and service menu | https://brta.gov.bd/ | site footer: 4 Nov 2026 | 200 | 8780e7c898dbd1b2 |
| S2 | Driving licence renewal process | https://brta.gov.bd/pages/static-pages/6922dbe0933eb65569e0d26a | 16 May 2023 | 200 | 78e18542ab0f7860 |
| S3 | Driving licence issue and renewal | https://brta.gov.bd/pages/static-pages/6922e12b933eb65569e2aa50 | 17 May 2023 | 200 | 76380321a1e09f27 |
| S4 | Tax token renewal | https://brta.gov.bd/pages/static-pages/6922e0ab933eb65569e281ad | 10 Mar 2022 | 200 | 81c7c114b8ac9830 |
| S5 | Fitness renewal | https://brta.gov.bd/pages/static-pages/6922db91933eb65569e0af12 | 29 Jun 2026 | 200 | 23a92f413fe30c44 |
| S6 | Ownership transfer documents | https://brta.gov.bd/pages/static-pages/6922dc6b933eb65569e10468 | 30 Jun 2026 | 200 | 9b87b0d0c2c63953 |
| S7 | Digital registration certificate biometric process | https://brta.gov.bd/pages/static-pages/6922dba6933eb65569e0b8fe | 1 Nov 2023 | 200 | 830adeb3b78a659b |
| S8 | BSP QR verification (e-tax token, e-licence, e-fitness) | https://bsp.brta.gov.bd/bsp/qr/qrScanner | not published | 200 | a70d64ba40b0b8e6 |
| S9 | BSP fee calculator | https://bsp.brta.gov.bd/feeCalculator | not published | 200 | cc221dfbe858bd6f |
| S10 | BRTA "all fees" page (scanned 2014 PDF attachment) | https://brta.gov.bd/pages/static-pages/6922dd24933eb65569e13aac | 10 Mar 2022 | 200 | 7db45b473d8e83c9 / PDF 4692b27dedf20c26 |

The QR scanner page checks `www.bsp.brta.gov.bd` with a certificate that does
not match the host name; the canonical `https://bsp.brta.gov.bd/bsp/qr/qrScanner`
answers 200 and is the URL to use.

## Document applicability

| App kind | Stable ID | Renewal authority and cycle | In-app action | Appointment |
| --- | --- | --- | --- | --- |
| Registration smart card | `registration` | BRTA issues the registration certificate; it is not a periodic renewal. Ownership transfer, correction, duplicate or a reissued/digital certificate are separate BRTA services (S6, S7). | Explain that no periodic renewal exists; link S6/S7; still allow recording a reissue/new card and its paid fee. | Required for biometrics when a new card is issued |
| Driving licence | `licence` | BRTA circle office; professional licences repeat a practical test. Late renewal after 15 days adds BDT 518 per year (S2, S3). | Full checklist; record new expiry, paid fee and receipt. | Circle office for biometrics |
| Tax token | `tax` | BRTA fee-collecting bank or online payment; only the previous tax-token original is listed (S4). | Checklist; record new expiry, paid fee and receipt. | None |
| Insurance | `insurance` | The insurer renews the policy, not BRTA. BRTA pages do not list an insurance renewal checklist. | Point to the insurer/policy; do not invent BRTA steps; allow recording the new policy expiry and paid premium. | None |
| Fitness certificate | `fitness` | BRTA circle office; on registration BRTA issues 5, 2 or 1 year fitness certificates by vehicle type and make year, then renewal is mandatory (S5). | Checklist, including appointment for Dhaka/Chattogram, vehicle presentation and documents. | Online appointment required in Dhaka and Chattogram circles |

Registration, tax token, insurance and fitness are vehicle-scoped; the driving
licence is person-scoped and may serve several bikes. The renewal event must
therefore stay attached to the document record and must not move the licence into
a bike's ownership scope.

## Checklist content derived from the official pages

Step IDs are persisted; the labels are resolved from localized resources.

| Kind | Step IDs | Official basis |
| --- | --- | --- |
| licence | `licence_form`, `licence_medical`, `licence_nid`, `licence_education`, `licence_fee_receipt`, `licence_photos`, `licence_practical_test` (professional only), `licence_biometrics` | S2 lists the form, registered-doctor medical certificate, attested NID copy, education certificate, fee receipt and two photo sizes; professional holders repeat a practical test and give biometrics at the circle office. S3 adds the existing licence scan for renewal/class changes. |
| tax | `tax_previous_token`, `tax_fee_payment`, `tax_fee_receipt` | S4: previous tax token original; renewal after paying the prescribed fee at a BRTA bank or online; bKash payments are couriered. |
| fitness | `fitness_form`, `fitness_appointment`, `fitness_previous_certificate`, `fitness_tax_token_copy`, `fitness_tin`, `fitness_vehicle_present`, `fitness_fee_receipt` | S5 lists the completed form, fee receipt, original fitness certificate, updated tax-token attested copy, TIN papers, advance-income-tax proof; the vehicle must be presented; Dhaka and Chattogram circles need an online appointment. |
| insurance | `insurance_insurer`, `insurance_policy_copy`, `insurance_new_expiry` | No BRTA renewal checklist exists. The guide says the policy is renewed with the insurer and only records the new expiry/premium. |
| registration | `registration_no_periodic_renewal`, `registration_transfer_or_correction`, `registration_official_sources` | S6 and S7: transfer, correction, duplicate and digital certificate are separate services; the certificate itself does not expire periodically. |

The driving-licence page (S3) also lists the existing-licence scan for renewal,
class change, class addition and licence-type change. That is a conditional step
shown only when the owner already stores a licence, not a separate checklist.

## Fees: keep estimates separate from confirmed payments

The official pages disagree with each other and are dated 2023:

- S2: ordinary renewal BDT 4152 inside 15 days, professional BDT 2427.
- S3: ordinary renewal BDT 4212, professional BDT 2487.
- Both add BDT 518 per year as a late fine after 15 days.
- The "all fees" attachment (S10) is a scanned 2014 PDF and cannot be read
  reliably.

Decision: the app does not hard-code a renewal fee, a total, or a computed
estimate that looks authoritative. It links to the official fee calculator (S9)
and the source pages, shows the fee rule that is documented (late-fine wording)
only as a "check before paying" note, and persists the amount the owner actually
paid for that renewal. A fee is therefore always a confirmed, owner-entered
payment on a dated renewal event, never a guessed amount attached to the
document.

## Official verification

S8 verifies certificates generated by BSP that carry a QR code: e-tax token,
e-driving licence and e-fitness. The owner can use the camera scanner or upload a
PDF/image. Uploading is an action on BRTA's own site; it is never performed by
MotoVault. The app opens the page only after an explicit owner tap and states
that nothing is uploaded until the owner chooses a file there. A missing or
changed URL must fail safely: the checklist and the recorded renewal stay usable
offline, and the app shows the portal name and the last-checked date instead of a
broken link.

## Safe fallback when a service URL is unavailable

- The source URL is opened through the platform browser boundary; failure is
  caught and reported as "BRTA site could not open" with the portal name
  (brta.gov.bd) and the source's last-checked date.
- The checklist, fee entry and renewal save work offline; a link is an aid, not a
  prerequisite.
- The app never retries a link in the background, never fetches document content
  and never substitutes a third-party URL.
- If BRTA changes a path, the fallback text names the BRTA service so the owner
  can find it, and the dated source stays visible.

## Risks

- Official pages and fees change; the page dates above are shown to the owner.
- S2 and S3 disagree on the renewal fee; S2 is 2023 and S3 is 2023 as well. The
  disagreement is itself the reason not to embed a number.
- Transfer rules (S6) are long and belong with used-bike handover (P11); P05
  only needs the registration applicability statement and the source link.
- There is no BRTA API and no machine-readable fee schedule. Do not invent one.
