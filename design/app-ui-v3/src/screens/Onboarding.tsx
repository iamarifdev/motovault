import { useEffect, useMemo, useState } from 'react'
import { AnimatePresence, motion } from 'motion/react'
import { ArrowLeft, Bike, Check, Search } from 'lucide-react'
import { useStore, useUI } from '../store'
import { BRANDS, brandOf } from '../data/catalog'
import { useFmt } from '../lib/bn'
import { haptic } from '../lib/haptics'
import { dur, ease, spring, stagger } from '../lib/motion'
import { Button, Chip, IconButton } from '../components/ui'
import { Keypad, useEntry, useHardwareKeys, useShow } from '../components/entry'

/**
 * Three inputs: brand, model, odometer. Market and emission standard are gone (nobody in Bangladesh needs BS4/Euro 5 here),
 * year defaults sensibly, and "last service" can be unknown so nobody is blocked.
 */
export function Onboarding() {
  const complete = useStore((s) => s.completeOnboarding)
  const setPreview = useUI((s) => s.setPreviewBrand)
  const f = useFmt()
  const show = useShow()
  const [step, setStep] = useState(0)
  const [brandId, setBrandId] = useState<string | null>(null)
  const [modelId, setModelId] = useState<string | null>(null)
  const [query, setQuery] = useState('')
  const [knows, setKnows] = useState(false)
  const e = useEntry<'odo'>({ odo: '' }, 'odo')
  const brand = brandId ? brandOf(brandId) : null
  const model = brand?.models.find((m) => m.id === modelId)
  const models = useMemo(() => brand?.models.filter((m) => m.name.toLowerCase().includes(query.toLowerCase())) ?? [], [brand, query])
  const odo = Number(e.vals.odo) || 0
  useHardwareKeys(step === 2 ? e.press : () => {})
  useEffect(() => () => setPreview(null), [setPreview])

  const pickBrand = (id: string) => { haptic('tick'); setBrandId(id); setModelId(null); setPreview(id); setStep(1) }
  const finish = () => {
    if (!brand || !model) return
    haptic('success')
    complete({ brandId: brand.id, modelId: model.id, name: model.name, odometer: odo, lastServiceKnown: knows })
    setPreview(null)
    useUI.getState().setTab('home')
  }

  return (
    <div className="onboard">
      <header className="ob-head">
        {step > 0 ? <IconButton icon={ArrowLeft} label="পেছনে" onClick={() => setStep(step - 1)} /> : <span className="ob-logo"><Bike size={26} aria-hidden /></span>}
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
                  <motion.button key={b.id} type="button" className={`brand${brandId === b.id ? ' on' : ''}`} style={{ ['--sw-l' as string]: b.light, ['--sw-d' as string]: b.dark }} onClick={() => pickBrand(b.id)}
                    initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ ...spring.list, delay: stagger(i, 0.03) }} whileTap={{ scale: 0.95 }}>
                    <i /><b>{b.name}</b><small>{f.d(String(b.models.length))}টি মডেল</small>
                  </motion.button>
                ))}
              </div>
              <p className="cap ob-foot">আপনার সব তথ্য এই ফোনেই থাকবে। কোনো অ্যাকাউন্ট লাগবে না।</p>
            </>
          )}

          {step === 1 && brand && (
            <>
              <h1>{brand.name} এর কোনটি?</h1>
              <label className="search">
                <Search size={20} aria-hidden />
                <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="মডেলের নাম খুঁজুন" aria-label="মডেল খুঁজুন" />
              </label>
              <div className="model-list">
                {models.map((m, i) => (
                  <motion.button key={m.id} type="button" className={`model${modelId === m.id ? ' on' : ''}`} onClick={() => { haptic('tick'); setModelId(m.id) }}
                    initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ ...spring.list, delay: stagger(i, 0.04) }} whileTap={{ scale: 0.98 }}>
                    <span className="model-photo">{m.photo ? <img src={m.photo} alt="" /> : <Bike size={34} aria-hidden />}</span>
                    <b>{m.name}</b>
                    {modelId === m.id && <motion.span className="model-check" initial={{ scale: 0 }} animate={{ scale: 1 }} transition={spring.press}><Check size={18} aria-hidden /></motion.span>}
                  </motion.button>
                ))}
                {models.length === 0 && <p className="field-msg">পাওয়া যায়নি। নিচে নিজের মডেল যোগ করুন।</p>}
              </div>
              <button type="button" className="link" onClick={() => { setModelId(brand.models[0].id); setStep(2) }}>আমার মডেল তালিকায় নেই</button>
              <Button full disabled={!modelId} onClick={() => setStep(2)}>{model ? `${model.name} নিয়ে এগিয়ে যান` : 'মডেল বেছে নিন'}</Button>
            </>
          )}

          {step === 2 && brand && (
            <>
              <h1>এখন ওডোমিটারে কত?</h1>
              <div className="odo-hero big">
                <strong><span className={odo ? '' : 'ph'}>{show(e.vals.odo) || f.d('0')}</span><small>{f.digits === 'bn' ? 'কিমি' : 'km'}</small></strong>
              </div>
              <div className="row-wrap">
                <Chip active={!knows} onClick={() => setKnows(false)}>শেষ সার্ভিস মনে নেই</Chip>
                <Chip active={knows} onClick={() => setKnows(true)}>মোটামুটি মনে আছে</Chip>
              </div>
              <p className="cap">{knows ? 'আগের সার্ভিসের মোটামুটি সময় ধরে রিমাইন্ডার চালু হবে। পরে ঠিক করা যাবে।' : 'আজ থেকে গুনতে শুরু করব। পরে সার্ভিস লিখলে সময় ঠিক হয়ে যাবে।'}</p>
              <Button full disabled={odo <= 0} onClick={finish}>{odo <= 0 ? 'কিমি লিখুন' : 'গ্যারেজ তৈরি করুন'}</Button>
              <Keypad onKey={e.press} decimal={false} />
            </>
          )}
        </motion.div>
      </AnimatePresence>
    </div>
  )
}
