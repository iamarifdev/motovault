import { useEffect, useMemo, useRef, useState } from 'react'
import { motion } from 'motion/react'
import { Camera, Check, ChevronRight, Gauge as GaugeIcon, History, Map as MapIcon, MapPin, Mic, MicOff, Pencil, Plus } from 'lucide-react'
import { restoreData, snapshotData, useStore, useUI } from '../store'
import { FUEL_LABEL } from '../data/catalog'
import { addDays, todayISO } from '../lib/dates'
import { toAscii, useFmt } from '../lib/bn'
import { parseAmount } from '../lib/smartParse'
import { estimateOdometer, fuelLogs } from '../lib/wear'
import { haptic } from '../lib/haptics'
import { ease, spring } from '../lib/motion'
import { startBanglaVoice, VOICE_UNAVAILABLE, type VoiceHandle } from '../lib/voiceCapture'
import { parseSpokenNumber } from '../lib/spokenNumber'
import type { FuelType } from '../types'
import { Sheet } from '../components/Sheet'
import { Button, Chip, IconButton } from '../components/ui'
import { Keypad, useEntry, useHardwareKeys, useShow } from '../components/entry'

type Field = 'amount' | 'litres' | 'price' | 'odo'
type PumpCam = 'off' | 'scanning' | 'done'
const MAX_FUEL_LITRES = 100
const MAX_ODOMETER_KM = 9_999_999
/** Design-demo pump reading shown in the mock viewfinder. */
const PUMP_SCAN = { amount: 500, price: 130, litres: 3.85, fuel: 'octane' as FuelType }

