import { useEffect, useMemo, useState } from 'react'
import { AnimatePresence, motion } from 'motion/react'
import { Camera, Check, ChevronDown, ChevronRight, Copy, Gauge as GaugeIcon, Mic, Minus, Plus, Wrench, X } from 'lucide-react'
import { restoreData, snapshotData, useStore, useUI } from '../store'
import { ITEMS, VISCOSITY, itemOf } from '../data/catalog'
import { PartPicker, WorkshopPicker } from './servicePickers'
import { addDays, todayISO, validISO } from '../lib/dates'
import { useFmt } from '../lib/bn'
import { estimateOdometer, wearOf } from '../lib/wear'
import { haptic } from '../lib/haptics'
import { ease, spring } from '../lib/motion'
import type { ItemKey, ServiceLine, ServiceLog } from '../types'
import type { ParsedLine } from '../lib/smartParse'
import { Sheet } from '../components/Sheet'
import { Button, Chip, IconButton } from '../components/ui'
import { Keypad, useEntry, useHardwareKeys, useShow } from '../components/entry'
import { SmartInput } from '../components/SmartInput'

interface Row { uid: string; item: ItemKey | 'custom'; name: string; qty: number; detail?: string }
type BillCam = 'off' | 'scanning' | 'done'

const BILL_LINES: ParsedLine[] = [
  { item: 'engine_oil', qty: 1, price: 900, source: 'ইঞ্জিন অয়েল ৯০০ টাকা' },
  { item: 'oil_filter', qty: 1, price: 250, source: 'অয়েল ফিল্টার ২৫০ টাকা' },
]

/**
 * One visit as a bill: only the parts that were done are listed, each with its price.
 * Due parts are suggested first, the full list lives in a picker, and the keypad
 * opens only while a price or the km is being typed.
 */
