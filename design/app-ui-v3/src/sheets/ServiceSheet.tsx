import { useMemo, useState } from 'react'
import { AnimatePresence, motion } from 'motion/react'
import { ChevronDown, Copy, Gauge as GaugeIcon, Minus, Plus, Sparkles, Trash2, Wrench } from 'lucide-react'
import { restoreData, snapshotData, useStore, useUI } from '../store'
import { ITEMS, VISCOSITY, itemOf } from '../data/catalog'
import { addDays, todayISO } from '../lib/dates'
import { useFmt } from '../lib/bn'
import { estimateOdometer } from '../lib/wear'
import { haptic } from '../lib/haptics'
import { spring } from '../lib/motion'
import type { ItemKey, ServiceLine, ServiceLog } from '../types'
import type { ParsedLine } from '../lib/smartParse'
import { Sheet } from '../components/Sheet'
import { Button, Chip } from '../components/ui'
import { Keypad, useEntry, useHardwareKeys, useShow } from '../components/entry'
import { SmartInput } from '../components/SmartInput'

interface Row { uid: string; item: ItemKey | 'custom'; name: string; qty: number; detail?: string }

/**
 * One visit, many parts, no dropdowns. Tap a part to add it with the price you paid last time;
 * tap the price to correct it. Oil specifics, labour and discount stay out of the way until asked for.
 */
