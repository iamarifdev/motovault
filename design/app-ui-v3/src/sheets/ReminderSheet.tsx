import { useState } from 'react'
import { useStore, useUI } from '../store'
import { Sheet } from '../components/Sheet'
import { Button, Segmented, Switch } from '../components/ui'
import { docName } from '../data/catalog'
import { addDays, todayISO } from '../lib/dates'
import { useFmt } from '../lib/bn'

export function ReminderSheet({ id, type }: { id?: string; type?: string }) {
  const settings = useStore((s) => s.settings)
  const docs = useStore((s) => s.docs)
  const rules = useStore((s) => s.rules)
  const access = useUI((s) => s.reminderPreview)
  const open = useUI((s) => s.open)
  const f = useFmt()
  const [lead, setLead] = useState(settings.reminderLeadDays ?? 30)
  const [minute, setMinute] = useState(settings.reminderMinute ?? 540)
  const [saved, setSaved] = useState(false)
  const [error, setError] = useState(false)
  const doc = type === 'document' ? docs.find((d) => d.id === id) : undefined
  const rule = type === 'service' ? rules.find((r) => r.id === id) : undefined
  const selected = doc || rule
  const save = () => {
    if (access === 'failed') { setError(true); return }
    useStore.setState((s) => ({ settings: { ...s.settings, reminderLeadDays: lead, reminderMinute: minute } }))
    setSaved(true)
  }
  const snooze = (days: number) => {
    if (access === 'failed') { setError(true); return }
    const until = `${addDays(todayISO(), days)}T09:00`
    useStore.setState((s) => type === 'document' ? { docs: s.docs.map((d) => d.id === id ? { ...d, snoozedUntil: until } : d) } : { rules: s.rules.map((r) => r.id === id ? { ...r, snoozedUntil: until } : r) })
  }
  return <Sheet title="রিমাইন্ডার" detent="tall" footer={<>
    {error && <p role="alert">সেভ হয়নি। আগের তথ্য রাখা আছে। আবার চেষ্টা করুন।</p>}
    {saved && <p role="status">রিমাইন্ডারের সময় সেভ হয়েছে</p>}
    {id ? <Button full kind="secondary" onClick={() => useUI.getState().close()}>ফিরে যান</Button> : <Button full onClick={save}>সেভ করুন</Button>}
  </>}>
    <div className="card-stack"><div className="setting" role="status">
      <b>{access === 'enabled' ? 'ফোনে জানানোর অনুমতি আছে' : access === 'denied' ? 'ফোনের সেটিংসে নোটিফিকেশন বন্ধ' : access === 'failed' ? 'রিমাইন্ডার আপডেট করা যায়নি' : 'ফোনে জানানো বন্ধ আছে'}</b>
      <p>অ্যাপের ভেতরে তারিখ দেখা যাবে। ফোনের সময় বদলালে রিমাইন্ডারও স্থানীয় সময় অনুসরণ করবে।</p>
      {access === 'denied' && <p>ফোনের Settings &gt; Notifications &gt; MotoVault থেকেও অনুমতি দিতে পারবেন।</p>}
      <Button kind="secondary" onClick={() => useUI.setState({ reminderPreview: 'enabled' })}>{access === 'denied' ? 'নোটিফিকেশন সেটিংস খুলুন' : access === 'enabled' ? 'অনুমতি আবার যাচাই করুন' : 'ফোনে জানানোর অনুমতি দিন'}</Button>
    </div></div>
    {selected ? <div className="card-stack"><div className="setting">
      <b>{doc ? docName(doc.kind) : rule!.name}</b>
      {doc && !settings.docReminders && <p>কাগজের রিমাইন্ডার আমি পাতায় বন্ধ আছে।</p>}
      {rule && !settings.serviceReminders && <p>সার্ভিসের রিমাইন্ডার আমি পাতায় বন্ধ আছে।</p>}
      <Switch label="এই রেকর্ডের রিমাইন্ডার" checked={doc?.reminder ?? rule?.reminder ?? true} onChange={(reminder) => {
        if (access === 'failed') { setError(true); return }
        useStore.setState((s) => doc ? {docs:s.docs.map((d) => d.id === id ? {...d,reminder} : d)} : {rules:s.rules.map((r) => r.id === id ? {...r,reminder} : r)})
      }} />
      {doc && <p>{doc.expiry ? `মেয়াদ: ${f.dateFull(doc.expiry)}` : 'মেয়াদ নেই। মেয়াদের রিমাইন্ডার নেই।'}</p>}
      {doc?.expiry && <Segmented label="এই কাগজের সতর্কতার সময়" value={doc.warningDays ?? lead} options={[{value:30,label:'৩০ দিন'},{value:14,label:'১৪ দিন'},{value:7,label:'৭ দিন'}]} onChange={(warningDays) => {
        if (access === 'failed') { setError(true); return }
        useStore.setState((s) => ({docs:s.docs.map((d) => d.id === id ? {...d,warningDays} : d)}))
      }} />}
      <p>পরে জানানো সার্ভিস বা নবায়ন সম্পন্ন করে না।</p>
      {rule && rule.baseline !== 'confirmed' && <p>শেষ সার্ভিস নিশ্চিত নয়। সার্ভিসের তথ্য দিলে সময় নির্ধারণ করা যাবে।</p>}
      {selected.snoozedUntil && <p role="status">পরে জানাবেন: {f.d(selected.snoozedUntil.replace('T', ' · '))}</p>}
      {((doc && doc.expiry) || rule?.baseline === 'confirmed') && <><Button kind="secondary" onClick={() => snooze(1)}>আগামীকাল সকাল ৯টা</Button><Button kind="secondary" onClick={() => snooze(7)}>৭ দিন পরে সকাল ৯টা</Button></>}
    </div></div> : <>
      <div className="card-stack"><div className="setting"><b>মেয়াদের কত দিন আগে</b><Segmented label="সতর্কতার সময়" value={lead} onChange={setLead} options={[{value:30,label:'৩০ দিন'},{value:14,label:'১৪ দিন'},{value:7,label:'৭ দিন'}]} /></div>
      <div className="setting"><b>ফোনের স্থানীয় সময়</b><Segmented label="রিমাইন্ডারের সময়" value={minute} onChange={setMinute} options={[{value:540,label:'সকাল ৯টা'},{value:1080,label:'সন্ধ্যা ৬টা'}]} /></div></div>
      <h3>কাগজ ও সার্ভিস</h3>
      {docs.length + rules.length === 0 && <p>কাগজ বা সার্ভিসের সময়সূচি যোগ করলে এখানে দেখা যাবে।</p>}
      {docs.map((d) => <Button key={d.id} kind="secondary" full onClick={() => open('reminders',{id:d.id,type:'document'})}>{docName(d.kind)}</Button>)}
      {rules.map((r) => <Button key={r.id} kind="secondary" full onClick={() => open('reminders',{id:r.id,type:'service'})}>{r.name}</Button>)}
    </>}
  </Sheet>
}
