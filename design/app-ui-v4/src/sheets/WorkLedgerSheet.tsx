import { useState } from 'react'
import { useStore, useUI } from '../store'
import { Sheet } from '../components/Sheet'
import { Button, Field, Switch } from '../components/ui'
import { parseNum, useFmt } from '../lib/bn'
import { todayISO } from '../lib/dates'
import { haptic } from '../lib/haptics'
import { summariseWork } from '../lib/workLedger'

const EMPTY = { gross: '', deduction: '', workKm: '', dayKm: '' }

/** P14 rider work ledger, matching Compose WorkLedgerSection: off by default, shifts kept when turned off. */
export function WorkLedgerSheet() {
  const workMode = useStore((s) => Boolean(s.settings.workMode))
  const shifts = useStore((s) => s.workShifts) ?? []
  const logs = useStore((s) => s.logs)
  const setSetting = useStore((s) => s.setSetting)
  const addWorkShift = useStore((s) => s.addWorkShift)
  const removeShift = (id: string) => useStore.setState((s) => ({ workShifts: (s.workShifts ?? []).filter((x) => x.id !== id) }))
  const showToast = useUI((s) => s.showToast)
  const f = useFmt()
  const [form, setForm] = useState(EMPTY)
  const summary = summariseWork(shifts, logs)

  const gross = parseNum(form.gross)
  const deduction = form.deduction.trim() ? parseNum(form.deduction) : 0
  const workKm = parseNum(form.workKm)
  const dayKm = parseNum(form.dayKm)
  const reason = gross === null || gross <= 0 ? 'শিফটের আয় লিখুন'
    : deduction === null || deduction < 0 || deduction > gross ? 'কর্তন ঠিক করুন'
      : workKm === null || workKm < 0 ? 'কাজের দূরত্ব লিখুন'
        : dayKm === null || dayKm < workKm ? 'দিনের মোট দূরত্ব কাজের দূরত্বের চেয়ে কম হতে পারবে না'
          : null

  const save = () => {
    if (reason || gross === null || workKm === null || dayKm === null) { haptic('error'); return }
    const id = addWorkShift({ date: todayISO(), grossPoisha: Math.round(gross * 100), deductionPoisha: Math.round((deduction ?? 0) * 100), workKm, dayKm })
    haptic('success')
    setForm(EMPTY)
    showToast(`শিফট সেভ হয়েছে · ${f.money(gross)}`, () => removeShift(id))
  }

  const set = (k: keyof typeof EMPTY) => (ev: { target: { value: string } }) => setForm((v) => ({ ...v, [k]: ev.target.value }))

  return (
    <Sheet title="রাইডার ওয়ার্ক লেজার" detent="tall" footer={workMode ? <Button full disabled={Boolean(reason)} onClick={save}>{reason ?? 'শিফট সেভ করুন'}</Button> : undefined}>
      <Switch checked={workMode} onChange={(v) => setSetting('workMode', v)} label="ওয়ার্ক মোড চালু করুন" sub="যে কোনো সময় বন্ধ করলে পুরোনো হিসাব রেখেই এন্ট্রি সরিয়ে নেওয়া যাবে।" />
      {workMode && <>
        <div className="card-stack"><div className="setting">
          <b>সমন্বিত হিসাব (বর্তমান বাইক)</b>
          <p className="cap" style={{ margin: 0 }}>বণ্টনকৃত খরচের পর নিট: {f.money(summary.netAfterAllocatedPoisha / 100)}</p>
          <p className="cap" style={{ margin: 0 }}>কাজের ভাগের তেল ও সার্ভিস খরচ: {f.money(summary.allocatedExpensePoisha / 100)} (মোট প্রকৃত: {f.money(summary.actualExpensePoisha / 100)})</p>
          {summary.incompleteMileage && <p className="field-msg warn">কিছু শিফটে দূরত্বের তথ্য পূর্ণ নেই; বণ্টন আংশিক।</p>}
        </div></div>
        <Field label="শিফটের মোট আয় (টাকা)"><input className="input" inputMode="decimal" value={form.gross} onChange={set('gross')} /></Field>
        <Field label="প্ল্যাটফর্ম কর্তন (টাকা)"><input className="input" inputMode="decimal" value={form.deduction} onChange={set('deduction')} /></Field>
        <Field label="কাজের দূরত্ব (কিমি)"><input className="input" inputMode="decimal" value={form.workKm} onChange={set('workKm')} /></Field>
        <Field label="দিনের মোট দূরত্ব (কিমি)"><input className="input" inputMode="decimal" value={form.dayKm} onChange={set('dayKm')} /></Field>
      </>}
      {!workMode && shifts.length > 0 && <p className="cap">{f.d(String(shifts.length))}টি শিফট সংরক্ষিত আছে।</p>}
    </Sheet>
  )
}
