import { motion } from 'motion/react'
import { Plus, ScanLine } from 'lucide-react'
import { useStore, useUI } from '../store'
import { docName } from '../data/catalog'
import { useFmt } from '../lib/bn'
import { dur, ease, spring, stagger } from '../lib/motion'
import { haptic } from '../lib/haptics'
import { Button, IconButton, SectionTitle, Switch } from '../components/ui'
import { DocArt, ExpiryChip, expiryOf } from '../components/DocCard'

export function Vault() {
  const docs = useStore((s) => s.docs)
  const settings = useStore((s) => s.settings)
  const setSetting = useStore((s) => s.setSetting)
  const open = useUI((s) => s.open)
  const f = useFmt()
  const sorted = [...docs].sort((a, b) => expiryOf(a).left - expiryOf(b).left)
  const next = sorted[0]

  return (
    <div className="screen-pad">
      <header className="page-head">
        <h1>কাগজপত্র</h1>
        <IconButton icon={Plus} label="কাগজ যোগ করুন" tone="tint" onClick={() => open('docAdd')} />
      </header>

      {next ? (
        <motion.button type="button" className={`next ${expiryOf(next).status}`} initial={{ opacity: 0, y: 14 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: dur.page, ease: ease.sky }} onClick={() => { haptic('tick'); open('docView', { id: next.id }) }}>
          <span className="cap">পরের নবায়ন</span>
          <h2>{docName(next.kind)}</h2>
          <div className="row-between"><span className="cap">মেয়াদ শেষ: {f.dateFull(next.expiry)}</span><ExpiryChip doc={next} /></div>
        </motion.button>
      ) : (
        <div className="empty"><ScanLine size={36} aria-hidden /><p>প্রথম কাগজটি স্ক্যান করুন। মেয়াদ ফুরানোর আগেই জানানো হবে।</p><Button icon={ScanLine} onClick={() => open('docAdd')}>কাগজ স্ক্যান করুন</Button></div>
      )}

      <section aria-label="সব কাগজ">
        <SectionTitle>সব কাগজ</SectionTitle>
        <div className="wallet">
          {sorted.map((d, i) => (
            <motion.button key={d.id} type="button" className={`wallet-card kind-${d.kind}`} style={{ zIndex: i }} onClick={() => { haptic('tick'); open('docView', { id: d.id }) }}
              initial={{ opacity: 0, y: 24 }} animate={{ opacity: 1, y: 0 }} transition={{ ...spring.list, delay: stagger(i, 0.06) }} whileTap={{ scale: 0.98 }}>
              <span className="wallet-top"><b>{docName(d.kind)}</b><ExpiryChip doc={d} /></span>
              <DocArt doc={d} />
            </motion.button>
          ))}
        </div>
      </section>

      <Switch checked={settings.docReminders} onChange={(v) => setSetting('docReminders', v)} label="মেয়াদের আগে জানান" sub="৩০ দিন আগে এবং মেয়াদের দিন, সকাল ৯টায়" />
      {docs[0] && <Button kind="ghost" full onClick={() => open('docView', { id: sorted[0].id })}>রাস্তায় দেখানোর মোড</Button>}
    </div>
  )
}
