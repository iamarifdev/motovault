import { useState } from 'react'
import { LockKeyhole, ShieldCheck } from 'lucide-react'
import { useStore, useUI } from '../store'
import { usePrivacyPreview } from '../lib/privacyPreview'
import { Sheet } from '../components/Sheet'
import { Button, Segmented } from '../components/ui'
import { useFmt } from '../lib/bn'

export function DemoAuthentication({ onGranted, onCancel }: { onGranted: () => void; onCancel: () => void }) {
  const [issue, setIssue] = useState<'cancel' | 'lockout' | 'unavailable' | null>(null)
  return <section className="privacy-auth" role="dialog" aria-modal="true" aria-label="ফোনের লক দিয়ে যাচাই">
    <LockKeyhole size={32} aria-hidden />
    <h2>ফোনের লক দিয়ে যাচাই</h2>
    <p>ফোনের PIN, পাসকোড বা বায়োমেট্রিক ব্যবহার করুন।</p>
    <p className="cap">প্রোটোটাইপ: ফোনের যাচাই অনুকরণ করা হচ্ছে। আসল PIN লিখবেন না।</p>
    {issue && <p role="alert">{issue === 'lockout' ? 'বায়োমেট্রিক আপাতত বন্ধ। ফোনের PIN বা পাসকোড ব্যবহার করুন।' : issue === 'unavailable' ? 'ফোনে স্ক্রিন লক নেই। ফোনের সেটিংসে PIN বা পাসকোড দিন। আগের তথ্য রাখা আছে।' : 'যাচাই বাতিল হয়েছে। তথ্য খোলা হয়নি।'}</p>}
    <Button full onClick={onGranted} disabled={issue === 'unavailable'}>PIN বা পাসকোড দিয়ে যাচাই (ডেমো)</Button>
    <Button full kind="secondary" onClick={onCancel}>বাতিল করুন</Button>
    <details><summary>ডেমো ফল পরীক্ষা</summary><div className="privacy-options">
      <Button kind="ghost" onClick={() => setIssue('lockout')}>বায়োমেট্রিক লকআউট</Button>
      <Button kind="ghost" onClick={() => setIssue('unavailable')}>ফোনে লক নেই</Button>
      <Button kind="ghost" onClick={() => setIssue('cancel')}>যাচাই বাতিল</Button>
      <Button kind="ghost" onClick={() => setIssue(null)}>আবার চেষ্টা করুন</Button>
    </div></details>
  </section>
}

