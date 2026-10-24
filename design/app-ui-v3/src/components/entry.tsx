import { useCallback, useEffect, useRef, useState } from 'react'
import { motion } from 'motion/react'
import { Delete } from 'lucide-react'
import { toAscii } from '../lib/bn'
import { useFmt } from '../lib/bn'
import { haptic } from '../lib/haptics'
import { spring } from '../lib/motion'

/** Editing model for the in-app keypad: one text buffer per field, one field active at a time. */
export function useEntry<K extends string>(initial: Record<K, string>, first: K) {
  const [vals, setVals] = useState(initial)
  const [active, setActiveState] = useState<K>(first)
  const activeRef = useRef(active)
  activeRef.current = active
  // A field that was just selected keeps its old value until the first key, which replaces it (like selecting all).
  const fresh = useRef(new Set<K>([first]))

  const setActive = useCallback((k: K) => { fresh.current.add(k); setActiveState(k) }, [])
  const set = useCallback((field: K, value: string) => { fresh.current.delete(field); setVals((v) => ({ ...v, [field]: value })) }, [])

  const press = useCallback((key: string) => {
    const k = activeRef.current
    const replace = fresh.current.has(k)
    fresh.current.delete(k)
    setVals((v) => {
      const cur = replace ? '' : (v[k] ?? '')
      let next = cur
      if (key === 'back') next = replace ? '' : cur.slice(0, -1)
      else if (key === '.') next = cur.includes('.') ? cur : (cur === '' ? '0.' : cur + '.')
      else if (/^\d$/.test(key)) {
        const frac = cur.split('.')[1]
        if (cur.length >= 9 || (frac !== undefined && frac.length >= 2)) next = cur
        else next = cur === '0' ? key : cur + key
      }
      return { ...v, [k]: next }
    })
  }, [])

  return { vals, active, setActive, press, set }
}

/** Desktop convenience: type digits (Latin or Bengali), . and Backspace. Ignored while a real text field has focus. */
export function useHardwareKeys(press: (k: string) => void, onEnter?: () => void) {
  useEffect(() => {
    const h = (e: KeyboardEvent) => {
      const t = e.target as HTMLElement
      if (t.tagName === 'INPUT' || t.tagName === 'TEXTAREA') return
      if (e.key === 'Backspace') { e.preventDefault(); press('back') }
      else if (e.key === '.') press('.')
      else if (e.key === 'Enter') onEnter?.()
      else if (e.key.length === 1) { const d = toAscii(e.key); if (/^\d$/.test(d)) press(d) }
    }
    window.addEventListener('keydown', h)
    return () => window.removeEventListener('keydown', h)
  }, [press, onEnter])
}

/** Groups the integer part (lakh style) while keeping what the rider is still typing, like a trailing dot. */
export function useShow() {
  const f = useFmt()
  return (raw: string) => {
    if (raw === '') return ''
    const [i, frac] = raw.split('.')
    const grouped = new Intl.NumberFormat('en-IN').format(Number(i || 0))
    return f.d(frac !== undefined ? `${grouped}.${frac}` : grouped)
  }
}

export function Keypad({ onKey, decimal = true }: { onKey: (k: string) => void; decimal?: boolean }) {
  const f = useFmt()
  const keys = ['1', '2', '3', '4', '5', '6', '7', '8', '9', decimal ? '.' : '', '0', 'back']
  return (
    <div className="keypad" role="group" aria-label="সংখ্যা লিখুন">
      {keys.map((k, i) =>
        k === '' ? <span key={i} /> : (
          <motion.button key={k} type="button" className={`key${k === 'back' ? ' key-back' : ''}`} whileTap={{ scale: 0.92, backgroundColor: 'var(--key-down)' }}
            transition={spring.press} aria-label={k === 'back' ? 'মুছুন' : k === '.' ? 'দশমিক' : undefined}
            onPointerDown={() => { haptic('tick'); onKey(k) }}>
            {k === 'back' ? <Delete size={24} aria-hidden /> : k === '.' ? '.' : f.d(k)}
          </motion.button>
        ),
      )}
    </div>
  )
}
