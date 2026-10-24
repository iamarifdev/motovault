import { motion } from 'motion/react'
import { FileText, Fuel, Gauge as GaugeIcon, Sparkles, Wrench } from 'lucide-react'
import { useUI } from '../store'
import { Sheet } from '../components/Sheet'
import { spring, stagger } from '../lib/motion'
import { haptic } from '../lib/haptics'

const OPTIONS = [
  { icon: Fuel, label: 'তেল ভরা', sub: 'টাকা লিখুন, লিটার নিজেই হবে', go: 'fuel' as const },
  { icon: Wrench, label: 'সার্ভিস', sub: 'এক ট্যাপে পার্টস যোগ', go: 'service' as const },
  { icon: GaugeIcon, label: 'ওডোমিটার', sub: 'আনুমানিক কিমি ঠিক করুন', go: 'odometer' as const },
  { icon: FileText, label: 'কাগজ', sub: 'স্ক্যান করে মেয়াদ রাখুন', go: 'docAdd' as const },
]

export function QuickAddSheet() {
  const open = useUI((s) => s.open)
  return (
    <Sheet title="কী যোগ করবেন?">
      <div className="qa-grid">
        {OPTIONS.map((o, i) => (
          <motion.button key={o.go} type="button" className="qa-card" initial={{ opacity: 0, y: 14 }} animate={{ opacity: 1, y: 0 }}
            transition={{ ...spring.list, delay: 0.08 + stagger(i, 0.04) }} whileTap={{ scale: 0.96 }}
            onClick={() => { haptic('tick'); open(o.go) }}>
            <span className="qa-icon"><o.icon size={28} strokeWidth={2} aria-hidden /></span>
            <b>{o.label}</b>
            <small>{o.sub}</small>
          </motion.button>
        ))}
      </div>
      <button type="button" className="qa-paste" onClick={() => { haptic('tick'); open('paste') }}>
        <Sparkles size={20} aria-hidden />
        <span><b>লিখে বা পেস্ট করে সাজান</b><small>মেকানিকের মেসেজ, বিকাশ বা নগদের টেক্সট</small></span>
      </button>
    </Sheet>
  )
}
