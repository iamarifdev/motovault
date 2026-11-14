import { useEffect, useRef, useState } from 'react'
import { AnimatePresence, motion } from 'motion/react'
import { FolderOpen, Fuel, Gauge as GaugeIcon, History, House, Plus, User, Wrench } from 'lucide-react'
import { useUI } from '../store'
import { spring } from '../lib/motion'
import { haptic } from '../lib/haptics'
import type { Tab } from '../types'

const TABS: { id: Tab; label: string; icon: typeof House }[] = [
  { id: 'home', label: 'গ্যারেজ', icon: House },
  { id: 'vault', label: 'ডকুমেন্ট', icon: FolderOpen },
  { id: 'history', label: 'রেকর্ড', icon: History },
  { id: 'me', label: 'প্রোফাইল', icon: User },
]

/**
 * Sky navigation geometry: 30 dp icons, 28 x 4 marker. The marker slides between tabs.
 * v4: Centre "+" opens quick-add on tap, and reveals a 1-tap radial shortcut strip on long-press.
 */
export function TabBar() {
  const tab = useUI((s) => s.tab)
  const setTab = useUI((s) => s.setTab)
  const open = useUI((s) => s.open)
  const [radial, setRadial] = useState(false)
  const holdTimer = useRef<number | null>(null)
  const held = useRef(false)

  useEffect(() => {
    if (!radial) return
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') setRadial(false) }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [radial])

  const clearHold = () => {
    if (holdTimer.current !== null) {
      window.clearTimeout(holdTimer.current)
      holdTimer.current = null
    }
  }

  const startHold = () => {
    held.current = false
    clearHold()
    holdTimer.current = window.setTimeout(() => {
      held.current = true
      haptic('success')
      setRadial(true)
    }, 360)
  }

  const item = (t: (typeof TABS)[number]) => {
    const Icon = t.icon
    const on = tab === t.id
    return (
      <button key={t.id} type="button" aria-current={on ? 'page' : undefined} className={`tab${on ? ' on' : ''}`} onClick={() => { setRadial(false); haptic('tick'); setTab(t.id) }}>
        <Icon size={28} strokeWidth={on ? 2.4 : 2} aria-hidden />
        <span>{t.label}</span>
        {on && <motion.i layoutId="tab-marker" className="tab-marker" transition={spring.marker} />}
      </button>
    )
  }

  return (
    <>
      <AnimatePresence>
        {radial && (
          <>
            <motion.div className="quick-scrim" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={() => setRadial(false)} />
            <motion.div
              className="tab-quick-pop"
              role="menu"
              aria-label="দ্রুত শর্টকাট"
              initial={{ opacity: 0, y: 16, scale: 0.9 }}
              animate={{ opacity: 1, y: 0, scale: 1, transition: spring.press }}
              exit={{ opacity: 0, y: 12, scale: 0.92, transition: { duration: 0.14 } }}
            >
              <button type="button" role="menuitem" onClick={() => { haptic('tick'); setRadial(false); open('fuel') }}><Fuel size={18} aria-hidden /><span>তেল</span></button>
              <button type="button" role="menuitem" onClick={() => { haptic('tick'); setRadial(false); open('service') }}><Wrench size={18} aria-hidden /><span>সার্ভিস</span></button>
              <button type="button" role="menuitem" onClick={() => { haptic('tick'); setRadial(false); open('odometer') }}><GaugeIcon size={18} aria-hidden /><span>মিটার রিডিং</span></button>
            </motion.div>
          </>
        )}
      </AnimatePresence>
      <nav className="tabbar" aria-label="প্রধান মেনু">
        {item(TABS[0])}
        {item(TABS[1])}
        <div className="tab-plus-slot">
          <motion.button
            type="button"
            className="tab-plus"
            aria-label="নতুন রেকর্ড যোগ করুন (চেপে ধরে রাখলে শর্টকাট)"
            whileTap={{ scale: 0.92 }}
            transition={spring.press}
            onPointerDown={startHold}
            onPointerUp={clearHold}
            onPointerCancel={clearHold}
            onPointerLeave={clearHold}
            onContextMenu={(e) => { e.preventDefault(); haptic('success'); setRadial(true) }}
            onClick={() => {
              if (held.current) { held.current = false; return }
              setRadial(false)
              haptic('success')
              open('quickAdd')
            }}
          >
            <Plus size={30} strokeWidth={2.4} aria-hidden />
          </motion.button>
        </div>
        {item(TABS[2])}
        {item(TABS[3])}
      </nav>
    </>
  )
}
