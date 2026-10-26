import { motion } from 'motion/react'
import { FolderOpen, History, House, Plus, User } from 'lucide-react'
import { useUI } from '../store'
import { spring } from '../lib/motion'
import { haptic } from '../lib/haptics'
import type { Tab } from '../types'

const TABS: { id: Tab; label: string; icon: typeof House }[] = [
  { id: 'home', label: 'গ্যারেজ', icon: House },
  { id: 'vault', label: 'কাগজপত্র', icon: FolderOpen },
  { id: 'history', label: 'রেকর্ড', icon: History },
  { id: 'me', label: 'প্রোফাইল', icon: User },
]

/**
 * Sky navigation geometry: 30 dp icons, 28 x 4 marker. The marker slides between tabs.
 * New: a centre "+" that opens quick-add from every tab, so logging is never more than one tap away.
 */
export function TabBar() {
  const tab = useUI((s) => s.tab)
  const setTab = useUI((s) => s.setTab)
  const open = useUI((s) => s.open)
  const item = (t: (typeof TABS)[number]) => {
    const Icon = t.icon
    const on = tab === t.id
    return (
      <button key={t.id} type="button" role="tab" aria-selected={on} className={`tab${on ? ' on' : ''}`} onClick={() => { haptic('tick'); setTab(t.id) }}>
        <Icon size={28} strokeWidth={on ? 2.4 : 2} aria-hidden />
        <span>{t.label}</span>
        {on && <motion.i layoutId="tab-marker" className="tab-marker" transition={spring.marker} />}
      </button>
    )
  }
  return (
    <nav className="tabbar" role="tablist" aria-label="প্রধান মেনু">
      {item(TABS[0])}
      {item(TABS[1])}
      <div className="tab-plus-slot">
        <motion.button type="button" className="tab-plus" aria-label="নতুন রেকর্ড যোগ করুন" whileTap={{ scale: 0.92 }} transition={spring.press}
          onClick={() => { haptic('success'); open('quickAdd') }}>
          <Plus size={30} strokeWidth={2.4} aria-hidden />
        </motion.button>
      </div>
      {item(TABS[2])}
      {item(TABS[3])}
    </nav>
  )
}
