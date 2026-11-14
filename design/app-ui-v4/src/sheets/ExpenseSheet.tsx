import { useState } from 'react'
import { useStore, useUI } from '../store'
import { useFmt } from '../lib/bn'
import { addDays, todayISO } from '../lib/dates'
import { haptic } from '../lib/haptics'
import { Sheet } from '../components/Sheet'
import { Button, Chip } from '../components/ui'
import { Keypad, useEntry, useHardwareKeys, useShow } from '../components/entry'

export const EXTRA_CATEGORIES = [
  { id: 3, label: 'পার্কিং' },
  { id: 4, label: 'টোল' },
  { id: 5, label: 'অন্যান্য' },
] as const

/** Parking, toll and other costs. Rendered as an app-level sheet so it covers the tab bar like every other sheet. */
export function ExpenseSheet({ id }: { id?: number }) {
  const editing = useStore((s) => (id === undefined ? undefined : (s.extraExpenses ?? []).find((r) => r.id === id)))
  const addExtraExpense = useStore((s) => s.addExtraExpense)
  const updateExtraExpense = useStore((s) => s.updateExtraExpense)
  const removeExtraExpense = useStore((s) => s.removeExtraExpense)
  const { close, showToast } = useUI.getState()
  const f = useFmt()
  const show = useShow()
  const today = todayISO()
  const yesterday = addDays(today, -1)
  const [category, setCategory] = useState(editing?.category ?? 5)
  const [date, setDate] = useState(editing?.date ?? today)
  const [dateKey, setDateKey] = useState<'today' | 'yesterday' | 'custom'>(!editing || editing.date === today ? 'today' : editing.date === yesterday ? 'yesterday' : 'custom')
  const [note, setNote] = useState(editing?.note ?? '')
  const [typingNote, setTypingNote] = useState(false)
  const e = useEntry<'amount'>({ amount: editing ? String(Math.round(editing.poisha / 100)) : '' }, 'amount')
  useHardwareKeys(typingNote ? () => {} : e.press)

  const amount = Number(e.vals.amount) || 0
  const realDate = dateKey === 'today' ? today : dateKey === 'yesterday' ? yesterday : date
  const reason = amount <= 0 ? 'টাকার পরিমাণ লিখুন'
    : !/^\d{4}-\d{2}-\d{2}$/.test(realDate) ? 'সঠিক তারিখ দিন'
      : realDate > today ? 'ভবিষ্যতের তারিখ দেওয়া যাবে না' : null

  const save = () => {
    if (reason) { haptic('error'); return }
    const payload = { category, poisha: Math.round(amount * 100), date: realDate, note: note.trim() }
    if (editing) {
      updateExtraExpense(editing.id, payload)
      showToast('খরচ ঠিক করা হয়েছে')
    } else {
      const newId = addExtraExpense(payload)
      showToast(`${f.money(amount)} খরচ যোগ হয়েছে`, () => removeExtraExpense(newId))
    }
    haptic('success')
    close()
  }

  return (
    <Sheet title={editing ? 'খরচ ঠিক করুন' : 'অন্যান্য খরচ যোগ'} footer={
      <>
        <Button full disabled={Boolean(reason)} onClick={save}>{reason ?? `${f.money(amount)} সেভ করুন`}</Button>
        {!typingNote && <Keypad onKey={e.press} decimal={false} />}
      </>
    }>
      <div className="chip-line" role="group" aria-label="খরচের ধরন">
        {EXTRA_CATEGORIES.map((c) => <Chip key={c.id} active={category === c.id} onClick={() => setCategory(c.id)}>{c.label}</Chip>)}
      </div>

      <button type="button" className={`amount-card compact${typingNote ? '' : ' active'}`} onClick={() => setTypingNote(false)}>
        <span className="cap">কত টাকা খরচ হয়েছে?</span>
        <span className="amount-line">
          <span className="cur">৳</span>
          <span className={`big${e.vals.amount ? '' : ' ph'}`}>{show(e.vals.amount) || f.d('0')}</span>
        </span>
      </button>

      <div className="chip-line" role="group" aria-label="তারিখ">
        <Chip active={dateKey === 'today'} onClick={() => setDateKey('today')}>আজ</Chip>
        <Chip active={dateKey === 'yesterday'} onClick={() => setDateKey('yesterday')}>গতকাল</Chip>
        {dateKey === 'custom'
          ? <input className="chip chip-date on" type="date" max={today} value={date} aria-label="তারিখ" onChange={(ev) => setDate(ev.target.value)} />
          : <Chip onClick={() => setDateKey('custom')}>অন্য দিন</Chip>}
      </div>

      <input className="input input-sm" value={note} maxLength={80} placeholder="নোট (ঐচ্ছিক) · যেমন: ব্রিজ টোল" aria-label="নোট"
        onFocus={() => setTypingNote(true)} onBlur={() => setTypingNote(false)} onChange={(ev) => setNote(ev.target.value)} />
    </Sheet>
  )
}
