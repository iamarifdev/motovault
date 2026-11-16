import { CircleCheck, Timer, TriangleAlert, UserRound } from 'lucide-react'
import { docName } from '../data/catalog'
import { diffDays, todayISO } from '../lib/dates'
import { useFmt } from '../lib/bn'
import type { DocKind, VaultDoc } from '../types'
import { useStore } from '../store'

const BRTA_CARD_FIELDS: Partial<Record<DocKind, { primaryLabel: string; primaryKey: 'holder' | 'number'; secondaryLabel: string; secondaryKey: 'holder' | 'number' }>> = {
  licence: { primaryLabel: 'নাম', primaryKey: 'holder', secondaryLabel: 'লাইসেন্স নং', secondaryKey: 'number' },
  registration: { primaryLabel: 'রেজিস্ট্রেশন নং', primaryKey: 'number', secondaryLabel: 'মালিকের নাম', secondaryKey: 'holder' },
}

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
export function DocArt({ doc, large, accessible = false }: { doc: VaultDoc; large?: boolean; accessible?: boolean }) {
  const f = useFmt()
  const number = doc.number || 'নম্বর দেওয়া নেই'
  const holder = doc.holder || 'নাম দেওয়া নেই'
  const expiryText = doc.expiry ? f.dateFull(doc.expiry) : 'মেয়াদ নেই'
  const a11yProps = {
    'aria-hidden': !accessible,
    role: accessible ? ('img' as const) : undefined,
    'aria-label': accessible ? `${docName(doc.kind)}। নম্বর ${number}। নাম ${holder}। ${doc.expiry ? `মেয়াদ ${expiryText}` : 'মেয়াদ নেই'}। ডেমো কপি।` : undefined,
  }

  const cardFields = BRTA_CARD_FIELDS[doc.kind]
  if (cardFields) {
    const values = { number, holder }
    const has = { number: Boolean(doc.number), holder: Boolean(doc.holder) }
    return (
      <div className={`docart kind-${doc.kind}${large ? ' large' : ''}`} {...a11yProps}>
        <div className="dl-photo" aria-hidden>
          <UserRound size={large ? 40 : 34} strokeWidth={1.6} />
        </div>
        <div className="dl-fields">
          <div className="dl-field">
            <small>{cardFields.primaryLabel}</small>
            <b className={has[cardFields.primaryKey] ? undefined : 'muted'}>{values[cardFields.primaryKey]}</b>
          </div>
          <div className="dl-field">
            <small>{cardFields.secondaryLabel}</small>
            <b className={has[cardFields.secondaryKey] ? undefined : 'muted'}>{values[cardFields.secondaryKey]}</b>
          </div>
          <div className="dl-field">
            <small>মেয়াদ</small>
            <b>{expiryText}</b>
          </div>
        </div>
        <span className="dl-sample">নমুনা · ডেমো</span>
      </div>
    )
  }

  return (
    <div className={`docart kind-${doc.kind}${large ? ' large' : ''}`} {...a11yProps}>
      <div className="docart-head"><b>{docName(doc.kind)}</b><small>নমুনা · ডেমো</small></div>
      <div className="docart-chip" />
      <div className="docart-body">
        <small>নম্বর</small><b>{number}</b>
        <small>নাম</small><b>{holder}</b>
        <small>মেয়াদ</small><b>{expiryText}</b>
      </div>
    </div>
  )
}
