import { Gauge as GaugeIcon } from 'lucide-react'
import { useStore, useUI } from '../store'
import { BRANDS, brandOf } from '../data/catalog'
import { byUrgency } from '../lib/wear'
import { todayISO } from '../lib/dates'
import { useFmt } from '../lib/bn'
import { haptic } from '../lib/haptics'
import { Sheet } from '../components/Sheet'
import { Button, Segmented, Switch } from '../components/ui'

export type SettingsSection = 'appearance' | 'language' | 'more' | 'reminders' | 'lock' | 'sale' | 'widget'

const TITLES: Record<SettingsSection, string> = {
  appearance: 'চেহারা',
  language: 'ভাষা ও সংখ্যা',
  more: 'আরও সেটিংস',
  reminders: 'রিমাইন্ডার',
  lock: 'অ্যাপ লক',
  sale: 'বিক্রয় ও হস্তান্তর প্যাক',
  widget: 'হোম স্ক্রিন উইজেট',
}

/** One Profile row opens one of these. Each section holds only its own controls. */
export function SettingsSheet({ section }: { section: SettingsSection }) {
  const ride = useStore((st) => st.ride)!
  const s = useStore((st) => st.settings)
  const set = useStore((st) => st.setSetting)
  const open = useUI((u) => u.open)
  const f = useFmt()
  const active = s.brandOverride ?? ride.brandId
  const time = (m: number) => `${f.d(String(Math.floor(m / 60)).padStart(2, '0'))}:${f.d(String(m % 60).padStart(2, '0'))}`

  return (
    <Sheet title={TITLES[section]}>
      <div className="card-stack">
        {section === 'appearance' && <>
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
          <div className="setting"><span className="setting-label">লেখার আকার</span>
            <Segmented label="লেখার আকার" value={s.textScale} onChange={(v) => set('textScale', v)} options={[{ value: 1, label: 'স্বাভাবিক' }, { value: 1.15, label: 'বড়' }, { value: 1.3, label: 'অনেক বড়' }]} />
          </div>
        </>}

        {section === 'language' && <>
          <div className="setting"><span className="setting-label">ভাষা (Language)</span>
            <Segmented label="ভাষা" value={s.language ?? 'bn'} onChange={(v) => set('language', v)} options={[{ value: 'bn', label: 'বাংলা' }, { value: 'en', label: 'English' }]} />
          </div>
          <div className="setting"><span className="setting-label">সংখ্যার ধরন</span>
            <Segmented label="সংখ্যার ধরন" value={s.digits} onChange={(v) => set('digits', v)} options={[{ value: 'bn', label: '১২৩ বাংলা' }, { value: 'en', label: '123 English' }]} />
          </div>
        </>}

        {section === 'more' && <>
          <Switch checked={s.haptics} onChange={(v) => set('haptics', v)} label="টাচ ভাইব্রেশন" sub="বাটন চাপলে ও সেভ করলে হালকা ভাইব্রেশন" />
          <Switch checked={s.reduceMotion} onChange={(v) => set('reduceMotion', v)} label="অ্যানিমেশন কমান" sub="স্ক্রিনের নড়াচড়া কমান" />
          <Switch checked={s.gps} onChange={(v) => set('gps', v)} label="তেল ও সার্ভিসে বর্তমান লোকেশন" sub="অনুমতি না দিলেও রেকর্ড সেভ হবে" />
        </>}

        {section === 'reminders' && <>
          <Switch checked={s.serviceReminders} onChange={(v) => set('serviceReminders', v)} label="সার্ভিসের সময় হলে জানান" sub="চেইন, মবিল ও অন্য যন্ত্রাংশ" />
          <Switch checked={s.docReminders} onChange={(v) => set('docReminders', v)} label="ডকুমেন্টের মেয়াদের আগে জানান" sub="মেয়াদের আগে এবং মেয়াদের দিন" />
          <div className="setting"><span className="setting-label">মেয়াদের কত দিন আগে</span>
            <Segmented label="সতর্কতার সময়" value={s.reminderLeadDays ?? 30} onChange={(v) => set('reminderLeadDays', v)} options={[{ value: 30, label: '৩০ দিন' }, { value: 14, label: '১৪ দিন' }, { value: 7, label: '৭ দিন' }]} />
          </div>
          <div className="setting"><span className="setting-label">কখন জানাবে ({time(s.reminderMinute ?? 540)})</span>
            <Segmented label="রিমাইন্ডারের সময়" value={s.reminderMinute ?? 540} onChange={(v) => set('reminderMinute', v)} options={[{ value: 540, label: 'সকাল ৯টা' }, { value: 1080, label: 'সন্ধ্যা ৬টা' }]} />
          </div>
          <div className="setting"><Button full kind="secondary" onClick={() => open('reminders')}>ফোনের নোটিফিকেশন যাচাই করুন</Button></div>
        </>}

        {section === 'lock' && <>
          <Switch checked={Boolean(s.privacyEnabled)} onChange={() => open('privacy')} label="অ্যাপ লক" sub="ফোন অন্য কারও হাতে দিলে ডকুমেন্টের নম্বর ও ছবি লুকানো থাকবে" />
          {s.privacyEnabled && (
            <div className="setting"><span className="setting-label">নিজে থেকে লক হবে</span>
              <Segmented label="নিজে থেকে লক হওয়ার সময়" value={s.privacyTimeoutSeconds ?? 60} onChange={(v) => set('privacyTimeoutSeconds', v)} options={[{ value: 60, label: '১ মিনিট' }, { value: 300, label: '৫ মিনিট' }, { value: 900, label: '১৫ মিনিট' }]} />
            </div>
          )}
          <div className="setting"><Button full kind="secondary" onClick={() => open('privacy')}>লক যাচাই ও বিস্তারিত</Button></div>
        </>}

        {section === 'sale' && (
          <div className="setting">
            <b>গোপনীয়তা সুরক্ষিত</b>
            <p className="cap" style={{ margin: 0 }}>নম্বর প্লেট, চ্যাসিস ও ইঞ্জিন নম্বর নিজে থেকে লুকানো থাকে।</p>
            <p className="cap" style={{ margin: 0 }}>BRTA মালিকানা বদলের জন্য স্মার্ট কার্ড, ট্যাক্স টোকেন ও বিক্রয় রশিদ সাথে রাখুন।</p>
          </div>
        )}
      </div>
      {section === 'widget' && <WidgetPreview />}
    </Sheet>
  )
}

