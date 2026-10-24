import { useMemo, useState } from 'react'
import { motion } from 'motion/react'
import { ChevronRight, CircleCheck, Droplets, FileText, Fuel, Gauge as GaugeIcon, TriangleAlert, Timer, Wrench, X } from 'lucide-react'
import { useStore, useUI } from '../store'
import { brandOf, itemOf, modelOf, docName } from '../data/catalog'
import { byUrgency, describeWear, economyOf, economySeries, estimateOdometer, fuelLogs } from '../lib/wear'
import { diffDays, todayISO } from '../lib/dates'
import { useFmt } from '../lib/bn'
import { ease, dur, spring, stagger } from '../lib/motion'
import { Button, IconButton, SectionTitle } from '../components/ui'
import { CountUp, Gauge, Spark } from '../components/Gauge'
import { haptic } from '../lib/haptics'

const STATUS_ICON = { ok: CircleCheck, soon: Timer, overdue: TriangleAlert, unknown: TriangleAlert } as const

const enter = (i: number) => ({
  initial: { opacity: 0, y: 14 },
  animate: { opacity: 1, y: 0 },
  transition: { duration: dur.page, ease: ease.sky, delay: stagger(i, 0.06) },
})

export function Home() {
  const ride = useStore((s) => s.ride)!
  const rules = useStore((s) => s.rules)
  const logs = useStore((s) => s.logs)
  const docs = useStore((s) => s.docs)
  const reminderLead = useStore((s) => s.settings.reminderLeadDays ?? 30)
  const f = useFmt()
  const open = useUI((s) => s.open)
  const setTab = useUI((s) => s.setTab)
  const [nudgeHidden, setNudgeHidden] = useState(false)
  const today = todayISO()

  const wears = useMemo(() => byUrgency(rules, ride.odometer, today), [rules, ride.odometer, today])
  const top = wears[0]
  const econ = useMemo(() => economyOf(logs), [logs])
  const series = useMemo(() => economySeries(logs), [logs])
  const estimate = useMemo(() => estimateOdometer(ride, logs, today), [ride, logs, today])
  const brand = brandOf(ride.brandId)
  const model = modelOf(ride.modelId)

  const lastFuel = fuelLogs(logs).at(-1)
  const sinceFuel = lastFuel ? diffDays(lastFuel.date, today) : null
  const renewals = docs
    .map((d) => ({ d, left: diffDays(today, d.expiry) }))
    .filter((x) => x.d.expiry && x.left <= (x.d.warningDays ?? reminderLead))
    .sort((a, b) => a.left - b.left)

  const TopIcon = top ? STATUS_ICON[top.status] : CircleCheck
  const topItem = top ? itemOf(top.rule.item) : undefined
  const progress = top ? Math.min(1, top.used) : 0

  return (
    <div className="screen-pad">
      <motion.header className="home-head" {...enter(0)}>
        <div>
          <span className="cap">আমার গ্যারেজ</span>
          <h1 className="bike-name">{ride.name}</h1>
          <span className="cap">{brand.name} · {f.d(String(ride.year))}</span>
        </div>
        <button type="button" className="avatar" aria-label="বাইকের সেটিংস" onClick={() => { haptic('tick'); setTab('me') }}>
          {model?.photo ? <img src={model.photo} alt="" /> : <GaugeIcon size={26} aria-hidden />}
        </button>
      </motion.header>

      {top && (
        <motion.section className={`next ${top.status}`} {...enter(1)} aria-label="পরের কাজ">
          <div className="next-top">
            <span className="next-icon"><TopIcon size={26} strokeWidth={2.2} aria-hidden /></span>
            <div className="next-text">
              <span className="cap">{top.status === 'unknown' ? 'সার্ভিসের তথ্য নিশ্চিত করুন' : top.status === 'ok' ? 'ব্যবধান বাকি · পরের কাজ' : top.status === 'soon' ? 'শিগগির দরকার' : 'এখনই দরকার'}</span>
              <h2>{top.rule.name.split(' ও ')[0].split(' পরিষ্কার')[0]}</h2>
              <p>{describeWear(top, f)}</p>
            </div>
          </div>
          {top.status !== 'unknown' && <div className="meter" aria-hidden>
            <motion.i initial={{ scaleX: 0 }} animate={{ scaleX: progress }} transition={{ duration: dur.sweep, ease: ease.sky, delay: 0.25 }} />
          </div>}
          <p className="next-sub">{top.status === 'unknown' ? 'অজানা তথ্য থেকে শতাংশ দেখানো হয় না' : `শেষ নিশ্চিত সার্ভিস: ${f.date(top.rule.lastDate)} · ${f.km(top.rule.lastKm)}`}</p>
          <Button full icon={Wrench} onClick={() => open('service', { item: top.rule.item })}>{topItem?.short ?? 'সার্ভিস'} লিখুন</Button>
        </motion.section>
      )}

      <motion.section className="quick" {...enter(2)} aria-label="দ্রুত রেকর্ড">
        {[
          { icon: Fuel, label: 'তেল ভরা', run: () => open('fuel') },
          { icon: Wrench, label: 'সার্ভিস', run: () => open('service') },
          { icon: GaugeIcon, label: 'কিমি', run: () => open('odometer') },
          { icon: FileText, label: 'কাগজ', run: () => open('docAdd') },
        ].map((q) => (
          <motion.button key={q.label} type="button" className="quick-btn" whileTap={{ scale: 0.94 }} transition={spring.press} onClick={() => { haptic('tick'); q.run() }}>
            <span><q.icon size={26} strokeWidth={2} aria-hidden /></span>
            <b>{q.label}</b>
          </motion.button>
        ))}
      </motion.section>

      {nudgeHidden || sinceFuel === null || sinceFuel < 10 ? null : (
        <motion.aside className="nudge" {...enter(3)}>
          <Droplets size={20} aria-hidden />
          <p>{f.d(String(sinceFuel))} দিন ধরে তেলের হিসাব নেই।</p>
          <Button size="sm" kind="secondary" onClick={() => open('fuel')}>যোগ করুন</Button>
          <IconButton icon={X} label="লুকান" onClick={() => setNudgeHidden(true)} />
        </motion.aside>
      )}

      <motion.section className="stats" {...enter(4)}>
        <button type="button" className="stat" onClick={() => { haptic('tick'); open('odometer') }}>
          <span className="cap">ওডোমিটার</span>
          <strong><CountUp value={ride.odometer} format={(n) => f.n(n)} /><small>{f.digits === 'bn' ? 'কিমি' : 'km'}</small></strong>
          <span className="stat-hint">আজ আনুমানিক {f.n(estimate)} · আপডেট</span>
          <span className="stat-hint">শেষ নিশ্চিত: {f.date(ride.odometerAt)}</span>
        </button>
        <button type="button" className="stat" onClick={() => { haptic('tick'); setTab('history') }}>
          <span className="cap">জ্বালানি গড়</span>
          <strong>{econ === null ? '--' : <CountUp value={econ} format={(n) => f.d(n.toFixed(1))} />}<small>{f.digits === 'bn' ? 'কিমি/লি' : 'km/L'}</small></strong>
          <span className="stat-hint"><Spark values={series.slice(-6)} /></span>
        </button>
      </motion.section>

      <motion.section {...enter(5)} aria-label="যন্ত্রাংশের অবস্থা">
        <SectionTitle>সার্ভিসের ব্যবধান বাকি</SectionTitle>
        <div className="gauges">
          {wears.map((w, i) => <Gauge key={w.rule.id} wear={w} index={i} onClick={() => open('gaugeDetail', { id: w.rule.id })} />)}
        </div>
      </motion.section>

      {renewals.length > 0 && (
        <motion.section {...enter(6)} aria-label="কাগজের মেয়াদ">
          <SectionTitle>কাগজের মেয়াদ</SectionTitle>
          <div className="stack">
            {renewals.map(({ d, left }) => (
              <button key={d.id} type="button" className={`renewal ${left < 0 ? 'overdue' : 'soon'}`} onClick={() => { haptic('tick'); setTab('vault') }}>
                {left < 0 ? <TriangleAlert size={20} aria-hidden /> : <Timer size={20} aria-hidden />}
                <span><b>{docName(d.kind)}</b><small>{left < 0 ? `${f.days(-left)} আগে মেয়াদ শেষ` : `মেয়াদ শেষ হতে ${f.days(left)}`}</small></span>
                <ChevronRight size={20} aria-hidden />
              </button>
            ))}
          </div>
        </motion.section>
      )}

      <motion.section className="bike-card" {...enter(7)} aria-label="আপনার বাইক">
        <div className="bike-photo">
          {model?.photo ? <img src={model.photo} alt={`${brand.name} ${ride.name}`} /> : <GaugeIcon size={64} aria-hidden />}
        </div>
        <p className="cap">নমুনা ছবি · রঙ ও যন্ত্রাংশে পার্থক্য থাকতে পারে</p>
      </motion.section>
    </div>
  )
}
