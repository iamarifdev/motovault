import { useState } from 'react'
import { AnimatePresence, motion } from 'motion/react'
import { Check, ChevronDown, ExternalLink, FileUp, Trash2 } from 'lucide-react'
import { useStore, useUI, restoreData, snapshotData } from '../store'
import { docName } from '../data/catalog'
import { FEE_CALCULATOR_URL, QR_VERIFY_URL, RENEWAL_CHECKED_ON, RENEWAL_GUIDES } from '../data/renewal'
import { addDays, todayISO } from '../lib/dates'
import { parseNum, useFmt } from '../lib/bn'
import { dur, ease } from '../lib/motion'
import { haptic } from '../lib/haptics'
import { Sheet } from '../components/Sheet'
import { Button, Chip, Field } from '../components/ui'

/**
 * Renewal is one fast update: pick the new expiry date, optionally record fee/receipt,
 * and expand the BRTA preparation checklist only when needed.
 */
export function RenewalSheet({ id }: { id: string }) {
  const doc = useStore((s) => s.docs.find((d) => d.id === id))
  const toggleDocStep = useStore((s) => s.toggleDocStep)
  const renewDoc = useStore((s) => s.renewDoc)
  const showToast = useUI((s) => s.showToast)
  const open = useUI((s) => s.open)
  const f = useFmt()
  const guide = doc ? RENEWAL_GUIDES[doc.kind] : null
  const [expiry, setExpiry] = useState(() => doc?.expiry ? addDays(doc.expiry, 365) : todayISO())
  const [fee, setFee] = useState('')
  const [receiptName, setReceiptName] = useState('')
  const [notes, setNotes] = useState('')
  const [guideOpen, setGuideOpen] = useState(false)

  if (!doc || !guide) return null

  const checked = doc.checkedSteps ?? []
  const progress = guide.steps.filter((step) => checked.includes(step.id)).length
  const previous = doc.expiry
  const expiryError = !expiry ? 'নতুন মেয়াদ দিন' : previous && expiry <= previous ? 'নতুন মেয়াদ আগের মেয়াদের পরে হতে হবে।' : null
  const feeValue = fee.trim() ? parseNum(fee) : null
  const feeError = fee.trim() && (feeValue === null || feeValue < 0) ? 'টাকার পরিমাণ ঠিক করুন' : null
  const canSave = !expiryError && !feeError

  const save = () => {
    if (!canSave) { haptic('error'); return }
    const snapshot = snapshotData()
    renewDoc(doc.id, { newExpiry: expiry, fee: feeValue, receiptName: receiptName || undefined, notes })
    haptic('success')
    open('docView', { id: doc.id })
    showToast(`${docName(doc.kind)} নবায়ন সেভ হয়েছে`, () => restoreData(snapshot))
  }

  const openLink = (url: string) => {
    haptic('tick')
    window.open(url, '_blank', 'noopener,noreferrer')
  }

  return (
    <Sheet title="নবায়ন" detent="tall" footer={<Button full onClick={save} disabled={!canSave}>{expiryError ?? feeError ?? 'নবায়ন সেভ করুন'}</Button>}>
      <div className="renew-summary">
        <span className="cap">{docName(doc.kind)}</span>
        <b>{f.dateFull(previous) ? `আগের মেয়াদ: ${f.dateFull(previous)}` : 'মেয়াদ নেই'}</b>
      </div>

      <Field label="নতুন মেয়াদ শেষের তারিখ" error={expiryError}>
        <input className="input attention" type="date" value={expiry} min={previous || todayISO()} onChange={(e) => setExpiry(e.target.value)} />
      </Field>
      <div className="row-wrap">
        {[1, 2, 5].map((years) => {
          const target = addDays(previous || todayISO(), 365 * years)
          return <Chip key={years} active={expiry === target} onClick={() => setExpiry(target)}>{f.d(String(years))} বছর পর</Chip>
        })}
      </div>

      {guide.feeCalculator && (
        <Field label="যত টাকা দিয়েছেন (ঐচ্ছিক)" error={feeError}>
          <input className="input" inputMode="decimal" value={fee} onChange={(e) => setFee(e.target.value)} placeholder="০" />
        </Field>
      )}

      <Field label="পেমেন্ট রসিদ (ঐচ্ছিক)" hint="চাইলে রসিদের ছবি সংযুক্ত রাখতে পারেন।">
        <div className="renew-receipt">
          <label className="btn btn-secondary btn-md">
            <FileUp size={18} aria-hidden /><span>{receiptName || 'রসিদের ছবি যোগ করুন'}</span>
            <input type="file" accept="image/jpeg,image/png,image/webp" hidden
              onChange={(e) => { const file = e.target.files?.[0]; if (file) { haptic('success'); setReceiptName(file.name) } }} />
          </label>
          {receiptName && (
            <button type="button" className="link danger" onClick={() => { haptic('tick'); setReceiptName('') }}>
              <Trash2 size={16} aria-hidden />রসিদ সরান
            </button>
          )}
        </div>
      </Field>

      <Field label="নোট (ঐচ্ছিক)">
        <input className="input" value={notes} maxLength={500} onChange={(e) => setNotes(e.target.value)} placeholder="যেমন: সার্কেল অফিসে গিয়েছিলাম" />
      </Field>

      <button type="button" className="more-toggle" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', width: '100%' }}
        aria-expanded={guideOpen} onClick={() => { haptic('tick'); setGuideOpen(!guideOpen) }}>
        <span>নবায়নে কী কী কাগজ লাগে দেখুন ({f.d(String(progress))}/{f.d(String(guide.steps.length))})</span>
        <ChevronDown size={18} style={{ transform: guideOpen ? 'rotate(180deg)' : 'none', transition: 'transform .18s ease' }} aria-hidden />
      </button>

      <AnimatePresence initial={false}>
        {guideOpen && (
          <motion.div key="renew-guide" className="clip" initial={{ height: 0, opacity: 0 }} animate={{ height: 'auto', opacity: 1, transition: { duration: dur.expand, ease: ease.expand } }} exit={{ height: 0, opacity: 0 }}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10, paddingTop: 4 }}>
              <span className="renew-source">সোর্স: BRTA · সর্বশেষ দেখা {f.date(RENEWAL_CHECKED_ON)}</span>
              <div className="renew-progress">
                <span>{f.d(String(progress))}/{f.d(String(guide.steps.length))} ধাপ হয়েছে</span>
                <span className="renew-bar"><i style={{ width: `${Math.round((progress / guide.steps.length) * 100)}%` }} /></span>
              </div>
              <div className="renew-steps">
                {guide.steps.map((step) => {
                  const on = checked.includes(step.id)
                  return (
                    <button key={step.id} type="button" role="checkbox" aria-checked={on} className={`renew-step${on ? ' on' : ''}`}
                      onClick={() => { haptic('tick'); toggleDocStep(doc.id, step.id) }}>
                      <span className="box">{on && <Check size={14} aria-hidden />}</span>
                      <span><b>{step.title}</b><small>{step.detail}</small></span>
                    </button>
                  )
                })}
              </div>
              {guide.feeCalculator && (
                <button type="button" className="link" onClick={() => openLink(FEE_CALCULATOR_URL)}>
                  <ExternalLink size={16} aria-hidden />BRTA ফি ক্যালকুলেটর
                </button>
              )}
              {guide.lateFine && <p className="renew-warn">{guide.lateFine}</p>}
              {guide.links.map((link) => (
                <button key={link.id} type="button" className="link" onClick={() => openLink(link.url)}>
                  <ExternalLink size={16} aria-hidden />{link.label}
                </button>
              ))}
              {guide.qrVerification && (
                <Button full kind="secondary" onClick={() => openLink(QR_VERIFY_URL)}>BRTA সাইটে কপি যাচাই করুন</Button>
              )}
              <p className="renew-source">লিংক না খুললেও ধাপ ও নবায়ন কাজ করবে। BRTA সাইট না খুললে brta.gov.bd-এ দেখুন।</p>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      <button type="button" className="link" onClick={() => open('docView', { id: doc.id })}>ডকুমেন্টে ফিরে যান</button>
    </Sheet>
  )
}
