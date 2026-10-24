import { Sun } from 'lucide-react'
import { useStore, useUI } from '../store'
import { docName } from '../data/catalog'
import { useFmt } from '../lib/bn'
import { Sheet } from '../components/Sheet'
import { Button, Switch } from '../components/ui'
import { DocArt, ExpiryChip } from '../components/DocCard'
import { useState } from 'react'

/** Roadside mode: one document, large, on a white background. A real build raises screen brightness while it is open. */
export function DocViewSheet({ id }: { id: string }) {
  const doc = useStore((s) => s.docs.find((d) => d.id === id))
  const setDocReminder = useStore((s) => s.setDocReminder)
  const close = useUI((s) => s.close)
  const f = useFmt()
  const [road, setRoad] = useState(false)
  if (!doc) return null
  return (
    <Sheet title={docName(doc.kind)} detent="tall">
      <div className={`doc-view${road ? ' road' : ''}`}>
        <DocArt doc={doc} large />
        <div className="row-between"><ExpiryChip doc={doc} /><span className="cap">মেয়াদ শেষ: {f.dateFull(doc.expiry)}</span></div>
      </div>
      <Switch checked={road} onChange={setRoad} label="রাস্তায় দেখানোর মোড" sub="সাদা পটভূমি, বড় ছবি, উজ্জ্বলতা বাড়ে (ডেমো)" />
      <Switch checked={doc.reminder} onChange={(v) => setDocReminder(doc.id, v)} label="মেয়াদের ৩০ দিন আগে জানান" />
      <Button full kind="secondary" icon={Sun} onClick={close}>ফিরে যান</Button>
    </Sheet>
  )
}
