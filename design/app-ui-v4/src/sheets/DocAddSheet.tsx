import { useEffect, useState } from 'react'
import { AnimatePresence, motion } from 'motion/react'
import { ChevronRight, CircleCheck, FileText, ScanLine } from 'lucide-react'
import { restoreData, snapshotData, useStore, useUI } from '../store'
import { DOC_KINDS, docName } from '../data/catalog'
import { addDays, todayISO } from '../lib/dates'
import { useFmt } from '../lib/bn'
import { haptic } from '../lib/haptics'
import { dur, ease, spring } from '../lib/motion'
import type { DocKind } from '../types'
import { Sheet } from '../components/Sheet'
import { Button, Chip, Field, Switch } from '../components/ui'

type Step = 'type' | 'scan' | 'review'
const HINTS = ['কাগজ সমতল জায়গায় রাখুন', 'আলোর ঝলক এড়িয়ে চলুন', 'সব প্রান্ত ফ্রেমের ভেতরে', 'ঠিক আছে, স্থির রাখুন']

/**
 * Two-step document add: pick & snap photo, then confirm expiry date (with 1-tap presets)
 * and inline reminder toggle. Document number and holder name remain completely optional.
 */
export function DocAddSheet({ existingId }: { existingId?: string }) {
  const docs = useStore((s) => s.docs)
  const addDoc = useStore((s) => s.addDoc)
  const updateDoc = useStore((s) => s.updateDoc)
  const leadDays = useStore((s) => s.settings.reminderLeadDays ?? 30)
  const { close, open, showToast } = useUI.getState()
  const f = useFmt()
  const today = todayISO()
  const existing = existingId ? docs.find((d) => d.id === existingId) : undefined
  const [step, setStep] = useState<Step>(existing ? 'review' : 'type')
  const [kind, setKind] = useState<DocKind>(existing?.kind ?? 'registration')
  const [hint, setHint] = useState(0)
  const [flash, setFlash] = useState(false)
  const [photoTaken, setPhotoTaken] = useState(false)
  const [expiry, setExpiry] = useState(() => existing?.expiry ?? addDays(today, 365))
  const [reminder, setReminder] = useState(existing?.reminder ?? true)
  const [number, setNumber] = useState(existing?.number ?? '')
  const [holder, setHolder] = useState(existing?.holder ?? '')
  const [showOptional, setShowOptional] = useState(Boolean(existing && (existing.number || existing.holder)))

  useEffect(() => {
    if (step !== 'scan') return
    const t = setInterval(() => setHint((h) => Math.min(HINTS.length - 1, h + 1)), 1100)
    return () => clearInterval(t)
  }, [step])

  const card = DOC_KINDS.find((d) => d.kind === kind)?.card
  const existingSameKind = !existing && docs.some((d) => d.kind === kind)
  const shutter = () => {
    haptic('success')
    setFlash(true)
    setPhotoTaken(true)
    setTimeout(() => { setFlash(false); setStep('review') }, 260)
  }

  const reason = existingId && !existing ? 'ডকুমেন্টটি আর পাওয়া যাচ্ছে না' : !expiry ? 'মেয়াদ শেষের তারিখ দিন' : null

  const save = () => {
    if (reason) { haptic('error'); return }
    const payload = {
      kind,
      number: number.trim(),
      holder: holder.trim(),
      expiry,
      reminder,
    }
    if (existing) {
      updateDoc(existing.id, payload)
      haptic('success')
      open('docView', { id: existing.id })
      showToast(`${docName(kind)} তথ্য ঠিক হয়েছে`)
      return
    }
    const snap = snapshotData()
    addDoc(payload)
    haptic('success')
    close()
    const remindDate = addDays(expiry, -leadDays)
    showToast(reminder ? `${docName(kind)} সেভ হয়েছে · ${f.date(remindDate)} রিমাইন্ডার` : `${docName(kind)} সেভ হয়েছে`, () => restoreData(snap))
  }

  const sheetTitle = existing ? `${docName(kind)} ঠিক করুন` : step === 'type' ? 'কোন ডকুমেন্ট?' : step === 'scan' ? 'ডকুমেন্টের ছবি তুলুন' : docName(kind)

  return (
    <Sheet title={sheetTitle} detent="tall">
      <AnimatePresence mode="wait" initial={false}>
        <motion.div key={step} className="step" initial={{ opacity: 0, x: 24 }} animate={{ opacity: 1, x: 0, transition: { duration: dur.page, ease: ease.sky } }} exit={{ opacity: 0, x: -24, transition: { duration: 0.12 } }}>
          {step === 'type' && (
            <>
              {DOC_KINDS.map((d) => (
                <button key={d.kind} type="button" className="pick" onClick={() => { haptic('tick'); setKind(d.kind); setPhotoTaken(false); setStep('scan'); setHint(0) }}>
                  <FileText size={24} aria-hidden /><span><b>{d.bn}</b><small>{d.card ? 'সামনে ও পেছনের ছবি' : 'পুরো পৃষ্ঠার ছবি'}</small></span><ChevronRight size={20} aria-hidden />
                </button>
              ))}
              <button type="button" className="link" onClick={() => { setPhotoTaken(false); setStep('review') }}>ছবি ছাড়া শুধু তারিখ লিখব</button>
            </>
          )}

          {step === 'scan' && (
            <>
              <div className={`viewfinder ${card ? 'card' : 'page'}`}>
                <div className="vf-bg" />
                <span className="corner tl" /><span className="corner tr" /><span className="corner bl" /><span className="corner br" />
                <ScanLine className="vf-icon" size={40} aria-hidden />
                <AnimatePresence mode="wait">
                  <motion.p key={hint} className={`vf-hint${hint === HINTS.length - 1 ? ' good' : ''}`} initial={{ opacity: 0, y: 6 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }}>{HINTS[hint]}</motion.p>
                </AnimatePresence>
                <AnimatePresence>{flash && <motion.i className="flash" initial={{ opacity: 0.9 }} animate={{ opacity: 0 }} exit={{ opacity: 0 }} transition={{ duration: 0.25 }} />}</AnimatePresence>
              </div>
              <p className="cap center">আসল অ্যাপে এখানে ফোনের ক্যামেরা খুলবে</p>
              <motion.button type="button" className="shutter" aria-label="ছবি তুলুন" whileTap={{ scale: 0.9 }} transition={spring.press} onClick={shutter}><i /></motion.button>
              <button type="button" className="link" onClick={() => setStep('review')}>ছবি ছাড়া এগিয়ে যান</button>
            </>
          )}

          {step === 'review' && (
            <>
              {photoTaken && (
                <div className="found" role="status">
                  <CircleCheck size={20} color="var(--ok)" aria-hidden />
                  <p>ডকুমেন্টের ছবি সংযুক্ত হয়েছে। মেয়াদ ঠিক করে সেভ করুন।</p>
                </div>
              )}

              {existingSameKind && (
                <p className="field-msg warn" role="status">আগে থেকে থাকা {docName(kind)} নতুন তথ্য দিয়ে আপডেট হবে।</p>
              )}

              <Field label="মেয়াদ শেষের তারিখ">
                <input className="input attention" type="date" value={expiry} min={existing ? undefined : today} onChange={(e) => setExpiry(e.target.value)} />
              </Field>
              <div className="row-wrap" role="group" aria-label="মেয়াদ বাছাই">
                {[1, 2, 5, 10].map((y) => {
                  const target = addDays(today, 365 * y)
                  return <Chip key={y} active={expiry === target} onClick={() => setExpiry(target)}>{f.d(String(y))} বছর পর</Chip>
                })}
              </div>

              <div className="card-stack">
                <Switch
                  checked={reminder}
                  onChange={setReminder}
                  label={`মেয়াদের ${f.n(leadDays)} দিন আগে রিমাইন্ডার দিন`}
                  sub={expiry ? `${f.date(addDays(expiry, -leadDays))} নোটিফিকেশন পাবেন` : 'তারিখ দিলে রিমাইন্ডার সেট হবে'}
                />
              </div>

              <button type="button" className="more-toggle" aria-expanded={showOptional} onClick={() => setShowOptional(!showOptional)}>
                {showOptional ? 'ঐচ্ছিক তথ্য লুকান' : 'ডকুমেন্টের নম্বর ও নাম যোগ করুন (ঐচ্ছিক)'}
              </button>
              <AnimatePresence initial={false}>
                {showOptional && (
                  <motion.div key="opt" className="clip" initial={{ height: 0, opacity: 0 }} animate={{ height: 'auto', opacity: 1 }} exit={{ height: 0, opacity: 0 }}>
                    <Field label="ডকুমেন্টের নম্বর (ঐচ্ছিক)"><input className="input" value={number} onChange={(e) => setNumber(e.target.value)} placeholder="যেমন: ঢাকা মেট্রো-ল ৬২-৪৫১৮" /></Field>
                    <Field label="মালিকের নাম (ঐচ্ছিক)"><input className="input" value={holder} onChange={(e) => setHolder(e.target.value)} placeholder="আপনার নাম" /></Field>
                  </motion.div>
                )}
              </AnimatePresence>

              <Button full disabled={Boolean(reason)} onClick={save}>{reason ?? 'ডকুমেন্টে সেভ করুন'}</Button>
            </>
          )}
        </motion.div>
      </AnimatePresence>
    </Sheet>
  )
}
