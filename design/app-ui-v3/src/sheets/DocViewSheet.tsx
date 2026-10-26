import { Sun } from 'lucide-react'
import { useStore, useUI } from '../store'
import { docName } from '../data/catalog'
import { useFmt } from '../lib/bn'
import { Sheet } from '../components/Sheet'
import { Button, Switch } from '../components/ui'
import { DocArt, ExpiryChip } from '../components/DocCard'
import { usePrivacyPreview } from '../lib/privacyPreview'

/** Roadside mode: one document, large, on a white background. A real build raises screen brightness while it is open. */
export function DocViewSheet({ id }: { id: string }) {
  const doc = useStore((s) => s.docs.find((d) => d.id === id))
  const setDocReminder = useStore((s) => s.setDocReminder)
  const close = useUI((s) => s.close)
  const f = useFmt()
  const privacyEnabled = useStore((s) => s.settings.privacyEnabled)
  if (!doc) return null
  return (
    <Sheet title={docName(doc.kind)} detent="tall">
      <div className="doc-view">
        <DocArt doc={doc} large accessible />
        <div className="row-between"><ExpiryChip doc={doc} /><span className="cap">{doc.expiry ? `মেয়াদ শেষ: ${f.dateFull(doc.expiry)}` : 'মেয়াদ নেই'}</span></div>
      </div>
      <Button full kind="secondary" onClick={() => privacyEnabled ? usePrivacyPreview.getState().inspect(doc) : useUI.getState().open('privacy')}>{privacyEnabled ? 'শুধু এই কাগজ দেখান' : 'সুরক্ষা চালু করে কাগজ দেখান'}</Button>
      {doc.expiry && <Switch checked={doc.reminder} onChange={(v) => setDocReminder(doc.id, v)} label="এই কাগজের রিমাইন্ডার" />}
      <Button full kind="secondary" onClick={() => useUI.getState().open('reminders',{id:doc.id,type:'document'})}>অনুমতি, সময় ও পরে জানান</Button>
      <Button full kind="secondary" icon={Sun} onClick={close}>ফিরে যান</Button>
    </Sheet>
  )
}
