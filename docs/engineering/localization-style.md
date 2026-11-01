# Bangla product-copy style

Status: adopted 2026-11-01. Applies to `values-bn/strings.xml` and native Bangla
copy. The reference voice is mainstream Bangladeshi consumer apps such as bKash
and Pathao: short, spoken, action-first Bangla with familiar English loanwords —
not literal English syntax and not formal bureaucratic Bangla.

## Voice

- Write to one person, directly: “আপনার বাইক”, “রেকর্ডটি দেখুন”.
- Lead with the action or result: “ব্যাকআপ তৈরি করুন”, not “ব্যাকআপ তৈরি করা
  আপনার জন্য প্রয়োজন”.
- Prefer one short sentence over a comma-heavy translation.
- Use active voice. Avoid “করা হয়েছে” chains when a direct verb works.
- Keep the same term for the same object across screens.
- Do not repeat “আপনি” in every sentence; Bangla drops it naturally.
- Error copy says what happened and what to do next, in one or two lines.

## Preferred terms

| Use | Avoid |
| --- | --- |
| সেটিংস, প্রোফাইল, নোটিফিকেশন, রিমাইন্ডার | বার্তা, সতর্কবার্তা, মনে করিয়ে দেওয়া |
| ব্যাকআপ, রিস্টোর, এক্সপোর্ট, আপডেট | প্রতিলিপি, পুনঃস্থাপন |
| ডকুমেন্ট (where it means a record/paper) | দস্তাবেজ, নথি |
| কাগজ (where the physical paper matters) | — |
| ফুয়েল | জ্বালানি (except formal/legal copy) |
| যাচাই করুন / কনফার্ম করুন | নিশ্চিত করা (when it means verify) |
| প্রয়োজন | দরকার |
| শীঘ্রই | শিগগির |
| ছাড়িয়ে গেছে | পেরিয়ে গেছে |
| অ্যাপিয়ারেন্স | চেহারা |
| হ্যাপটিক ফিডব্যাক | টাচ ভাইব্রেশন |
| টেক্সট সাইজ | লেখার আকার |
| পছন্দ ও সতর্কতা | ব্যবহার ও সতর্কতা |

Use English loanwords where Bangladeshi users already say them: সেটিংস, প্রোফাইল,
নোটিফিকেশন, রিমাইন্ডার, ব্যাকআপ, রিস্টোর, আপডেট, সার্ভিস, ওডোমিটার, হ্যাপটিক.
Do not translate these into rare formal words just to avoid English.

## Numbers and units

- Currency: `৳১,২৩৪` with the app digit preference; use the resource symbol, not a
  hardcoded `Tk`.
- Distance: `কিমি`; economy: `কিমি/লি`; fuel: `লিটার`.
- Dates: day + localized month + year; keep `YYYY-MM-DD` only as an input hint.
- Percentages: `৭৫%`, not `৭৫ শতাংশ` in compact UI.
- Use `%1$s` placeholders and let `Fmt` localize digits.

## Punctuation and tone

- Use `।` for full stops, `,` for short pauses, `—` sparingly.
- Avoid exclamation marks in errors and permission copy.
- Use `…` for progress, not three dots.
- Buttons are verb-first and short: `সেভ করুন`, `রিস্টোর করুন`, `আবার চেষ্টা করুন`.
- Destructive actions name the object: `ডকুমেন্ট ডিলিট করুন`, not `মুছে ফেলুন`
  alone.
- Permission copy explains the benefit before the OS prompt.

## Examples

| Robotic | Product Bangla |
| --- | --- |
| কাগজের মেয়াদ উত্তীর্ণের তারিখ | মেয়াদ শেষের তারিখ |
| আপনার তথ্য বদলেছে। আবার দেখে চেষ্টা করুন। | তথ্য বদলে গেছে। আবার দেখে চেষ্টা করুন। |
| সার্ভিসের সময় হলে জানান | সার্ভিসের সময় হলে জানিয়ে দিন |
| অনুমতি, সময় ও পরে জানান | রিমাইন্ডার সেটিংস |
| মনে করিয়ে দেব? | রিমাইন্ডার দিতে চান? |
| শিগগির দরকার | শীঘ্রই প্রয়োজন |
| কাগজ স্ক্যান করুন করুন | কাগজ স্ক্যান করুন |
| ফোনে জানানোর অনুমতি দিন | নোটিফিকেশন চালু করুন |
| এই ডিভাইসে ইংরেজি লেখা পড়া যায় | এই ডিভাইস ইংরেজি লেখা পড়তে পারে |
| শুধু তথ্য আছে। আসল কপি নেই | শুধু তথ্য আছে, আসল কপি নেই |

## Review checklist

- Does it sound like something a Bangladeshi rider would say, not a translation?
- Is the action/result in the first clause?
- Is the same object named consistently with the rest of the app?
- Are placeholders, digits, currency and units correct in both languages?
- Is the English version equally short and natural?
- Run `rtk run python3 -m unittest tools.tests.test_localization_resources` and
  `rtk run python3 tools/ui_literal_gate.py`.