function WidgetPreview() {
  const ride = useStore((s) => s.ride)!
  const rules = useStore((s) => s.rules)
  const privacyEnabled = useStore((s) => Boolean(s.settings.privacyEnabled))
  const open = useUI((u) => u.open)
  const f = useFmt()
  const top = byUrgency(rules, ride.odometer, todayISO())[0]
  return (
    <div>
      <div className="widget" style={{ margin: '0 0 8px' }} aria-label="হোম স্ক্রিন উইজেট">
        {privacyEnabled ? (
          <p className="widget-line">অ্যাপ লক চালু আছে। আনলক না করা পর্যন্ত তথ্য গোপন থাকবে।</p>
        ) : (
          <>
            <div className="widget-head"><GaugeIcon size={16} aria-hidden /><b>{ride.name}</b><span>{f.km(ride.odometer)}</span></div>
            <p className={`widget-line ${top?.status}`}>{top?.rule.name.split(' ও ')[0].split(' পরিষ্কার')[0]} · {!top || top.status === 'unknown' ? 'তথ্য নিশ্চিত নয়' : top.status === 'overdue' ? 'এখনই প্রয়োজন' : top.status === 'soon' ? 'সময় কাছাকাছি' : 'ব্যবধান বাকি'}</p>
          </>
        )}
        <div className="widget-btns">
          <button type="button" onClick={() => { haptic('tick'); open('fuel') }}>⛽ তেল</button>
          <button type="button" onClick={() => { haptic('tick'); open('odometer') }}>📏 মিটার রিডিং</button>
        </div>
      </div>
      <p className="cap" style={{ margin: 0 }}>ফোনের হোম স্ক্রিনে চাপ দিয়ে ধরে MotoVault উইজেট যোগ করুন।</p>
    </div>
  )
}
