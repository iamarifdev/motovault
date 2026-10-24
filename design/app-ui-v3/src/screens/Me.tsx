import { useState } from 'react'
import { Bell, Gauge as GaugeIcon, LayoutGrid, MapPin, Share2, Vibrate } from 'lucide-react'
import { useStore, useUI } from '../store'
import { BRANDS, brandOf, modelOf } from '../data/catalog'
import { byUrgency } from '../lib/wear'
import { todayISO } from '../lib/dates'
import { useFmt } from '../lib/bn'
import { haptic } from '../lib/haptics'
import { Button, SectionTitle, Segmented, Switch } from '../components/ui'

function WidgetPreview() {
  const ride = useStore((s) => s.ride)!
  const rules = useStore((s) => s.rules)
  const f = useFmt()
  const top = byUrgency(rules, ride.odometer, todayISO())[0]
  return (
    <div className="widget" aria-label="হোম স্ক্রিন উইজেটের নমুনা">
      <div className="widget-head"><GaugeIcon size={16} aria-hidden /><b>{ride.name}</b><span>{f.km(ride.odometer)}</span></div>
      <p className={`widget-line ${top?.status}`}>{top?.rule.name.split(' ও ')[0].split(' পরিষ্কার')[0]} · {!top || top.status === 'unknown' ? 'তথ্য নিশ্চিত নয়' : top.status === 'overdue' ? 'এখনই' : top.status === 'soon' ? 'শিগগির' : 'ব্যবধান বাকি'}</p>
      <div className="widget-btns"><span>⛽ তেল</span><span>📏 কিমি</span></div>
    </div>
  )
}

