# P07 opt-in Bangla voice provider plan

Status: proposal only, 12 November 2026. The owner subsequently confirmed that this plan should remain in review; do not start its online implementation or activate a service. The owner requested this plan after the isolated iOS 27 runtime listed no Bangla Speech locales. No audio was uploaded, cloud project created, paid service enabled, credential added or provider accuracy measured.

The actual native capability report is `evidence/P07-ios-voice-capabilities.json`. It records bn-BD absent, local/provider unavailable and speech authorization not requested. It is simulator evidence for this runtime, not a claim about every physical iPhone.

## Proposed provider

Evaluate Google Cloud Speech-to-Text V2 first, with `bn-BD`, `chirp_2` and `asia-southeast1`. Google's current language table explicitly lists that combination. This is a proposed pilot choice, not a measured accuracy or latency result. Keep the provider behind an application port so an evaluated alternative can replace it. [Google supported languages](https://docs.cloud.google.com/speech-to-text/docs/speech-to-text-supported-languages), [Chirp 2 synchronous recognition](https://docs.cloud.google.com/speech-to-text/docs/models/chirp-2).

| Candidate | What the fetched official documentation establishes | Decision for this plan |
| --- | --- | --- |
| Google Cloud STT V2 | Explicit bn-BD + chirp_2 + asia-southeast1 listing. | First controlled pilot. |
| Azure Speech | The speech-to-text table lists bn-IN. bn-BD entries elsewhere include text-to-speech. These are different capabilities. | Requires separate bn-BD STT verification before selection. [Language support](https://learn.microsoft.com/en-gb/azure/ai-services/speech-service/language-support?tabs=stt). |
| OpenAI transcription | The fetched guide describes accepted language-code forms and rejects unsupported codes; it does not establish a bn-BD locale contract. | Evaluation alternative, no assumed Bangladesh-accent accuracy. [Official transcription guide](https://developers.openai.com/api/docs/guides/speech-to-text). |

Do not substitute bn-IN or English silently. Documented language support does not replace the representative-owner Bangla/noise/correction pilot.

## Owner flow

1. Keep typing/paste and on-device-only voice available as independent choices. On-device-only never uploads or silently switches provider.
2. Add a separately named online Bangla option. Before the first recording, show the provider, proposed processing region, network requirement and that this app/backend do not retain audio. Explain the actual provider policy after it is verified for the chosen account/API configuration.
3. The explicit action is Agree and record. Then request microphone permission. Do not upload or request microphone access when merely opening a sheet.
4. Show recording state and Stop and transcribe / Cancel. Stop authorizes transcription of this recording. Cancel before Stop discards the in-memory buffer without uploading. Backgrounding stops and discards capture.
5. Show Sending / Cancel after Stop. Cancellation invalidates late results and closes our request. Audio already transmitted cannot be recalled; do not promise that cancellation prevents provider processing or billing.
6. Return the transcript to the same editable `CaptureReviewSheet`. Preserve the existing text on timeout/failure. The owner reviews parsed amounts/items/odometer and uses the existing fixed Save. No backend or provider can save a vault record.
7. Offline, unavailable locale, exhausted quota or provider failure offers typing/paste. Retry is explicit, with no automatic re-upload or provider switch.

Proposed copy remains a resource task, reviewed in English and modern Bangla together:

| Resource intent | English draft | Bangla draft |
| --- | --- | --- |
| Online mode | Online Bangla voice | ইন্টারনেটে বাংলা ভয়েস |
| Consent title | Send audio to Google Cloud? | অডিও Google Cloud-এ পাঠাবেন? |
| Consent action | Agree and record | রাজি আছি, বলা শুরু করুন |
| Stop action | Stop and transcribe | থামিয়ে লেখা তৈরি করুন |
| Limit/fallback | Voice limit reached. Type or paste instead. | ভয়েসের সীমা শেষ হয়েছে। লিখুন বা পেস্ট করুন। |

No new product strings or cloud behavior are implemented by this plan.

## App and backend boundary

Proposed data path:

`explicit consent -> native RAM audio buffer -> authenticated MotoVault voice endpoint -> regional Google Recognize -> transcript -> shared reviewed draft -> existing vault writer`

- Add an application `VoiceTranscriber` port with explicit request ID, locale, typed failures and cancellation. Keep HTTP, provider SDKs, authentication and audio capture out of the domain and reusable Sky module.
- Native capture produces mono 16 kHz PCM16 in RAM. Proposed maximum: 30 seconds, 960,000 PCM bytes. Bound the encoded request body at 2 MiB and validate the actual sample count/format at the backend. These are proposed product limits, not vendor limits.
- Use one synchronous transcription after Stop. No Cloud Storage bucket, asynchronous job, audio archive, server-side queue, transcript database or app audio file is needed for this slice.
- A proposed `POST /voice/transcriptions` endpoint returns only transcript, request ID, locale and provider code. It receives no vehicle, vault, payment history, document, key or owner identifier.
- Keep provider credentials on the backend through least-privilege workload identity. No service key, reusable provider token or shared secret belongs in the APK, framework, plist or remote config.
- MotoVault currently has no voice backend/auth service. Resolve deployment ownership, endpoint identity, install-scoped short-lived authentication and replay/abuse protection before implementation. Platform attestation can strengthen admission but must have an explicit unsupported-device policy; a public unauthenticated paid proxy is not acceptable.
- Reserve quota atomically before accepting audio. Bind a short-lived request token to the installation, request ID, locale and byte/duration limit. Reject expired, replayed and concurrent duplicate requests. A local biometric grant is not remote API authentication.
- A request generation/cancellation token prevents a late transcript from replacing a newer draft. A failure never clears typed text or an existing review.

Google currently limits synchronous recognition to 10 MB or one minute, whichever comes first. The proposed 30-second/2-MiB envelope stays below that API boundary. [V2 limits](https://docs.cloud.google.com/speech-to-text/docs/quotas).

## Privacy and operation

- Keep data logging disabled. Google's published Cloud STT FAQ states that sync/stream audio is processed in memory, with request metadata retained for operations; content use changes with the logging opt-in. The fetched FAQ is in the V1 documentation tree. Confirm that policy and the chosen V2 project's settings/terms before promising provider retention behavior to owners. [Data usage FAQ](https://docs.cloud.google.com/speech-to-text/docs/v1/data-usage-faq).
- App and backend buffers have no durable retention. Clear/release buffers on success, denial, cancel, background, timeout and error. Configure the ingress proxy/framework to avoid disk request buffering and request-body capture.
- Disable audio/transcript content in access logs, APM, traces, crash reports and provider debug logging. Keep only sanitized request ID, provider/locale code, result class, duration/size and quota units for bounded operational monitoring.
- Do not put an owner transcript into a provider prompt for another purpose. No training, analytics, translation, financial inference or automatic transaction creation belongs in this path.
- Document the actual processing region and provider terms in the consent/privacy view. Review the app-store privacy declaration and account/provider settings before release. This is an implementation gate, not a claim that those settings already exist.
- Keep a backend kill switch. Disabling cloud voice leaves manual/paste/on-device-only paths working and requires no owner-data migration.

## Cost controls

The posted V2 Standard list rate is USD 0.016 per audio minute in the first usage band, with per-second rounding. At that planning rate, 1,000 ten-second requests are about USD 2.67 and 1,000 thirty-second requests about USD 8 for recognition alone. These are arithmetic estimates, not an account quote; confirm the selected model/region SKU, taxes and backend/auth/network costs before paid activation. [Official pricing](https://cloud.google.com/speech-to-text/pricing).

Proposed pilot admission limits: one concurrent request per installation, at most 20 starts per installation/day and a shared 500-minute monthly recognition pool. Reserve the worst-case 30 seconds before upload and reconcile conservatively. Make limits server-enforced and configurable. Billing alerts are monitoring, not a hard spending cap. No automatic paid retry is allowed.

## Bounded implementation sequence

1. Review this provider/region/privacy/cost proposal and name the service owner. Confirm an authorized cloud project, endpoint identity, account settings and budget. Keep P07-04 incomplete while these prerequisites are open.
2. Extend v3 with online-provider consent, recording, sending, cancellation, quota/offline/error and transcript review states. Review paired resources before native UI changes.
3. Add the application port and one backend adapter. Implement authentication, bounds, in-memory processing, cancellation, sanitized observability and quota admission. Keep the feature disabled by default.
4. Add native in-memory capture for the explicit online mode. Reuse the existing review/parser/writer boundary and stale-result guards. Avoid changing persisted owner records.
5. Run focused contract tests once: unsupported locale, oversized/invalid audio, expired/replayed token, quota/race, timeout/cancel, no automatic retry, no body logging and late response. Use a fake provider only for backend contract failures; do not label that as actual speech recognition.
6. With an approved staging service, run actual synthetic Bengali audio through the real provider. Record model/region/configuration and distinguish transcript correctness from parsed-field correctness. Inspect transient-buffer cleanup and sanitized logs.
7. Complete physical Android/iPhone bn-BD/noise/permission/background tests and the representative-owner effort pilot in `P07-CAPTURE-PILOT.md`. Close P07-04/P07-05 only with actual evidence and the required review.

## Remaining decisions and evidence

The provider/region choice is proposed. Account ownership, deployment/auth design, paid quota and consent/privacy review are not approved or provisioned. The owner requested a plan, not cloud activation. No provider accuracy, real audio, staging, physical device or pilot evidence exists for this proposal.
