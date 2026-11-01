import { useState } from 'react'
import { Sheet } from '../components/Sheet'
import { Button } from '../components/ui'
import { useStore } from '../store'

const COPY = {
  bn: {
    title: 'আগের ভল্ট', close: 'বন্ধ করুন', demo: 'এটি শুধু ডিজাইন ডেমো। আপনার তথ্য বদলাবে না।',
    description: 'ব্যাকআপ রিস্টোরের আগে সেভ থাকা ভল্ট এখানে দেখতে পারবেন।',
    sample: 'ডেমো ভল্ট · ১৬ অক্টোবর ২০২৬', details: '১টি বাইক · ৮টি রেকর্ড · ৩টি কাগজ',
    review: 'ভল্টটি দেখুন', empty: 'আগের কোনো ভল্ট নেই।', showEmpty: 'খালি অবস্থা দেখুন',
    showSample: 'ডেমো ভল্ট দেখুন', confirmTitle: 'এই ভল্টে ফিরে যাবেন?',
    confirmBody: 'এখনকার ভল্টটি আগে নিরাপদে সেভ হবে। আগের ভল্টের কাগজও আবার পাওয়া যাবে।',
    device: 'এই রিকভারি শুধু এই ডিভাইসে কাজ করে। ফোনের নিরাপত্তা কি হারালে এটি খোলা যাবে না।',
    confirm: 'ফিরে যান', cancel: 'বাতিল', failed: 'ভল্টটি খোলা যায়নি। আপনার বর্তমান তথ্য বদলায়নি।',
    failDemo: 'সমস্যার অবস্থা দেখুন', retry: 'আবার দেখুন', success: 'আগের ভল্টে ফিরে গেছেন।',
    back: 'তালিকায় ফিরুন', loading: 'ভল্ট যাচাই করা হচ্ছে…', loadingDemo: 'যাচাইয়ের অবস্থা দেখুন',
    loadingDone: 'ডেমো যাচাই শেষ করুন', cancelled: 'বাতিল করা হয়েছে। আপনার বর্তমান তথ্য বদলায়নি।',
    stale: 'এই ভল্ট দেখার পর বর্তমান তথ্য বদলেছে। আবার দেখে নিশ্চিত করুন।', staleDemo: 'পরিবর্তনের অবস্থা দেখুন',
    keyUnavailable: 'ফোনের নিরাপত্তা কি পাওয়া যাচ্ছে না। আগের তথ্য মুছে ফেলা হয়নি।', keyDemo: 'কি সমস্যার অবস্থা দেখুন'
  },
  en: {
    title: 'Previous vaults', close: 'Close', demo: 'Design demo only. Your records will not change.',
    description: 'Review vaults saved before a backup restore.',
    sample: 'Demo vault · 16 October 2026', details: '1 bike · 8 records · 3 documents',
    review: 'Review vault', empty: 'No previous vaults yet.', showEmpty: 'Show empty state',
    showSample: 'Show demo vault', confirmTitle: 'Return to this vault?',
    confirmBody: 'Your current vault will be saved first. Documents in the previous vault will be available again.',
    device: 'This recovery works only on this device. It cannot be opened if the device security key is lost.',
    confirm: 'Return to vault', cancel: 'Cancel', failed: 'Could not open this vault. Your current records are unchanged.',
    failDemo: 'Show failure state', retry: 'Review again', success: 'The previous vault is restored.',
    back: 'Back to list', loading: 'Checking the vault…', loadingDemo: 'Show checking state',
    loadingDone: 'Finish demo check', cancelled: 'Cancelled. Your current records are unchanged.',
    stale: 'Your current records changed after this review. Review them again before continuing.', staleDemo: 'Show changed-record state',
    keyUnavailable: 'The device security key is unavailable. The previous records have not been deleted.', keyDemo: 'Show key-unavailable state'
  }
} as const

export const RECOVERY_ENTRY = {
  bn: { title: 'ব্যাকআপ ও রিকভারি', heading: 'আগের ভল্ট ফিরে দেখুন', description: 'রিস্টোরের আগে সেভ থাকা তথ্য দেখুন এবং চাইলে ফিরে যান।', action: 'আগের ভল্ট দেখুন' },
  en: { title: 'Backup and recovery', heading: 'Review a previous vault', description: 'Review the records saved before a restore and return to them if needed.', action: 'View previous vaults' }
} as const

type View = 'list' | 'empty' | 'review' | 'failed' | 'success' | 'loading' | 'cancelled' | 'stale' | 'keyUnavailable'

/** Interactive design states use synthetic data and never change the prototype vault. */
export function RecoverySheet() {
  const language = useStore((s) => (s.settings as { language?: string }).language) === 'en' ? 'en' : 'bn'
  const text = COPY[language]
  const [view, setView] = useState<View>('list')
  return (
    <Sheet title={text.title} closeLabel={text.close} detent="tall">
      <p className="cap">{text.demo}</p>
      {view === 'list' && <>
        <p>{text.description}</p>
        <div className="card-stack"><div className="setting"><b>{text.sample}</b><p>{text.details}</p><Button full kind="secondary" onClick={() => setView('review')}>{text.review}</Button></div></div>
        <Button full kind="ghost" onClick={() => setView('empty')}>{text.showEmpty}</Button>
        <Button full kind="ghost" onClick={() => setView('loading')}>{text.loadingDemo}</Button>
        <Button full kind="ghost" onClick={() => setView('keyUnavailable')}>{text.keyDemo}</Button>
      </>}
      {view === 'empty' && <><p>{text.empty}</p><Button full kind="secondary" onClick={() => setView('list')}>{text.showSample}</Button></>}
      {view === 'review' && <>
        <h3>{text.confirmTitle}</h3><p>{text.sample}</p><p>{text.details}</p><p>{text.confirmBody}</p><p className="cap">{text.device}</p>
        <Button full onClick={() => setView('success')}>{text.confirm}</Button>
        <Button full kind="secondary" onClick={() => setView('cancelled')}>{text.cancel}</Button>
        <Button full kind="ghost" onClick={() => setView('failed')}>{text.failDemo}</Button>
        <Button full kind="ghost" onClick={() => setView('stale')}>{text.staleDemo}</Button>
      </>}
      {view === 'failed' && <><p role="alert">{text.failed}</p><Button full kind="secondary" onClick={() => setView('review')}>{text.retry}</Button></>}
      {view === 'success' && <><p role="status">{text.success}</p><Button full kind="secondary" onClick={() => setView('list')}>{text.back}</Button></>}
      {view === 'loading' && <><p role="status">{text.loading}</p><Button full kind="secondary" onClick={() => setView('list')}>{text.loadingDone}</Button></>}
      {view === 'cancelled' && <><p role="status">{text.cancelled}</p><Button full kind="secondary" onClick={() => setView('list')}>{text.back}</Button></>}
      {view === 'stale' && <><p role="alert">{text.stale}</p><Button full kind="secondary" onClick={() => setView('list')}>{text.back}</Button></>}
      {view === 'keyUnavailable' && <><p role="alert">{text.keyUnavailable}</p><Button full kind="secondary" onClick={() => setView('list')}>{text.back}</Button></>}
    </Sheet>
  )
}
