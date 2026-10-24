import { useMemo, useState } from 'react'
import { Fuel, Wrench } from 'lucide-react'
import { useUI } from '../store'
import { parseAmount, parseServiceText } from '../lib/smartParse'
import { useFmt } from '../lib/bn'
import { haptic } from '../lib/haptics'
import { Sheet } from '../components/Sheet'
import { Button, Chip } from '../components/ui'
import { itemOf } from '../data/catalog'

const SAMPLES = [
  { label: 'নগদ/বিকাশ মেসেজ', text: 'Payment Tk 500.00 to Padma Filling Station successful. TrxID 9K3L2M. Balance Tk 2,140.50' },
  { label: 'মেকানিকের মেসেজ', text: 'ভাই আপনার বাইকের ইঞ্জিন অয়েল ৯০০ টাকা, অয়েল ফিল্টার ২৫০ টাকা, চেইন লুব ১৫০ টাকা' },
]

const FUEL_WORDS = /(পাম্প|ফিলিং|filling|fuel|পেট্রোল|অকটেন|octane|petrol)/i

/**
 * Share-to-MotoVault concept: paste any message and it becomes a fuel or service draft.
 * Nothing is read in the background. The rider shares or pastes, then confirms in the normal sheet.
 */
export function PasteSheet() {
  const f = useFmt()
  const open = useUI((s) => s.open)
  const [text, setText] = useState('')
  const service = useMemo(() => parseServiceText(text), [text])
  const amount = useMemo(() => parseAmount(text), [text])
  const isFuel = amount !== null && FUEL_WORDS.test(text) && service.lines.length === 0
  const hasService = service.lines.length > 0

  return (
    <Sheet title="লিখে বা পেস্ট করে সাজান">
      <p className="lead">বিকাশ, নগদ বা মেকানিকের মেসেজ এখানে দিন। কিছু নিজে নিজে পড়া হয় না।</p>
      <div className="row-wrap">{SAMPLES.map((s) => <Chip key={s.label} onClick={() => setText(s.text)}>{s.label}</Chip>)}</div>
      <textarea className="input area" rows={4} value={text} aria-label="মেসেজ" placeholder="এখানে পেস্ট করুন" onChange={(e) => setText(e.target.value)} />
      {isFuel && (
        <div className="found">
          <Fuel size={22} aria-hidden /><p>তেলের খরচ <b>{f.money(amount!)}</b> মনে হচ্ছে।</p>
          <Button size="sm" onClick={() => { haptic('success'); open('fuel', { amount: amount! }) }}>তেল ভরা খুলুন</Button>
        </div>
      )}
      {hasService && (
        <div className="found">
          <Wrench size={22} aria-hidden />
          <p>{service.lines.map((l) => itemOf(l.item)?.short).join(', ')}{amount !== null ? ` · ${f.money(service.lines.reduce((a, l) => a + (l.price ?? 0) * l.qty, 0))}` : ''}</p>
          <Button size="sm" onClick={() => { haptic('success'); open('service', { rows: service.lines }) }}>সার্ভিস খুলুন</Button>
        </div>
      )}
      {text && !isFuel && !hasService && <p className="field-msg">কিছু চেনা যায়নি। তেল ভরা বা সার্ভিস নিজে যোগ করতে পারেন।</p>}
    </Sheet>
  )
}
