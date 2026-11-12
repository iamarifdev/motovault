import { useEffect, useMemo, useRef, useState } from 'react'
import captureCopy from '../data/capture-copy.json'
import { captureMoney } from '../lib/captureMoney'
import { Mic, MicOff } from 'lucide-react'
import { itemOf } from '../data/catalog'
import { parseServiceText, type ParsedLine } from '../lib/smartParse'
import { useFmt } from '../lib/bn'
import { Button } from './ui'
import { haptic } from '../lib/haptics'

type Recognition = { lang: string; interimResults: boolean; onresult: (e: { results: ArrayLike<ArrayLike<{ transcript: string }>> }) => void; onend: () => void; onerror: () => void; start: () => void; stop: () => void }


/**
 * Type, paste or speak. It parses as you go, so there is no "arrange the draft" button.
 * Speech uses an explicitly started provider. Unavailable recognition keeps manual input usable.
 */
export function SmartInput({ onApply, initial = '', applyLabel = 'যোগ করুন' }: { onApply: (lines: ParsedLine[]) => void; initial?: string; applyLabel?: string }) {
  const f = useFmt()
  const c = captureCopy.bn
  const [voiceError, setVoiceError] = useState('')
  const [text, setText] = useState(initial)
  const [listening, setListening] = useState(false)
  const rec = useRef<Recognition | null>(null)
  const parsed = useMemo(() => parseServiceText(text), [text])

  const cancel = () => { const r = rec.current; rec.current = null; if (r) { r.onresult = () => {}; r.stop() }; setListening(false) }
  useEffect(() => { const hide = () => { if (document.hidden) cancel() }; document.addEventListener('visibilitychange', hide); return () => { document.removeEventListener('visibilitychange', hide); cancel() } }, [])
  const speak = () => {
    haptic('tick')
    const SR = (window as unknown as { SpeechRecognition?: new () => Recognition; webkitSpeechRecognition?: new () => Recognition }).SpeechRecognition
      ?? (window as unknown as { webkitSpeechRecognition?: new () => Recognition }).webkitSpeechRecognition
    if (listening) { rec.current?.stop(); return }
    if (!SR) { setVoiceError(c.voiceUnavailable); return }
    setVoiceError('')
    const r = new SR()
    r.lang = 'bn-BD'; r.interimResults = false
    r.onresult = (ev) => setText((t) => (t ? t + ', ' : '') + ev.results[0][0].transcript)
    r.onend = () => setListening(false)
    r.onerror = () => { setListening(false); setVoiceError(c.voiceFailed) }
    rec.current = r; setListening(true); r.start()
  }

  return (
    <div className="smart">
      <textarea className="input area" rows={3} value={text} maxLength={600} aria-label="লিখুন বা পেস্ট করুন" placeholder="যেমন: ইঞ্জিন অয়েল ৯০০ টাকা, ফিল্টার ৩০০ টাকা" onChange={(e) => setText(e.target.value)} />
      <p>{c.voiceDisclosure}</p>
      {voiceError && <p role="alert">{voiceError}</p>}
      <div className="row-wrap">
        <Button kind="secondary" size="sm" icon={listening ? MicOff : Mic} onClick={speak}>{listening ? c.voiceStop : c.voiceStart}</Button>
        {listening && <Button kind="secondary" size="sm" onClick={cancel}>{c.voiceCancel}</Button>}
        <span className="grow" />
        <Button size="sm" disabled={parsed.lines.length === 0} onClick={() => onApply(parsed.lines)}>{applyLabel}{parsed.lines.length ? ` (${f.d(String(parsed.lines.length))})` : ''}</Button>
      </div>
      {parsed.lines.length > 0 && (
        <ul className="parsed" aria-label="যা বুঝেছি">
          {parsed.lines.map((l, i) => (
            <li key={i}><b>{itemOf(l.item)?.bn}</b><span>{l.qty > 1 ? `${f.d(String(l.qty))} টি · ` : ''}{l.price !== null ? captureMoney(l.price, f, c.moneyValue) : 'দাম জানা নেই'}</span></li>
          ))}
        </ul>
      )}
      {parsed.unmatched.length > 0 && <p className="field-msg warn">চেনা যায়নি: {parsed.unmatched.join(', ')}</p>}
    </div>
  )
}
