import { useEffect, useState } from 'react'
import { AnimatePresence, motion } from 'motion/react'
import { Bell, ChevronRight, FileText, ScanLine } from 'lucide-react'
import { useStore, useUI } from '../store'
import { DOC_KINDS, docName } from '../data/catalog'
import { addDays, todayISO } from '../lib/dates'
import { useFmt } from '../lib/bn'
import { haptic } from '../lib/haptics'
import { dur, ease, spring } from '../lib/motion'
import type { DocKind } from '../types'
import { Sheet } from '../components/Sheet'
import { Button, Chip, Field } from '../components/ui'

type Step = 'type' | 'scan' | 'review' | 'remind'
const HINTS = ['কাগজ সমতল জায়গায় রাখুন', 'আলোর ঝলক এড়িয়ে চলুন', 'সব প্রান্ত ফ্রেমের ভেতরে', 'ঠিক আছে, স্থির রাখুন']

/**
 * Scan, check, save, and only then offer the renewal reminder, while the expiry date is fresh in mind.
 * The scan is simulated here. Review keeps the honest rule from the Kotlin app: nothing saves until the rider confirms the values.
 */
export function DocAddSheet() {
  const addDoc = useStore((s) => s.addDoc)
  const setDocReminder = useStore((s) => s.setDocReminder)
  const { close, showToast } = useUI.getState()
  const f = useFmt()
  const today = todayISO()
  const [step, setStep] = useState<Step>('type')
  const [kind, setKind] = useState<DocKind>('registration')
  const [hint, setHint] = useState(0)
  const [flash, setFlash] = useState(false)
  const [number, setNumber] = useState('')
  const [holder, setHolder] = useState('')
  const [expiry, setExpiry] = useState('')
  const [checked, setChecked] = useState(false)
  const [savedId, setSavedId] = useState<string | null>(null)

  useEffect(() => {
    if (step !== 'scan') return
    const t = setInterval(() => setHint((h) => Math.min(HINTS.length - 1, h + 1)), 1100)
    return () => clearInterval(t)
  }, [step])

  const card = DOC_KINDS.find((d) => d.kind === kind)?.card
  const shutter = () => {
    haptic('success'); setFlash(true)
    setNumber(kind === 'licence' ? 'DL-1990-123456' : 'ঢাকা মেট্রো-হ ১১-২২৩৪'); setHolder('মো. রাফিদ হাসান')
    setTimeout(() => { setFlash(false); setStep('review') }, 260)
  }
  const reason = !number.trim() ? 'কাগজের নম্বর লিখুন' : !holder.trim() ? 'মালিকের নাম লিখুন' : !expiry ? 'মেয়াদ শেষের তারিখ দিন' : !checked ? 'আসল কাগজের সঙ্গে মিলিয়ে টিক দিন' : null

  const save = () => {
    if (reason) { haptic('error'); return }
    const id = addDoc({ kind, number: number.trim(), holder: holder.trim(), expiry, reminder: false })
    haptic('success'); setSavedId(id); setStep('remind')
  }
  const remindDate = expiry ? addDays(expiry, -30) : today
  const finish = (on: boolean) => {
    if (savedId) setDocReminder(savedId, on)
    close()
    showToast(on ? `${docName(kind)} সেভ হয়েছে · ${f.date(remindDate)} জানানো হবে` : `${docName(kind)} সেভ হয়েছে`)
  }

  return (
    <Sheet title={step === 'type' ? 'কোন কাগজ?' : step === 'scan' ? 'কাগজ স্ক্যান' : step === 'review' ? 'তথ্য মিলিয়ে নিন' : 'মনে করিয়ে দেব?'} detent="tall">
      <AnimatePresence mode="wait" initial={false}>
        <motion.div key={step} className="step" initial={{ opacity: 0, x: 24 }} animate={{ opacity: 1, x: 0, transition: { duration: dur.page, ease: ease.sky } }} exit={{ opacity: 0, x: -24, transition: { duration: 0.12 } }}>
          {step === 'type' && (
            <>
              {DOC_KINDS.map((d) => (
                <button key={d.kind} type="button" className="pick" onClick={() => { haptic('tick'); setKind(d.kind); setStep('scan'); setHint(0) }}>
                  <FileText size={24} aria-hidden /><span><b>{d.bn}</b><small>{d.card ? 'সামনে ও পেছনের ছবি' : 'পুরো পৃষ্ঠা'}</small></span><ChevronRight size={20} aria-hidden />
                </button>
              ))}
              <button type="button" className="link" onClick={() => { setStep('review') }}>স্ক্যান ছাড়া নিজে লিখব</button>
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
              <p className="cap center">ডেমো: আসল অ্যাপে এখানে লাইভ ক্যামেরা দেখাবে</p>
              <motion.button type="button" className="shutter" aria-label="ছবি তুলুন" whileTap={{ scale: 0.9 }} transition={spring.press} onClick={shutter}><i /></motion.button>
              <button type="button" className="link" onClick={() => setStep('review')}>নিজে লিখব</button>
            </>
          )}

          {step === 'review' && (
            <>
              <p className="lead">স্ক্যান থেকে যা পাওয়া গেছে তা দেখুন। ভুল থাকলে বদলান।</p>
              <Field label="কাগজের নম্বর"><input className="input" value={number} onChange={(e) => setNumber(e.target.value)} placeholder="নম্বর" /></Field>
              <Field label="মালিকের নাম"><input className="input" value={holder} onChange={(e) => setHolder(e.target.value)} placeholder="নাম" /></Field>
              <Field label="মেয়াদ শেষের তারিখ" error={null} hint="চিহ্নিত জায়গা আসল কাগজ দেখে মেলান">
                <input className="input attention" type="date" value={expiry} min={today} onChange={(e) => setExpiry(e.target.value)} />
              </Field>
              <div className="row-wrap">
                {[1, 2, 5].map((y) => <Chip key={y} active={expiry === addDays(today, 365 * y)} onClick={() => setExpiry(addDays(today, 365 * y))}>{f.d(String(y))} বছর পর</Chip>)}
              </div>
              <button type="button" className={`check-row${checked ? ' on' : ''}`} role="checkbox" aria-checked={checked} onClick={() => { haptic('tick'); setChecked(!checked) }}>
                <span className="box">{checked && '✓'}</span> আসল কাগজ দেখে তথ্য মিলিয়ে নিয়েছি
              </button>
              <Button full disabled={Boolean(reason)} onClick={save}>{reason ?? 'কাগজপত্রে সেভ করুন'}</Button>
            </>
          )}

          {step === 'remind' && (
            <div className="remind">
              <span className="remind-icon"><Bell size={34} aria-hidden /></span>
              <h3>মেয়াদ শেষ হওয়ার ৩০ দিন আগে জানাব?</h3>
              <p>{f.date(remindDate)} সকাল ৯টায় একটি নোটিফিকেশন যাবে। নবায়নের সময় থাকবে।</p>
              <Button full icon={Bell} onClick={() => finish(true)}>হ্যাঁ, জানান</Button>
              <Button full kind="ghost" onClick={() => finish(false)}>এখন নয়</Button>
            </div>
          )}
        </motion.div>
      </AnimatePresence>
    </Sheet>
  )
}
