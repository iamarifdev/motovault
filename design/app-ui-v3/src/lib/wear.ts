import type { FuelLog, LogEntry, Ride, Rule } from '../types'
import type { Fmt } from './bn'
import { diffDays } from './dates'

export type WearStatus = 'ok' | 'soon' | 'overdue'

export interface Wear {
  rule: Rule
  /** 0..100, how much of the interval is left. */
  life: number
  used: number
  status: WearStatus
  remainingKm: number | null
  remainingDays: number | null
  /** Which limit is closer: the card should talk about that one. */
  limit: 'km' | 'days'
}

export function wearOf(rule: Rule, odometer: number, today: string): Wear {
  const usedKm = rule.intervalKm > 0 ? (odometer - rule.lastKm) / rule.intervalKm : 0
  const usedDays = rule.intervalDays > 0 ? diffDays(rule.lastDate, today) / rule.intervalDays : 0
  const used = Math.max(usedKm, usedDays)
  const remainingKm = rule.intervalKm > 0 ? Math.round(rule.intervalKm - (odometer - rule.lastKm)) : null
  const remainingDays = rule.intervalDays > 0 ? rule.intervalDays - diffDays(rule.lastDate, today) : null
  return {
    rule,
    used,
    life: Math.max(0, Math.min(100, Math.round((1 - used) * 100))),
    status: used >= 1 ? 'overdue' : used >= 0.75 ? 'soon' : 'ok',
    remainingKm,
    remainingDays,
    limit: usedKm >= usedDays ? 'km' : 'days',
  }
}

export const byUrgency = (rules: Rule[], odometer: number, today: string) =>
  rules.map((r) => wearOf(r, odometer, today)).sort((a, b) => b.used - a.used)

/** Says how far overdue a part is, or how much is left. The old app printed "0 km left" under "overdue". */
export function describeWear(w: Wear, f: Fmt): string {
  if (w.status === 'overdue') {
    const km = w.remainingKm !== null && w.remainingKm < 0 ? -w.remainingKm : 0
    const days = w.remainingDays !== null && w.remainingDays < 0 ? -w.remainingDays : 0
    return w.limit === 'km' || km > 0 && days === 0 ? `${f.km(km)} পেরিয়েছে` : `${f.days(days)} পেরিয়েছে`
  }
  const parts: string[] = []
  if (w.remainingKm !== null) parts.push(f.km(w.remainingKm))
  if (w.remainingDays !== null) parts.push(f.days(w.remainingDays))
  return parts.join(' বা ') + ' বাকি'
}

export const statusLabel = (s: WearStatus) => (s === 'overdue' ? 'সার্ভিস দরকার' : s === 'soon' ? 'শিগগির' : 'ভালো আছে')

export function fuelLogs(logs: LogEntry[]): FuelLog[] {
  return logs.filter((l): l is FuelLog => l.type === 'fuel').sort((a, b) => a.odo - b.odo)
}

/** km/L for each completed full-to-full cycle, oldest first. Partial fills count toward the next full one. */
export function economySeries(logs: LogEntry[]): number[] {
  let baseOdo: number | null = null
  let litres = 0
  const out: number[] = []
  for (const l of fuelLogs(logs)) {
    if (baseOdo !== null) litres += l.liters
    if (l.full) {
      if (baseOdo !== null && litres > 0) out.push((l.odo - baseOdo) / litres)
      baseOdo = l.odo
      litres = 0
    }
  }
  return out
}

/** Weighted average over all completed cycles. */
export function economyOf(logs: LogEntry[]): number | null {
  let baseOdo: number | null = null
  let litres = 0, km = 0, liters = 0
  for (const l of fuelLogs(logs)) {
    if (baseOdo !== null) litres += l.liters
    if (l.full) {
      if (baseOdo !== null && litres > 0) { km += l.odo - baseOdo; liters += litres }
      baseOdo = l.odo
      litres = 0
    }
  }
  return liters > 0 ? km / liters : null
}

/** Best guess of today's odometer from recent riding. Shown as a one-tap confirmation, never saved silently. */
export function estimateOdometer(ride: Ride, logs: LogEntry[], today: string): number {
  const fuel = fuelLogs(logs)
  const last = fuel[fuel.length - 1]
  const first = fuel.length > 1 ? fuel[Math.max(0, fuel.length - 6)] : undefined
  let perDay = 25
  if (last && first && last.date !== first.date) perDay = Math.max(5, (last.odo - first.odo) / Math.max(1, diffDays(first.date, last.date)))
  const since = Math.max(0, diffDays(ride.odometerAt, today))
  return Math.round((ride.odometer + perDay * since) / 5) * 5
}
