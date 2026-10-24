import { useMemo, useRef, useState } from 'react'
import { Mic, MicOff } from 'lucide-react'
import { itemOf } from '../data/catalog'
import { parseServiceText, type ParsedLine } from '../lib/smartParse'
import { useFmt } from '../lib/bn'
import { useUI } from '../store'
import { Button } from './ui'
import { haptic } from '../lib/haptics'

type Recognition = { lang: string; interimResults: boolean; onresult: (e: { results: ArrayLike<ArrayLike<{ transcript: string }>> }) => void; onend: () => void; onerror: () => void; start: () => void; stop: () => void }

const DEMO_SPEECH = 'ইঞ্জিন অয়েল ৯০০ টাকা, অয়েল ফিল্টার ৩০০ টাকা'

/**
 * Type, paste or speak. It parses as you go, so there is no "arrange the draft" button.
 * Speech uses the browser's bn-BD recogniser where it exists; otherwise it fills a demo sentence.
 */
export function SmartInput({ onApply, initial = '', applyLabel = 'যোগ করুন' }: { onApply: (lines: ParsedLine[]) => void; initial?: string; applyLabel?: string }) {
  const f = useFmt()
  const [text, setText] = useState(initial)
  const [listening, setListening] = useState(false)
  const rec = useRef<Recognition | null>(null)
  const parsed = useMemo(() => parseServiceText(text), [text])

  const speak = () => {
    haptic('tick')
    const SR = (window as unknown as { SpeechRecognition?: new () => Recognition; webkitSpeechRecognition?: new () => Recognition }).SpeechRecognition
      ?? (window as unknown as { webkitSpeechRecognition?: new () => Recognition }).webkitSpeechRecognition
    if (listening) { rec.current?.stop(); return }
    if (!SR) {
      setText(DEMO_SPEECH)
      useUI.getState().showToast('ডেমো: আসল অ্যাপে ফোনের বাংলা ভয়েস চলবে', undefined, 'warn')
      return
    }
    const r = new SR()
    r.lang = 'bn-BD'; r.interimResults = false
    r.onresult = (ev) => setText((t) => (t ? t + ', ' : '') + ev.results[0][0].transcript)
    r.onend = () => setListening(false)
    r.onerror = () => setListening(false)
    rec.current = r; setListening(true); r.start()
  }

  return (
    <div className="smart">
      <textarea className="input area" rows={3} value={text} maxLength={600} aria-label="লিখুন বা পেস্ট করুন" placeholder="যেমন: ইঞ্জিন অয়েল ৯০০ টাকা, ফিল্টার ৩০০ টাকা" onChange={(e) => setText(e.target.value)} />
      <div className="row-wrap">
        <Button kind="secondary" size="sm" icon={listening ? MicOff : Mic} onClick={speak}>{listening ? 'থামান' : 'বলুন'}</Button>
        <span className="grow" />
        <Button size="sm" disabled={parsed.lines.length === 0} onClick={() => onApply(parsed.lines)}>{applyLabel}{parsed.lines.length ? ` (${f.d(String(parsed.lines.length))})` : ''}</Button>
      </div>
      {parsed.lines.length > 0 && (
        <ul className="parsed" aria-label="যা বুঝেছি">
          {parsed.lines.map((l, i) => (
            <li key={i}><b>{itemOf(l.item)?.bn}</b><span>{l.qty > 1 ? `${f.d(String(l.qty))} টি · ` : ''}{l.price !== null ? f.money(l.price) : 'দাম জানা নেই'}</span></li>
          ))}
        </ul>
      )}
      {parsed.unmatched.length > 0 && <p className="field-msg warn">চেনা যায়নি: {parsed.unmatched.join(', ')}</p>}
    </div>
  )
}
