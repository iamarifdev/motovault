import { useMemo, useState } from 'react'
import { AnimatePresence, motion, useMotionValue, useTransform } from 'motion/react'
import { ChevronDown, Fuel, Trash2, Wrench } from 'lucide-react'
import { useStore, useUI } from '../store'
import { useFmt } from '../lib/bn'
import { economyOf, fuelLogs } from '../lib/wear'
import { addDays, todayISO } from '../lib/dates'
import { dur, ease, spring } from '../lib/motion'
import { haptic } from '../lib/haptics'
import type { FuelLog, LogEntry, ServiceLog } from '../types'
import { Chip } from '../components/ui'

type Filter = 'all' | 'service' | 'fuel'

function Row({ log, onDelete }: { log: LogEntry; onDelete: () => void }) {
  const f = useFmt()
  const [openRow, setOpenRow] = useState(false)
  const x = useMotionValue(0)
  const reveal = useTransform(x, [-88, -16, 0], [1, 0.5, 0])
  const fuel = log.type === 'fuel' ? (log as FuelLog) : null
  const svc = log.type === 'service' ? (log as ServiceLog) : null
  const title = fuel ? `তেল ভরা · ${f.liters(fuel.liters)}` : svc!.lines.length > 1 ? `${svc!.lines[0].name} ও আরও ${f.d(String(svc!.lines.length - 1))}টি` : svc!.lines[0]?.name ?? 'সার্ভিস'
  const amount = fuel ? fuel.amount : svc!.total

  return (
    <li className="hrow-wrap">
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
                {fuel && <>
                  <p>{f.money(fuel.price)} প্রতি লিটার · {fuel.fuel === 'octane' ? 'অকটেন' : 'পেট্রোল'}</p>
                  <p>{fuel.full ? 'ট্যাংক পূর্ণ করা হয়েছে' : 'আংশিক, পরের পূর্ণ ট্যাংকে হিসাব হবে'}</p>
                  {fuel.place && <p>{fuel.place}</p>}
                </>}
                {svc && <>
                  {svc.lines.map((l, i) => <p key={i} className="line-p"><span>{l.name}{l.detail ? ` · ${l.detail}` : ''}{l.qty > 1 ? ` × ${f.d(String(l.qty))}` : ''}</span><b>{l.price === null ? 'দাম জানা নেই' : f.money(l.price * l.qty)}</b></p>)}
                  {svc.labour > 0 && <p className="line-p"><span>শ্রম</span><b>{f.money(svc.labour)}</b></p>}
                  {svc.discount > 0 && <p className="line-p"><span>ছাড়</span><b>−{f.money(svc.discount)}</b></p>}
                  {svc.workshop && <p>{svc.workshop}</p>}
                </>}
                <button type="button" className="link danger" onClick={onDelete}><Trash2 size={16} aria-hidden /> রেকর্ড মুছুন</button>
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
  const f = useFmt()
  const [filter, setFilter] = useState<Filter>('all')
  const today = todayISO()

  const view = useMemo(() => logs.filter((l) => filter === 'all' || l.type === filter).sort((a, b) => (a.date < b.date ? 1 : a.date > b.date ? -1 : b.odo - a.odo)), [logs, filter])
  const groups = useMemo(() => {
    const m = new Map<string, LogEntry[]>()
    view.forEach((l) => { const k = l.date.slice(0, 7); m.set(k, [...(m.get(k) ?? []), l]) })
    return [...m.entries()]
  }, [view])

  const since = addDays(today, -30)
  const recent = logs.filter((l) => l.date >= since)
  const spend = recent.reduce((a, l) => a + (l.type === 'fuel' ? l.amount : l.total), 0)
  const fuel = fuelLogs(logs)
  const km = recent.length > 1 ? Math.max(...recent.map((l) => l.odo)) - Math.min(...recent.map((l) => l.odo)) : 0
  const econ = economyOf(logs)

  const del = (id: string) => {
    const r = removeLog(id)
    if (!r) return
    haptic('warning')
    showToast('রেকর্ড মুছে গেছে', () => restoreLog(r.entry, r.index))
  }

  return (
    <div className="screen-pad">
      <header className="page-head"><h1>ইতিহাস</h1></header>
      <section className="summary" aria-label="গত ৩০ দিন">
        <div><small>গত ৩০ দিনের খরচ</small><b>{f.money(spend)}</b></div>
        <div><small>চালানো</small><b>{f.km(km)}</b></div>
        <div><small>জ্বালানি গড়</small><b>{econ === null ? '--' : `${f.d(econ.toFixed(1))} ${f.digits === 'bn' ? 'কিমি/লি' : 'km/L'}`}</b></div>
      </section>
      <div className="row-wrap" role="group" aria-label="ফিল্টার">
        {(['all', 'service', 'fuel'] as const).map((k) => <Chip key={k} active={filter === k} onClick={() => setFilter(k)}>{k === 'all' ? 'সব' : k === 'service' ? 'সার্ভিস' : 'জ্বালানি'}</Chip>)}
        <span className="grow" /><span className="cap">{f.d(String(view.length))}টি রেকর্ড</span>
      </div>

      {groups.length === 0 && <div className="empty"><p>এখনও কিছু নেই। নিচের + চাপুন।</p></div>}
      {groups.map(([key, items]) => (
        <section key={key} aria-label={f.month(`${key}-01`)}>
          <h3 className="month">{f.month(`${key}-01`)}</h3>
          <ul className="hlist">
            <AnimatePresence initial={false}>
              {items.map((l) => (
                <motion.div key={l.id} layout exit={{ opacity: 0, height: 0, transition: { duration: 0.2 } }} transition={spring.list}>
                  <Row log={l} onDelete={() => del(l.id)} />
                </motion.div>
              ))}
            </AnimatePresence>
          </ul>
        </section>
      ))}
      {fuel.length === 0 && null}
    </div>
  )
}
