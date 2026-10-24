import { useEffect, useRef, useState } from 'react'
import { animate, motion, useReducedMotion } from 'motion/react'
import { CircleCheck, Timer, TriangleAlert, HelpCircle } from 'lucide-react'
import { useStore } from '../store'
import { ease, dur, stagger, spring } from '../lib/motion'
import { describeWear, statusLabel, type Wear } from '../lib/wear'
import { useFmt } from '../lib/bn'
import { haptic } from '../lib/haptics'

const ICON = { ok: CircleCheck, soon: Timer, overdue: TriangleAlert, unknown: HelpCircle } as const

export function useMotionOff() {
  const system = useReducedMotion()
  const manual = useStore((s) => s.settings.reduceMotion)
  return Boolean(system || manual)
}

/** Counts to the new value. Instant when reduced motion is on. */
export function CountUp({ value, format }: { value: number; format: (n: number) => string }) {
  const off = useMotionOff()
  const [shown, setShown] = useState(off ? value : 0)
  const from = useRef(off ? value : 0)
  useEffect(() => {
    if (off) { setShown(value); from.current = value; return }
    const c = animate(from.current, value, { duration: 0.8, ease: ease.sky, onUpdate: (v) => { from.current = v; setShown(v) } })
    return () => c.stop()
  }, [value, off])
  return <>{format(shown)}</>
}

/**
 * Ring that sweeps in from empty. Status is never colour alone: each state has its own icon and wording.
 */
export function Gauge({ wear, index, onClick }: { wear: Wear; index: number; onClick: () => void }) {
  const f = useFmt()
  const off = useMotionOff()
  const unknown = wear.status === 'unknown'
  const Icon = ICON[wear.status]
  const text = wear.status === 'overdue' ? describeWear(wear, f) : statusLabel(wear.status)
  const name = wear.rule.name.split(' ও ')[0].split(' পরিষ্কার')[0]
  return (
    <motion.button type="button" className={`gauge ${wear.status}`} onClick={() => { haptic('tick'); onClick() }} whileTap={{ scale: 0.96 }} transition={spring.press}
      aria-label={`${name}, ${unknown ? 'আগের সার্ভিস নিশ্চিত নয়' : `সার্ভিসের ব্যবধান ${f.d(String(wear.life))} শতাংশ বাকি`}, ${text}`}>
      <span className="ring">
        <svg viewBox="0 0 80 80" aria-hidden>
          <circle className="track" cx="40" cy="40" r="32" />
          {!unknown && <motion.circle className="arc" cx="40" cy="40" r="32" initial={{ pathLength: off ? wear.life / 100 : 0 }} animate={{ pathLength: Math.max(0.02, wear.life / 100) }}
            transition={{ duration: off ? 0 : dur.sweep, ease: ease.sky, delay: off ? 0 : stagger(index, 0.07) }} />}
        </svg>
        <span className="ring-num">{unknown ? '?' : <><CountUp value={wear.life} format={(n) => f.d(String(Math.round(n)))} /><small>%</small></>}</span>
      </span>
      <b className="gauge-name">{name}</b>
      <small>সার্ভিসের ব্যবধান বাকি</small>
      <span className="gauge-state"><Icon size={14} strokeWidth={2.2} aria-hidden />{text}</span>
    </motion.button>
  )
}

export function Spark({ values }: { values: number[] }) {
  if (values.length < 2) return null
  const w = 72, h = 26, min = Math.min(...values), max = Math.max(...values), span = Math.max(0.5, max - min)
  const pts = values.map((v, i) => `${(i / (values.length - 1)) * w},${h - 3 - ((v - min) / span) * (h - 6)}`).join(' ')
  return (
    <svg className="spark" viewBox={`0 0 ${w} ${h}`} width={w} height={h} aria-hidden>
      <motion.polyline points={pts} fill="none" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
        initial={{ pathLength: 0 }} animate={{ pathLength: 1 }} transition={{ duration: dur.sweep, ease: ease.sky }} />
    </svg>
  )
}