/**
 * Fuel in one tap or one snap. Riders can scan the pump meter, speak in Bangla,
 * tap a Taka preset, or type — with zero confirmation-checkbox gates.
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
  const latestFuelDate = fuelLogs(logs).reduce((latest, entry) => (entry.date > latest ? entry.date : latest), '')

  const [fuel, setFuel] = useState<FuelType>(existing?.fuel ?? 'octane')
  const [mode, setMode] = useState<'taka' | 'litre'>('taka')
  // Default is a partial fill; tapping "ফুল ট্যাংক" overrides it.
  const [full, setFull] = useState(existing?.full ?? false)
  const [missedFill, setMissedFill] = useState(existing?.missedFill ?? false)
  const [priceConfirmedKey, setPriceConfirmedKey] = useState<string | null>(null)
  const [dateKey, setDateKey] = useState<'today' | 'yesterday' | 'custom'>(existing ? 'custom' : 'today')
  const [custom, setCustom] = useState(existing?.date ?? today)
  const [place, setPlace] = useState(existing?.place ?? '')
  const [odoTouched, setOdoTouched] = useState(Boolean(existing))
  const [saveFailed, setSaveFailed] = useState(false)
  const [pumpCam, setPumpCam] = useState<PumpCam>('off')
  const [pumpPicker, setPumpPicker] = useState(false)
  const [listening, setListening] = useState(false)
  const [voiceNote, setVoiceNote] = useState('')
  const voiceRef = useRef<VoiceHandle | null>(null)
  const e = useEntry<Field>({
    amount: existing ? String(existing.amount) : preset ? String(preset) : '',
    litres: existing ? String(existing.liters) : '',
    price: String(existing?.price ?? lastPrice.octane),
    odo: String(existing?.odo ?? estimate),
  }, 'amount')

  useEffect(() => () => voiceRef.current?.stop(), [])

  useEffect(() => {
    if (pumpCam !== 'scanning') return
    const timer = window.setTimeout(() => {
      setPumpCam('done')
      haptic('success')
    }, 1200)
    return () => window.clearTimeout(timer)
  }, [pumpCam])

  /** The scan is only a draft until the rider taps "এই হিসাব নিন"; the scanned price still needs a price check. */
  const applyPumpScan = () => {
    haptic('success')
    setFuel(PUMP_SCAN.fuel)
    setMode('taka')
    e.set('amount', String(PUMP_SCAN.amount))
    e.set('price', String(PUMP_SCAN.price))
    e.setActive('amount')
    setPumpCam('off')
  }

  const date = dateKey === 'today' ? today : dateKey === 'yesterday' ? addDays(today, -1) : custom
  const price = Number(e.vals.price) || 0
  const unchangedAmount = existing && Number(e.vals.amount) === existing.amount && price === existing.price
  const unchangedLitres = existing && Number(e.vals.litres) === existing.liters && price === existing.price
  const taka = mode === 'taka' ? Number(e.vals.amount) || 0 : unchangedLitres ? existing.amount : (Number(e.vals.litres) || 0) * price
  const liters = mode === 'taka' ? (unchangedAmount ? existing.liters : (price > 0 ? taka / price : 0)) : Number(e.vals.litres) || 0
  const odo = Number(e.vals.odo) || 0

  const ordered = [...otherFuel, ...(existing ? [{ ...existing, odo, date }] : [])].sort((a, b) => a.odo - b.odo)
  const editOrderInvalid = Boolean(existing) && ordered.some((entry, index) => index > 0 && (entry.odo <= ordered[index - 1].odo || entry.date < ordered[index - 1].date))
  const odoError = existing ? (editOrderInvalid ? 'ফুয়েলের কিমি ও তারিখের ক্রম ঠিক করুন' : null) : odo <= lastOdo && lastOdo > 0 ? `আগের বার তেল ভরার কিমি (${f.n(lastOdo)}) এর চেয়ে বেশি দিন` : null
  const literWarn = liters > 40 ? 'লিটার অনেক বেশি মনে হচ্ছে, মিলিয়ে নিন' : null
  const parsedDate = new Date(`${date}T00:00:00Z`)
  const dateValid = !Number.isNaN(parsedDate.getTime()) && parsedDate.toISOString().slice(0, 10) === date
  // A price matching the last confirmed price (or the unchanged edited record) needs no new check.
  const priceKnown = price === lastPrice[fuel] || Boolean(existing && existing.price === price && existing.fuel === fuel)
  const priceConfirmed = priceKnown || priceConfirmedKey === `${fuel}:${price}`
  const reason = taka <= 0 ? (mode === 'taka' ? 'টাকার পরিমাণ লিখুন' : 'লিটার লিখুন')
    : price <= 0 ? 'প্রতি লিটারের দাম লিখুন'
      : !priceConfirmed ? 'নতুন দাম কনফার্ম করুন'
      : !odoTouched ? 'মিটার রিডিং মিলিয়ে নিন'
      : liters > MAX_FUEL_LITRES ? `লিটার ${f.n(MAX_FUEL_LITRES)}-এর বেশি হতে পারবে না`
        : odo < 0 || odo > MAX_ODOMETER_KM ? 'সঠিক মিটার রিডিং দিন'
          : odoError ? 'কিমি ঠিক করুন'
            : !dateValid ? 'সঠিক তারিখ দিন'
              : date > today ? 'ভবিষ্যতের তারিখ দেওয়া যাবে না'
                : !existing && latestFuelDate && date < latestFuelDate ? 'শেষ ফুয়েলের তারিখ বা তার পরের দিন দিন' : existingId && !existing ? 'রেকর্ডটি আর পাওয়া যাচ্ছে না' : null

  const pressKey = (k: string) => { if (e.active === 'odo') setOdoTouched(true); e.press(k) }
  useHardwareKeys(pumpCam === 'off' && !pumpPicker ? pressKey : () => {})

  const setFuelType = (t: FuelType) => { setFuel(t); e.set('price', String(lastPrice[t])) }
  const bump = (n: number) => { e.set('odo', String(odo + n)); setOdoTouched(true) }

  const toggleVoice = () => {
    haptic('tick')
    if (listening) {
      voiceRef.current?.stop()
      setListening(false)
      return
    }
    setPumpCam('off')
    setVoiceNote('')
    setListening(true)
    voiceRef.current = startBanglaVoice({
      onResult: (transcript) => {
        const normalized = toAscii(transcript).toLowerCase()
        const parsed = parseAmount(transcript) ?? parseSpokenNumber(transcript)
        if (parsed === null || parsed <= 0) {
          setVoiceNote(`শোনা গেছে: "${transcript}" · টাকার অংক বোঝা যায়নি, টাইপ করুন।`)
          return
        }
        const detectedFuel: FuelType = /পেট্রোল|petrol/.test(normalized) ? 'petrol' : /অকটেন|octane/.test(normalized) ? 'octane' : fuel
        setMode('taka')
        if (detectedFuel !== fuel) setFuelType(detectedFuel)
        e.set('amount', String(Math.round(parsed)))
        e.setActive('amount')
        setVoiceNote(`শোনা গেছে: "${transcript}" · মিলিয়ে নিন`)
        haptic('success')
      },
      onEnd: () => {
        voiceRef.current = null
        setListening(false)
      },
      onError: (reason) => {
        setVoiceNote(reason === 'unavailable' ? VOICE_UNAVAILABLE : 'কথা বোঝা যায়নি। আবার বলুন বা প্রিসেট চাপুন।')
      },
    })
  }

  const save = () => {
    if (reason) { haptic('error'); return }
    // Synthetic design state; no owner data is written when this fixture is enabled.
    if (import.meta.env.DEV && new URLSearchParams(window.location.search).get('fuelSaveFailure') === '1') {
      setSaveFailed(true); haptic('error'); return
    }
    const snap = snapshotData()
    const values = { date, odo, amount: existing ? taka : Math.round(taka), liters: existing ? liters : Math.round(liters * 100) / 100, price, full, fuel, place, priceConfirmed: true, missedFill }
    if (existing) updateFuel(existing.id, values)
    else addFuel(values)
    haptic('success'); close()
    showToast(existing ? 'ফুয়েল রেকর্ড ঠিক হয়েছে' : `${f.money(taka)} · ${f.liters(liters)} যোগ হয়েছে`, () => restoreData(snap))
  }

  const label = (k: Field) => (e.active === k ? 'active' : '')
  const amountText = mode === 'taka' ? e.vals.amount : e.vals.litres
  const PRESET_AMOUNTS = [200, 300, 500, 1000]
  const applyPreset = (val: number) => {
    haptic('tick')
    setMode('taka')
    e.setActive('amount')
    e.set('amount', String(val))
  }

  const priceUnit = f.digits === 'bn' ? 'লি' : 'L'
  const switchMode = (m: 'taka' | 'litre') => { haptic('tick'); setMode(m); e.setActive(m === 'taka' ? 'amount' : 'litres') }
  const recentPumps = useMemo(() => {
    const seen = new Map<string, string>()
    ;[...fuelLogs(logs)].sort((a, b) => (a.date < b.date ? 1 : -1)).forEach((l) => { if (l.place && !seen.has(l.place)) seen.set(l.place, l.date) })
    places.forEach((p) => { if (!seen.has(p)) seen.set(p, '') })
    return [...seen.entries()].slice(0, 5)
  }, [logs, places])

  if (pumpPicker) {
    return (
      <Sheet title="কোন পাম্পে?" onClose={() => setPumpPicker(false)} closeLabel="পাম্প বাছাই বন্ধ করুন">
        <PumpPicker value={place} recent={recentPumps} onPick={(name) => { haptic('tick'); setPlace(name); setPumpPicker(false) }} />
      </Sheet>
    )
  }

  return (
    <Sheet title={existing ? 'ফুয়েল রেকর্ড ঠিক করুন' : 'তেল ভরা'} actions={pumpCam === 'off' ? <>
      <IconButton icon={Camera} label="পাম্প মিটারের ছবি তুলুন" onClick={() => { voiceRef.current?.stop(); setListening(false); setPumpCam('scanning') }} />
      <IconButton icon={listening ? MicOff : Mic} label={listening ? 'শোনা বন্ধ করুন' : 'মুখে বলুন'} tone={listening ? 'tint' : undefined} onClick={toggleVoice} />
    </> : null} footer={pumpCam === 'off' ? (
      <>
        {saveFailed && <p className="field-msg err" role="alert">সেভ করা যায়নি। তথ্য মুছে যায়নি, আবার চেষ্টা করুন।</p>}
        <Button full disabled={Boolean(reason)} onClick={save}>{reason ?? `${f.money(taka)} সেভ করুন`}</Button>
        <Keypad onKey={pressKey} />
      </>
    ) : undefined}>
      {pumpCam !== 'off' ? (
        <motion.div className="cam" initial={{ opacity: 0, scale: 0.96 }} animate={{ opacity: 1, scale: 1, transition: spring.sheet }} exit={{ opacity: 0, transition: { duration: 0.15, ease: ease.sky } }}>
          <div className="cam-dash" aria-hidden>
            <svg viewBox="0 0 320 144">
              <rect x="6" y="6" width="308" height="132" rx="18" className="dash-body" />
              <rect x="28" y="22" width="264" height="46" rx="8" className="lcd" />
              <text x="160" y="55" textAnchor="middle" className="lcd-text" style={{ fontSize: '30px', letterSpacing: '2px' }}>TK 500.00</text>
              <rect x="28" y="78" width="264" height="40" rx="8" className="lcd" />
              <text x="160" y="106" textAnchor="middle" className="lcd-text" style={{ fontSize: '24px', letterSpacing: '2px' }}>3.85 L  OCT</text>
            </svg>
            <div className="cam-reticle" />
            {pumpCam === 'scanning' && <motion.i className="scan-line" initial={{ y: 24 }} animate={{ y: [24, 114, 24] }} transition={{ duration: 1.2, ease: 'easeInOut', repeat: Infinity }} />}
          </div>
          {pumpCam === 'scanning' ? (
            <div className="cam-result">
              <p className="cam-msg">পাম্প মিটার স্ক্যান করা হচ্ছে…</p>
              <Button size="sm" kind="secondary" onClick={() => setPumpCam('off')}>বাতিল করুন</Button>
            </div>
          ) : (
            <div className="cam-result">
              <p className="cam-msg">ডেমো ছবি · টাকা: <b>{f.money(PUMP_SCAN.amount)}</b> · লিটার: <b>{f.d(String(PUMP_SCAN.litres))}</b> · {FUEL_LABEL[PUMP_SCAN.fuel]}</p>
              <div className="row-wrap">
                <Button size="sm" onClick={applyPumpScan}>এই হিসাব নিন</Button>
                <Button size="sm" kind="secondary" onClick={() => setPumpCam('scanning')}>আবার ছবি তুলুন</Button>
                <Button size="sm" kind="secondary" onClick={() => setPumpCam('off')}>বাতিল</Button>
              </div>
            </div>
          )}
        </motion.div>
      ) : (<>
        <div className={`amount-card compact ${e.active === 'amount' || e.active === 'litres' ? 'active' : ''}`}>
          <div className="amount-top">
            <span className="cap">{mode === 'taka' ? 'কত টাকার তেল?' : 'কত লিটার?'}</span>
            <span className="mode-seg" role="group" aria-label="কীভাবে লিখবেন">
              <button type="button" aria-pressed={mode === 'taka'} className={mode === 'taka' ? 'on' : ''} onClick={() => switchMode('taka')}>টাকা</button>
              <button type="button" aria-pressed={mode === 'litre'} className={mode === 'litre' ? 'on' : ''} onClick={() => switchMode('litre')}>লিটার</button>
            </span>
          </div>
          <button type="button" className="amount-line" aria-label={mode === 'taka' ? 'টাকার পরিমাণ' : 'লিটার'} onClick={() => e.setActive(mode === 'taka' ? 'amount' : 'litres')}>
            <span className="cur">{mode === 'taka' ? '৳' : ''}</span>
            <span className={`big${amountText ? '' : ' ph'}`}>{show(amountText) || f.d('0')}</span>
            {(e.active === 'amount' || e.active === 'litres') && <motion.i className="caret" animate={{ opacity: [1, 0, 1] }} transition={{ duration: 1, repeat: Infinity }} />}
            <span className="cur">{mode === 'litre' ? 'লিটার' : ''}</span>
          </button>
          <div className="amount-sub">
            <span>{mode === 'taka' ? <>≈ {f.d(liters.toFixed(2))} লিটার</> : <>≈ {f.money(taka)}</>}</span>
            <button type="button" className={`pill ${label('price')}`} aria-label="প্রতি লিটারের দাম বদলান" onClick={() => { e.setActive('price'); haptic('tick') }}>
              {e.active === 'price' ? f.d(e.vals.price || '0') : f.money(price)}/{priceUnit} <Pencil size={13} aria-hidden />
            </button>
            {!priceKnown && price > 0 && (
              <button type="button" className={`pill confirm${priceConfirmed ? ' active' : ''}`} aria-pressed={priceConfirmed}
                onClick={() => { haptic('tick'); setPriceConfirmedKey(priceConfirmed ? null : `${fuel}:${price}`) }}>
                {priceConfirmed ? <><Check size={13} aria-hidden /> দাম ঠিক</> : 'নতুন দাম · ঠিক?'}
              </button>
            )}
          </div>
          {(voiceNote || literWarn) && <p className={`field-msg${literWarn ? ' warn' : ''}`} role="status">{literWarn ?? voiceNote}</p>}
        </div>

        {mode === 'taka' && (
          <div className="chip-line" role="group" aria-label="দ্রুত টাকার অংক">
            {PRESET_AMOUNTS.map((amt) => <Chip key={amt} active={Number(e.vals.amount) === amt} onClick={() => applyPreset(amt)}>{f.money(amt)}</Chip>)}
          </div>
        )}

        <button type="button" className={`pump-field${place ? ' set' : ''}`} onClick={() => { haptic('tick'); setPumpPicker(true) }}>
          <MapPin size={18} aria-hidden />
          <span>{place || 'পাম্প বেছে নিন (ঐচ্ছিক)'}</span>
          <ChevronRight size={18} aria-hidden className="muted-icon" />
        </button>

        <div className={`odo-row compact ${label('odo')}${odoError ? ' bad' : ''}`}>
          <button type="button" className="odo-hit" onClick={() => e.setActive('odo')}>
            <GaugeIcon size={20} aria-hidden />
            <span className="odo-main">
              <small>{odoError ?? (odoTouched ? 'মিটার রিডিং' : 'মিটার রিডিং · আনুমানিক')}</small>
              <b>{show(e.vals.odo) || f.d('0')} {f.digits === 'bn' ? 'কিমি' : 'km'}</b>
            </span>
          </button>
          {e.active === 'odo'
            ? <span className="odo-bumps">{[10, 50, 100].map((n) => <button key={n} type="button" className="pill" onClick={() => bump(n)}>+{f.d(String(n))}</button>)}</span>
            : !odoTouched && <button type="button" className="pill confirm" onClick={() => { haptic('tick'); setOdoTouched(true) }}><Check size={13} aria-hidden /> ঠিক আছে</button>}
        </div>

        <div className="chip-line" role="group" aria-label="আরও তথ্য">
          <Chip active={full} onClick={() => setFull(!full)}>ফুল ট্যাংক</Chip>
          <span className="vr" />
          {(['octane', 'petrol'] as const).map((t) => <Chip key={t} active={fuel === t} onClick={() => setFuelType(t)}>{FUEL_LABEL[t]}</Chip>)}
          <span className="vr" />
          <Chip active={dateKey === 'today'} onClick={() => setDateKey('today')}>আজ</Chip>
          <Chip active={dateKey === 'yesterday'} onClick={() => setDateKey('yesterday')}>গতকাল</Chip>
          {dateKey === 'custom'
            ? <input className="chip chip-date on" type="date" max={today} value={custom} aria-label="তারিখ" onChange={(ev) => setCustom(ev.target.value)} />
            : <Chip onClick={() => setDateKey('custom')}>অন্য দিন</Chip>}
          <span className="vr" />
          <Chip active={missedFill} onClick={() => setMissedFill(!missedFill)}>মাঝের ফিল বাদ পড়েছে</Chip>
        </div>
      </>)}
    </Sheet>
  )
}

