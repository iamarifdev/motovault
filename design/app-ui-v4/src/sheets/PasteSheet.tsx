import { useEffect, useMemo, useRef, useState } from 'react'
import captureCopy from '../data/capture-copy.json'
import { captureMoney } from '../lib/captureMoney'
import { Fuel, Mic, MicOff, Wrench } from 'lucide-react'
import { useUI } from '../store'
import { parseAmount, parseServiceText } from '../lib/smartParse'
import { useFmt } from '../lib/bn'
import { haptic } from '../lib/haptics'
import { startBanglaVoice, VOICE_UNAVAILABLE, type VoiceHandle } from '../lib/voiceCapture'
import { Sheet } from '../components/Sheet'
import { Button, Chip } from '../components/ui'
import { itemOf } from '../data/catalog'

const SAMPLES = [
  { label: 'নগদ/বিকাশ মেসেজ', text: 'Payment Tk 500.00 to Padma Filling Station successful. TrxID 9K3L2M. Balance Tk 2,140.50' },
  { label: 'মেকানিকের মেসেজ', text: 'ভাই আপনার বাইকের ইঞ্জিন অয়েল ৯০০ টাকা, অয়েল ফিল্টার ২৫০ টাকা, চেইন লুব ১৫০ টাকা' },
]

const FUEL_WORDS = /(পাম্প|ফিলিং|filling|fuel|পেট্রোল|অকটেন|octane|petrol)/i

/**
 * Share-to-MotoVault & Bangla voice parser: speak or paste any message and it becomes a fuel or service draft.
 */
export function PasteSheet() {
  const f = useFmt()
  const c = captureCopy.bn
  const previewState = useUI((s) => s.sharePreview ?? 'warm')
  const reviewable = previewState === 'warm' || previewState === 'cold'
  const open = useUI((s) => s.open)
  const [text, setText] = useState('')
  const [listening, setListening] = useState(false)
  const [voiceError, setVoiceError] = useState('')
  const voiceRef = useRef<VoiceHandle | null>(null)
  const service = useMemo(() => parseServiceText(text), [text])
  const amount = useMemo(() => parseAmount(text), [text])
  const isFuel = amount !== null && FUEL_WORDS.test(text) && service.lines.length === 0
  const hasService = service.lines.length > 0

  useEffect(() => () => voiceRef.current?.stop(), [])

  const toggleVoice = () => {
    haptic('tick')
    if (listening) {
      voiceRef.current?.stop()
      setListening(false)
      return
    }
    setVoiceError('')
    setListening(true)
    voiceRef.current = startBanglaVoice({
      onResult: (transcript) => {
        setText(transcript)
        haptic('success')
      },
      onEnd: () => {
        voiceRef.current = null
        setListening(false)
      },
      onError: (reason) => {
        setVoiceError(reason === 'unavailable' ? VOICE_UNAVAILABLE : 'কথা বোঝা যায়নি। আবার বলুন বা লিখে দিন।')
      },
    })
  }

  return (
    <Sheet title="লিখে, বলে বা পেস্ট করে সাজান">
      {!reviewable && <p role="alert">{c[previewState]}</p>}
      <p className="lead">মুখে বলুন অথবা বিকাশ, নগদ বা মেকানিকের মেসেজ এখানে দিন।</p>
      {previewState !== 'locked' && (
        <div className="row-wrap">
          <Button size="sm" kind="secondary" icon={listening ? MicOff : Mic} onClick={toggleVoice}>
            {listening ? 'শুনছে…' : 'মুখে বলুন'}
          </Button>
          {SAMPLES.map((s) => <Chip key={s.label} onClick={() => setText(s.text)}>{s.label}</Chip>)}
        </div>
      )}
      {voiceError && <p className="field-msg err" role="alert">{voiceError}</p>}
      {previewState !== 'locked' && <textarea className="input area" rows={4} value={text} maxLength={10000} aria-label="মেসেজ" placeholder="এখানে পেস্ট করুন বা লিখুন" onChange={(e) => setText(e.target.value)} />}
      {reviewable && isFuel && (
        <div className="found">
          <Fuel size={22} aria-hidden /><p>তেলের খরচ <b>{captureMoney(amount!, f, c.moneyValue)}</b> মনে হচ্ছে।</p>
          <Button size="sm" onClick={() => { haptic('success'); open('fuel', { amount: amount! }) }}>তেল ভরা খুলুন</Button>
        </div>
      )}
      {reviewable && hasService && (
        <div className="found">
          <Wrench size={22} aria-hidden />
          <p>{service.lines.map((l) => itemOf(l.item)?.short).join(', ')}{amount !== null ? ` · ${captureMoney(service.lines.reduce((a, l) => a + (l.price ?? 0) * l.qty, 0), f, c.moneyValue)}` : ''}</p>
          <Button size="sm" onClick={() => { haptic('success'); open('service', { rows: service.lines }) }}>সার্ভিস খুলুন</Button>
        </div>
      )}
      {text && !isFuel && !hasService && <p className="field-msg">কিছু চেনা যায়নি। তেল ভরা বা সার্ভিস নিজে যোগ করতে পারেন।</p>}
    </Sheet>
  )
}