export function ServiceSheet({ item, rows: preset }: { item?: ItemKey; rows?: ParsedLine[] }) {
  const ride = useStore((s) => s.ride)!
  const logs = useStore((s) => s.logs)
  const rules = useStore((s) => s.rules)
  const lastItemPrice = useStore((s) => s.lastItemPrice)
  const workshops = useStore((s) => s.workshops)
  const addService = useStore((s) => s.addService)
  const removeLog = useStore((s) => s.removeLog)
  const { close, showToast } = useUI.getState()
  const f = useFmt()
  const show = useShow()
  const today = todayISO()
  const estimate = useMemo(() => estimateOdometer(ride, logs, today), [ride, logs, today])
  const lastService = logs.find((l): l is ServiceLog => l.type === 'service')

  const initialRows = useMemo<Row[]>(() => {
    if (preset?.length) return preset.map((p, i) => ({ uid: `${p.item}-${i}`, item: p.item, name: itemOf(p.item)?.bn ?? '', qty: p.qty }))
    if (item) return [{ uid: item, item, name: itemOf(item)?.bn ?? '', qty: 1 }]
    return []
  }, [item, preset])

  const initialVals = useMemo(() => {
    const v: Record<string, string> = { labour: '', misc: '', discount: '', odo: String(estimate) }
    initialRows.forEach((r) => { const p = preset?.find((x) => `${x.item}-${preset.indexOf(x)}` === r.uid)?.price ?? (r.item !== 'custom' ? lastItemPrice[r.item] : undefined); v[`p:${r.uid}`] = p ? String(p) : '' })
    return v
  }, [])  // eslint-disable-line react-hooks/exhaustive-deps

  const [rows, setRows] = useState<Row[]>(initialRows)
  const [dateKey, setDateKey] = useState<'today' | 'yesterday' | 'custom'>('today')
  const [custom, setCustom] = useState(today)
  const [workshop, setWorkshop] = useState('')
  const [smart, setSmart] = useState(false)
  const [more, setMore] = useState(false)
  const [odoTouched, setOdoTouched] = useState(false)
  const [keys, setKeys] = useState(Boolean(initialRows.length))
  const e = useEntry<string>(initialVals, initialRows[0] ? `p:${initialRows[0].uid}` : 'odo')

  const date = dateKey === 'today' ? today : dateKey === 'yesterday' ? addDays(today, -1) : custom
  const num = (k: string) => (e.vals[k] === '' || e.vals[k] === undefined ? null : Number(e.vals[k]))
  const priceOf = (r: Row) => num(`p:${r.uid}`)
  const subtotal = rows.reduce((a, r) => a + (priceOf(r) ?? 0) * r.qty, 0)
  const total = Math.max(0, subtotal + (num('labour') ?? 0) + (num('misc') ?? 0) - (num('discount') ?? 0))
  const unknown = rows.filter((r) => priceOf(r) === null).length
  const odo = num('odo') ?? 0
  const resets = [...new Set(rows.flatMap((r) => rules.filter((x) => r.item !== 'custom' && itemOf(r.item)?.ruleName && itemOf(r.item)?.ruleName === itemOf(x.item)?.ruleName)))]
  const reason = rows.length === 0 ? 'কী কী করালেন বেছে নিন' : odo <= 0 ? 'কিমি লিখুন' : null

  useHardwareKeys(e.press)

  const focus = (k: string) => { e.setActive(k); setKeys(true) }
  const toggle = (key: ItemKey) => {
    const found = rows.find((r) => r.uid === key)
    if (found) { setRows(rows.filter((r) => r.uid !== key)); return }
    const price = lastItemPrice[key]
    e.set(`p:${key}`, price ? String(price) : '')
    setRows([...rows, { uid: key, item: key, name: itemOf(key)?.bn ?? '', qty: 1 }])
    focus(`p:${key}`)
  }
  const qty = (uid: string, d: number) => { haptic('tick'); setRows(rows.map((r) => (r.uid === uid ? { ...r, qty: Math.max(1, Math.min(20, r.qty + d)) } : r))) }

  const repeatLast = () => {
    if (!lastService) return
    haptic('success')
    const next: Row[] = lastService.lines.map((l, i) => ({ uid: `${l.item}-r${i}`, item: l.item, name: l.name, qty: l.qty, detail: l.detail }))
    lastService.lines.forEach((l, i) => e.set(`p:${l.item}-r${i}`, l.price ? String(l.price) : ''))
    setRows(next); setKeys(false)
  }

  const applyParsed = (lines: ParsedLine[]) => {
    haptic('success')
    const base = rows.length
    const added = lines.map((p, i) => ({ uid: `${p.item}-p${base + i}`, item: p.item, name: itemOf(p.item)?.bn ?? '', qty: p.qty }))
    added.forEach((r, i) => e.set(`p:${r.uid}`, String(lines[i].price ?? lastItemPrice[r.item as ItemKey] ?? '')))
    setRows([...rows, ...added]); setSmart(false)
  }

  const save = () => {
    if (reason) { haptic('error'); return }
    const snap = snapshotData()
    const lines: ServiceLine[] = rows.map((r) => ({ item: r.item, name: r.name, qty: r.qty, price: priceOf(r), detail: r.detail }))
    const id = addService({ date, odo, lines, labour: num('labour') ?? 0, misc: num('misc') ?? 0, discount: num('discount') ?? 0, workshop, notes: '' })
    haptic('success'); close()
    showToast(`সার্ভিস যোগ হয়েছে · ${f.money(total)}${resets.length ? ` · ${f.d(String(resets.length))}টি রিমাইন্ডার নতুন` : ''}`, () => { removeLog(id); restoreData(snap) })
  }

  const cls = (k: string) => (e.active === k && keys ? 'active' : '')

  return (
    <Sheet title="সার্ভিস" detent="tall" footer={
      <>
        <div className="total-row"><span>মোট{unknown > 0 ? ` · ${f.d(String(unknown))}টির দাম জানা নেই` : ''}</span><b>{f.money(total)}</b></div>
        <Button full disabled={Boolean(reason)} icon={Wrench} onClick={save}>{reason ?? 'সার্ভিস সেভ করুন'}</Button>
        <AnimatePresence initial={false}>
          {keys && (
            <motion.div key="kp" className="clip" initial={{ height: 0 }} animate={{ height: 'auto' }} exit={{ height: 0 }} transition={spring.list}>
              <button type="button" className="kp-toggle" aria-label="কীপ্যাড লুকান" onClick={() => setKeys(false)}><ChevronDown size={22} aria-hidden /></button>
              <Keypad onKey={e.press} />
            </motion.div>
          )}
        </AnimatePresence>
      </>
    }>
      <div className="row-wrap">
        {lastService && <Button kind="secondary" size="sm" icon={Copy} onClick={repeatLast}>আগের সার্ভিসের মতো</Button>}
        <Button kind="secondary" size="sm" icon={Sparkles} onClick={() => setSmart(!smart)}>লিখে বা বলে সাজান</Button>
      </div>
      <AnimatePresence initial={false}>
        {smart && (
          <motion.div key="smart" className="clip" initial={{ height: 0, opacity: 0 }} animate={{ height: 'auto', opacity: 1 }} exit={{ height: 0, opacity: 0 }}>
            <SmartInput onApply={applyParsed} />
          </motion.div>
        )}
      </AnimatePresence>

      <div className="chips-grid" role="group" aria-label="কী করালেন">
        {ITEMS.map((it) => (
          <Chip key={it.key} active={rows.some((r) => r.uid === it.key)} onClick={() => toggle(it.key)} hint={lastItemPrice[it.key] ? f.money(lastItemPrice[it.key]!) : undefined}>{it.short}</Chip>
        ))}
      </div>

      <ul className="lines" aria-label="যোগ করা কাজ">
        <AnimatePresence initial={false} mode="popLayout">
          {rows.map((r) => (
            <motion.li key={r.uid} layout className="line" initial={{ opacity: 0, scale: 0.96 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.96 }} transition={spring.list}>
              <div className="line-top">
                <b>{r.name}</b>
                <div className="stepper" aria-label="পরিমাণ">
                  <button type="button" aria-label="কমান" onClick={() => qty(r.uid, -1)}><Minus size={16} /></button>
                  <span>{f.d(String(r.qty))}</span>
                  <button type="button" aria-label="বাড়ান" onClick={() => qty(r.uid, 1)}><Plus size={16} /></button>
                </div>
                <button type="button" className={`price-btn ${cls(`p:${r.uid}`)}`} onClick={() => focus(`p:${r.uid}`)} aria-label={`${r.name} এর দাম`}>
                  {priceOf(r) === null ? 'দাম?' : f.money((priceOf(r) ?? 0) * r.qty)}
                </button>
                <button type="button" className="icon-plain" aria-label="বাদ দিন" onClick={() => { haptic('tick'); setRows(rows.filter((x) => x.uid !== r.uid)) }}><Trash2 size={18} /></button>
              </div>
              {r.item === 'engine_oil' && (
                <div className="row-wrap tight" role="group" aria-label="অয়েলের গ্রেড">
                  {VISCOSITY.map((v) => <Chip key={v} active={r.detail === v} onClick={() => setRows(rows.map((x) => (x.uid === r.uid ? { ...x, detail: x.detail === v ? undefined : v } : x)))}>{v}</Chip>)}
                </div>
              )}
            </motion.li>
          ))}
        </AnimatePresence>
      </ul>

      {resets.length > 0 && <p className="reset-note"><Wrench size={14} aria-hidden /> রিমাইন্ডার নতুন করে গোনা হবে: {resets.map((r) => r.name.split(' ও ')[0].split(' পরিষ্কার')[0]).join(', ')}</p>}

      <div className="row-wrap">
        <Chip active={dateKey === 'today'} onClick={() => setDateKey('today')}>আজ</Chip>
        <Chip active={dateKey === 'yesterday'} onClick={() => setDateKey('yesterday')}>গতকাল</Chip>
        <Chip active={dateKey === 'custom'} onClick={() => setDateKey('custom')}>অন্য দিন</Chip>
      </div>
      {dateKey === 'custom' && <input className="input" type="date" max={today} value={custom} aria-label="তারিখ" onChange={(ev) => setCustom(ev.target.value || today)} />}

      <button type="button" className={`odo-row ${cls('odo')}`} onClick={() => { focus('odo'); setOdoTouched(true) }}>
        <GaugeIcon size={22} aria-hidden />
        <span className="odo-main"><small>ওডোমিটার</small><b>{show(e.vals.odo) || f.d('0')} {f.digits === 'bn' ? 'কিমি' : 'km'}</b></span>
        {!odoTouched && <span className="tag">আনুমানিক</span>}
      </button>

      <div className="row-wrap">
        {workshops.slice(0, 2).map((w) => <Chip key={w} active={workshop === w} onClick={() => setWorkshop(workshop === w ? '' : w)}>{w}</Chip>)}
      </div>
      <input className="input" placeholder="ওয়ার্কশপ বা মেকানিক (ঐচ্ছিক)" aria-label="ওয়ার্কশপ" value={workshop} maxLength={60} onChange={(ev) => setWorkshop(ev.target.value)} />

      <button type="button" className="more-toggle" aria-expanded={more} onClick={() => setMore(!more)}>{more ? 'শ্রম ও অন্যান্য লুকান' : 'শ্রম, অন্যান্য খরচ বা ছাড় আছে?'}</button>
      <AnimatePresence initial={false}>
        {more && (
          <motion.div key="more" className="clip" initial={{ height: 0, opacity: 0 }} animate={{ height: 'auto', opacity: 1 }} exit={{ height: 0, opacity: 0 }}>
            <div className="trio">
              {([['labour', 'শ্রম'], ['misc', 'অন্যান্য'], ['discount', 'ছাড়']] as const).map(([k, label]) => (
                <button key={k} type="button" className={`mini ${cls(k)}`} onClick={() => focus(k)}>
                  <small>{label}</small><b>{e.vals[k] ? f.money(Number(e.vals[k])) : '—'}</b>
                </button>
              ))}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </Sheet>
  )
}
