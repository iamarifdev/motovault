import type { DocKind, ItemKey } from '../types'

export interface Model { id: string; name: string; photo?: string }
export interface Brand { id: string; name: string; light: string; dark: string; models: Model[] }

// Accent pairs are the ones already contrast-checked in Appearance.kt (all 18 pass 4.5:1 with button text).
export const BRANDS: Brand[] = [
  { id: 'yamaha', name: 'Yamaha', light: '#315ABE', dark: '#88A8FF', models: [
    { id: 'yamaha-r15-v4', name: 'R15 V4', photo: '/bikes/yamaha-r15-v4.webp' }, { id: 'yamaha-fzs-v3', name: 'FZS V3' }, { id: 'yamaha-mt15', name: 'MT-15' } ] },
  { id: 'honda', name: 'Honda', light: '#B42A38', dark: '#FF7B84', models: [
    { id: 'honda-sp125', name: 'SP 125', photo: '/bikes/honda-sp125.webp' }, { id: 'honda-shine', name: 'Shine 100' }, { id: 'honda-hornet', name: 'Hornet 2.0' } ] },
  { id: 'bajaj', name: 'Bajaj', light: '#355D9E', dark: '#96B9FA', models: [
    { id: 'bajaj-n160', name: 'Pulsar N160', photo: '/bikes/bajaj-pulsar-n160.webp' }, { id: 'bajaj-ns125', name: 'Pulsar NS125' }, { id: 'bajaj-platina', name: 'Platina 110' } ] },
  { id: 'tvs', name: 'TVS', light: '#A52B41', dark: '#F4828B', models: [
    { id: 'tvs-apache160', name: 'Apache RTR 160 4V', photo: '/bikes/tvs-apache-160.webp' }, { id: 'tvs-raider', name: 'Raider 125' } ] },
  { id: 'suzuki', name: 'Suzuki', light: '#246294', dark: '#7EBAF5', models: [
    { id: 'suzuki-gixxer-sf', name: 'Gixxer SF', photo: '/bikes/suzuki-gixxer-sf.webp' }, { id: 'suzuki-gixxer', name: 'Gixxer' } ] },
  { id: 'hero', name: 'Hero', light: '#B1362C', dark: '#FF8983', models: [
    { id: 'hero-hunk', name: 'Hunk 150R', photo: '/bikes/hero-hunk-150r.webp' }, { id: 'hero-splendor', name: 'Splendor+' } ] },
  { id: 'ktm', name: 'KTM', light: '#AA4700', dark: '#FF7900', models: [{ id: 'ktm-duke125', name: 'Duke 125' }] },
  { id: 'cfmoto', name: 'CFMOTO', light: '#00717A', dark: '#69D2DB', models: [{ id: 'cfmoto-250nk', name: '250NK' }] },
  { id: 'enfield', name: 'Royal Enfield', light: '#805E28', dark: '#DBB77C', models: [
    { id: 'enfield-hunter', name: 'Hunter 350' }, { id: 'enfield-classic', name: 'Classic 350' } ] },
]

export const brandOf = (id: string) => BRANDS.find((b) => b.id === id) ?? BRANDS[0]
export const modelOf = (id: string) => BRANDS.flatMap((b) => b.models).find((m) => m.id === id)

export interface ServiceItem { key: ItemKey; bn: string; short: string; unit: string; ruleName?: string }

export const ITEMS: ServiceItem[] = [
  { key: 'engine_oil', bn: 'ইঞ্জিন অয়েল', short: 'অয়েল', unit: 'বোতল', ruleName: 'oil' },
  { key: 'oil_filter', bn: 'অয়েল ফিল্টার', short: 'ফিল্টার', unit: 'টি', ruleName: 'oil' },
  { key: 'air_filter', bn: 'এয়ার ফিল্টার', short: 'এয়ার ফিল্টার', unit: 'টি', ruleName: 'air' },
  { key: 'spark_plug', bn: 'স্পার্ক প্লাগ', short: 'স্পার্ক প্লাগ', unit: 'টি', ruleName: 'spark' },
  { key: 'chain_sprocket', bn: 'চেইন ও স্প্রকেট', short: 'চেইন', unit: 'সেট', ruleName: 'chain' },
  { key: 'brake_pad', bn: 'ব্রেক প্যাড', short: 'ব্রেক প্যাড', unit: 'সেট', ruleName: 'brakes' },
  { key: 'coolant', bn: 'কুল্যান্ট', short: 'কুল্যান্ট', unit: 'বোতল' },
  { key: 'tyre', bn: 'টায়ার', short: 'টায়ার', unit: 'টি' },
  { key: 'battery', bn: 'ব্যাটারি', short: 'ব্যাটারি', unit: 'টি' },
  { key: 'general_service', bn: 'সাধারণ সার্ভিস', short: 'সাধারণ সার্ভিস', unit: 'বার' },
]
export const itemOf = (key: string) => ITEMS.find((i) => i.key === key)

export const VISCOSITY = ['10W-30', '10W-40', '20W-40', '20W-50']

export const DOC_KINDS: { kind: DocKind; bn: string; card: boolean }[] = [
  { kind: 'registration', bn: 'রেজিস্ট্রেশন স্মার্ট কার্ড', card: true },
  { kind: 'licence', bn: 'ড্রাইভিং লাইসেন্স', card: true },
  { kind: 'tax', bn: 'ট্যাক্স টোকেন', card: false },
  { kind: 'insurance', bn: 'বিমা', card: false },
  { kind: 'fitness', bn: 'ফিটনেস সনদ', card: false },
]
export const docName = (k: DocKind) => DOC_KINDS.find((d) => d.kind === k)?.bn ?? k

export const FUEL_LABEL = { octane: 'অকটেন', petrol: 'পেট্রোল' } as const