export function Me() {
  const ride = useStore((s) => s.ride)!
  const s = useStore((st) => st.settings)
  const set = useStore((st) => st.setSetting)
  const startOnboarding = useStore((st) => st.startOnboarding)
  const resetDemo = useStore((st) => st.resetDemo)
  const open = useUI((u) => u.open)
  const f = useFmt()
  const [widget, setWidget] = useState(false)
  const brand = brandOf(ride.brandId)
  const model = modelOf(ride.modelId)
  const active = s.brandOverride ?? ride.brandId

  return (
    <div className="screen-pad">
      <header className="page-head"><h1>আমি</h1></header>

      <section className="profile">
        <div className="profile-photo">{model?.photo ? <img src={model.photo} alt="" /> : <GaugeIcon size={32} aria-hidden />}</div>
        <div><b>{brand.name} {ride.name}</b><small>{f.d(String(ride.year))} · {f.km(ride.odometer)}</small></div>
        <Button size="sm" kind="secondary" onClick={startOnboarding}>বাইক বদলান</Button>
      </section>

      <section aria-label="চেহারা">
        <SectionTitle>চেহারা</SectionTitle>
        <div className="card-stack">
          <div className="setting"><span className="setting-label">থিম</span>
            <Segmented label="থিম" value={s.theme} onChange={(v) => set('theme', v)} options={[{ value: 'system', label: 'ডিভাইসের মতো' }, { value: 'light', label: 'লাইট' }, { value: 'dark', label: 'ডার্ক' }]} />
          </div>
          <div className="setting"><span className="setting-label">রঙ</span>
            <div className="swatches" role="radiogroup" aria-label="অ্যাকসেন্ট রঙ">
              {BRANDS.map((b) => (
                <button key={b.id} type="button" role="radio" aria-checked={active === b.id} aria-label={b.name} className={active === b.id ? 'on' : ''}
                  style={{ ['--sw-l' as string]: b.light, ['--sw-d' as string]: b.dark }} onClick={() => { haptic('tick'); set('brandOverride', b.id === ride.brandId ? null : b.id) }} />
              ))}
            </div>
            <small className="cap">{s.brandOverride ? `${brandOf(s.brandOverride).name} এর রঙ` : 'আপনার বাইকের ব্র্যান্ডের রঙ'}</small>
          </div>
          <div className="setting"><span className="setting-label">সংখ্যা</span>
            <Segmented label="সংখ্যা" value={s.digits} onChange={(v) => set('digits', v)} options={[{ value: 'bn', label: '১২৩ বাংলা' }, { value: 'en', label: '123 English' }]} />
          </div>
          <div className="setting"><span className="setting-label">লেখার আকার</span>
            <Segmented label="লেখার আকার" value={s.textScale} onChange={(v) => set('textScale', v)} options={[{ value: 1, label: 'স্বাভাবিক' }, { value: 1.15, label: 'বড়' }, { value: 1.3, label: 'অনেক বড়' }]} />
          </div>
        </div>
      </section>

      <section aria-label="আচরণ">
        <SectionTitle>আচরণ</SectionTitle>
        <div className="card-stack">
          <Switch checked={s.haptics} onChange={(v) => set('haptics', v)} label="ভাইব্রেশন ফিডব্যাক" sub="সেভ, ভুল ও ট্যাপে হালকা কাঁপুনি" />
          <Switch checked={s.reduceMotion} onChange={(v) => set('reduceMotion', v)} label="অ্যানিমেশন কমান" sub="ফোনের সেটিং চালু থাকলে এটি নিজেই চালু হয়" />
          <Switch checked={s.gps} onChange={(v) => set('gps', v)} label="তেল ও সার্ভিসে বর্তমান লোকেশন" sub="অনুমতি না দিলেও রেকর্ড সেভ হবে" />
        </div>
      </section>

      <section aria-label="রিমাইন্ডার">
        <SectionTitle>রিমাইন্ডার</SectionTitle>
        <div className="card-stack">
          <Switch checked={s.serviceReminders} onChange={(v) => set('serviceReminders', v)} label="সার্ভিসের সময় হলে জানান" sub={`চেইন, অয়েল ও অন্য যন্ত্রাংশ · ${f.d(String(Math.floor((s.reminderMinute ?? 540) / 60)).padStart(2,'0'))}:${f.d(String((s.reminderMinute ?? 540) % 60).padStart(2,'0'))}`} />
          <Switch checked={s.docReminders} onChange={(v) => set('docReminders', v)} label="কাগজের মেয়াদের আগে জানান" sub={`${f.n(s.reminderLeadDays ?? 30)} দিন আগে এবং মেয়াদের দিন। ফোনের অনুমতি প্রয়োজন।`} />
        </div>
        <Button full kind="secondary" onClick={() => open('reminders')}>অনুমতি, সময় ও পরে জানান</Button>
      </section>

      <section aria-label="নতুন ধারণা">
        <SectionTitle>নতুন ধারণা (ডেমো)</SectionTitle>
        <div className="card-stack">
          <button type="button" className="idea" onClick={() => open('paste')}><Share2 size={22} aria-hidden /><span><b>শেয়ার বা পেস্ট করে যোগ</b><small>বিকাশ, নগদ বা মেকানিকের মেসেজ থেকে খসড়া</small></span></button>
          <button type="button" className="idea" onClick={() => { haptic('tick'); setWidget(!widget) }}><LayoutGrid size={22} aria-hidden /><span><b>হোম স্ক্রিন উইজেট</b><small>অ্যাপ না খুলেই পরের কাজ আর দুটি বোতাম</small></span></button>
          {widget && <WidgetPreview />}
          <div className="idea static"><Bell size={22} aria-hidden /><span><b>সার্ভিসের নোটিফিকেশন</b><small>"চেইনের সময় হয়েছে, লিখে রাখবেন?" এক ট্যাপে সার্ভিস খোলে</small></span></div>
          <div className="idea static"><Vibrate size={22} aria-hidden /><span><b>ভয়েস ও ক্যামেরা</b><small>সার্ভিস শিটে "বলুন", কিমি শিটে ছবি তুলে পড়া</small></span></div>
          <div className="idea static"><MapPin size={22} aria-hidden /><span><b>আনুমানিক কিমি</b><small>আগের চলা থেকে হিসাব, আপনি শুধু নিশ্চিত করেন</small></span></div>
        </div>
      </section>

      <section className="about">
        <p className="cap">MotoVault প্রোটোটাইপ · শুধু ডেমো ডেটা · সব তথ্য এই ডিভাইসেই থাকে</p>
        <Button kind="ghost" full onClick={resetDemo}>ডেমো ডেটা নতুন করে দিন</Button>
      </section>
    </div>
  )
}
