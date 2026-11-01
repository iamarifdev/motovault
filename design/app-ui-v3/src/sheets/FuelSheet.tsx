import { useMemo, useState } from 'react'
import { AnimatePresence, motion } from 'motion/react'
import { Gauge as GaugeIcon, MapPin, Pencil } from 'lucide-react'
import { restoreData, snapshotData, useStore, useUI } from '../store'
import { FUEL_LABEL } from '../data/catalog'
import { addDays, todayISO } from '../lib/dates'
import { useFmt } from '../lib/bn'
import { estimateOdometer, fuelLogs } from '../lib/wear'
import { haptic } from '../lib/haptics'
import { spring } from '../lib/motion'
import type { FuelType } from '../types'
import { Sheet } from '../components/Sheet'
import { Button, Chip } from '../components/ui'
import { Keypad, useEntry, useHardwareKeys, useShow } from '../components/entry'

type Field = 'amount' | 'litres' | 'price' | 'odo'
const MAX_FUEL_LITRES = 100
const MAX_ODOMETER_KM = 9_999_999

/**
 * Fuel in two taps and one number. Riders think in taka ("৫০০ টাকার তেল"), so taka is the first field and litres follow.
 * Odometer is pre-filled with an estimate, date defaults to today, and "full tank" is on because most fill-ups are full.
 */
