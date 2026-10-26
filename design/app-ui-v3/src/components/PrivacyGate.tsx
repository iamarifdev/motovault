import { useEffect, useState } from 'react'
import { LockKeyhole } from 'lucide-react'
import { usePrivacyPreview } from '../lib/privacyPreview'
import { Button } from './ui'
import { DocArt } from './DocCard'
import { docName } from '../data/catalog'
import { DemoAuthentication } from '../sheets/PrivacySheet'

export function PrivacyGate() {
  const selected = usePrivacyPreview((s) => s.selected)
  const mode = usePrivacyPreview((s) => s.mode)
  const [auth, setAuth] = useState(false)
  useEffect(() => {
    const back = (e: KeyboardEvent) => { if (e.key === 'Escape') { e.preventDefault(); if (auth) setAuth(false); else usePrivacyPreview.getState().lock() } }
    window.addEventListener('keydown', back)
    return () => window.removeEventListener('keydown', back)
  }, [auth])
  return <main className={`privacy-gate screen-pad${selected && mode === 'inspection' ? ' inspection' : ''}`}>
    {auth ? <DemoAuthentication onGranted={() => { setAuth(false); usePrivacyPreview.getState().unlock() }} onCancel={() => setAuth(false)} /> : selected && mode === 'inspection' ? <>
      <p className="cap">শুধু এই কাগজ · সংরক্ষিত ডেমো কপি</p><h1>{docName(selected.kind)}</h1>
      <DocArt doc={selected} large accessible />
      <p>এই মোডে অন্য কাগজ, সম্পাদনা বা পাঠানোর বোতাম নেই।</p>
      <Button full onClick={() => setAuth(true)}>ফোনের লক দিয়ে ফিরুন</Button>
      <Button full kind="secondary" onClick={() => usePrivacyPreview.getState().lock()}>কাগজ লুকিয়ে লক করুন</Button>
    </> : <>
      <LockKeyhole size={42} aria-hidden /><h1>MotoVault লক করা আছে</h1><p>ফোনের লক দিয়ে আপনার তথ্য খুলুন।</p>
      <Button full onClick={() => setAuth(true)}>ফোনের লক দিয়ে খুলুন</Button>
      <details><summary>ফাইল খুলছে না?</summary><p>অ্যাপ মুছবেন না বা ডেটা সাফ করবেন না। চাবি না পাওয়া গেলে আসল ফাইল থেকে আবার যোগ করার সাহায্য নিন। ফোনের PIN হারানো ফাইলের চাবি ফেরায় না।</p></details>
      <p className="cap">এটি কেবল ডেমো। ব্রাউজারের ডেটা বা ফাইল এখানে এনক্রিপ্ট করা হচ্ছে না।</p>
    </>}
  </main>
}
