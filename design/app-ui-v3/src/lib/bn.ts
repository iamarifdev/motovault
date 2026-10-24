import { useMemo } from 'react'
import { useStore } from '../store'
import type { Digits } from '../types'

const BN = '০১২৩৪৫৬৭৮৯'

/** Accepts Bengali, Arabic-Indic or Latin digits. Every number field normalises through this. */
export const toAscii = (s: string) =>
  s.replace(/[০-৯]/g, (d) => String(BN.indexOf(d))).replace(/[٠-٩]/g, (d) => String(d.charCodeAt(0) - 0x0660))
export const toBn = (s: string) => s.replace(/[0-9]/g, (d) => BN[Number(d)])

export const parseNum = (s: string): number | null => {
  const t = toAscii(s).replace(/,/g, '').trim()
  if (t === '' || t === '.') return null
  const v = Number(t)
  return Number.isFinite(v) ? v : null
}

// Bangladesh uses lakh/crore grouping (1,23,456). en-IN gives the same grouping with Latin digits.
const group = new Intl.NumberFormat('en-IN', { maximumFractionDigits: 2 })

const MONTHS_BN = ['জানুয়ারি', 'ফেব্রুয়ারি', 'মার্চ', 'এপ্রিল', 'মে', 'জুন', 'জুলাই', 'আগস্ট', 'সেপ্টেম্বর', 'অক্টোবর', 'নভেম্বর', 'ডিসেম্বর']
const MONTHS_EN = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December']

export function makeFmt(digits: Digits) {
  const d = (s: string) => (digits === 'bn' ? toBn(s) : s)
  const months = digits === 'bn' ? MONTHS_BN : MONTHS_EN
  const parts = (iso: string) => {
    const [y, m, day] = iso.split('-').map(Number)
    return { y, m: m - 1, day }
  }
  return {
    digits,
    d,
    n: (x: number, dec = 0) => d(dec === 0 ? group.format(Math.round(x)) : group.format(Number(x.toFixed(dec)))),
    raw: (s: string) => d(s),
    money: (x: number) => '৳' + d(group.format(Math.round(x))),
    km: (x: number) => d(group.format(Math.round(x))) + (digits === 'bn' ? ' কিমি' : ' km'),
    liters: (x: number) => d(group.format(Number(x.toFixed(2)))) + (digits === 'bn' ? ' লিটার' : ' L'),
    date: (iso: string) => { const p = parts(iso); return `${d(String(p.day))} ${months[p.m]}` },
    dateFull: (iso: string) => { const p = parts(iso); return `${d(String(p.day))} ${months[p.m]} ${d(String(p.y))}` },
    month: (iso: string) => { const p = parts(iso); return `${months[p.m]} ${d(String(p.y))}` },
    days: (x: number) => d(String(x)) + (digits === 'bn' ? ' দিন' : ' days'),
  }
}
export type Fmt = ReturnType<typeof makeFmt>

export function useFmt(): Fmt {
  const digits = useStore((s) => s.settings.digits)
  return useMemo(() => makeFmt(digits), [digits])
}
