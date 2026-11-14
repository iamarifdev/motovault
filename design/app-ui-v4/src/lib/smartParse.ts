import type { ItemKey } from '../types'
import { toAscii } from './bn'

const ALIASES: [string, ItemKey][] = [
  ['oil filter', 'oil_filter'], ['অয়েল ফিল্টার', 'oil_filter'], ['তেলের ফিল্টার', 'oil_filter'], ['ফিল্টার', 'oil_filter'],
  ['engine oil', 'engine_oil'], ['ইঞ্জিন অয়েল', 'engine_oil'], ['ইঞ্জিন তেল', 'engine_oil'], ['অয়েল', 'engine_oil'], ['mobil', 'engine_oil'],
  ['chain', 'chain_sprocket'], ['চেইন', 'chain_sprocket'], ['air filter', 'air_filter'], ['এয়ার ফিল্টার', 'air_filter'],
  ['spark plug', 'spark_plug'], ['স্পার্ক', 'spark_plug'], ['brake', 'brake_pad'], ['ব্রেক', 'brake_pad'],
  ['coolant', 'coolant'], ['কুল্যান্ট', 'coolant'], ['tyre', 'tyre'], ['tire', 'tyre'], ['টায়ার', 'tyre'],
  ['battery', 'battery'], ['ব্যাটারি', 'battery'], ['general service', 'general_service'], ['সার্ভিস', 'general_service'],
].sort((a, b) => b[0].length - a[0].length) as [string, ItemKey][]

const PRICE = /(?:৳|bdt\s*|tk\s*|taka\s*|টাকা\s*)(\d+(?:\.\d+)?)|(\d+(?:\.\d+)?)\s*(?:৳|bdt|tk|taka|টাকা|তক)/i
const QTY = /(?:^|\s)(\d+(?:\.\d+)?)\s*(?:x|টি|টা|পিস)\b/i

export interface ParsedLine { item: ItemKey; qty: number; price: number | null; source: string }

/** Local, forgiving parser. Everything it returns is a draft the rider confirms. Handles Bengali digits and common Bangla words. */
export function parseServiceText(input: string): { lines: ParsedLine[]; unmatched: string[] } {
  const lines: ParsedLine[] = []
  const unmatched: string[] = []
  input.split(/\n|;|,|।/).map((s) => s.trim()).filter(Boolean).slice(0, 20).forEach((source) => {
    const text = toAscii(source).toLowerCase()
    const hit = ALIASES.find(([alias]) => text.includes(alias))
    if (!hit) { unmatched.push(source); return }
    const price = PRICE.exec(text)
    const qty = QTY.exec(text)
    lines.push({
      item: hit[1], source,
      qty: qty ? Number(qty[1]) : 1,
      price: price ? Number(price[1] ?? price[2]) : null,
    })
  })
  return { lines, unmatched }
}

/** Pulls an amount out of pasted payment text (bKash, Nagad, pump slip). Returns taka or null. */
export function parseAmount(input: string): number | null {
  const t = toAscii(input).replace(/,/g, '')
  const m = /(?:tk|bdt|৳|taka|টাকা)\s*(\d+(?:\.\d+)?)|(\d+(?:\.\d+)?)\s*(?:tk|bdt|৳|taka|টাকা)/i.exec(t)
  return m ? Number(m[1] ?? m[2]) : null
}
