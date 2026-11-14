import type { DocKind } from '../types'

/** Official sources checked for P05 on 5 November 2026; the prototype shows the same date as the app. */
export const RENEWAL_CHECKED_ON = '2026-11-05'
export const QR_VERIFY_URL = 'https://bsp.brta.gov.bd/bsp/qr/qrScanner'
export const FEE_CALCULATOR_URL = 'https://bsp.brta.gov.bd/feeCalculator'

export interface RenewalStep { id: string; title: string; detail: string }
export interface RenewalLink { id: string; label: string; url: string }
export interface RenewalGuide {
  kind: DocKind
  steps: RenewalStep[]
  links: RenewalLink[]
  feeCalculator: boolean
  qrVerification: boolean
  lateFine?: string
}

const LICENCE: RenewalGuide = {
  kind: 'licence', feeCalculator: true, qrVerification: true,
  lateFine: 'মেয়াদ শেষের ১৫ দিন পর হলে প্রতি বছরে ৫১৮ টাকা জরিমানা যোগ হতে পারে। BRTA-তে দেখে নিন।',
  links: [{ id: 'licence_instructions', label: 'BRTA-র নির্দেশনা দেখুন', url: 'https://brta.gov.bd/pages/static-pages/6922dbe0933eb65569e0d26a' }],
  steps: [
    { id: 'licence_form', title: 'আবেদনের ফরম', detail: 'নির্ধারিত ফরম পূরণ ও সই করুন।' },
    { id: 'licence_medical', title: 'মেডিকেল সার্টিফিকেট', detail: 'রেজিস্টার্ড ডাক্তারের সার্টিফিকেট নিন।' },
    { id: 'licence_nid', title: 'NID কপি', detail: 'জাতীয় পরিচয়পত্রের সত্যায়িত কপি নিন।' },
    { id: 'licence_education', title: 'শিক্ষার সনদ', detail: 'শিক্ষাগত যোগ্যতার সনদ নিন।' },
    { id: 'licence_fee_receipt', title: 'ফি জমার রসিদ', detail: 'নির্ধারিত ফি জমিয়ে রসিদ নিন।' },
    { id: 'licence_photos', title: 'ছবি', detail: 'পাসপোর্ট সাইজ ১ কপি ও স্ট্যাম্প সাইজ ১ কপি ছবি নিন।' },
    { id: 'licence_practical_test', title: 'ব্যবহারিক পরীক্ষা', detail: 'পেশাদার লাইসেন্সে আবার পরীক্ষা দিতে হয়।' },
    { id: 'licence_biometrics', title: 'বায়োমেট্রিক', detail: 'সার্কেল অফিসে ছবি, সই ও আঙুলের ছাপ দিতে হবে।' },
  ],
}

const TAX: RenewalGuide = {
  kind: 'tax', feeCalculator: true, qrVerification: true,
  links: [{ id: 'tax_instructions', label: 'BRTA-র নির্দেশনা দেখুন', url: 'https://brta.gov.bd/pages/static-pages/6922e0ab933eb65569e281ad' }],
  steps: [
    { id: 'tax_previous_token', title: 'আগের ট্যাক্স টোকেন', detail: 'আগের টোকেনের মূল কপি নিন।' },
    { id: 'tax_fee_payment', title: 'ফি জমা', detail: 'BRTA-র ব্যাংক বা অনলাইনে ফি দিন।' },
    { id: 'tax_fee_receipt', title: 'জমার রসিদ', detail: 'জমার রসিদ সংরক্ষণ করুন।' },
  ],
}

const FITNESS: RenewalGuide = {
  kind: 'fitness', feeCalculator: true, qrVerification: true,
  links: [{ id: 'fitness_instructions', label: 'BRTA-র নির্দেশনা দেখুন', url: 'https://brta.gov.bd/pages/static-pages/6922db91933eb65569e0af12' }],
  steps: [
    { id: 'fitness_form', title: 'আবেদনের ফরম', detail: 'পূরণ করা আবেদনপত্র প্রস্তুত রাখুন।' },
    { id: 'fitness_appointment', title: 'অ্যাপয়েন্টমেন্ট', detail: 'ঢাকা ও চট্টগ্রাম সার্কেলে আগে অনলাইনে সিরিয়াল নিন।' },
    { id: 'fitness_previous_certificate', title: 'আগের ফিটনেস সনদ', detail: 'আগের সনদের মূল কপি নিন।' },
    { id: 'fitness_tax_token_copy', title: 'ট্যাক্স টোকেন কপি', detail: 'হালনাগাদ ট্যাক্স টোকেনের সত্যায়িত কপি নিন।' },
    { id: 'fitness_tin', title: 'TIN ও আয়কর', detail: 'TIN কাগজ ও অগ্রিম আয়করের প্রমাণ নিন।' },
    { id: 'fitness_vehicle_present', title: 'গাড়ি হাজির', detail: 'সার্কেল অফিসে মোটরযান হাজির করুন।' },
    { id: 'fitness_fee_receipt', title: 'ফি জমার রসিদ', detail: 'নির্ধারিত ফি জমিয়ে রসিদ নিন।' },
  ],
}

const INSURANCE: RenewalGuide = {
  kind: 'insurance', feeCalculator: false, qrVerification: false,
  links: [],
  steps: [
    { id: 'insurance_insurer', title: 'বিমা কোম্পানি', detail: 'নবায়ন BRTA-তে হয় না, বিমা কোম্পানিতে করুন।' },
    { id: 'insurance_policy_copy', title: 'পলিসি কপি', detail: 'নতুন পলিসি বা সনদের কপি নিন।' },
    { id: 'insurance_new_expiry', title: 'নতুন মেয়াদ', detail: 'নতুন মেয়াদ শেষের তারিখ লিখুন।' },
  ],
}

const REGISTRATION: RenewalGuide = {
  kind: 'registration', feeCalculator: true, qrVerification: false,
  links: [
    { id: 'registration_transfer', label: 'মালিকানা বদলী ও সংশোধনের নিয়ম', url: 'https://brta.gov.bd/pages/static-pages/6922dc6b933eb65569e10468' },
    { id: 'registration_digital_certificate', label: 'ডিজিটাল সার্টিফিকেটের প্রক্রিয়া', url: 'https://brta.gov.bd/pages/static-pages/6922dba6933eb65569e0b8fe' },
  ],
  steps: [
    { id: 'registration_no_periodic_renewal', title: 'মেয়াদ নেই', detail: 'রেজিস্ট্রেশন সনদের মেয়াদ শেষ হয় না।' },
    { id: 'registration_transfer_or_correction', title: 'মালিকানা বদল বা সংশোধন', detail: 'বদল, সংশোধন বা ডুপ্লিকেট লাগলে সার্কেল অফিসে যান।' },
    { id: 'registration_official_sources', title: 'BRTA-র নিয়ম', detail: 'প্রয়োজনীয় কাগজপত্র BRTA-র সাইটে দেখুন।' },
  ],
}

export const RENEWAL_GUIDES: Record<DocKind, RenewalGuide> = {
  licence: LICENCE, tax: TAX, fitness: FITNESS, insurance: INSURANCE, registration: REGISTRATION,
}
