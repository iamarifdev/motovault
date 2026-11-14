import { useState } from 'react'
import { Plus } from 'lucide-react'
import { useStore, useUI } from '../store'
import { useFmt } from '../lib/bn'
import { addDays, todayISO } from '../lib/dates'
import { haptic } from '../lib/haptics'
import type { ExtraExpense } from '../types'
import { Button, Chip } from './ui'
import { EXTRA_CATEGORIES } from '../sheets/ExpenseSheet'

const EMPTY_EXPENSES: ExtraExpense[] = []

const PERIODS = ['৩০ দিন', 'এই মাস', 'সব'] as const
const CATEGORIES = [
  { id: 0, label: 'সব' },
  { id: 1, label: 'তেল' },
  { id: 2, label: 'সার্ভিস' },
] as const

/** Clean summary of spending over fuel, service, and persisted extra expenses. */
export function CostInsights({
  recorded,
  onFilter,
}: {
  recorded: { date: string; category: number; amount: number; km?: number }[]
  onFilter: (period: number, category: number) => void
}) {
  const f = useFmt()
  const expenses = useStore((s) => s.extraExpenses) ?? EMPTY_EXPENSES
  const removeExtraExpense = useStore((s) => s.removeExtraExpense)
  const restoreExtraExpense = useStore((s) => s.restoreExtraExpense)
  const showToast = useUI((s) => s.showToast)
  const open = useUI((s) => s.open)
  const [period, setPeriod] = useState(0)
  const [category, setCategory] = useState(0)
  const today = todayISO()
  const since = period === 0 ? addDays(today, -29) : period === 1 ? `${today.slice(0, 7)}-01` : ''

  const rows = [
    ...recorded.map((r) => ({ ...r, poisha: Math.round(r.amount * 100) })),
    ...expenses,
  ].filter((r) => r.date >= since && r.date <= today && (!category || r.category === category))

  const totalPoisha = rows.reduce((s, r) => s + r.poisha, 0)
  const fuelSum = rows.filter((r) => r.category === 1).reduce((s, r) => s + r.poisha, 0)
  const svcSum = rows.filter((r) => r.category === 2).reduce((s, r) => s + r.poisha, 0)
  const otherSum = rows.filter((r) => r.category !== 1 && r.category !== 2).reduce((s, r) => s + r.poisha, 0)
  const pct = (v: number) => `${Math.max(4, Math.round((v / Math.max(1, totalPoisha)) * 100))}%`

  const openExpense = (row: ExtraExpense | null) => {
    haptic('tick')
    open('expense', row ? { id: row.id } : {})
  }

  return (
    <>
      <section className="card" style={{ padding: 16 }}>
        <div className="row-between">
          <h2>মোট খরচ</h2>
          <Button size="sm" kind="secondary" icon={Plus} onClick={() => openExpense(null)}>অন্যান্য খরচ যোগ</Button>
        </div>

        <div className="row-wrap" role="group" aria-label="সময়">
          {PERIODS.map((label, i) => (
            <Chip key={label} active={period === i} onClick={() => { haptic('tick'); setPeriod(i); onFilter(i, category) }}>{label}</Chip>
          ))}
        </div>

        <div className="row-wrap" role="group" aria-label="খরচের ধরন">
          {CATEGORIES.map((c) => (
            <Chip key={c.id} active={category === c.id} onClick={() => { haptic('tick'); setCategory(c.id); onFilter(period, c.id) }}>{c.label}</Chip>
          ))}
        </div>

        <p style={{ fontSize: '1.75em', fontWeight: 700, margin: '2px 0' }}>{f.money(totalPoisha / 100)}</p>

        {totalPoisha > 0 && (
          <div className="cost-stack" aria-label="খরচের অনুপাত">
            <div className="cost-bar">
              {fuelSum > 0 && <i style={{ width: pct(fuelSum), background: 'var(--brand)' }} />}
              {svcSum > 0 && <i style={{ width: pct(svcSum), background: 'var(--ok)' }} />}
              {otherSum > 0 && <i style={{ width: pct(otherSum), background: 'var(--warn)' }} />}
            </div>
            <div className="cost-legend">
              {fuelSum > 0 && <span><i style={{ background: 'var(--brand)' }} />তেল {f.money(fuelSum / 100)}</span>}
              {svcSum > 0 && <span><i style={{ background: 'var(--ok)' }} />সার্ভিস {f.money(svcSum / 100)}</span>}
              {otherSum > 0 && <span><i style={{ background: 'var(--warn)' }} />অন্যান্য {f.money(otherSum / 100)}</span>}
            </div>
          </div>
        )}

        {expenses.map((row) => {
          const catLabel = EXTRA_CATEGORIES.find((c) => c.id === row.category)?.label ?? 'অন্যান্য'
          return (
            <div className="line-p" key={row.id}>
              <button type="button" className="link" style={{ padding: '4px 0' }} onClick={() => openExpense(row)}>
                {catLabel}{row.note ? ` (${row.note})` : ''} · {f.money(row.poisha / 100)}
              </button>
              <button type="button" className="link danger" style={{ padding: '4px 0' }} onClick={() => {
                haptic('warning')
                const removed = removeExtraExpense(row.id)
                if (removed) showToast('খরচ মুছে গেছে', () => restoreExtraExpense(removed.entry, removed.index))
              }}>
                মুছুন
              </button>
            </div>
          )
        })}
      </section>

    </>
  )
}
