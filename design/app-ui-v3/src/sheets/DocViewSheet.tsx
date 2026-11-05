import { ExternalLink, RefreshCw, Sun } from 'lucide-react'
import { useStore, useUI } from '../store'
import { docName } from '../data/catalog'
import { QR_VERIFY_URL, RENEWAL_GUIDES } from '../data/renewal'
import { useFmt } from '../lib/bn'
import { haptic } from '../lib/haptics'
import { Sheet } from '../components/Sheet'
import { Button, Switch } from '../components/ui'
import { DocArt, ExpiryChip } from '../components/DocCard'
import { usePrivacyPreview } from '../lib/privacyPreview'

/** Roadside mode: one document, large, on a white background. A real build raises screen brightness while it is open. */
export function DocViewSheet({ id }: { id: string }) {
  const doc = useStore((s) => s.docs.find((d) => d.id === id))
  const setDocReminder = useStore((s) => s.setDocReminder)
  const close = useUI((s) => s.close)
  const open = useUI((s) => s.open)
  const f = useFmt()
  const privacyEnabled = useStore((s) => s.settings.privacyEnabled)
  if (!doc) return null
  const guide = RENEWAL_GUIDES[doc.kind]
  const renewals = doc.renewals ?? []
  return (
    <Sheet title={docName(doc.kind)} detent="tall">
      <div className="doc-view">
        <DocArt doc={doc} large accessible />
        <div className="row-between"><ExpiryChip doc={doc} /><span className="cap">{doc.expiry ? `মেয়াদ শেষ: ${f.dateFull(doc.expiry)}` : 'মেয়াদ নেই'}</span></div>
      </div>
      <Button full kind="secondary" onClick={() => privacyEnabled ? usePrivacyPreview.getState().inspect(doc) : useUI.getState().open('privacy')}>{privacyEnabled ? 'শুধু এই কাগজ দেখান' : 'সুরক্ষা চালু করে কাগজ দেখান'}</Button>
      {doc.expiry && <Switch checked={doc.reminder} onChange={(v) => setDocReminder(doc.id, v)} label="এই কাগজের রিমাইন্ডার" />}

      <Button full icon={RefreshCw} onClick={() => open('renewal', { id: doc.id })}>নবায়ন</Button>
      {guide.qrVerification && (
        <>
          <Button full kind="secondary" icon={ExternalLink} onClick={() => { haptic('tick'); window.open(QR_VERIFY_URL, '_blank', 'noopener,noreferrer') }}>BRTA সাইটে কপি যাচাই করুন</Button>
          <p className="renew-source">আপনি নিজে ফাইল দিলে তবেই BRTA সাইটে আপলোড হবে।</p>
        </>
      )}

      <section className="renew-history" aria-label="নবায়নের ইতিহাস">
        <h3>নবায়নের ইতিহাস</h3>
        {renewals.length === 0 ? <p className="renew-source">এখনো কোনো নবায়ন নেই।</p> : renewals.map((renewal) => (
          <article key={renewal.id}>
            <div className="row-between"><b>{f.dateFull(renewal.newExpiry)}</b><span>{renewal.fee === null ? 'ফি নেই' : f.money(renewal.fee)}</span></div>
            <small className="renew-source">আগের মেয়াদ: {f.dateFull(renewal.previousExpiry)} · {f.dateFull(renewal.renewedOn)} তারিখে{savedReceiptLabel(renewal.receiptName)}</small>
            {renewal.notes && <small>{renewal.notes}</small>}
          </article>
        ))}
      </section>

      <Button full kind="secondary" onClick={() => useUI.getState().open('reminders',{id:doc.id,type:'document'})}>অনুমতি, সময় ও পরে জানান</Button>
      <Button full kind="secondary" icon={Sun} onClick={close}>ফিরে যান</Button>
    </Sheet>
  )
}

function savedReceiptLabel(name?: string) {
  return name ? ` · রসিদ: ${name}` : ''
}
