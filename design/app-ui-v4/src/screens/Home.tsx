import { useMemo, useState } from 'react'
import { AnimatePresence, motion } from 'motion/react'
import {
  ChevronDown,
  ChevronRight,
  CircleCheck,
  CloudRain,
  FileText,
  Fuel,
  Gauge as GaugeIcon,
  Timer,
  TriangleAlert,
  Wrench,
} from 'lucide-react'
import { useStore, useUI } from '../store'
import { docName } from '../data/catalog'
import { byUrgency, describeWear, economyOf, estimateOdometer, fuelLogs } from '../lib/wear'
import { diffDays, todayISO } from '../lib/dates'
import { useFmt } from '../lib/bn'
import { dur, ease, spring, stagger } from '../lib/motion'
import { Button, Chip } from '../components/ui'
import { useMotionOff } from '../components/Gauge'
import { BikeShowroom } from '../components/BikeShowroom'
import { haptic } from '../lib/haptics'

type WearFilter = 'all' | 'due'

const STATUS_ICON = {
  ok: CircleCheck,
  soon: Timer,
  overdue: TriangleAlert,
  unknown: TriangleAlert,
} as const

export function Home() {
  const ride = useStore((s) => s.ride)!
  const rules = useStore((s) => s.rules)
  const logs = useStore((s) => s.logs)
  const docs = useStore((s) => s.docs)
  const reminderLead = useStore((s) => s.settings.reminderLeadDays ?? 30)
  const f = useFmt()
  const open = useUI((s) => s.open)
  const motionOff = useMotionOff()
  const [seasonalOpen, setSeasonalOpen] = useState(false)
  const [filter, setFilter] = useState<WearFilter>('due')
  const [showAllWears, setShowAllWears] = useState(false)
  const today = todayISO()

  const enter = (i: number) =>
    motionOff
      ? {}
      : {
          initial: { opacity: 0, y: 12 },
          animate: { opacity: 1, y: 0 },
          transition: { duration: dur.page, ease: ease.sky, delay: stagger(i, 0.05) },
        }

  const wears = useMemo(() => byUrgency(rules, ride.odometer, today), [rules, ride.odometer, today])
  const econ = useMemo(() => economyOf(logs), [logs])
  const estimate = useMemo(() => estimateOdometer(ride, logs, today), [ride, logs, today])

  const lastFuel = fuelLogs(logs).at(-1)
  const sinceFuel = lastFuel ? diffDays(lastFuel.date, today) : null
  const fuelReminder = sinceFuel !== null && sinceFuel >= 10

  const renewals = useMemo(
    () =>
      docs
        .map((d) => ({ d, left: diffDays(today, d.expiry) }))
        .filter((x) => x.d.expiry && x.left <= (x.d.warningDays ?? reminderLead))
        .sort((a, b) => a.left - b.left),
    [docs, today, reminderLead],
  )

  const dueWears = useMemo(() => wears.filter((w) => w.status !== 'ok'), [wears])
  const overdueCount = wears.filter((w) => w.status === 'overdue').length

  const heroStatus = overdueCount > 0 || renewals.some((r) => r.left < 0)
    ? 'overdue'
    : dueWears.length > 0 || renewals.length > 0
      ? 'soon'
      : 'ok'
  const HeroPillIcon = STATUS_ICON[heroStatus]

  // One list for parts and papers: due items first (overdue, then papers, then soon/unknown).
  type JobRow = { key: string; tone: 'overdue' | 'soon' | 'unknown' | 'ok'; title: string; sub: string; meta?: string; bar?: number; doc?: boolean; onPress: () => void }
  const dueJobs = useMemo<JobRow[]>(() => {
    const wearRow = (w: (typeof wears)[number]): JobRow => ({
      key: `wear-${w.rule.id}`, tone: w.status, title: w.rule.name.split(' ও ')[0].split(' পরিষ্কার')[0], sub: describeWear(w, f),
      meta: w.status === 'unknown' ? 'শেষ সার্ভিসের তথ্য দিন' : `শেষ: ${f.km(w.rule.lastKm)} · ${f.date(w.rule.lastDate)}`,
      bar: w.status === 'unknown' ? 1 : Math.max(0.08, Math.min(1, w.used)), onPress: () => open('gaugeDetail', { id: w.rule.id }),
    })
    const docRow = ({ d, left }: (typeof renewals)[number]): JobRow => ({
      key: `doc-${d.id}`, tone: left < 0 ? 'overdue' : 'soon', title: docName(d.kind), doc: true,
      sub: left < 0 ? `${f.days(-left)} আগে মেয়াদ শেষ` : `মেয়াদ শেষ হতে ${f.days(left)}`, meta: 'ডকুমেন্ট', onPress: () => open('docView', { id: d.id }),
    })
    return [...dueWears.filter((w) => w.status === 'overdue').map(wearRow), ...renewals.map(docRow), ...dueWears.filter((w) => w.status !== 'overdue').map(wearRow)]
  }, [dueWears, renewals, f, open])
  const allJobs = useMemo<JobRow[]>(() => [...dueJobs, ...wears.filter((w) => w.status === 'ok').map((w) => ({
    key: `wear-${w.rule.id}`, tone: 'ok' as const, title: w.rule.name.split(' ও ')[0].split(' পরিষ্কার')[0], sub: describeWear(w, f),
    meta: `শেষ: ${f.km(w.rule.lastKm)} · ${f.date(w.rule.lastDate)}`, bar: Math.max(0.08, Math.min(1, w.used)), onPress: () => open('gaugeDetail', { id: w.rule.id }),
  }))], [dueJobs, wears, f, open])
  const listFilter = dueJobs.length === 0 ? 'all' : filter
  const jobs = listFilter === 'due' ? dueJobs : allJobs
  const nextJob = dueWears[0] ?? wears[0] ?? null

  return (
    <div className="screen-pad">
      <motion.div {...enter(0)}>
        <BikeShowroom
          health={heroStatus}
          healthIcon={HeroPillIcon}
          healthLabel={dueWears.length + renewals.length > 0 ? `${f.d(String(dueWears.length + renewals.length))}টি কাজ বাকি` : 'সব ঠিক আছে'}
          estimate={estimate}
          econ={econ}
          fuelNote={fuelReminder ? `${f.d(String(sinceFuel))} দিন তেলের হিসাব নেই` : lastFuel ? `শেষ তেল: ${f.date(lastFuel.date)}` : 'খরচ ও মাইলেজ'}
          nextJob={nextJob}
        />
      </motion.div>

      {/* Zone 2A: Thumb-Zone Quick Actions */}
      <motion.section className="quick" {...enter(1)} aria-label="দ্রুত রেকর্ড">
        {[
          { icon: Fuel, label: 'তেল ভরা', dot: fuelReminder, run: () => open('fuel') },
          { icon: Wrench, label: 'সার্ভিস', dot: false, run: () => open('service') },
          { icon: GaugeIcon, label: 'মিটার রিডিং', dot: false, run: () => open('odometer') },
          { icon: FileText, label: 'ডকুমেন্ট', dot: renewals.length > 0, run: () => open('docAdd') },
        ].map((q) => (
          <motion.button
            key={q.label}
            type="button"
            className="quick-btn"
            whileTap={motionOff ? undefined : { scale: 0.94 }}
            transition={spring.press}
            onClick={() => {
              haptic('tick')
              q.run()
            }}
          >
            {q.dot && <i className="quick-dot" aria-label="নতুন কাজ বাকি" />}
            <span>
              <q.icon size={24} strokeWidth={2} aria-hidden />
            </span>
            <b>{q.label}</b>
          </motion.button>
        ))}
      </motion.section>

      <motion.section {...enter(2)} aria-label="কাজ ও যন্ত্রাংশ" className="jobs">
        <div className="jobs-head">
          <h2>কাজ ও যন্ত্রাংশ</h2>
          <div className="jobs-tabs" role="group" aria-label="তালিকা বাছাই">
            {dueJobs.length > 0 && <Chip active={listFilter === 'due'} onClick={() => { haptic('tick'); setFilter('due') }}>বাকি {f.d(String(dueJobs.length))}</Chip>}
            <Chip active={listFilter === 'all'} onClick={() => { haptic('tick'); setFilter('all') }}>সব {f.d(String(allJobs.length))}</Chip>
          </div>
        </div>
        {dueJobs.length === 0 && (
          <div className="inbox-card calm" role="status">
            <CircleCheck size={20} strokeWidth={2.2} color="var(--ok)" aria-hidden />
            <span style={{ fontSize: 13.5, fontWeight: 600 }}>সব ঠিক আছে। কোনো সার্ভিস বা ডকুমেন্ট বাকি নেই।</span>
          </div>
        )}
        <div className="wear-list">
          {(listFilter === 'all' && !showAllWears ? jobs.slice(0, 5) : jobs).map((j, i) => {
            const Icon = j.doc ? FileText : STATUS_ICON[j.tone]
            return (
              <button key={j.key} type="button" className="wear-row" onClick={() => { haptic('tick'); j.onPress() }}>
                <div className="wear-row-top">
                  <span className={`wear-icon ${j.tone}`}><Icon size={17} strokeWidth={2.2} aria-hidden /></span>
                  <div className="wear-info">
                    <div className="wear-name">{j.title}</div>
                    {j.meta && <div className="wear-meta">{j.meta}</div>}
                  </div>
                  <div className="wear-trailing">
                    <span className={`wear-badge ${j.tone}`}>{j.sub}</span>
                    <ChevronRight size={16} color="var(--text-3)" aria-hidden />
                  </div>
                </div>
                {j.bar !== undefined && (
                  <div className="wear-bar-track" aria-hidden>
                    <motion.div className={`wear-bar-fill ${j.tone}`} initial={motionOff ? false : { scaleX: 0 }} animate={{ scaleX: j.bar }}
                      transition={{ duration: dur.sweep, ease: ease.sky, delay: motionOff ? 0 : Math.min(0.28, i * 0.04) }} />
                  </div>
                )}
              </button>
            )
          })}
        </div>
        {listFilter === 'all' && jobs.length > 5 && (
          <button type="button" className="link" style={{ alignSelf: 'center' }} onClick={() => { haptic('tick'); setShowAllWears(!showAllWears) }}>
            {showAllWears ? 'কম দেখুন' : `আরও দেখুন (${f.d(String(jobs.length - 5))}টি)`}
          </button>
        )}
      </motion.section>

      {/* Zone 4: Compact Seasonal Care & Emergency Pill */}
      <motion.section {...enter(3)} aria-label="বর্ষায় যত্ন ও জরুরি সাহায্য">
        <div className="seasonal-pill">
          <button
            type="button"
            className="seasonal-head"
            aria-expanded={seasonalOpen}
            onClick={() => {
              haptic('tick')
              setSeasonalOpen(!seasonalOpen)
            }}
          >
            <span className="seasonal-icon">
              <CloudRain size={18} aria-hidden />
            </span>
            <span>
              <b>বর্ষায় বাইকের যত্ন ও জরুরি সাহায্য</b>
              <small className="cap">
                {seasonalOpen ? 'বন্ধ করুন' : 'চেকলিস্ট ও জরুরি হেল্পলাইন'}
              </small>
            </span>
            <motion.span
              animate={{ rotate: seasonalOpen ? 180 : 0 }}
              transition={{ duration: dur.expand, ease: ease.expand }}
              className="hchev"
            >
              <ChevronDown size={18} aria-hidden />
            </motion.span>
          </button>
          <AnimatePresence initial={false}>
            {seasonalOpen && (
              <motion.div
                className="clip"
                initial={{ height: 0, opacity: 0 }}
                animate={{ height: 'auto', opacity: 1 }}
                exit={{ height: 0, opacity: 0 }}
              >
                <p className="stat-hint" style={{ margin: 0 }}>
                  বৃষ্টিতে চালানোর পর চেইন, ব্রেক ও ইলেকট্রিক্যাল কানেক্টর পরীক্ষা করুন।
                </p>
                <Button size="sm" kind="secondary" onClick={() => open('breakdown')}>
                  ব্রেকডাউন ও জরুরি সহায়তা
                </Button>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </motion.section>
    </div>
  )
}
