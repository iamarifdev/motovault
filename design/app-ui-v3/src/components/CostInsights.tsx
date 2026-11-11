import { useState } from 'react'
import copy from '../data/cost-copy.json'
import { useFmt } from '../lib/bn'
import { todayISO, addDays } from '../lib/dates'
import { Button, Chip } from './ui'
import { Sheet } from './Sheet'
import { Keypad, useEntry } from './entry'

type Expense = { id: number; category: number; poisha: number; date: string; note: string }
/** Isolated demo records never write owner data or the prototype's vehicle store. */
export function CostInsights({ recorded, onFilter }: { recorded: { date: string; category: number; amount: number; km: number }[]; onFilter: (period: number, category: number) => void }) {
  const c = copy.bn
  const f = useFmt()
  const money = (amount: number) => c.money.replace('%1$s', f.n(amount, 2))
  const [period, setPeriod] = useState(0)
  const [category, setCategory] = useState(0)
  const [expenses, setExpenses] = useState<Expense[]>([])
  const [sheet, setSheet] = useState<'expense' | 'budget' | 'details' | null>(null)
  const [budget, setBudget] = useState(0)
  const [allowance, setAllowance] = useState(0)
  const [undo, setUndo] = useState<Expense[] | null>(null)
  const [editing, setEditing] = useState<Expense | null>(null)
  const [entryCategory, setEntryCategory] = useState(4)
  const [date, setDate] = useState(todayISO())
  const [note, setNote] = useState('')
  const e = useEntry<'amount' | 'allowance'>({ amount: '', allowance: '' }, 'amount')
  const today = todayISO()
  const since = period === 0 ? addDays(today, -29) : period === 1 ? `${today.slice(0, 7)}-01` : ''
  const rows = [...recorded.map(r => ({ ...r, poisha: Math.round(r.amount * 100) })), ...expenses]
    .filter(r => r.date >= since && r.date <= today && (!category || r.category === category))
  const readings = recorded.filter(r => r.date >= since && r.date <= today).sort((a, b) => a.date.localeCompare(b.date) || a.km - b.km)
  const first = readings[0], last = readings.at(-1)
  const distance = first && last && last.date > first.date && last.km > first.km && readings.every((r, i) => !i || r.km >= readings[i - 1].km) ? last.km - first.km : null
  const coveredCost = first && last ? rows.filter(r => r.date > first.date && r.date <= last.date).reduce((sum, r) => sum + r.poisha, 0) : 0
  const valid = /^\d+(\.\d{1,2})?$/.test(e.vals.amount) && date <= today && /^\d{4}-\d{2}-\d{2}$/.test(date)
  const edit = (row: Expense | null) => {
    setEditing(row); setDate(row?.date ?? today); setNote(row?.note ?? ''); setEntryCategory(row?.category ?? 4)
    e.set('amount', row ? String(row.poisha / 100) : ''); e.setActive('amount'); setSheet('expense')
  }
  return <>
    <section className="card" style={{ padding: 16 }}>
      <h3>{c.title}</h3>
      <div className="row-wrap">{[c.recent, c.month, c.all].map((label, i) => <Chip key={label} active={period === i} onClick={() => { setPeriod(i); onFilter(i, category) }}>{label}</Chip>)}</div>
      <div className="row-wrap">{c.categories.map((label, i) => <Chip key={label} active={category === i} onClick={() => { setCategory(i); onFilter(period, i) }}>{label}</Chip>)}</div>
      <h2>{money(rows.reduce((s, r) => s + r.poisha, 0) / 100)}</h2>
      <p>{c.recorded}</p>{distance && first && last ? <><p>{c.coverage.replace('%1$s', f.n(distance)).replace('%2$s', f.date(first.date)).replace('%3$s', f.date(last.date))}</p><p>{c.perKm.replace('%1$s', money(coveredCost / 100 / distance))}</p><p>{c.ratioScope}</p></> : <p>{c.missing}</p>}
      <div className="row-wrap"><Button onClick={() => edit(null)}>{c.expense}</Button><Chip onClick={() => { e.set('amount', String(budget)); e.set('allowance', String(allowance)); setSheet('budget') }}>{c.budget}</Chip><Chip onClick={() => setSheet('details')}>{c.details}</Chip></div>
      {expenses.map(row => <div className="line-p" key={row.id}><button className="link" onClick={() => edit(row)}>{c.categories[row.category]} · {money(row.poisha / 100)}</button><button className="link danger" onClick={() => { setUndo(expenses); setExpenses(expenses.filter(r => r.id !== row.id)) }}>{c.delete}</button></div>)}
      {undo && <button className="link" onClick={() => { setExpenses(undo); setUndo(null) }}>{c.undo}</button>}
    </section>
    {sheet && <Sheet title={sheet === 'expense' ? c.expense : sheet === 'budget' ? c.budget : c.details} onClose={() => setSheet(null)} footer={sheet !== 'details' && <><Button full disabled={!valid} onClick={() => {
      if (sheet === 'budget') { setBudget(Number(e.vals.amount)); setAllowance(Number(e.vals.allowance) || 0) }
      else { setUndo(expenses); const row = { id: editing?.id ?? Date.now(), category: entryCategory, poisha: Math.round(Number(e.vals.amount) * 100), date, note }; setExpenses([...expenses.filter(r => r.id !== row.id), row]) }
      setSheet(null)
    }}>{c.save}</Button><Keypad onKey={e.press} /></>}>
      {sheet === 'details' ? <>{c.categories.slice(1).map((label, i) => <p className="line-p" key={label}><span>{label}</span><b>{money(rows.filter(r => r.category === i + 1).reduce((s, r) => s + r.poisha, 0) / 100)}</b></p>)}<p>{c.budget}: {money(budget)}</p><p>{c.allowance}: {money(allowance)}</p><p>{c.plan}</p></> : <>
        {sheet === 'expense' && <><div className="row-wrap">{c.categories.slice(4).map((label, i) => <Chip key={label} active={entryCategory === i + 4} onClick={() => setEntryCategory(i + 4)}>{label}</Chip>)}</div><label>{c.date}<input type="date" max={today} value={date} onChange={ev => setDate(ev.target.value)} /></label><label>{c.note}<input value={note} onChange={ev => setNote(ev.target.value)} /></label></>}
        <button className="field" onClick={() => e.setActive('amount')}>{c.amount}: {e.vals.amount}</button>
        {sheet === 'budget' && <><button className="field" onClick={() => e.setActive('allowance')}>{c.allowance}: {e.vals.allowance}</button><p>{c.plan}</p></>}
        {!valid && <p role="status">{c.invalid}</p>}
      </>}
    </Sheet>}
  </>
}
