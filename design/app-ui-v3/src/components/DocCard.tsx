import { CircleCheck, Timer, TriangleAlert } from 'lucide-react'
import { docName } from '../data/catalog'
import { diffDays, todayISO } from '../lib/dates'
import { useFmt } from '../lib/bn'
import type { VaultDoc } from '../types'
import { useStore } from '../store'

export function expiryOf(doc: VaultDoc, lead = 30) {
  if (!doc.expiry) return { left: Infinity, status: 'none' } as const
  const left = diffDays(todayISO(), doc.expiry)
  return { left, status: left < 0 ? 'overdue' : left <= (doc.warningDays ?? lead) ? 'soon' : 'ok' } as const
}

/** Expiry chip with an icon and words, so the state never relies on colour alone. */
export function ExpiryChip({ doc }: { doc: VaultDoc }) {
  const f = useFmt()
  const lead = useStore((s) => s.settings.reminderLeadDays ?? 30)
  const { left, status } = expiryOf(doc, lead)
  const Icon = status === 'overdue' ? TriangleAlert : status === 'soon' ? Timer : CircleCheck
  const text = status === 'none' ? 'মেয়াদ নেই' : status === 'overdue' ? `${f.days(-left)} আগে শেষ` : status === 'soon' ? `${f.days(left)} বাকি` : 'মেয়াদ আছে'
  return <span className={`expiry ${status}`}><Icon size={14} strokeWidth={2.2} aria-hidden />{text}</span>
}

/** Stand-in for the scanned original. The real app shows the saved photo. */
export function DocArt({ doc, large }: { doc: VaultDoc; large?: boolean }) {
  const f = useFmt()
  return (
    <div className={`docart kind-${doc.kind}${large ? ' large' : ''}`} aria-hidden>
      <div className="docart-head"><b>{docName(doc.kind)}</b><small>নমুনা · ডেমো</small></div>
      <div className="docart-chip" />
      <div className="docart-body">
        <small>নম্বর</small><b>{doc.number}</b>
        <small>নাম</small><b>{doc.holder}</b>
        <small>মেয়াদ</small><b>{doc.expiry ? f.dateFull(doc.expiry) : 'মেয়াদ নেই'}</b>
      </div>
    </div>
  )
}
