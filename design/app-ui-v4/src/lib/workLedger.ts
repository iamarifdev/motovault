import type { LogEntry, WorkShift } from '../types'

export interface WorkSummary {
  shifts: number
  netEarningsPoisha: number
  allocatedExpensePoisha: number
  actualExpensePoisha: number
  netAfterAllocatedPoisha: number
  incompleteMileage: boolean
}

/**
 * Mirrors Compose WorkLedger.reconcile: each shift day takes the share of that day's
 * fuel and service cost that matches work km / total km for the day.
 */
export function summariseWork(shifts: WorkShift[], logs: LogEntry[]): WorkSummary {
  const costOn = (date: string) => logs
    .filter((l) => l.date === date && (l.type === 'fuel' || l.visitState !== 'quote'))
    .reduce((sum, l) => sum + Math.round((l.type === 'fuel' ? l.amount : l.total) * 100), 0)
  const days = new Map<string, { work: number; total: number }>()
  let incompleteMileage = false
  for (const s of shifts) {
    if (s.dayKm <= 0 || s.workKm > s.dayKm) incompleteMileage = true
    const d = days.get(s.date) ?? { work: 0, total: 0 }
    days.set(s.date, { work: d.work + s.workKm, total: Math.max(d.total, s.dayKm) })
  }
  let allocated = 0
  let actual = 0
  days.forEach(({ work, total }, date) => {
    const cost = costOn(date)
    actual += cost
    if (total > 0) allocated += Math.round(cost * Math.min(1, Math.max(0, work / total)))
  })
  const net = shifts.reduce((sum, s) => sum + s.grossPoisha - s.deductionPoisha, 0)
  return {
    shifts: shifts.length,
    netEarningsPoisha: net,
    allocatedExpensePoisha: allocated,
    actualExpensePoisha: actual,
    netAfterAllocatedPoisha: net - allocated,
    incompleteMileage,
  }
}
