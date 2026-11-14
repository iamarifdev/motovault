import { useEffect, useMemo, useRef, useState } from 'react'
import { AnimatePresence, motion } from 'motion/react'
import { ArrowLeft, Bike, Camera, Check, Mic, MicOff, Search, X } from 'lucide-react'
import { useStore, useUI } from '../store'
import { BRANDS, brandOf } from '../data/catalog'
import { addDays, todayISO } from '../lib/dates'
import { useFmt } from '../lib/bn'
import { parseSpokenNumber } from '../lib/spokenNumber'
import { haptic } from '../lib/haptics'
import { dur, ease, spring, stagger } from '../lib/motion'
import { startBanglaVoice, VOICE_UNAVAILABLE, type VoiceHandle } from '../lib/voiceCapture'
import { Button, Chip, IconButton } from '../components/ui'
import { Keypad, useEntry, useHardwareKeys, useShow } from '../components/entry'

type ServiceTiming = 'recent' | 'months' | 'unknown'
type CamState = 'off' | 'scanning' | 'done'

function defaultYearFor(range: [number, number]): number {
  const [minY, maxY] = range
  const target = new Date().getFullYear() - 2
  return Math.max(minY, Math.min(maxY, target))
}

/** Design-demo meter reading. The scan result is a draft: it is applied only when the rider taps "এই কিমি নিন". */
const OCR_SAMPLE_KM = 12450

/**
 * Zero-hassle 3-step setup: Brand, Model/Version (no technical spec overload),
 * and Odometer via Camera OCR, Bangla Voice, Keypad, or one-tap Skip.
 */
