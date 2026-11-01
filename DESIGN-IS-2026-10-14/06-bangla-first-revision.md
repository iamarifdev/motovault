# Bangla-first audience revision

This revision supersedes English-first assumptions in the earlier review. Audience: native Bangladeshi motorcycle owners. Primary language: easy, modern Bangladeshi Bangla. Secondary language: English. This is a design specification proposal, not an implemented localization or tested font recommendation.

## Revised judgment

Local motorcycle choices and taka pricing provide relevant context, but the examined screens still use English navigation, explanations and forms. That limits understanding for the core audience. Language fit must be addressed before judging the app polished for Bangladeshi riders. Preserve the brand palette and photography; revise the reading experience and task vocabulary.

## Language and labels

Use conversational, respectful Bangla. Avoid literary wording, literal translation and unnecessary technical vocabulary. Keep familiar motorcycle terms such as সার্ভিস, ইঞ্জিন অয়েল, ট্যাক্স টোকেন and মাইলেজ when they communicate better. Do not translate manufacturer/model names, official identifiers or document originals.

| Current role | Proposed Bangla | Reason |
|---|---|---|
| Garage tab | আমার বাইক | Names the user's subject directly. |
| Vault tab | কাগজপত্র | Easier than a translated storage metaphor. |
| History tab | হিসাব | Covers ownership records; validate whether users also expect expense totals here. |
| History screen title | আগের রেকর্ড | Clearer when listing service and fuel events. |
| Appearance | থিম | Familiar, short secondary setting label. |
| Change bike | বাইক বদলান | Explicit action; retain warning if records will be removed. |
| Odometer | মিটারের রিডিং | More approachable than a technical instrument name. |
| Log fuel | তেল ভরার হিসাব যোগ করুন | Describes the record being created. |
| Log service | সার্ভিস যোগ করুন | Short and familiar. |
| Add document | কাগজপত্র যোগ করুন | Predictable action. |
| Inspection mode | কাগজপত্র দেখান | Describes intent without a specialist mode name. |
| Service soon | সার্ভিসের সময় কাছাকাছি | Communicates timing, not mechanical failure. |
| Record saved | সেভ হয়েছে | Familiar concise feedback. |
| Saved on device | এই ফোনে সেভ আছে | Concrete storage location. |

These are draft labels for rider testing. In particular, compare হিসাব with রেকর্ড for the combined fuel/service tab. Short navigation labels and longer descriptive screen headings may differ while keeping the same meaning.

Example errors:

- “তারিখটি বেছে নিন।”
- “নাম ও নম্বর লিখে তথ্য ঠিক আছে কি না দেখে নিন।”
- “সেভ করা যায়নি। আপনার আগের তথ্য ঠিক আছে। আবার চেষ্টা করুন।”
- “আগের সার্ভিসের তারিখ জানেন না? পরে যোগ করতে পারবেন।”
- “ছবিতে আলো বেশি হতে পারে। একটু সরিয়ে আবার তুলুন, অথবা এই ছবিই ব্যবহার করুন।”

Use task-specific reassurance only where the implementation guarantees it. Do not claim that information is safe or recoverable solely because it is local.

## Bangla typography and layout

Make the Bangla family the primary typography decision. Evaluate candidate fonts with actual screenshots before selecting one; check ি/ী/ু/ূ marks, conjuncts such as রেজিস্ট্রেশন, mixed identifiers and numerals. Existing DM Sans/Barlow styling only establishes the Latin design.

Suggested starting body size: 16-18sp. Supporting functional text: 14sp where feasible. Use comfortable line height, normal letter spacing and content-driven button height. Avoid all-caps eyebrow styling and condensed Bangla text. Preserve Latin manufacturer/model names, but ensure they do not dominate the task instructions.

Bangla labels may wrap differently. Do not shrink them to match English button widths. Support two-line labels, flexible cards and screen-reader reading order. Test 200% text, long names, smaller phones and both themes. The earlier spacing/radius proposals remain starting points, not fixed rules.

## Dates, numbers, units and input

Use a date picker instead of asking for YYYY-MM-DD. Display an unambiguous local-language date such as “১৪ অক্টোবর ২০২৬”. Keep machine storage formats separate from visible formatting.

Support both বাংলা digits and 0-9 in number entry, including pasted values. Normalize input for calculations without changing registration numbers or official document identifiers. Bangla interface language does not by itself prove riders prefer Bangla digits on every dashboard; test preference, especially matching a motorcycle's meter.

Use familiar unit labels: কিমি, লিটার, কিমি/লিটার, টাকা or ৳ with a consistent amount layout. Keep precision, grouping and decimal entry predictable. Do not silently reinterpret commas or punctuation in ways that change a value.

OCR must preserve original document text and allow manual correction in either script. Verify Bengali OCR capability before promising automatic reading. Keep originals unchanged and do not translate names, registration numbers or official text in the stored image/PDF.

## Language selection

Default to বাংলা for the intended Bangla-first product, with an easy বাংলা / English choice during setup and in settings. Persist the choice and preserve all user data when switching. Do not put English translations beneath every Bangla label; that doubles visual noise. Use familiar borrowed words where appropriate and optional help for unfamiliar terms.

Translate the complete owned experience: onboarding, labels, validation, empty/loading states, permission explanations, reminders, export and recovery. Platform dialogs follow OS language; the app should explain what users are about to see rather than promise it controls the OS text.

## Engagement and trust

Useful Bangla reminders and quick actions should lead engagement. Example: “ট্যাক্স টোকেনের মেয়াদ শেষ হতে আর ৭ দিন।” Only show that statement when the stored expiry and date calculation support it. Let users control notifications and avoid repeated pressure.

Make local-storage consequences understandable: explain what happens if the phone is lost or the app is removed, and provide only backup/recovery actions actually supported. Explain maintenance percentages as estimates from time/distance rather than measured bike condition.

The UI can feel Bangladeshi through language, motorcycles, currency, documents and familiar workflows. Flag colors, ornamental patterns and extra cultural decoration are unnecessary unless they serve the task.

## Revised priorities and acceptance

1. Establish complete Bangla/English strings and tested Bangla typography.
2. Simplify setup and routine tasks with Bangla instructions and unknown-history choices.
3. Make dates, numeric entry, validation and correction work naturally in either script.
4. Make document retrieval, draft protection and recovery clear in Bangla.
5. Test representative Bangla-speaking riders, including people with limited English and older users. Observe whether they can set up, log a fill, correct a service and show a document without explanation.

No new aggregate score is assigned: language alignment changes the interpretation of usefulness and understandability, but a revised numeric score needs rendered Bangla and task evidence. The earlier 16/30 remains a provisional score of the examined English-first design, not approval for this audience.