export function PrivacySheet() {
  const settings = useStore((s) => s.settings)
  const docs = useStore((s) => s.docs)
  const setSetting = useStore((s) => s.setSetting)
  const [step, setStep] = useState<'review' | 'enableAuth' | 'disableAuth' | 'migrating' | 'failed' | 'saved'>('review')
  const f = useFmt()
  if (step === 'enableAuth' || step === 'disableAuth') return <Sheet title="গোপনীয়তা" detent="tall" onClose={() => setStep('review')}>
    <DemoAuthentication onCancel={() => setStep('review')} onGranted={() => {
      if (step === 'disableAuth') { setSetting('privacyEnabled', false); setStep('review') }
      else setStep('migrating')
    }} />
  </Sheet>
  return <Sheet title="গোপনীয়তা" detent="tall" footer={step === 'migrating' ? <Button full kind="secondary" onClick={() => setStep('review')}>থামান</Button> : <Button full kind="secondary" onClick={() => useUI.getState().close()}>হয়েছে</Button>}>
    <div className="privacy-content"><div className="privacy-hero"><ShieldCheck size={32} aria-hidden /><h2>কাগজ আপনার নিয়ন্ত্রণে</h2><p>ফোন অন্য কাউকে দেওয়ার আগে শুধু বেছে নেওয়া কাগজটি দেখান।</p></div>
    {step === 'migrating' ? <div className="card-stack"><div className="setting" role="status">
      <h3>ফাইল যাচাই হচ্ছে</h3><p>আসল কপির সঙ্গে সুরক্ষিত কপি পুরোপুরি মিলিয়ে নেওয়া হবে। যাচাই শেষ না হওয়া পর্যন্ত আগের কপি রাখা থাকবে।</p>
      <p className="cap">এটি ডেমো। এখানে সত্যিকারের ফাইল এনক্রিপ্ট হচ্ছে না।</p>
      <Button full onClick={() => { setSetting('privacyEnabled', true); usePrivacyPreview.getState().unlock(); setStep('saved') }}>সফল যাচাই দেখুন (ডেমো)</Button>
      <Button full kind="secondary" onClick={() => setStep('failed')}>সেভ ব্যর্থ দেখুন (ডেমো)</Button>
    </div></div> : <>
      {step === 'failed' && <div className="privacy-warning" role="alert"><h3>সুরক্ষা চালু করা যায়নি</h3><p>আগের রেকর্ড ও আসল কপি রাখা আছে। জায়গা ও ফোনের লক দেখে আবার চেষ্টা করুন।</p><Button full onClick={() => setStep('enableAuth')}>আবার চেষ্টা করুন</Button></div>}
      {step === 'saved' && <p role="status">ডেমো সুরক্ষা চালু হয়েছে। অ্যাপ ছাড়লে বা সময় শেষ হলে আবার লক হবে।</p>}
      <div className="card-stack"><div className="setting">
        <b>{settings.privacyEnabled ? 'অ্যাপ লক চালু' : 'অ্যাপ লক বন্ধ'}</b>
        <p>{f.n(docs.length)}টি ডেমো কাগজ। {docs.length ? 'তালিকায় নম্বর ও ছবি লুকানো থাকবে।' : 'এখনও কাগজ নেই। পরে যোগ করা কাগজেও একই সুরক্ষা থাকবে।'}</p>
        {settings.privacyEnabled ? <>
          <Segmented label="নিজে থেকে লক হওয়ার সময়" value={settings.privacyTimeoutSeconds ?? 60} options={[{ value: 60, label: '১ মিনিট' }, { value: 300, label: '৫ মিনিট' }, { value: 900, label: '১৫ মিনিট' }]} onChange={(v) => setSetting('privacyTimeoutSeconds', v)} />
          <p>অ্যাপ পেছনে গেলে সঙ্গে সঙ্গে লক হবে।</p>
          <Button full icon={LockKeyhole} onClick={() => usePrivacyPreview.getState().lock()}>এখন লক করুন</Button>
          <Button full kind="ghost" onClick={() => setStep('disableAuth')}>ফোনের লক দিয়ে অ্যাপ লক বন্ধ করুন</Button>
        </> : <Button full icon={LockKeyhole} onClick={() => setStep('enableAuth')}>ফোনের লক দিয়ে চালু করুন</Button>}
      </div></div>
      <div className="card-stack"><div className="setting"><h3>কোন তথ্য সুরক্ষিত হবে</h3><p>আসল ফাইল, কাটা ছবি, প্রিভিউ ও উদ্ধার করা ফাইল সুরক্ষিত হবে। নাম, কাগজের নম্বর, তারিখ এবং আগে রেকর্ডের ভেতরে রাখা ছবি এই সুরক্ষার বাইরে থাকতে পারে। অ্যাপের সব তথ্য এনক্রিপ্ট হচ্ছে না।</p><p>অন্য অ্যাপে পাঠানো কপি সেই অ্যাপ রাখতে পারে। পাঠানোর আগে আবার ফোনের লক দিয়ে যাচাই করতে হবে।</p></div></div>
      <div className="privacy-warning"><h3>চাবি হারালে</h3><p>ফোনের PIN দিয়ে হারানো এনক্রিপশন চাবি ফেরানো যায় না। অ্যাপ মুছবেন না বা ডেটা সাফ করবেন না। ফাইল না খুললে আসল কপি থেকে আবার যোগ করার সাহায্য নিন। এই ফোনের এনক্রিপ্ট করা ফাইল অন্য ফোনে কপি করলেই খুলবে না।</p></div>
    </>}</div>
  </Sheet>
}
