import { useEffect, useMemo, useRef, useState } from 'react'
import { AnimatePresence, motion } from 'motion/react'
import { Camera, Check, Mic, MicOff } from 'lucide-react'
import { restoreData, snapshotData, useStore, useUI } from '../store'
import { diffDays, todayISO } from '../lib/dates'
import { useFmt } from '../lib/bn'
import { parseSpokenNumber } from '../lib/spokenNumber'
import { estimateOdometer } from '../lib/wear'
import { haptic } from '../lib/haptics'
import { ease, spring } from '../lib/motion'
import { startBanglaVoice, VOICE_UNAVAILABLE, type VoiceHandle } from '../lib/voiceCapture'
import { Sheet } from '../components/Sheet'
import { Button, Chip, IconButton } from '../components/ui'
import { CountUp } from '../components/Gauge'
import { Keypad, useEntry, useHardwareKeys, useShow } from '../components/entry'
import captureCopy from '../data/capture-copy.json'

/**
 * The reading is pre-filled with an estimate from recent riding, so the common case is one tap on "ঠিক আছে".
 * Photo and voice readings pre-fill the field for fast 1-tap confirmation.
 */
export function OdometerSheet() {
  const ride = useStore((s) => s.ride)!
  const logs = useStore((s) => s.logs)
  const setOdometer = useStore((s) => s.setOdometer)
  const photoState = useUI((s) => s.odoPhotoPreview ?? 'clear')
  const { close, showToast } = useUI.getState()
  const f = useFmt()
  const show = useShow()
  const today = todayISO()
  const estimate = useMemo(() => estimateOdometer(ride, logs, today), [ride, logs, today])
  const e = useEntry<'odo'>({ odo: String(estimate) }, 'odo')
  const [touched, setTouched] = useState(false)
  const [cam, setCam] = useState<'off' | 'scanning' | 'done'>('off')
  const [candidate, setCandidate] = useState<number | null>(null)
  const [listening, setListening] = useState(false)
  const [voiceNote, setVoiceNote] = useState('')
  const voiceRef = useRef<VoiceHandle | null>(null)
  const c = captureCopy.bn
  const read = estimate + 3
  const odo = Number(e.vals.odo) || 0
  const bad = odo < ride.odometer ? `বর্তমান কিমির (${f.n(ride.odometer)}) চেয়ে কম হতে পারে না` : null
  const ago = diffDays(ride.odometerAt, today)

  useEffect(() => () => voiceRef.current?.stop(), [])

  useHardwareKeys((k) => { setTouched(true); e.press(k) })
  useEffect(() => {
    if (cam !== 'scanning') return
    const t = setTimeout(() => {
      setCam('done')
      if (photoState === 'clear') setCandidate(read)
      haptic('success')
    }, 1700)
    return () => clearTimeout(t)
  }, [cam, photoState, read])

  const toggleVoice = () => {
    haptic('tick')
    if (listening) {
      voiceRef.current?.stop()
      setListening(false)
      return
    }
    setCam('off')
    setVoiceNote('')
    setListening(true)
    voiceRef.current = startBanglaVoice({
      onResult: (transcript) => {
        const parsed = parseSpokenNumber(transcript)
        if (parsed === null || parsed <= 0) {
          setVoiceNote(`শোনা গেছে: "${transcript}" · কিমি বোঝা যায়নি, টাইপ করুন।`)
          return
        }
        e.set('odo', String(parsed))
        setTouched(true)
        setVoiceNote(`শোনা গেছে: "${transcript}" → ${f.km(parsed)} · মিলিয়ে নিন`)
        haptic('success')
      },
      onEnd: () => {
        voiceRef.current = null
        setListening(false)
      },
      onError: (reason) => {
        setVoiceNote(reason === 'unavailable' ? VOICE_UNAVAILABLE : 'কথা বোঝা যায়নি। আবার বলুন বা কিমি লিখুন।')
      },
    })
  }

  const save = (value = odo) => {
    if (value < ride.odometer) { haptic('error'); return }
    const snap = snapshotData()
    setOdometer(value); haptic('success'); close()
    showToast(`মিটার রিডিং ${f.km(value)} করা হয়েছে`, () => restoreData(snap))
  }
  const bump = (n: number) => { e.set('odo', String(odo + n)); setTouched(true); haptic('tick') }

  return (
    <Sheet title="মিটার রিডিং" actions={<>
      <IconButton icon={Camera} label={c.photo} tone={cam !== 'off' ? 'tint' : undefined} onClick={() => {
        voiceRef.current?.stop()
        setListening(false)
        setCandidate(null)
        setCam(cam === 'off' ? 'scanning' : 'off')
      }} />
      <IconButton icon={listening ? MicOff : Mic} label={listening ? 'শোনা বন্ধ করুন' : 'মুখে বলুন'} tone={listening ? 'tint' : undefined} onClick={toggleVoice} />
    </>} footer={
      <>
        <Button full disabled={Boolean(bad) || cam !== 'off'} icon={Check} onClick={() => save()}>{bad ? 'কিমি ঠিক করুন' : touched ? 'সেভ করুন' : 'ঠিক আছে, সেভ করুন'}</Button>
        {cam === 'off' && <Keypad onKey={(k) => { setTouched(true); e.press(k) }} decimal={false} />}
      </>
    }>

      {voiceNote && <p className="field-msg" role="status" style={{ fontWeight: 600, color: 'var(--brand)' }}>{voiceNote}</p>}

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
              <div className="cam-reticle" />
              {cam === 'scanning' && <motion.i className="scan-line" initial={{ y: 40 }} animate={{ y: [40, 110, 40] }} transition={{ duration: 1.4, ease: 'easeInOut', repeat: Infinity }} />}
            </div>
            {cam === 'scanning' ? <><p className="cam-msg">{c.reading}</p><Button size="sm" kind="secondary" onClick={() => setCam('off')}>{c.cancel}</Button></> : (
              <div className="cam-result">
                <p>ডেমো ছবি · {c.source}</p>
                <p className="cam-msg">{photoState === 'permission' ? c.denied : photoState === 'blur' ? c.empty : photoState === 'multiple' ? c.choice : c.review}</p>
                {(photoState === 'clear' || photoState === 'multiple') && <div className="row-wrap">{(photoState === 'multiple' ? [read, read + 100] : [read]).map(value => <Chip key={value} active={candidate === value} onClick={() => setCandidate(value)}>{f.km(value)}</Chip>)}</div>}
                <div className="row-wrap">
                  <Button size="sm" disabled={candidate === null} onClick={() => { if (candidate === null) return; e.set('odo', String(candidate)); setTouched(true); setCam('off') }}>{c.use}</Button>
                  <Button size="sm" kind="secondary" onClick={() => setCam('off')}>{c.manual}</Button>
                  <Button size="sm" kind="secondary" onClick={() => { setCandidate(null); setCam('scanning') }}>{c.retry}</Button>
                </div>
              </div>
            )}
          </motion.div>
        )}
      </AnimatePresence>
    </Sheet>
  )
}
