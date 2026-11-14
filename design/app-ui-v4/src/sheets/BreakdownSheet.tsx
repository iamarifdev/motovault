import { FileText, PhoneCall, ShieldAlert, Wrench } from 'lucide-react'
import { useStore, useUI } from '../store'
import { Sheet } from '../components/Sheet'
import { Button } from '../components/ui'
import { haptic } from '../lib/haptics'

const TIPS = [
  {
    icon: Wrench,
    title: 'বাইক স্টার্ট নিচ্ছে না',
    detail: 'কিল সুইচ, সাইড-স্ট্যান্ড সেন্সর, রিজার্ভ ফুয়েল এবং ব্যাটারির তারের লুজ আছে কি না দেখে নিন।',
  },
  {
    icon: ShieldAlert,
    title: 'চাকা পাংচার বা হাওয়া কম',
    detail: 'নিরাপদ পাশে বাইক দাঁড় করিয়ে পার্কিং লাইট দিন। টিউবলেস চাকা হলে রিম বাঁকা না হওয়া পর্যন্ত আস্তে পাম্পে নিন।',
  },
  {
    icon: FileText,
    title: 'চেকপোস্টে ডকুমেন্ট দেখানো',
    detail: 'স্মার্ট কার্ড, ড্রাইভিং লাইসেন্স ও ট্যাক্স টোকেন রাস্তায় দেখানোর মোডে একসাথে বড় করে দেখাতে পারবেন।',
  },
] as const

export function BreakdownSheet() {
  const docs = useStore((s) => s.docs)
  const { close, open, setTab } = useUI.getState()

  const callEmergency = () => {
    haptic('warning')
    window.open('tel:999', '_self')
  }

  const openDocs = () => {
    haptic('tick')
    if (docs[0]) {
      open('docView', { id: docs[0].id })
      return
    }
    close()
    setTab('vault')
  }

  return (
    <Sheet title="জরুরি সহায়তা">
      <div className="inbox-card">
        <div className="setting">
          <b>জাতীয় জরুরি সেবা · ৯৯৯</b>
          <p className="cap" style={{ margin: 0 }}>
            দুর্ঘটনা বা রাস্তায় বিপদে পড়লে ২৪ ঘণ্টা ফ্রি কল করতে পারেন।
          </p>
          <Button full icon={PhoneCall} onClick={callEmergency}>
            ৯৯৯ জরুরি সহায়তা কল
          </Button>
        </div>
      </div>

      <div className="card-stack">
        {TIPS.map((tip) => (
          <div key={tip.title} className="setting">
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <span className="me-group-icon"><tip.icon size={18} aria-hidden /></span>
              <b>{tip.title}</b>
            </div>
            <p className="cap" style={{ margin: 0 }}>{tip.detail}</p>
          </div>
        ))}
      </div>

      <Button full kind="secondary" icon={FileText} onClick={openDocs}>
        {docs[0] ? 'রাস্তায় ডকুমেন্ট দেখানোর মোড' : 'ডকুমেন্ট খুলুন'}
      </Button>
    </Sheet>
  )
}