export function FuelSheet({ amount: preset, existingId }: { amount?: number; existingId?: string }) {
  const ride = useStore((s) => s.ride)!
  const logs = useStore((s) => s.logs)
  const lastPrice = useStore((s) => s.lastPrice)
  const places = useStore((s) => s.places)
  const addFuel = useStore((s) => s.addFuel)
  const updateFuel = useStore((s) => s.updateFuel)
  const { close, showToast } = useUI.getState()
  const f = useFmt()
  const show = useShow()
  const today = todayISO()

  const estimate = useMemo(() => estimateOdometer(ride, logs, today), [ride, logs, today])
  const existing = fuelLogs(logs).find((entry) => entry.id === existingId)
  const otherFuel = fuelLogs(logs).filter((entry) => entry.id !== existingId)
  const lastOdo = otherFuel.at(-1)?.odo ?? 0
  const latestFuelDate = fuelLogs(logs).reduce((latest, entry) => entry.date > latest ? entry.date : latest, '')

  const [fuel, setFuel] = useState<FuelType>(existing?.fuel ?? 'octane')
  const [mode, setMode] = useState<'taka' | 'litre'>('taka')
  const [full, setFull] = useState(existing?.full ?? true)
  const [dateKey, setDateKey] = useState<'today' | 'yesterday' | 'custom'>(existing ? 'custom' : 'today')
  const [custom, setCustom] = useState(existing?.date ?? today)
  const [place, setPlace] = useState(existing?.place ?? '')
  const [odoTouched, setOdoTouched] = useState(Boolean(existing))
  const [saveFailed, setSaveFailed] = useState(false)
  const e = useEntry<Field>({ amount: existing ? String(existing.amount) : preset ? String(preset) : '', litres: existing ? String(existing.liters) : '', price: String(existing?.price ?? lastPrice.octane), odo: String(existing?.odo ?? estimate) }, 'amount')

  const date = dateKey === 'today' ? today : dateKey === 'yesterday' ? addDays(today, -1) : custom
  const price = Number(e.vals.price) || 0
  const unchangedAmount = existing && Number(e.vals.amount) === existing.amount && price === existing.price
  const unchangedLitres = existing && Number(e.vals.litres) === existing.liters && price === existing.price
  const taka = mode === 'taka' ? Number(e.vals.amount) || 0 : unchangedLitres ? existing.amount : (Number(e.vals.litres) || 0) * price
  const liters = mode === 'taka' ? unchangedAmount ? existing.liters : (price > 0 ? taka / price : 0) : Number(e.vals.litres) || 0
  const odo = Number(e.vals.odo) || 0

  const ordered = [...otherFuel, ...(existing ? [{ ...existing, odo, date }] : [])].sort((a, b) => a.odo - b.odo)
  const editOrderInvalid = Boolean(existing) && ordered.some((entry, index) => index > 0 && (entry.odo <= ordered[index - 1].odo || entry.date < ordered[index - 1].date))
  const odoError = existing ? (editOrderInvalid ? 'জ্বালানির কিমি ও তারিখের ক্রম ঠিক করুন' : null) : odo <= lastOdo && lastOdo > 0 ? `শেষ জ্বালানির কিমি (${f.n(lastOdo)}) এর চেয়ে বেশি দিন` : null
  const literWarn = liters > 40 ? 'লিটার অনেক বেশি মনে হচ্ছে, মিলিয়ে নিন' : null
  const parsedDate = new Date(`${date}T00:00:00Z`)
  const dateValid = !Number.isNaN(parsedDate.getTime()) && parsedDate.toISOString().slice(0, 10) === date
  const reason = taka <= 0 ? (mode === 'taka' ? 'টাকার পরিমাণ লিখুন' : 'লিটার লিখুন')
    : price <= 0 ? 'প্রতি লিটারের দাম লিখুন'
      : liters > MAX_FUEL_LITRES ? `লিটার ${f.n(MAX_FUEL_LITRES)}-এর বেশি হতে পারবে না`
        : odo < 0 || odo > MAX_ODOMETER_KM ? 'সঠিক ওডোমিটার দিন'
          : odoError ? 'কিমি ঠিক করুন'
            : !dateValid ? 'সঠিক তারিখ দিন'
              : date > today ? 'ভবিষ্যতের তারিখ দেওয়া যাবে না'
                : !existing && latestFuelDate && date < latestFuelDate ? 'শেষ জ্বালানির তারিখ বা তার পরের দিন দিন' : existingId && !existing ? 'রেকর্ডটি আর পাওয়া যাচ্ছে না' : null

  useHardwareKeys(e.press)

  const setFuelType = (t: FuelType) => { setFuel(t); e.set('price', String(lastPrice[t])) }
  const bump = (n: number) => { e.set('odo', String(odo + n)); setOdoTouched(true) }

  const save = () => {
    if (reason) { haptic('error'); return }
    // Synthetic design state; no owner data is written when this fixture is enabled.
    if (new URLSearchParams(window.location.search).get('fuelSaveFailure') === '1') {
      setSaveFailed(true); haptic('error'); return
    }
    const snap = snapshotData()
    const values = { date, odo, amount: existing ? taka : Math.round(taka), liters: existing ? liters : Math.round(liters * 100) / 100, price, full, fuel, place }
    if (existing) updateFuel(existing.id, values)
    else addFuel(values)
    haptic('success'); close()
    showToast(existing ? 'জ্বালানির রেকর্ড ঠিক হয়েছে' : `${f.money(taka)} · ${f.liters(liters)} যোগ হয়েছে`, () => restoreData(snap))
  }

  const label = (k: Field) => (e.active === k ? 'active' : '')
  const amountText = mode === 'taka' ? e.vals.amount : e.vals.litres

  return (
    <Sheet title={existing ? 'জ্বালানির রেকর্ড ঠিক করুন' : 'জ্বালানি'} footer={
      <>
        {saveFailed && <p className="field-msg err" role="alert">সেভ করা যায়নি। তথ্য মুছে যায়নি, আবার চেষ্টা করুন।</p>}
        <Button full disabled={Boolean(reason)} onClick={save}>{reason ?? `${f.money(taka)} সেভ করুন`}</Button>
        <Keypad onKey={e.press} />
      </>
    }>
      <button type="button" className={`amount-card ${e.active === 'amount' || e.active === 'litres' ? 'active' : ''}`} onClick={() => e.setActive(mode === 'taka' ? 'amount' : 'litres')}>
        <span className="cap">{mode === 'taka' ? 'কত টাকার তেল?' : 'কত লিটার?'}</span>
        <span className="amount-line">
          <span className="cur">{mode === 'taka' ? '৳' : ''}</span>
          <span className={`big${amountText ? '' : ' ph'}`}>{show(amountText) || f.d('0')}</span>
          {(e.active === 'amount' || e.active === 'litres') && <motion.i className="caret" animate={{ opacity: [1, 0, 1] }} transition={{ duration: 1, repeat: Infinity }} />}
          <span className="cur">{mode === 'litre' ? 'লিটার' : ''}</span>
        </span>
        <span className="amount-sub">
          {mode === 'taka' ? <>≈ {f.d(liters.toFixed(2))} লিটার</> : <>≈ {f.money(taka)}</>}
          <span
            role="button" tabIndex={0} className={`pill ${label('price')}`} aria-label="প্রতি লিটারের দাম বদলান"
            onClick={(ev) => { ev.stopPropagation(); e.setActive('price'); haptic('tick') }}
            onKeyDown={(ev) => { if (ev.key === 'Enter') e.setActive('price') }}
          >
            {e.active === 'price' ? f.d(e.vals.price || '0') : f.money(price)}/{f.digits === 'bn' ? 'লি' : 'L'} <Pencil size={13} aria-hidden />
          </span>
        </span>
      </button>

      <div className="row-wrap">
        <Chip active={mode === 'taka'} onClick={() => { setMode('taka'); e.setActive('amount') }}>টাকা ধরে</Chip>
        <Chip active={mode === 'litre'} onClick={() => { setMode('litre'); e.setActive('litres') }}>লিটার ধরে</Chip>
        <span className="vr" />
        {(['octane', 'petrol'] as const).map((t) => <Chip key={t} active={fuel === t} onClick={() => setFuelType(t)}>{FUEL_LABEL[t]}</Chip>)}
      </div>
      {literWarn && <p className="field-msg warn" role="alert">{literWarn}</p>}

      <div className="row-wrap">
        <Chip active={full} onClick={() => setFull(!full)}>{full ? 'ট্যাংক পূর্ণ' : 'আংশিক ভরেছি'}</Chip>
        <Chip active={dateKey === 'today'} onClick={() => setDateKey('today')}>আজ</Chip>
        <Chip active={dateKey === 'yesterday'} onClick={() => setDateKey('yesterday')}>গতকাল</Chip>
        <Chip active={dateKey === 'custom'} onClick={() => setDateKey('custom')}>অন্য দিন</Chip>
      </div>
      <AnimatePresence initial={false}>
        {dateKey === 'custom' && (
          <motion.div key="date" initial={{ height: 0, opacity: 0 }} animate={{ height: 'auto', opacity: 1 }} exit={{ height: 0, opacity: 0 }} className="clip">
            <input className="input" type="date" max={today} value={custom} aria-label="তারিখ" onChange={(ev) => setCustom(ev.target.value)} />
          </motion.div>
        )}
      </AnimatePresence>

      <button type="button" className={`odo-row ${label('odo')}${odoError ? ' bad' : ''}`} onClick={() => e.setActive('odo')}>
        <GaugeIcon size={22} aria-hidden />
        <span className="odo-main">
          <small>ওডোমিটার</small>
          <b>{show(e.vals.odo) || f.d('0')} {f.digits === 'bn' ? 'কিমি' : 'km'}</b>
        </span>
        {!odoTouched && <span className="tag">আনুমানিক</span>}
      </button>
      {odoError && <p className="field-msg err" role="alert">{odoError}</p>}
      <AnimatePresence initial={false}>
        {e.active === 'odo' && (
          <motion.div key="bump" className="row-wrap clip" initial={{ height: 0, opacity: 0 }} animate={{ height: 'auto', opacity: 1 }} exit={{ height: 0, opacity: 0 }} transition={spring.list}>
            {[10, 50, 100].map((n) => <Chip key={n} onClick={() => bump(n)}>+{f.d(String(n))}</Chip>)}
            <Chip onClick={() => { e.set('odo', String(estimate)); setOdoTouched(false) }}>আনুমানিক</Chip>
          </motion.div>
        )}
      </AnimatePresence>

      <div className="row-wrap">
        <MapPin size={18} aria-hidden className="muted-icon" />
        {places.slice(0, 3).map((p) => <Chip key={p} active={place === p} onClick={() => setPlace(place === p ? '' : p)}>{p}</Chip>)}
      </div>
      <input className="input" placeholder="পাম্পের নাম (ঐচ্ছিক)" aria-label="পাম্পের নাম" value={place} maxLength={60} onChange={(ev) => setPlace(ev.target.value)} />
    </Sheet>
  )
}
