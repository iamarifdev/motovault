import { useUI } from '../store'
import { Sheet } from '../components/Sheet'
import { Button } from '../components/ui'

/** P15 cloud backup status, matching Compose CloudBackupSheet. It is a status page, not the local recovery flow. */
export function CloudBackupSheet() {
  const open = useUI((s) => s.open)
  return (
    <Sheet title="ক্লাউড ব্যাকআপ ও সিঙ্ক" footer={<Button full onClick={() => open('recovery')}>লোকাল .mva ব্যাকআপ খুলুন</Button>}>
      <div className="card-stack">
        <div className="setting">
          <small className="cap">সংযোগের অবস্থা</small>
          <b>সংযুক্ত নয় — লোকাল ভল্ট সম্পূর্ণ অফলাইনে কাজ করে</b>
        </div>
        <div className="setting">
          <p style={{ margin: 0 }}>আপনার ভল্টের পাসওয়ার্ড ডিভাইসের বাইরে যায় না; ক্লাউডে কেবল এনক্রিপ্টেড .mva আর্কাইভ থাকে।</p>
          <p className="cap" style={{ margin: 0 }}>লাইভ ক্লাউড প্রোভাইডার সেটআপ এখনও চালু হয়নি। লোকাল এনক্রিপ্টেড ব্যাকআপ ও ক্রস-ডিভাইস যাচাই সক্রিয় আছে।</p>
        </div>
      </div>
    </Sheet>
  )
}