export function Onboarding() {
  const complete = useStore((s) => s.completeOnboarding)
  const setPreview = useUI((s) => s.setPreviewBrand)
  const addingBike = useUI((s) => s.addingBike)
  const f = useFmt()
  const show = useShow()
  const [step, setStep] = useState(0)
  const [brandId, setBrandId] = useState<string | null>(null)
  const [familyId, setFamilyId] = useState<string | null>(null)
  const [variantId, setVariantId] = useState<string | null>(null)
  const [releaseYear, setReleaseYear] = useState<number>(new Date().getFullYear() - 1)
  const [customMode, setCustomMode] = useState(false)
  const [customName, setCustomName] = useState('')
  const [customCc, setCustomCc] = useState('150')
  const [query, setQuery] = useState('')
  const [serviceTiming, setServiceTiming] = useState<ServiceTiming>('unknown')
  const [camState, setCamState] = useState<CamState>('off')
  const [listening, setListening] = useState(false)
  const [voiceNote, setVoiceNote] = useState('')
  const voiceRef = useRef<VoiceHandle | null>(null)
  const e = useEntry<'odo'>({ odo: '' }, 'odo')

  const brand = brandId ? brandOf(brandId) : null
  const models = useMemo(() => {
    const q = query.trim().toLowerCase()
    return (brand?.models ?? []).filter((m) => (
      !q ||
      m.name.toLowerCase().includes(q) ||
      m.variants.some((v) => v.name.toLowerCase().includes(q) || v.label.toLowerCase().includes(q))
    ))
  }, [brand, query])

  const selectedFamily = brand?.models.find((m) => m.id === familyId) ?? null
  const selectedVariant = selectedFamily?.variants.find((v) => v.id === variantId) ?? selectedFamily?.variants[0] ?? null

  const odo = Number(e.vals.odo) || 0
  useHardwareKeys(step === 2 && camState === 'off' ? e.press : () => {})

  useEffect(() => () => {
    setPreview(null)
    voiceRef.current?.stop()
  }, [setPreview])

  useEffect(() => {
    if (camState !== 'scanning') return
    const timer = window.setTimeout(() => {
      setCamState('done')
      haptic('success')
    }, 1100)
    return () => window.clearTimeout(timer)
  }, [camState])

  useEffect(() => {
    if (step !== 2) {
      setCamState('off')
      voiceRef.current?.stop()
      setListening(false)
    }
  }, [step])

  const pickBrand = (id: string) => {
    haptic('tick')
    setBrandId(id)
    setFamilyId(null)
    setVariantId(null)
    setCustomMode(false)
    setQuery('')
    setPreview(id)
    setStep(1)
  }

  const pickFamily = (mId: string) => {
    const fam = brand?.models.find((m) => m.id === mId)
    if (!fam) return
    haptic('tick')
    setCustomMode(false)
    setFamilyId(mId)
    const firstVar = fam.variants[0]
    setVariantId(firstVar.id)
    setReleaseYear(defaultYearFor(firstVar.years))
  }

  const pickVariant = (vId: string) => {
    if (!selectedFamily) return
    const v = selectedFamily.variants.find((x) => x.id === vId)
    if (!v) return
    haptic('tick')
    setVariantId(v.id)
    setReleaseYear(defaultYearFor(v.years))
  }

  const toggleVoice = () => {
    haptic('tick')
    if (listening) {
      voiceRef.current?.stop()
      setListening(false)
      return
    }
    setCamState('off')
    setVoiceNote('')
    setListening(true)
    voiceRef.current = startBanglaVoice({
      onResult: (transcript) => {
        const km = parseSpokenNumber(transcript)
        if (km !== null && km > 0 && km < 1_000_000) {
          e.set('odo', String(km))
          setVoiceNote(`শোনা গেছে: "${transcript}" · মিলিয়ে নিন`)
          haptic('success')
        } else {
          setVoiceNote('সংখ্যাটি বোঝা যায়নি, অনুগ্রহ করে আবার বলুন বা টাইপ করুন।')
        }
      },
      onEnd: () => {
        voiceRef.current = null
        setListening(false)
      },
      onError: (reason) => {
        setVoiceNote(reason === 'unavailable' ? VOICE_UNAVAILABLE : 'ভয়েস বুঝতে সমস্যা হয়েছে। আবার বলুন বা কিমি লিখুন।')
      },
    })
  }

  const applyScannedKm = () => {
    haptic('success')
    e.set('odo', String(OCR_SAMPLE_KM))
    setCamState('off')
  }

  const canProceedStep1 = customMode ? customName.trim().length > 0 : Boolean(selectedVariant)

  const finish = (finalOdo: number) => {
    if (!brand) return
    if (!customMode && !selectedVariant) return
    haptic('success')
    const today = todayISO()
    const chosenId = customMode ? `${brand.id}-custom` : selectedVariant!.id
    const chosenName = customMode ? customName.trim() : selectedVariant!.name
    const chosenCc = customMode ? (Number(customCc) || undefined) : selectedVariant!.cc
    const chosenLabel = customMode ? undefined : selectedVariant!.label
    const chosenSpecs = customMode ? undefined : selectedVariant!.specs

    const knowService = finalOdo > 0 && serviceTiming !== 'unknown'
    const lastServiceKm = serviceTiming === 'recent'
      ? Math.max(0, finalOdo - 300)
      : serviceTiming === 'months'
        ? Math.max(0, finalOdo - 1200)
        : undefined
    const lastServiceDate = serviceTiming === 'recent'
      ? addDays(today, -14)
      : serviceTiming === 'months'
        ? addDays(today, -60)
        : undefined

    complete({
      brandId: brand.id,
      modelId: chosenId,
      name: chosenName,
      year: releaseYear,
      cc: chosenCc,
      variantLabel: chosenLabel,
      specs: chosenSpecs,
      odometer: finalOdo,
      lastServiceKnown: knowService,
      lastServiceKm,
      lastServiceDate,
    })
    setPreview(null)
    useUI.getState().setTab('home')
  }

  return (
    <div className="onboard">
      <header className="ob-head">
        {step > 0
          ? <IconButton icon={ArrowLeft} label="পেছনে" onClick={() => setStep(step - 1)} />
          : addingBike
            ? <IconButton icon={X} label="বাতিল করুন" onClick={() => useUI.getState().setAddingBike(false)} />
            : <span className="ob-logo"><Bike size={26} aria-hidden /></span>}
        <div className="dots" aria-label={`ধাপ ${f.d(String(step + 1))} / ${f.d('3')}`}>
          {[0, 1, 2].map((i) => <motion.i key={i} className={i <= step ? 'on' : ''} animate={{ scaleX: i === step ? 1 : 0.9 }} transition={spring.press} />)}
        </div>
        <span className="cap">{f.d(String(step + 1))} / {f.d('3')}</span>
      </header>

      <AnimatePresence mode="wait" initial={false}>
        <motion.div key={step} className="ob-body" initial={{ opacity: 0, x: 28 }} animate={{ opacity: 1, x: 0, transition: { duration: dur.page, ease: ease.sky } }} exit={{ opacity: 0, x: -28, transition: { duration: 0.12 } }}>
          {step === 0 && (
            <>
              <h1>কোন বাইক চালান?</h1>
              <p className="lead">ব্র্যান্ড বেছে নিন। অ্যাপের রঙ আপনার বাইকের সঙ্গে মিলবে।</p>
              <div className="brand-grid">
                {BRANDS.map((b, i) => (
                  <motion.button key={b.id} type="button" className={`brand${brandId === b.id ? ' on' : ''}`} onClick={() => pickBrand(b.id)}
                    initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ ...spring.list, delay: stagger(i, 0.03) }} whileTap={{ scale: 0.95 }}>
                    <span className="brand-logo" style={{ ['--logo-url' as string]: `url(${b.logo})` }} aria-hidden />
                    <b>{b.name}</b>
                    <small>{f.d(String(b.models.length))}টি মডেল</small>
                  </motion.button>
                ))}
              </div>
              <p className="cap ob-foot">আপনার সব তথ্য এই ফোনেই থাকবে। কোনো অ্যাকাউন্ট লাগবে না।</p>
            </>
          )}

          {step === 1 && brand && (
            <>
              <h1>{brand.name} এর কোন মডেল?</h1>
              <p className="lead">মডেলটি খুঁজে ট্যাপ করুন।</p>

              <label className="search">
                <Search size={20} aria-hidden />
                <input value={query} onChange={(ev) => setQuery(ev.target.value)} placeholder="মডেল খুঁজুন, যেমন R15, FZS, Gixxer" aria-label="মডেল খুঁজুন" />
              </label>

              <div className="model-list">
                {models.map((m, i) => {
                  const isOpen = !customMode && selectedFamily?.id === m.id
                  const activeVar = isOpen ? selectedVariant! : m.variants[0]

                  return (
                    <motion.div key={m.id} className={`model-group${isOpen ? ' on' : ''}`}
                      initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ ...spring.list, delay: stagger(i, 0.03) }}>
                      <button type="button" className="model" onClick={() => pickFamily(m.id)}>
                        <span className="model-photo">
                          {(isOpen ? activeVar?.photo : m.photo)
                            ? <img src={isOpen ? activeVar?.photo : m.photo} alt="" />
                            : <Bike size={34} aria-hidden />}
                        </span>
                        <span style={{ display: 'flex', flexDirection: 'column', minWidth: 0 }}>
                          <b>{m.name}</b>
                          <span className="model-meta-row">
                            <small className="cap">{f.d(String(activeVar?.cc ?? m.cc))} cc</small>
                            {m.variants.length > 1 && (
                              <span className="model-var-pill">{f.d(String(m.variants.length))}টি ভার্সন</span>
                            )}
                          </span>
                        </span>
                        {isOpen && (
                          <motion.span className="model-check" initial={{ scale: 0 }} animate={{ scale: 1 }} transition={spring.press}>
                            <Check size={18} aria-hidden />
                          </motion.span>
                        )}
                      </button>

                      <AnimatePresence initial={false}>
                        {isOpen && activeVar && m.variants.length > 1 && (
                          <motion.div
                            className="model-expand"
                            initial={{ height: 0, opacity: 0 }}
                            animate={{ height: 'auto', opacity: 1, transition: { duration: dur.expand, ease: ease.expand } }}
                            exit={{ height: 0, opacity: 0, transition: { duration: 0.14 } }}
                          >
                            <div className="variant-showcase">
                              <div className="variant-stage">
                                {activeVar.photo ? (
                                  <img src={activeVar.photo} alt={activeVar.name} />
                                ) : (
                                  <div className="hero-bike-fallback">
                                    <Bike size={40} aria-hidden />
                                  </div>
                                )}
                              </div>
                            </div>
                            <div>
                              <span className="variant-group-label">ভার্সন বেছে নিন</span>
                              <div className="variant-chips">
                                {m.variants.map((v) => (
                                  <Chip key={v.id} active={activeVar.id === v.id} onClick={() => pickVariant(v.id)}>
                                    {v.label}
                                  </Chip>
                                ))}
                              </div>
                            </div>
                          </motion.div>
                        )}
                      </AnimatePresence>
                    </motion.div>
                  )
                })}
                {models.length === 0 && <p className="field-msg">তালিকায় পাওয়া যায়নি। নিচে নিজের মডেলের নাম লিখুন।</p>}
              </div>

              {customMode ? (
                <div className="custom-model-card">
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <b>নিজের বাইকের নাম লিখুন</b>
                    <button type="button" className="link" style={{ padding: 0 }} onClick={() => setCustomMode(false)}>তালিকা দেখুন</button>
                  </div>
                  <label className="mini">
                    <small>মডেলের নাম</small>
                    <input className="input" value={customName} onChange={(ev) => setCustomName(ev.target.value)} placeholder="যেমন Pulsar 150" />
                  </label>
                  <label className="mini">
                    <small>ইঞ্জিন সিসি (ঐচ্ছিক)</small>
                    <input className="input" inputMode="numeric" value={customCc} onChange={(ev) => setCustomCc(ev.target.value.replace(/\D/g, ''))} />
                  </label>
                </div>
              ) : (
                <button type="button" className="link" onClick={() => { haptic('tick'); setCustomMode(true); setFamilyId(null); setVariantId(null) }}>
                  আমার বাইক তালিকায় নেই
                </button>
              )}

              <Button full disabled={!canProceedStep1} onClick={() => setStep(2)}>
                {customMode
                  ? (customName.trim() ? `${customName.trim()} বেছে নিন` : 'মডেলের নাম লিখুন')
                  : selectedVariant
                    ? `${selectedVariant.name} বেছে নিন`
                    : 'একটি মডেল বেছে নিন'}
              </Button>
            </>
          )}

          {step === 2 && brand && (
            <>
              <h1>মিটারে এখন কত কিমি আছে?</h1>
              <div className="odo-hero big">
                <strong><span className={odo ? '' : 'ph'}>{show(e.vals.odo) || f.d('0')}</span><small>{f.digits === 'bn' ? 'কিমি' : 'km'}</small></strong>
              </div>

              <div className="capture-bar">
                <Button kind="secondary" size="sm" icon={Camera} onClick={() => { haptic('tick'); voiceRef.current?.stop(); setListening(false); setCamState(camState === 'off' ? 'scanning' : 'off') }}>
                  মিটারের ছবি তুলুন
                </Button>
                <Button kind="secondary" size="sm" icon={listening ? MicOff : Mic} onClick={toggleVoice}>
                  {listening ? 'শুনছে…' : 'মুখে বলুন'}
                </Button>
              </div>

              {voiceNote && <p className="field-msg" role="status" style={{ textAlign: 'center', fontWeight: 600, color: 'var(--brand)' }}>{voiceNote}</p>}

              <AnimatePresence initial={false}>
                {camState !== 'off' && (
                  <motion.div className="cam" initial={{ opacity: 0, scale: 0.96 }} animate={{ opacity: 1, scale: 1, transition: spring.sheet }} exit={{ opacity: 0, transition: { duration: 0.15, ease: ease.sky } }}>
                    <div className="cam-dash" aria-hidden>
                      <svg viewBox="0 0 320 140">
                        <rect x="6" y="6" width="308" height="128" rx="20" className="dash-body" />
                        <rect x="40" y="42" width="240" height="54" rx="8" className="lcd" />
                        <text x="160" y="84" textAnchor="middle" className="lcd-text">012450</text>
                        <text x="268" y="34" className="lcd-km">km</text>
                      </svg>
                      <div className="cam-reticle" />
                      {camState === 'scanning' && <motion.i className="scan-line" initial={{ y: 36 }} animate={{ y: [36, 102, 36] }} transition={{ duration: 1.2, ease: 'easeInOut', repeat: Infinity }} />}
                    </div>
                    {camState === 'scanning' ? (
                      <div className="cam-result">
                        <p className="cam-msg">মিটার স্ক্যান করা হচ্ছে…</p>
                        <Button size="sm" kind="secondary" onClick={() => setCamState('off')}>বাতিল করুন</Button>
                      </div>
                    ) : (
                      <div className="cam-result">
                        <p className="cam-msg">ডেমো ছবি · মিটারে পাওয়া গেছে: <b>{f.km(OCR_SAMPLE_KM)}</b></p>
                        <div className="row-wrap">
                          <Button size="sm" onClick={applyScannedKm}>এই কিমি নিন</Button>
                          <Button size="sm" kind="secondary" onClick={() => setCamState('off')}>নিজে লিখুন</Button>
                        </div>
                      </div>
                    )}
                  </motion.div>
                )}
              </AnimatePresence>

              <div>
                <span className="variant-group-label">শেষবার সার্ভিস কবে করিয়েছেন?</span>
                <div className="row-wrap">
                  <Chip active={serviceTiming === 'recent'} onClick={() => setServiceTiming('recent')}>এই মাসে</Chip>
                  <Chip active={serviceTiming === 'months'} onClick={() => setServiceTiming('months')}>২–৩ মাস আগে</Chip>
                  <Chip active={serviceTiming === 'unknown'} onClick={() => setServiceTiming('unknown')}>মনে নেই</Chip>
                </div>
              </div>

              <div className="stack">
                <Button full disabled={odo <= 0 || camState === 'scanning'} onClick={() => finish(odo)}>
                  {odo <= 0 ? 'কিমি দিন অথবা নিচে চাপুন' : 'শুরু করুন'}
                </Button>
                <button type="button" className="link" onClick={() => finish(0)}>
                  এখন মনে নেই · পরে দেব
                </button>
              </div>

              {camState === 'off' && <Keypad onKey={e.press} decimal={false} />}
            </>
          )}
        </motion.div>
      </AnimatePresence>
    </div>
  )
}
