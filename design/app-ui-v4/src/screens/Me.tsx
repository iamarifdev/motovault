import { Bell, ChevronRight, Cloud, DatabaseBackup, Gauge as GaugeIcon, Globe, LayoutGrid, Lock, Package, Palette, Settings2 } from 'lucide-react'
import { useStore, useUI } from '../store'
import { brandOf, modelOf } from '../data/catalog'
import { useFmt } from '../lib/bn'
import { haptic } from '../lib/haptics'
import type { SettingsSection } from '../sheets/SettingsSheet'

const EMPTY_BIKES: NonNullable<ReturnType<typeof useStore.getState>['garageBikes']> = []

/** One settings row: icon, name, current value, chevron. Tapping opens a small sheet. */
function Row({ icon: Icon, label, value, tone, onClick }: { icon: typeof Palette; label: string; value?: string; tone?: 'warn' | 'ok'; onClick: () => void }) {
  return (
    <li>
      <button type="button" className="set-row" onClick={() => { haptic('tick'); onClick() }}>
        <span className="set-icon"><Icon size={19} aria-hidden /></span>
        <span className="set-label">{label}</span>
        {value && <span className={`set-value${tone ? ` ${tone}` : ''}`}>{value}</span>}
        <ChevronRight size={18} aria-hidden className="muted-icon" />
      </button>
    </li>
  )
}

function Group({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="set-group" aria-label={title}>
      <h2>{title}</h2>
      <ul>{children}</ul>
    </section>
  )
}

export function Me() {
  const ride = useStore((st) => st.ride)!
  const s = useStore((st) => st.settings)
  const docsCount = useStore((st) => st.docs.length)
  const garageBikes = useStore((st) => st.garageBikes) ?? EMPTY_BIKES
  const open = useUI((u) => u.open)
  const f = useFmt()
  const settings = (section: SettingsSection) => open('settings', { section })

  const brand = brandOf(ride.brandId)
  const model = modelOf(ride.modelId)
  const bikeCount = 1 + garageBikes.filter((b) => !b.ride.archived).length
  const themeLabel = s.theme === 'dark' ? 'ডার্ক' : s.theme === 'light' ? 'লাইট' : 'ডিভাইসের মতো'
  const remindersOn = s.serviceReminders || s.docReminders

  return (
    <div className="screen-pad">
      <header className="page-head"><h1>প্রোফাইল</h1></header>

      <button type="button" className="profile set-bike" onClick={() => { haptic('tick'); open('bikeSwitcher') }}>
        <div className="profile-photo">{model?.photo ? <img src={model.photo} alt="" /> : <GaugeIcon size={32} aria-hidden />}</div>
        <div>
          <b>{brand.name} {ride.name}</b>
          <small>{f.d(String(ride.year))} · {f.km(ride.odometer)}{bikeCount > 1 ? ` · ${f.d(String(bikeCount))}টি বাইক` : ''}</small>
        </div>
        <span className="set-value">বদলান</span>
        <ChevronRight size={18} aria-hidden className="muted-icon" />
      </button>

      <Group title="নিরাপত্তা ও ডেটা">
        <Row icon={Lock} label="অ্যাপ লক" value={s.privacyEnabled ? 'চালু' : `বন্ধ · ${f.d(String(docsCount))}টি ডকুমেন্ট`} tone={s.privacyEnabled ? 'ok' : 'warn'} onClick={() => settings('lock')} />
        <Row icon={DatabaseBackup} label="ফোনে ব্যাকআপ ও পুনরুদ্ধার" onClick={() => open('recovery')} />
        <Row icon={Cloud} label="ক্লাউড ব্যাকআপ" value="সংযুক্ত নয়" onClick={() => open('cloud-backup')} />
        <Row icon={Package} label="বিক্রয় ও হস্তান্তর প্যাক" onClick={() => settings('sale')} />
      </Group>

      <Group title="রিমাইন্ডার">
        <Row icon={Bell} label="সার্ভিস ও ডকুমেন্ট" value={remindersOn ? `চালু · ${f.d(String(s.reminderLeadDays ?? 30))} দিন আগে` : 'বন্ধ'} tone={remindersOn ? undefined : 'warn'} onClick={() => settings('reminders')} />
      </Group>

      <Group title="অ্যাপ">
        <Row icon={Palette} label="চেহারা" value={`${themeLabel} · ${brandOf(s.brandOverride ?? ride.brandId).name}`} onClick={() => settings('appearance')} />
        <Row icon={Globe} label="ভাষা ও সংখ্যা" value={`${s.language === 'en' ? 'English' : 'বাংলা'} · ${s.digits === 'bn' ? '১২৩' : '123'}`} onClick={() => settings('language')} />
        <Row icon={Settings2} label="আরও সেটিংস" value="ভাইব্রেশন, অ্যানিমেশন, লোকেশন" onClick={() => settings('more')} />
        <Row icon={LayoutGrid} label="হোম স্ক্রিন উইজেট" onClick={() => settings('widget')} />
      </Group>

      <section className="about">
        <p className="cap">MotoVault প্রোটোটাইপ v4 · সব তথ্য এই ডিভাইসেই থাকে</p>
      </section>
    </div>
  )
}
