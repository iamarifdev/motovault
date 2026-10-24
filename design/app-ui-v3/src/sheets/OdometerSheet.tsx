import { useEffect, useMemo, useState } from 'react'
import { AnimatePresence, motion } from 'motion/react'
import { Camera, Check } from 'lucide-react'
import { restoreData, snapshotData, useStore, useUI } from '../store'
import { diffDays, todayISO } from '../lib/dates'
import { useFmt } from '../lib/bn'
import { estimateOdometer } from '../lib/wear'
import { haptic } from '../lib/haptics'
import { ease, spring } from '../lib/motion'
import { Sheet } from '../components/Sheet'
import { Button, Chip } from '../components/ui'
import { CountUp } from '../components/Gauge'
import { Keypad, useEntry, useHardwareKeys, useShow } from '../components/entry'

/**
 * The reading is pre-filled with an estimate from recent riding, so the common case is one tap on "ঠিক আছে".
 * Photo reading is a mock here (a real build would use the camera and on-device text recognition) and always asks for confirmation.
 */
export function OdometerSheet() {
  const ride = useStore((s) => s.ride)!
  const logs = useStore((s) => s.logs)
  const setOdometer = useStore((s) => s.setOdometer)
  const { close, showToast } = useUI.getState()
  const f = useFmt()
  const show = useShow()
  const today = todayISO()
  const estimate = useMemo(() => estimateOdometer(ride, logs, today), [ride, logs, today])
  const e = useEntry<'odo'>({ odo: String(estimate) }, 'odo')
  const [touched, setTouched] = useState(false)
  const [cam, setCam] = useState<'off' | 'scanning' | 'done'>('off')
  const read = estimate + 3
  const odo = Number(e.vals.odo) || 0
  const bad = odo < ride.odometer ? `বর্তমান কিমির (${f.n(ride.odometer)}) চেয়ে কম হতে পারে না` : null
  const ago = diffDays(ride.odometerAt, today)

  useHardwareKeys((k) => { setTouched(true); e.press(k) })
  useEffect(() => {
    if (cam !== 'scanning') return
    const t = setTimeout(() => { setCam('done'); haptic('success') }, 1700)
    return () => clearTimeout(t)
  }, [cam])

  const save = (value = odo) => {
    if (value < ride.odometer) { haptic('error'); return }
    const snap = snapshotData()
    setOdometer(value); haptic('success'); close()
    showToast(`ওডোমিটার ${f.km(value)} করা হয়েছে`, () => restoreData(snap))
  }
  const bump = (n: number) => { e.set('odo', String(odo + n)); setTouched(true); haptic('tick') }

  return (
    <Sheet title="ওডোমিটার" footer={
      <>
        <Button full disabled={Boolean(bad)} icon={Check} onClick={() => save()}>{bad ? 'কিমি ঠিক করুন' : touched ? 'সেভ করুন' : 'ঠিক আছে, সেভ করুন'}</Button>
        {cam === 'off' && <Keypad onKey={(k) => { setTouched(true); e.press(k) }} decimal={false} />}
      </>
    }>
      <div className="odo-hero">
        <span className="cap">{touched ? 'আপনার লেখা কিমি' : 'আজ আনুমানিক'}</span>
        <strong><CountUp value={odo} format={(n) => show(String(Math.round(n)))} /><small>{f.digits === 'bn' ? 'কিমি' : 'km'}</small></strong>
        <span className="cap">শেষ আপডেট: {ago === 0 ? 'আজ' : `${f.days(ago)} আগে`} · {f.km(ride.odometer)}</span>
      </div>
      {bad && <p className="field-msg err" role="alert">{bad}</p>}
      <div className="row-wrap">
        {[10, 50, 100, 500].map((n) => <Chip key={n} onClick={() => bump(n)}>+{f.d(String(n))}</Chip>)}
        <Chip onClick={() => { e.set('odo', String(estimate)); setTouched(false) }}>আনুমানিক</Chip>
      </div>
      <Button kind="secondary" icon={Camera} full onClick={() => { setCam('scanning') }}>ডিসপ্লের ছবি তুলে পড়ুন (ডেমো)</Button>

      <AnimatePresence>
        {cam !== 'off' && (
          <motion.div className="cam" initial={{ opacity: 0, scale: 0.96 }} animate={{ opacity: 1, scale: 1, transition: spring.sheet }} exit={{ opacity: 0, transition: { duration: 0.15, ease: ease.sky } }}>
            <div className="cam-dash" aria-hidden>
              <svg viewBox="0 0 320 150">
                <rect x="6" y="6" width="308" height="138" rx="20" className="dash-body" />
                <rect x="40" y="48" width="240" height="56" rx="8" className="lcd" />
                <text x="160" y="92" textAnchor="middle" className="lcd-text">{String(read).padStart(6, '0')}</text>
                <text x="268" y="40" className="lcd-km">km</text>
              </svg>
              {cam === 'scanning' && <motion.i className="scan-line" initial={{ y: 40 }} animate={{ y: [40, 110, 40] }} transition={{ duration: 1.4, ease: 'easeInOut', repeat: Infinity }} />}
            </div>
            {cam === 'scanning' ? <p className="cam-msg">পড়া হচ্ছে… ফোন স্থির রাখুন</p> : (
              <div className="cam-result">
                <p className="cam-msg">পড়া হয়েছে: <b>{f.n(read)} {f.digits === 'bn' ? 'কিমি' : 'km'}</b></p>
                <div className="row-wrap">
                  <Button size="sm" onClick={() => { e.set('odo', String(read)); setTouched(true); setCam('off') }}>ঠিক আছে</Button>
                  <Button size="sm" kind="secondary" onClick={() => setCam('off')}>নিজে লিখব</Button>
                </div>
              </div>
            )}
          </motion.div>
        )}
      </AnimatePresence>
    </Sheet>
  )
}