/** Demo stand-in for the Google Maps place picker (Compose uses the Places SDK). */
const NEARBY_DEMO = ['মেঘনা পেট্রোলিয়াম, তেজগাঁও', 'পদ্মা অয়েল, ফার্মগেট', 'যমুনা ফিলিং স্টেশন, মহাখালী', 'ট্রাস্ট ফিলিং স্টেশন, বনানী']

function PumpPicker({ value, recent, onPick }: { value: string; recent: [string, string][]; onPick: (name: string) => void }) {
  const f = useFmt()
  const [query, setQuery] = useState(value)
  const [map, setMap] = useState(false)
  const q = query.trim().toLowerCase()
  const shown = recent.filter(([name]) => !q || name.toLowerCase().includes(q))

  if (map) {
    return (
      <div className="map-pick">
        <div className="map-demo" aria-hidden>
          <i className="road r1" /><i className="road r2" /><i className="road r3" />
          {NEARBY_DEMO.map((_, i) => <MapPin key={i} className={`pin p${i}`} size={26} />)}
          <span className="map-tag">ডেমো ম্যাপ</span>
        </div>
        <p className="cap">কাছের পাম্প</p>
        <ul className="pick-list">
          {NEARBY_DEMO.map((name) => (
            <li key={name}><button type="button" onClick={() => onPick(name)}><MapPin size={18} aria-hidden /><span>{name}</span></button></li>
          ))}
        </ul>
        <Button kind="secondary" full onClick={() => setMap(false)}>ফিরে যান</Button>
      </div>
    )
  }

  return (
    <div className="map-pick">
      <input className="input" autoFocus value={query} maxLength={60} placeholder="পাম্পের নাম লিখুন বা খুঁজুন" aria-label="পাম্পের নাম" onChange={(ev) => setQuery(ev.target.value)} />
      <button type="button" className="pump-field map" onClick={() => { haptic('tick'); setMap(true) }}>
        <MapIcon size={18} aria-hidden /><span>ম্যাপ থেকে বেছে নিন</span><ChevronRight size={18} aria-hidden className="muted-icon" />
      </button>
      {shown.length > 0 && <p className="cap">সম্প্রতি গিয়েছেন</p>}
      <ul className="pick-list">
        {query.trim() && !recent.some(([name]) => name === query.trim()) && (
          <li><button type="button" onClick={() => onPick(query.trim())}><Plus size={18} aria-hidden /><span>"{query.trim()}" ব্যবহার করুন</span></button></li>
        )}
        {shown.map(([name, date]) => (
          <li key={name}><button type="button" className={name === value ? 'on' : ''} onClick={() => onPick(name)}>
            <History size={18} aria-hidden /><span>{name}{date && <small>শেষ: {f.date(date)}</small>}</span>
          </button></li>
        ))}
      </ul>
      {value && <Button kind="secondary" full onClick={() => onPick('')}>পাম্প মুছে দিন</Button>}
    </div>
  )
}