export function ServiceSheet({ item, rows: preset, existingId }: { item?: ItemKey; rows?: ParsedLine[]; existingId?: string }) {
  const ride = useStore((s) => s.ride)!
  const logs = useStore((s) => s.logs)
  const rules = useStore((s) => s.rules)
  const lastItemPrice = useStore((s) => s.lastItemPrice)
  const workshops = useStore((s) => s.workshops)
  const addService = useStore((s) => s.addService)
  const updateService = useStore((s) => s.updateService)
  const removeLog = useStore((s) => s.removeLog)
  const { close, showToast } = useUI.getState()
  const f = useFmt()
  const show = useShow()
  const today = todayISO()
  const estimate = useMemo(() => estimateOdometer(ride, logs, today), [ride, logs, today])
  const existing = logs.find((l): l is ServiceLog => l.id === existingId && l.type === 'service')
  const lastService = logs.find((l): l is ServiceLog => l.type === 'service')

  const initialRows = useMemo<Row[]>(() => {
    if (existing) return existing.lines.map((l, i) => ({ uid: l.item !== 'custom' && existing.lines.filter((x) => x.item === l.item).length === 1 ? l.item : `${l.item}-e${i}`, item: l.item, name: l.name, qty: l.qty, detail: l.detail }))
    if (preset?.length) return preset.map((p, i) => ({ uid: `${p.item}-${i}`, item: p.item, name: itemOf(p.item)?.bn ?? '', qty: p.qty }))
    if (item) return [{ uid: item, item, name: itemOf(item)?.bn ?? '', qty: 1 }]
    return []
  }, [existing, item, preset])

  const initialVals = useMemo(() => {
    const v: Record<string, string> = {
      labour: existing && existing.labour > 0 ? String(existing.labour) : '',
      misc: '',
      discount: existing && existing.discount > 0 ? String(existing.discount) : '',
      odo: String(existing ? existing.odo : estimate),
    }
    initialRows.forEach((r, idx) => {
      const p = existing
        ? existing.lines[idx]?.price
        : (preset?.find((x) => `${x.item}-${preset.indexOf(x)}` === r.uid)?.price ?? (r.item !== 'custom' ? lastItemPrice[r.item] : undefined))
      v[`p:${r.uid}`] = p !== null && p !== undefined ? String(p) : ''
    })
    return v
  }, [])  // eslint-disable-line react-hooks/exhaustive-deps

  const [rows, setRows] = useState<Row[]>(initialRows)
  const [dateKey, setDateKey] = useState<'today' | 'yesterday' | 'custom'>(existing ? 'custom' : 'today')
  const [custom, setCustom] = useState(existing?.date ?? today)
  const [workshop, setWorkshop] = useState(existing?.workshop ?? '')
  const [gradeUid, setGradeUid] = useState<string | null>(null)
  const [smart, setSmart] = useState(false)
  const [billCam, setBillCam] = useState<BillCam>('off')
  const [more, setMore] = useState(Boolean(existing && (existing.visitState === 'quote' || existing.workshopPhone || existing.repairCase || existing.warrantyMonths)))
  const [extras, setExtras] = useState(false)
  const [picker, setPicker] = useState<'none' | 'parts' | 'workshop'>('none')
  const [odoTouched, setOdoTouched] = useState(Boolean(existing))
  // The keypad shows only while a price or km field is being typed.
  const [keys, setKeys] = useState(false)
  const [visitState, setVisitState] = useState<'paid' | 'quote'>(existing?.visitState ?? 'paid')
  const [workshopPhone, setWorkshopPhone] = useState(existing?.workshopPhone ?? '')
  const [repairCase, setRepairCase] = useState(existing?.repairCase ?? '')
  const [warrantyMonths, setWarrantyMonths] = useState(existing?.warrantyMonths ?? 0)
  const e = useEntry<string>(initialVals, initialRows[0] ? `p:${initialRows[0].uid}` : 'odo')

  useEffect(() => {
    if (billCam !== 'scanning') return
    const timer = window.setTimeout(() => {
      setBillCam('done')
      haptic('success')
    }, 1100)
    return () => window.clearTimeout(timer)
  }, [billCam])

  const date = dateKey === 'today' ? today : dateKey === 'yesterday' ? addDays(today, -1) : custom
  const num = (k: string) => (e.vals[k] === '' || e.vals[k] === undefined ? null : Number(e.vals[k]))
  const priceOf = (r: Row) => num(`p:${r.uid}`)
  const subtotal = rows.reduce((a, r) => a + (priceOf(r) ?? 0) * r.qty, 0)
  const total = Math.max(0, subtotal + (num('labour') ?? 0) - (num('discount') ?? 0))
  const unknown = rows.filter((r) => priceOf(r) === null).length
  const odo = num('odo') ?? 0
  const resets = [...new Set(rows.flatMap((r) => rules.filter((x) => r.item !== 'custom' && itemOf(r.item)?.ruleName && itemOf(r.item)?.ruleName === itemOf(x.item)?.ruleName)))]
  const reason = existingId && !existing ? 'রেকর্ডটি আর পাওয়া যাচ্ছে না'
    : rows.length === 0 ? 'কী কী করালেন বেছে নিন'
      : odo <= 0 || odo > 9_999_999 ? 'সঠিক কিমি লিখুন'
        : !odoTouched ? 'মিটার রিডিং মিলিয়ে নিন'
          : !validISO(date) ? 'সঠিক তারিখ দিন'
            : date > today ? 'ভবিষ্যতের তারিখ দেওয়া যাবে না'
              : null

  const pressKey = (k: string) => { if (e.active === 'odo') setOdoTouched(true); e.press(k) }
  useHardwareKeys(picker === 'none' ? pressKey : () => {})

  const focus = (k: string) => { e.setActive(k); setKeys(true) }
  const toggle = (key: ItemKey) => {
    if (rows.some((r) => r.uid === key)) return
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
    setRows([...rows, ...added]); setSmart(false); setBillCam('off')
  }

  const save = () => {
    if (reason) { haptic('error'); return }
    const snap = snapshotData()
    const lines: ServiceLine[] = rows.map((r) => ({ item: r.item, name: r.name, qty: r.qty, price: priceOf(r), detail: r.detail }))
    const payload = {
      date, odo, lines, labour: num('labour') ?? 0, misc: existing?.misc ?? 0, discount: num('discount') ?? 0,
      workshop, notes: existing?.notes ?? '', visitState,
      workshopPhone: workshopPhone.trim() || undefined,
      repairCase: repairCase.trim() || undefined,
      warrantyMonths: warrantyMonths || undefined,
    }
    if (existing) {
      updateService(existing.id, payload)
      haptic('success'); close()
      showToast('সার্ভিস রেকর্ড ঠিক হয়েছে', () => restoreData(snap))
      return
    }
    const id = addService(payload)
    haptic('success'); close()
    const resetNote = resets.length && visitState === 'paid' ?` · ${f.d(String(resets.length))}টি রিমাইন্ডার নতুন` : ''
    showToast(`সার্ভিস যোগ হয়েছে · ${f.money(total)}${resetNote}`, () => { removeLog(id); restoreData(snap) })
  }

  const cls = (k: string) => (e.active === k && keys ? 'active' : '')
  const dueKeys = rules.filter((r) => wearOf(r, ride.odometer, today).status !== 'ok' && wearOf(r, ride.odometer, today).status !== 'unknown').map((r) => r.item)
  const picked = rows.filter((r) => r.item !== 'custom').map((r) => r.item as ItemKey)
  const suggestions = [...new Set([...dueKeys, ...ITEMS.filter((it) => lastItemPrice[it.key]).map((it) => it.key), ...ITEMS.map((it) => it.key)])].filter((k) => !picked.includes(k)).slice(0, 6)
  const showExtras = extras || Boolean(num('labour')) || Boolean(num('discount'))
  const dateLabel = dateKey === 'today' ? 'আজ' : dateKey === 'yesterday' ? 'গতকাল' : validISO(date) ? f.date(date) : 'তারিখ'

  const applyPicker = (keys: ItemKey[], customName: string | null) => {
    haptic('success')
    let next = rows.filter((r) => r.item === 'custom' || keys.includes(r.item as ItemKey))
    keys.filter((k) => !next.some((r) => r.item === k)).forEach((k) => {
      e.set(`p:${k}`, lastItemPrice[k] ? String(lastItemPrice[k]) : '')
      next = [...next, { uid: k, item: k, name: itemOf(k)?.bn ?? '', qty: 1 }]
    })
    if (customName) {
      const uid = `custom-${Date.now()}`
      e.set(`p:${uid}`, '')
      next = [...next, { uid, item: 'custom', name: customName, qty: 1 }]
      focus(`p:${uid}`)
    }
    setRows(next)
    setPicker('none')
  }

  if (picker === 'parts') {
    return (
      <Sheet title="কাজ বেছে নিন" onClose={() => setPicker('none')} closeLabel="কাজ বাছাই বন্ধ করুন" detent="tall">
        <PartPicker selected={picked} lastItemPrice={lastItemPrice} due={dueKeys} onDone={applyPicker} />
      </Sheet>
    )
  }
  if (picker === 'workshop') {
    return (
      <Sheet title="ওয়ার্কশপ" onClose={() => setPicker('none')} closeLabel="ওয়ার্কশপ বাছাই বন্ধ করুন">
        <WorkshopPicker value={workshop} recent={workshops} onPick={(w) => { haptic('tick'); setWorkshop(w); setPicker('none') }} />
      </Sheet>
    )
  }

  const openPicker = (p: 'parts' | 'workshop') => { haptic('tick'); setKeys(false); setPicker(p) }

  return (
    <Sheet title={existing ? 'সার্ভিস রেকর্ড ঠিক করুন' : 'সার্ভিস'} actions={<>
      <IconButton icon={Camera} label="রশিদের ছবি তুলুন" tone={billCam !== 'off' ? 'tint' : undefined} onClick={() => { setSmart(false); setKeys(false); setBillCam(billCam === 'off' ? 'scanning' : 'off') }} />
      <IconButton icon={Mic} label="মুখে বলুন বা লিখুন" tone={smart ? 'tint' : undefined} onClick={() => { setBillCam('off'); setKeys(false); setSmart(!smart) }} />
      {lastService && <IconButton icon={Copy} label="আগের সার্ভিসের মতো" onClick={repeatLast} />}
    </>} footer={
      <>
        <div className="total-row"><span>মোট{unknown > 0 ? ` · ${f.d(String(unknown))}টির দাম জানা নেই` : ''}</span><b>{f.money(total)}</b></div>
        <Button full disabled={Boolean(reason)} icon={Wrench} onClick={save}>{reason ?? (visitState === 'quote' ? 'কোটেশন সেভ করুন' : 'সার্ভিস সেভ করুন')}</Button>
        <AnimatePresence initial={false}>
          {keys && (
            <motion.div key="kp" className="clip" initial={{ height: 0 }} animate={{ height: 'auto' }} exit={{ height: 0 }} transition={spring.list}>
              <button type="button" className="kp-toggle" aria-label="কীপ্যাড লুকান" onClick={() => setKeys(false)}><ChevronDown size={22} aria-hidden /></button>
              <Keypad onKey={pressKey} />
            </motion.div>
          )}
        </AnimatePresence>
      </>
    }>
      <AnimatePresence initial={false}>
        {billCam !== 'off' && (
          <motion.div key="bill-cam" className="cam" initial={{ opacity: 0, scale: 0.96 }} animate={{ opacity: 1, scale: 1, transition: spring.sheet }} exit={{ opacity: 0, transition: { duration: 0.15, ease: ease.sky } }}>
            <div className="cam-dash" aria-hidden>
              <svg viewBox="0 0 320 132">
                <rect x="16" y="10" width="288" height="112" rx="12" fill="#f7f8f6" stroke="#c8d0ca" strokeWidth="2" />
                <text x="34" y="42" style={{ fontSize: '15px', fontWeight: 700, fill: '#1f2922' }}>সার্ভিস ক্যাশ মেমো</text>
                <text x="34" y="70" style={{ fontSize: '14px', fill: '#354238' }}>১. ইঞ্জিন অয়েল ............ ৯০০/-</text>
                <text x="34" y="96" style={{ fontSize: '14px', fill: '#354238' }}>২. অয়েল ফিল্টার .......... ২৫০/-</text>
              </svg>
              <div className="cam-reticle" />
              {billCam === 'scanning' && <motion.i className="scan-line" initial={{ y: 18 }} animate={{ y: [18, 108, 18] }} transition={{ duration: 1.1, ease: 'easeInOut', repeat: Infinity }} />}
            </div>
            {billCam === 'scanning' ? (
              <div className="cam-result">
                <p className="cam-msg">রশিদ স্ক্যান করা হচ্ছে…</p>
                <Button size="sm" kind="secondary" onClick={() => setBillCam('off')}>বাতিল করুন</Button>
              </div>
            ) : (
              <div className="cam-result">
                <p className="cam-msg">ডেমো ছবি · পাওয়া গেছে:<b>ইঞ্জিন অয়েল {f.money(900)}</b>, <b>অয়েল ফিল্টার {f.money(250)}</b></p>
                <div className="row-wrap">
                  <Button size="sm" onClick={() => applyParsed(BILL_LINES)}>এই কাজগুলো যোগ করুন</Button>
                  <Button size="sm" kind="secondary" onClick={() => setBillCam('off')}>বাতিল করুন</Button>
                </div>
              </div>
            )}
          </motion.div>
        )}
        {smart && (
          <motion.div key="smart" className="clip" style={{ flexShrink: 0 }} initial={{ height: 0, opacity: 0 }} animate={{ height: 'auto', opacity: 1 }} exit={{ height: 0, opacity: 0 }}>
            <SmartInput onApply={applyParsed} />
          </motion.div>
        )}
      </AnimatePresence>

      <div className={`odo-row compact ${cls('odo')}`}>
        <button type="button" className="odo-hit" onClick={() => focus('odo')}>
          <GaugeIcon size={20} aria-hidden />
          <span className="odo-main">
            <small>{odoTouched ? 'মিটার রিডিং ও তারিখ' : 'মিটার রিডিং · আনুমানিক'}</small>
            <b>{show(e.vals.odo) || f.d('0')} {f.digits === 'bn' ? 'কিমি' : 'km'} · {dateLabel}</b>
          </span>
        </button>
        {!odoTouched && <button type="button" className="pill confirm" onClick={() => { haptic('tick'); setOdoTouched(true) }}><Check size={13} aria-hidden /> ঠিক আছে</button>}
      </div>
      {e.active === 'odo' && keys && (
        <div className="chip-line" role="group" aria-label="তারিখ">
          <Chip active={dateKey === 'today'} onClick={() => setDateKey('today')}>আজ</Chip>
          <Chip active={dateKey === 'yesterday'} onClick={() => setDateKey('yesterday')}>গতকাল</Chip>
          {dateKey === 'custom'
            ? <input className="chip chip-date on" type="date" max={today} value={custom} aria-label="তারিখ" onChange={(ev) => setCustom(ev.target.value || today)} />
            : <Chip onClick={() => setDateKey('custom')}>অন্য দিন</Chip>}
        </div>
      )}

      {rows.length === 0 ? (
        <section className="bill-empty" aria-label="কী কী কাজ হয়েছে">
          <p className="bill-title">কী কী কাজ হয়েছে?</p>
          <div className="chip-line" role="group" aria-label="সাধারণ কাজ">
            {suggestions.map((k) => (
              <Chip key={k} icon={Plus} onClick={() => toggle(k)} hint={lastItemPrice[k] ? f.money(lastItemPrice[k]!) : undefined}>
                {dueKeys.includes(k) && <i className="due-dot" aria-label="সময় হয়েছে" />}{itemOf(k)?.short}
              </Chip>
            ))}
          </div>
          <button type="button" className="pump-field" onClick={() => openPicker('parts')}>
            <Plus size={18} aria-hidden /><span>অন্য কাজ বা পার্টস খুঁজুন</span><ChevronRight size={18} aria-hidden className="muted-icon" />
          </button>
        </section>
      ) : (
        <section className="bill" aria-label="যোগ করা কাজ">
          <ul>
            <AnimatePresence initial={false} mode="popLayout">
              {rows.map((r) => {
                const activeLine = e.active === `p:${r.uid}` && keys
                return (
                  <motion.li key={r.uid} layout className={`bill-line${activeLine ? ' on' : ''}`} initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} transition={spring.list}>
                    <div className="bill-main">
                      <span className="bill-name">
                        <b>{itemOf(r.item)?.short ?? r.name}{r.qty > 1 ? ` × ${f.d(String(r.qty))}` : ''}</b>
                        {r.item === 'engine_oil' && (
                          <button type="button" className={`pill${r.detail ? ' active' : ''}`} onClick={() => { haptic('tick'); setGradeUid(gradeUid === r.uid ? null : r.uid) }}>
                            {r.detail ?? 'গ্রেড?'}
                          </button>
                        )}
                      </span>
                      <button type="button" className={`price-btn ${cls(`p:${r.uid}`)}`} onClick={() => focus(`p:${r.uid}`)} aria-label={`${r.name} এর দাম`}>
                        {priceOf(r) === null ? 'দাম?' : f.money((priceOf(r) ?? 0) * r.qty)}
                      </button>
                      <button type="button" className="icon-plain" aria-label={`${r.name} বাদ দিন`} onClick={() => { haptic('tick'); setRows(rows.filter((x) => x.uid !== r.uid)) }}><X size={18} /></button>
                    </div>
                    {gradeUid === r.uid && (
                      <div className="chip-line" role="group" aria-label="অয়েলের গ্রেড">
                        {VISCOSITY.map((v) => <Chip key={v} active={r.detail === v} onClick={() => { setRows(rows.map((x) => (x.uid === r.uid ? { ...x, detail: x.detail === v ? undefined : v } : x))); setGradeUid(null) }}>{v}</Chip>)}
                      </div>
                    )}
                    {activeLine && r.item !== 'general_service' && (
                      <div className="bill-qty">
                        <span>পরিমাণ</span>
                        <div className="stepper" aria-label="পরিমাণ">
                          <button type="button" aria-label="কমান" onClick={() => qty(r.uid, -1)}><Minus size={16} /></button>
                          <span>{f.d(String(r.qty))}</span>
                          <button type="button" aria-label="বাড়ান" onClick={() => qty(r.uid, 1)}><Plus size={16} /></button>
                        </div>
                      </div>
                    )}
                  </motion.li>
                )
              })}
              {showExtras && ([['labour', 'সার্ভিস চার্জ'], ['discount', 'ছাড়']] as const).map(([k, label]) => (
                <motion.li key={k} layout className="bill-line extra" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
                  <div className="bill-main">
                    <span className="bill-name"><b>{label}</b></span>
                    <button type="button" className={`price-btn ${cls(k)}`} onClick={() => focus(k)} aria-label={label}>
                      {e.vals[k] ? `${k === 'discount' ? '−' : ''}${f.money(Number(e.vals[k]))}` : '৳০'}
                    </button>
                  </div>
                </motion.li>
              ))}
            </AnimatePresence>
          </ul>
          <div className="bill-add">
            <button type="button" onClick={() => openPicker('parts')}><Plus size={16} aria-hidden /> আরও কাজ</button>
            {!showExtras && <button type="button" onClick={() => { haptic('tick'); setExtras(true); focus('labour') }}><Plus size={16} aria-hidden /> চার্জ / ছাড়</button>}
          </div>
        </section>
      )}

      {resets.length > 0 && visitState === 'paid' && <p className="reset-note"><Wrench size={14} aria-hidden /> রিমাইন্ডার নতুন করে গোনা হবে: {resets.map((r) => r.name.split(' ও ')[0].split(' পরিষ্কার')[0]).join(', ')}</p>}

      <button type="button" className={`pump-field${workshop ? ' set' : ''}`} onClick={() => openPicker('workshop')}>
        <Wrench size={18} aria-hidden /><span>{workshop || 'ওয়ার্কশপ বেছে নিন (ঐচ্ছিক)'}</span><ChevronRight size={18} aria-hidden className="muted-icon" />
      </button>

      <button type="button" className="more-toggle" aria-expanded={more} onClick={() => { setKeys(false); setMore(!more) }}>
        {more ? 'আরও তথ্য লুকান' : `আরও তথ্য${visitState === 'quote' ? ' · কোটেশন' : ''}${warrantyMonths ? ` · ওয়ারেন্টি ${f.d(String(warrantyMonths))} মাস` : ''} (ফোন, ওয়ারেন্টি, কোটেশন)`}
      </button>
      <AnimatePresence initial={false}>
        {more && (
          <motion.div key="more" className="clip more-box" initial={{ height: 0, opacity: 0 }} animate={{ height: 'auto', opacity: 1 }} exit={{ height: 0, opacity: 0 }}>
            <Chip active={visitState === 'quote'} onClick={() => setVisitState(visitState === 'quote' ? 'paid' : 'quote')}>শুধু হিসাব / কোটেশন (কাজ এখনো হয়নি)</Chip>
            {visitState === 'quote' && <p className="field-msg">কোটেশন সেভ হলে রিমাইন্ডার নতুন করে গোনা হবে না।</p>}
            <input className="input input-sm" inputMode="tel" placeholder="ওয়ার্কশপের ফোন (ঐচ্ছিক)" aria-label="ওয়ার্কশপের ফোন" value={workshopPhone} maxLength={20} onFocus={() => setKeys(false)} onChange={(ev) => setWorkshopPhone(ev.target.value)} />
            <input className="input input-sm" placeholder="বারবার হওয়া সমস্যা (ঐচ্ছিক)" aria-label="বারবার হওয়া সমস্যা" value={repairCase} maxLength={80} onFocus={() => setKeys(false)} onChange={(ev) => setRepairCase(ev.target.value)} />
            <div className="chip-line" role="group" aria-label="ওয়ারেন্টি">
              {[0, 6, 12].map((m) => <Chip key={m} active={warrantyMonths === m} onClick={() => setWarrantyMonths(m)}>{m === 0 ? 'ওয়ারেন্টি নেই' : `${f.d(String(m))} মাস ওয়ারেন্টি`}</Chip>)}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </Sheet>
  )
}
