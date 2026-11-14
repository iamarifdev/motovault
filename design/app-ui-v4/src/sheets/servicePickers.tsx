import { useState } from 'react'
import { Check, History, Plus, Search } from 'lucide-react'
import { ITEMS } from '../data/catalog'
import { useFmt } from '../lib/bn'
import { haptic } from '../lib/haptics'
import type { ItemKey } from '../types'
import { Button } from '../components/ui'

/** Full part list with search. Rows already on the bill start ticked; Done applies the whole selection. */
export function PartPicker({ selected, lastItemPrice, due, onDone }: {
  selected: ItemKey[]
  lastItemPrice: Partial<Record<ItemKey, number>>
  due: ItemKey[]
  onDone: (keys: ItemKey[], customName: string | null) => void
}) {
  const f = useFmt()
  const [picked, setPicked] = useState<ItemKey[]>(selected)
  const [query, setQuery] = useState('')
  const q = query.trim().toLowerCase()
  const match = (it: (typeof ITEMS)[number]) => !q || it.bn.toLowerCase().includes(q) || it.short.toLowerCase().includes(q)
  const regular = ITEMS.filter((it) => it.ruleName && match(it))
  const other = ITEMS.filter((it) => !it.ruleName && match(it))
  const exact = ITEMS.some((it) => it.bn === query.trim() || it.short === query.trim())
  const toggle = (k: ItemKey) => { haptic('tick'); setPicked((cur) => (cur.includes(k) ? cur.filter((x) => x !== k) : [...cur, k])) }
  const added = picked.filter((k) => !selected.includes(k)).length
  const removed = selected.filter((k) => !picked.includes(k)).length

  const group = (title: string, items: typeof ITEMS) => items.length > 0 && (
    <>
      <p className="cap">{title}</p>
      <ul className="pick-list">
        {items.map((it) => {
          const on = picked.includes(it.key)
          return (
            <li key={it.key}>
              <button type="button" className={on ? 'on' : ''} aria-pressed={on} onClick={() => toggle(it.key)}>
                <span className={`tick${on ? ' on' : ''}`}>{on && <Check size={14} aria-hidden />}</span>
                <span>{it.bn}{due.includes(it.key) && <small className="due">সময় হয়েছে</small>}</span>
                {lastItemPrice[it.key] ? <small className="pick-price">শেষ {f.money(lastItemPrice[it.key]!)}</small> : null}
              </button>
            </li>
          )
        })}
      </ul>
    </>
  )

  return (
    <div className="map-pick">
      <label className="search-field">
        <Search size={18} aria-hidden />
        <input value={query} maxLength={40} placeholder="খুঁজুন বা নতুন কাজের নাম লিখুন" aria-label="কাজ খুঁজুন" onChange={(ev) => setQuery(ev.target.value)} />
      </label>
      {q && !exact && (
        <ul className="pick-list">
          <li><button type="button" onClick={() => onDone(picked, query.trim())}><Plus size={18} aria-hidden /><span>"{query.trim()}" নতুন কাজ হিসেবে যোগ করুন</span></button></li>
        </ul>
      )}
      {group('নিয়মিত সার্ভিস', regular)}
      {group('মেরামত ও অন্যান্য', other)}
      <div className="picker-foot">
        <Button full disabled={!added && !removed} onClick={() => onDone(picked, null)}>
          {added || removed ? (added ? `${f.d(String(added))}টি কাজ যোগ করুন` : 'বাদ দিন') : 'কাজ বেছে নিন'}
        </Button>
      </div>
    </div>
  )
}

/** Workshop name with recent workshops first, same pattern as the fuel pump picker. */
export function WorkshopPicker({ value, recent, onPick }: { value: string; recent: string[]; onPick: (name: string) => void }) {
  const [query, setQuery] = useState(value)
  const q = query.trim().toLowerCase()
  const shown = recent.filter((w) => !q || w.toLowerCase().includes(q))
  return (
    <div className="map-pick">
      <input className="input" autoFocus value={query} maxLength={60} placeholder="ওয়ার্কশপ বা মেকানিকের নাম" aria-label="ওয়ার্কশপ" onChange={(ev) => setQuery(ev.target.value)} />
      <ul className="pick-list">
        {query.trim() && !recent.includes(query.trim()) && (
          <li><button type="button" onClick={() => onPick(query.trim())}><Plus size={18} aria-hidden /><span>"{query.trim()}" ব্যবহার করুন</span></button></li>
        )}
        {shown.map((w) => (
          <li key={w}><button type="button" className={w === value ? 'on' : ''} onClick={() => onPick(w)}><History size={18} aria-hidden /><span>{w}</span></button></li>
        ))}
      </ul>
      {value && <Button kind="secondary" full onClick={() => onPick('')}>ওয়ার্কশপ মুছে দিন</Button>}
    </div>
  )
}
