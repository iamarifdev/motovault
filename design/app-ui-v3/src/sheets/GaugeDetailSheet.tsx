import { useStore, useUI } from '../store'
import { Sheet } from '../components/Sheet'
import { Button } from '../components/ui'
import { useFmt } from '../lib/bn'
import { describeWear, wearOf } from '../lib/wear'
import { todayISO } from '../lib/dates'

export function GaugeDetailSheet({ id }: { id: string }) {
  const ride = useStore((s) => s.ride)!
  const rule = useStore((s) => s.rules.find((r) => r.id === id))
  const open = useUI((s) => s.open)
  const f = useFmt()
  if (!rule) return <Sheet title="সার্ভিসের ব্যবধান"><p>এই সময়সূচি আর নেই।</p></Sheet>
  const wear = wearOf(rule, ride.odometer, todayISO())
  return <Sheet title={rule.name}>
    <p className="field-hint">এটি রেকর্ড করা সার্ভিসের ব্যবধান। যন্ত্রাংশের শারীরিক অবস্থা মাপা হয়নি।</p>
    <div className="card-stack">
      <div className="setting"><b>{wear.status === 'unknown' ? 'আগের সার্ভিস নিশ্চিত নয়' : `ব্যবধান ${f.n(wear.life)}% বাকি`}</b><p>{describeWear(wear, f)}</p></div>
      <div className="setting"><span>হিসাবের ভিত্তি</span><p>{rule.baseline === 'confirmed' ? `${f.km(rule.lastKm)} · ${f.dateFull(rule.lastDate)}` : rule.baseline === 'estimate' ? 'মালিকের অনুমান। সার্ভিসের তথ্য দিয়ে নিশ্চিত করুন।' : 'শেষ সার্ভিসের তথ্য নেই'}</p></div>
      <div className="setting"><span>কাজ</span><p>{rule.action === 'inspect' ? 'পরীক্ষা' : rule.action === 'lubricate' ? 'লুব্রিকেট' : rule.action === 'adjust' ? 'অ্যাডজাস্ট' : rule.action === 'clean' ? 'পরিষ্কার' : 'বদলানো'}</p></div>
      <div className="setting"><span>সময়সূচির উৎস</span><p>{rule.source || 'মালিকের উদাহরণ। ম্যানুয়ালের সঙ্গে মিলিয়ে নিন।'}</p><small>{rule.sourceRevision ? `${rule.sourceRevision}${rule.sourcePage ? ` · পৃষ্ঠা ${rule.sourcePage}` : ''}` : 'নিজের নির্ধারিত'}</small>{rule.applicability && <small>{rule.applicability}</small>}<small>{rule.intervalKm > 0 ? f.km(rule.intervalKm) : 'দূরত্বের সীমা নেই'} বা {rule.intervalDays > 0 ? f.days(rule.intervalDays) : 'সময়ের সীমা নেই'}, যেটি আগে হয়</small></div>
      <div className="setting"><span>শেষ নিশ্চিত কিমি</span><p>{f.km(ride.odometer)} · {f.dateFull(ride.odometerAt)}</p><small>পুরোনো কিমি হলে দূরত্বের হিসাবও পুরোনো।</small></div>
    </div>
    <Button full onClick={() => open('odometer')}>বর্তমান কিমি নিশ্চিত করুন</Button>
    <Button full kind="secondary" onClick={() => open('service', { item: rule.item })}>সার্ভিস লিখুন</Button>
  </Sheet>
}
