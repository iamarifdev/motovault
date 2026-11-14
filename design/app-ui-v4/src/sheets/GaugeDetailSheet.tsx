import { useState } from 'react'
import { Pencil } from 'lucide-react'
import { restoreData, snapshotData, useStore, useUI } from '../store'
import { Sheet } from '../components/Sheet'
import { Button } from '../components/ui'
import { toAscii, useFmt } from '../lib/bn'
import { describeWear, wearOf } from '../lib/wear'
import { todayISO } from '../lib/dates'
import { haptic } from '../lib/haptics'

export function GaugeDetailSheet({ id }: { id: string }) {
  const ride = useStore((s) => s.ride)!
  const rule = useStore((s) => s.rules.find((r) => r.id === id))
  const resetRule = useStore((s) => s.resetRule)
  const setRuleLast = useStore((s) => s.setRuleLast)
  const { open, close, showToast } = useUI.getState()
  const f = useFmt()
  const today = todayISO()
  const known = rule && rule.baseline !== 'unknown'
  const [editing, setEditing] = useState(false)
  const [km, setKm] = useState(known ? String(rule.lastKm) : '')
  const [date, setDate] = useState(known ? rule.lastDate : today)

  if (!rule) return <Sheet title="সার্ভিসের হিসাব"><p>এই যন্ত্রাংশের তথ্য পাওয়া যাচ্ছে না।</p></Sheet>

  const wear = wearOf(rule, ride.odometer, today)
  const intervalParts: string[] = []
  if (rule.intervalKm > 0) intervalParts.push(f.km(rule.intervalKm))
  if (rule.intervalDays > 0) intervalParts.push(f.days(rule.intervalDays))

  const kmNum = Number(toAscii(km).replace(/[^\d]/g, ''))
  const editError = !km.trim() ? 'কত কিমিতে করেছিলেন লিখুন'
    : kmNum > ride.odometer ? `বর্তমান কিমি (${f.n(ride.odometer)}) এর চেয়ে বেশি হতে পারবে না`
      : !/^\d{4}-\d{2}-\d{2}$/.test(date) ? 'সঠিক তারিখ দিন'
        : date > today ? 'ভবিষ্যতের তারিখ দেওয়া যাবে না' : null

  const handleReset = () => {
    const snap = snapshotData()
    resetRule(rule.id)
    haptic('success')
    close()
    showToast('রিমাইন্ডার আজ থেকে নতুন করে শুরু হয়েছে', () => restoreData(snap))
  }

  const saveLast = () => {
    if (editError) { haptic('error'); return }
    const snap = snapshotData()
    setRuleLast(rule.id, kmNum, date)
    haptic('success')
    setEditing(false)
    showToast('শেষবারের তথ্য ঠিক হয়েছে', () => restoreData(snap))
  }

  return (
    <Sheet title={rule.name}>
      <div className="card-stack">
        <div className="setting">
          <b>{describeWear(wear, f)}</b>
          <p className="cap" style={{ margin: 0 }}>
            {wear.status === 'unknown'
              ? 'কবে করিয়েছেন জানালে সঠিক হিসাব শুরু হবে।'
              : `বর্তমান চলা: ${f.km(ride.odometer)}`}
          </p>
        </div>
        {editing ? (
          <div className="setting last-edit">
            <span>শেষবার করা হয়েছে</span>
            <div className="last-edit-row">
              <label className="last-edit-field">
                <small>কত কিমিতে</small>
                <input className="input" inputMode="numeric" autoFocus value={km} placeholder={f.n(ride.odometer)}
                  aria-label="শেষবার কত কিমিতে" onChange={(ev) => setKm(ev.target.value)} />
              </label>
              <label className="last-edit-field">
                <small>কোন তারিখে</small>
                <input className="input" type="date" max={today} value={date} aria-label="শেষবার কোন তারিখে" onChange={(ev) => setDate(ev.target.value)} />
              </label>
            </div>
            {editError && km.trim() && <p className="field-msg err" role="alert">{editError}</p>}
            <div className="row-wrap">
              <Button size="sm" disabled={Boolean(editError)} onClick={saveLast}>সেভ করুন</Button>
              <Button size="sm" kind="secondary" onClick={() => setEditing(false)}>বাতিল</Button>
            </div>
          </div>
        ) : (
          <button type="button" className="setting setting-edit" aria-label="শেষবার কবে করা হয়েছে, বদলান" onClick={() => { haptic('tick'); setEditing(true) }}>
            <span>
              <span>শেষবার করা হয়েছে</span>
              <p style={{ margin: 0 }}>
                {rule.baseline === 'unknown'
                  ? 'এখনো তথ্য দেওয়া হয়নি · লিখুন'
                  : `${f.km(rule.lastKm)} · ${f.dateFull(rule.lastDate)}`}
              </p>
            </span>
            <Pencil size={18} aria-hidden className="muted-icon" />
          </button>
        )}
        {intervalParts.length > 0 && (
          <div className="setting">
            <span>কত দিন পর পর</span>
            <p style={{ margin: 0 }}>প্রতি {intervalParts.join(' বা ')}</p>
          </div>
        )}
      </div>
      {!editing && <Button full onClick={handleReset}>আজ করিয়েছি · রিসেট করুন</Button>}
      {!editing && <Button full kind="secondary" onClick={() => open('service', { item: rule.item })}>সার্ভিস ও খরচ লিখুন</Button>}
    </Sheet>
  )
}
