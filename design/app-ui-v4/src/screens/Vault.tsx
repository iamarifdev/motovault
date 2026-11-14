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
  const next = sorted.find((d) => d.expiry)

  return (
    <div className="screen-pad">
      <header className="page-head">
        <h1>ডকুমেন্ট</h1>
        <IconButton icon={Plus} label="ডকুমেন্ট যোগ করুন" tone="tint" onClick={() => open('docAdd')} />
      </header>

      {next ? (
        <motion.button type="button" className={`next ${expiryOf(next).status}`} initial={{ opacity: 0, y: 14 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: dur.page, ease: ease.sky }} onClick={() => { haptic('tick'); open('docView', { id: next.id }) }}>
          <span className="cap">পরের নবায়ন</span>
          <h2>{docName(next.kind)}</h2>
          <div className="row-between"><span className="cap">মেয়াদ শেষ: {f.dateFull(next.expiry)}</span><ExpiryChip doc={next} /></div>
        </motion.button>
      ) : (
        <div className="empty"><ScanLine size={36} aria-hidden /><p>{docs.length ? 'এই ডকুমেন্টগুলোর মেয়াদ শেষের তারিখ নেই।' : 'প্রথম ডকুমেন্টটি যোগ করুন। অনুমতি দিলে মেয়াদের আগে জানানো হবে।'}</p><Button icon={ScanLine} onClick={() => open('docAdd')}>ডকুমেন্ট স্ক্যান করুন</Button></div>
      )}

      <section aria-label="সব ডকুমেন্ট">
        <SectionTitle>সব ডকুমেন্ট</SectionTitle>
        <div className="wallet">
          {sorted.map((d, i) => (
            <motion.button key={d.id} type="button" className={`wallet-card kind-${d.kind}`} style={{ zIndex: i }} onClick={() => { haptic('tick'); open('docView', { id: d.id }) }}
              initial={{ opacity: 0, y: 24 }} animate={{ opacity: 1, y: 0 }} transition={{ ...spring.list, delay: stagger(i, 0.06) }} whileTap={{ scale: 0.98 }}>
              <span className="wallet-top"><b>{docName(d.kind)}</b><ExpiryChip doc={d} /></span>
              {settings.privacyEnabled ? <div className="privacy-mask"><ScanLine size={28} aria-hidden /><p>নম্বর ও ছবি লুকানো আছে। খুলতে ট্যাপ করুন।</p></div> : <DocArt doc={d} />}
            </motion.button>
          ))}
        </div>
      </section>

      <Switch checked={settings.docReminders} onChange={(v) => setSetting('docReminders', v)} label="মেয়াদের আগে রিমাইন্ডার দিন" sub={`${f.n(settings.reminderLeadDays ?? 30)} দিন আগে এবং মেয়াদের দিন।`} />
    </div>
  )
}
