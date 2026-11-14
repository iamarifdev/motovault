import { toAscii } from './bn'

const WORDS_1_99 = [
  'এক', 'দুই', 'তিন', 'চার', 'পাঁচ', 'ছয়', 'সাত', 'আট', 'নয়', 'দশ',
  'এগারো', 'বারো', 'তেরো', 'চোদ্দ', 'পনেরো', 'ষোলো', 'সতেরো', 'আঠারো', 'উনিশ', 'বিশ',
  'একুশ', 'বাইশ', 'তেইশ', 'চব্বিশ', 'পঁচিশ', 'ছাব্বিশ', 'সাতাশ', 'আঠাশ', 'উনত্রিশ', 'ত্রিশ',
  'একত্রিশ', 'বত্রিশ', 'তেত্রিশ', 'চৌত্রিশ', 'পঁয়ত্রিশ', 'ছত্রিশ', 'সাঁইত্রিশ', 'আটত্রিশ', 'উনচল্লিশ', 'চল্লিশ',
  'একচল্লিশ', 'বিয়াল্লিশ', 'তেতাল্লিশ', 'চুয়াল্লিশ', 'পঁয়তাল্লিশ', 'ছেচল্লিশ', 'সাতচল্লিশ', 'আটচল্লিশ', 'উনপঞ্চাশ', 'পঞ্চাশ',
  'একান্ন', 'বাহান্ন', 'তিপ্পান্ন', 'চুয়ান্ন', 'পঞ্চান্ন', 'ছাপ্পান্ন', 'সাতান্ন', 'আটান্ন', 'উনষাট', 'ষাট',
  'একষট্টি', 'বাষট্টি', 'তেষট্টি', 'চৌষট্টি', 'পঁয়ষট্টি', 'ছেষট্টি', 'সাতষট্টি', 'আটষট্টি', 'উনসত্তর', 'সত্তর',
  'একাত্তর', 'বাহাত্তর', 'তিয়াত্তর', 'চুয়াত্তর', 'পঁচাত্তর', 'ছিয়াত্তর', 'সাতাত্তর', 'আটাত্তর', 'উনআশি', 'আশি',
  'একাশি', 'বিরাশি', 'তিরাশি', 'চুরাশি', 'পঁচাশি', 'ছিয়াশি', 'সাতাশি', 'আটাশি', 'উননব্বই', 'নব্বই',
  'একানব্বই', 'বিরানব্বই', 'তিরানব্বই', 'চুরানব্বই', 'পঁচানব্বই', 'ছিয়ানব্বই', 'সাতানব্বই', 'আটানব্বই', 'নিরানব্বই',
]

const VALUES = new Map<string, number>(WORDS_1_99.map((w, i) => [w, i + 1]))
// Common spoken and spelling variants.
;([['শূন্য', 0], ['দু', 2], ['দুই', 2], ['ছ', 6], ['ছয়', 6], ['নই', 9], ['চৌদ্দ', 14], ['ষোল', 16], ['আঠার', 18], ['পনের', 15], ['তের', 13], ['বার', 12], ['এগার', 11], ['সতের', 17]] as const)
  .forEach(([w, v]) => VALUES.set(w, v))

const HUNDRED = ['শো', 'শত', 'শ']
const SCALES: [string, number][] = [['হাজার', 1000], ['লাখ', 100_000], ['লক্ষ', 100_000]]

function small(token: string): number | null {
  if (/^\d+$/.test(token)) return Number(token)
  if (VALUES.has(token)) return VALUES.get(token)!
  for (const suffix of HUNDRED) {
    if (!token.endsWith(suffix)) continue
    const head = token.slice(0, -suffix.length)
    if (head === '' || head === 'এক') return 100
    const h = VALUES.get(head)
    if (h !== undefined && h < 10) return h * 100
  }
  return null
}

/**
 * Reads a spoken number in Bangla words, digits, or a mix ("বারো হাজার চারশো পঞ্চাশ", "১২ হাজার ৩৫০").
 * Returns null when nothing in the text is a number, so callers never invent a value.
 */
export function parseSpokenNumber(input: string): number | null {
  const tokens = toAscii(input.normalize('NFC')).replace(/,/g, '').split(/[\s-]+/).filter(Boolean)
  let total = 0
  let current = 0
  let found = false
  for (const raw of tokens) {
    const token = raw.replace(/(টা|টি|টাকা|টাকার|কিমি)$/u, '')
    const scale = SCALES.find(([w]) => token === w)
    if (scale) {
      total += (current || 1) * scale[1]
      current = 0
      found = true
      continue
    }
    const v = small(token)
    if (v === null) continue
    current += v
    found = true
  }
  return found ? total + current : null
}
