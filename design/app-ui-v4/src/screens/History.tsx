import { useMemo, useRef, useState } from 'react'
import { AnimatePresence, motion, useMotionValue, useTransform } from 'motion/react'
import { ArrowUp, ChevronDown, Fuel, Pencil, Trash2, Wrench } from 'lucide-react'
import { useStore, useUI } from '../store'
import { useFmt } from '../lib/bn'
import { addDays, todayISO } from '../lib/dates'
import { dur, ease, spring } from '../lib/motion'
import { haptic } from '../lib/haptics'
import type { FuelLog, LogEntry, ServiceLog } from '../types'
import { CostInsights } from '../components/CostInsights'
import { summariseWork } from '../lib/workLedger'

type Filter = 'all' | 'service' | 'fuel'

function Row({ log, onDelete, onEdit }: { log: LogEntry; onDelete: () => void; onEdit: () => void }) {
  const f = useFmt()
  const [openRow, setOpenRow] = useState(false)
  const x = useMotionValue(0)
  const reveal = useTransform(x, [-88, -16, 0], [1, 0.5, 0])
  const fuel = log.type === 'fuel' ? (log as FuelLog) : null
  const svc = log.type === 'service' ? (log as ServiceLog) : null
  const title = fuel ? `তেল ভরা · ${f.liters(fuel.liters)}` : svc!.lines.length > 1 ? `${svc!.lines[0].name} ও আরও ${f.d(String(svc!.lines.length - 1))}টি` : svc!.lines[0]?.name ?? 'সার্ভিস'
  const amount = fuel ? fuel.amount : svc!.total

  return (
    <li className={`hrow-wrap${openRow ? ' open' : ''}`}>
      <motion.button type="button" className="hrow-del" aria-label="মুছুন" style={{ opacity: reveal }} onClick={onDelete}><Trash2 size={22} aria-hidden /></motion.button>
      <motion.div className="hrow" style={{ x }} drag="x" dragConstraints={{ left: -88, right: 0 }} dragElastic={0.12} dragMomentum={false}
        onDragEnd={(_, i) => { if (i.offset.x < -60) haptic('warning') }}>
        <button type="button" className="hrow-main" aria-expanded={openRow} onClick={() => { haptic('tick'); setOpenRow(!openRow) }}>
          <span className={`hicon ${fuel ? 'fuel' : 'svc'}`}>{fuel ? <Fuel size={22} aria-hidden /> : <Wrench size={22} aria-hidden />}</span>
          <span className="htext"><b>{title}</b><small>{f.date(log.date)} · {f.km(log.odo)}</small></span>
          <span className="hamount">{f.money(amount)}</span>
          <motion.span animate={{ rotate: openRow ? 180 : 0 }} transition={spring.press} className="hchev"><ChevronDown size={18} aria-hidden /></motion.span>
        </button>
        <AnimatePresence initial={false}>
          {openRow && (
            <motion.div className="hdetail" initial={{ height: 0, opacity: 0 }} animate={{ height: 'auto', opacity: 1, transition: { duration: dur.expand, ease: ease.expand } }} exit={{ height: 0, opacity: 0, transition: { duration: 0.15 } }}>
              <div className="hdetail-in">
                <dl className="kv">
                  {fuel && <>
                    <div><dt>দাম</dt><dd>{f.money(fuel.price)}/লিটার · {fuel.fuel === 'octane' ? 'অকটেন' : 'পেট্রোল'}</dd></div>
                    <div><dt>ট্যাংক</dt><dd>{fuel.full ? 'ফুল ট্যাংক' : 'আংশিক'}{fuel.missedFill ? ' · মাঝের ফিল বাদ' : ''}</dd></div>
                    {fuel.place && <div><dt>পাম্প</dt><dd>{fuel.place}</dd></div>}
                  </>}
                  {svc && <>
                    {svc.lines.map((l, i) => <div key={i}><dt>{l.name}{l.detail ? ` · ${l.detail}` : ''}{l.qty > 1 ? ` × ${f.d(String(l.qty))}` : ''}</dt><dd>{l.price === null ? 'দাম জানা নেই' : f.money(l.price * l.qty)}</dd></div>)}
                    {svc.labour > 0 && <div><dt>সার্ভিস চার্জ</dt><dd>{f.money(svc.labour)}</dd></div>}
                    {svc.discount > 0 && <div><dt>ছাড়</dt><dd>−{f.money(svc.discount)}</dd></div>}
                    {svc.workshop && <div><dt>ওয়ার্কশপ</dt><dd>{svc.workshop}{svc.workshopPhone ? ` · ${svc.workshopPhone}` : ''}</dd></div>}
                  </>}
                </dl>
                {svc && (svc.visitState === 'quote' || svc.repairCase || svc.warrantyMonths) && (
                  <p className="svc-badges">
                    {svc.visitState === 'quote' && <span className="tag">কোটেশন</span>}
                    {svc.repairCase && <span className="tag">সমস্যা: {svc.repairCase}</span>}
                    {svc.warrantyMonths ? <span className="tag">ওয়ারেন্টি {f.d(String(svc.warrantyMonths))} মাস</span> : null}
                  </p>
                )}
                {fuel && !fuel.full && <p className="hnote">পরের ফুল ট্যাংকে মাইলেজ হিসাব হবে</p>}
                <div className="hactions">
                  <button type="button" className="hact" onClick={onEdit}><Pencil size={16} aria-hidden /> ঠিক করুন</button>
                  <button type="button" className="hact danger" onClick={onDelete}><Trash2 size={16} aria-hidden /> মুছুন</button>
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </motion.div>
    </li>
  )
}

export function History() {
  const logs = useStore((s) => s.logs)
  const removeLog = useStore((s) => s.removeLog)
  const restoreLog = useStore((s) => s.restoreLog)
  const showToast = useUI((s) => s.showToast)
  const open = useUI((s) => s.open)
  const f = useFmt()
  const [period, setPeriod] = useState(0)
  const [category, setCategory] = useState(0)
  const today = todayISO()

  const since = period === 0 ? addDays(today, -29) : period === 1 ? `${today.slice(0, 7)}-01` : ''
  const view = useMemo(() => logs.filter((l) => l.date >= since && l.date <= today && (!category || (l.type === 'fuel' ? 1 : 2) === category)).sort((a, b) => (a.date < b.date ? 1 : a.date > b.date ? -1 : b.odo - a.odo)), [logs, category, since, today])
  const groups = useMemo(() => {
    const m = new Map<string, LogEntry[]>()
    view.forEach((l) => { const k = l.date.slice(0, 7); m.set(k, [...(m.get(k) ?? []), l]) })
    return [...m.entries()]
  }, [view])

  const del = (id: string) => {
    const r = removeLog(id)
    if (!r) return
    haptic('warning')
    showToast('রেকর্ড মুছে গেছে', () => restoreLog(r.entry, r.index))
  }

  const topRef = useRef<HTMLDivElement>(null)
  return (
    <div ref={topRef} className="screen-pad">
      <header className="page-head"><h1>রেকর্ড</h1></header>
      <CostInsights recorded={logs.map(log => ({ date: log.date, category: log.type === 'fuel' ? 1 : 2, amount: log.type === 'fuel' ? log.amount : log.total, km: log.odo }))} onFilter={(p, c) => { setPeriod(p); setCategory(c) }} />
      <WorkLedgerCard />
      {groups.length === 0 && <div className="empty"><p>এখনও কিছু নেই। নিচের + চাপুন।</p></div>}
      {groups.map(([key, items]) => (
        <section key={key} aria-label={f.month(`${key}-01`)}>
          <h3 className="month sticky">{f.month(`${key}-01`)}</h3>
          <ul className="hlist">
            <AnimatePresence initial={false}>
              {items.map((l) => (
                <motion.div key={l.id} layout exit={{ opacity: 0, height: 0, transition: { duration: 0.2 } }} transition={spring.list}>
                  <Row log={l} onDelete={() => del(l.id)} onEdit={() => open(l.type === 'fuel' ? 'fuel' : 'service', { id: l.id })} />
                </motion.div>
              ))}
            </AnimatePresence>
          </ul>
        </section>
      ))}
      {view.length > 6 && (
        <button type="button" className="top-jump" onClick={() => { haptic('tick'); topRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' }) }}>
          <ArrowUp size={15} aria-hidden /> উপরে ফিরে যান
        </button>
      )}
    </div>
  )
}

/** P14 entry point, as shipped in Compose HistoryScreen (WorkLedgerHistoryCard). */
function WorkLedgerCard() {
  const workMode = useStore((s) => Boolean(s.settings.workMode))
  const shifts = useStore((s) => s.workShifts)
  const logs = useStore((s) => s.logs)
  const open = useUI((s) => s.open)
  const f = useFmt()
  const summary = summariseWork(shifts ?? [], logs)
  return (
    <section className="card-stack" aria-label="রাইডার ওয়ার্ক লেজার">
      <button type="button" className="setting" style={{ textAlign: 'left', width: '100%' }} onClick={() => { haptic('tick'); open('work-ledger') }}>
        <b>রাইডার ওয়ার্ক লেজার (ঐচ্ছিক)</b>
        <small className="cap" style={{ display: 'block' }}>
          {workMode ? `${f.d(String(summary.shifts))}টি শিফট · নিট ${f.money(summary.netAfterAllocatedPoisha / 100)}` : 'ডিফল্টভাবে বন্ধ — বন্ধ করলেও সংরক্ষিত শিফটের তথ্য থেকে যাবে'}
        </small>
        <span className="link" style={{ padding: 0 }}>ওয়ার্ক লেজার দেখুন</span>
      </button>
    </section>
  )
}
