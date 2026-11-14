import { useEffect, useMemo, useRef, useState } from 'react'
import { motion } from 'motion/react'
import captureCopy from '../data/capture-copy.json'
import { captureMoney } from '../lib/captureMoney'
import { Mic, MicOff } from 'lucide-react'
import { itemOf } from '../data/catalog'
import { parseServiceText, type ParsedLine } from '../lib/smartParse'
import { useFmt } from '../lib/bn'
import { startBanglaVoice, VOICE_UNAVAILABLE, type VoiceHandle } from '../lib/voiceCapture'
import { Button } from './ui'
import { haptic } from '../lib/haptics'

/**
 * Type, paste or speak. It parses as you go so the rider just confirms the detected parts.
 */
export function SmartInput({ onApply, initial = '', applyLabel = 'যোগ করুন' }: { onApply: (lines: ParsedLine[]) => void; initial?: string; applyLabel?: string }) {
  const f = useFmt()
  const c = captureCopy.bn
  const [voiceError, setVoiceError] = useState('')
  const [text, setText] = useState(initial)
  const [listening, setListening] = useState(false)
  const voiceRef = useRef<VoiceHandle | null>(null)
  const parsed = useMemo(() => parseServiceText(text), [text])

  const cancel = () => {
    voiceRef.current?.stop()
    voiceRef.current = null
    setListening(false)
  }

  useEffect(() => {
    const hide = () => { if (document.hidden) cancel() }
    document.addEventListener('visibilitychange', hide)
    return () => { document.removeEventListener('visibilitychange', hide); cancel() }
  }, [])

  const speak = () => {
    haptic('tick')
    if (listening) {
      cancel()
      return
    }
    setVoiceError('')
    setListening(true)
    voiceRef.current = startBanglaVoice({
      onResult: (transcript) => {
        setText((t) => (t ? `${t}, ${transcript}` : transcript))
        haptic('success')
      },
      onEnd: () => {
        voiceRef.current = null
        setListening(false)
      },
      onError: (reason) => {
        setVoiceError(reason === 'unavailable' ? VOICE_UNAVAILABLE : c.voiceFailed)
      },
    })
  }

  return (
    <div className="smart">
      <textarea className="input area" rows={3} value={text} maxLength={600} aria-label="লিখুন বা পেস্ট করুন" placeholder="যেমন: ইঞ্জিন অয়েল ৯০০ টাকা, ফিল্টার ২৫০ টাকা" onChange={(e) => setText(e.target.value)} />
      {voiceError && <p className="field-msg err" role="alert">{voiceError}</p>}
      <div className="row-wrap">
        <Button kind="secondary" size="sm" icon={listening ? MicOff : Mic} onClick={speak}>{listening ? c.voiceStop : 'মুখে বলুন'}</Button>
        {listening && (
          <span className="row-wrap tight" aria-label="ভয়েস শুনছে">
            {[0, 0.15, 0.3].map((d) => (
              <motion.span
                key={d}
                style={{ width: 4, height: 14, borderRadius: 2, background: 'var(--brand)', display: 'inline-block' }}
                animate={{ scaleY: [0.4, 1.2, 0.5] }}
                transition={{ duration: 0.7, repeat: Infinity, delay: d }}
              />
            ))}
          </span>
        )}
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
