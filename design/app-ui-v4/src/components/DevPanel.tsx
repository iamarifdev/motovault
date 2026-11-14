import { useEffect, useState } from 'react'
import { BRANDS } from '../data/catalog'
import { useStore, useUI } from '../store'
import { Button, Segmented, Switch } from './ui'
import { haptic } from '../lib/haptics'
import type { AppData, Settings } from '../types'

const CHANGES: { id: string; text: string }[] = [
  { id: 'V4-Onboard', text: 'Zero-hassle setup: Brand -> clean Model/Version (no CC filters or spec overload) -> Odometer via Camera OCR, Bangla Voice, 3-chip service timing, or "এখন মনে নেই · পরে দেব" skip.' },
  { id: 'V4-Fuel', text: 'Pump meter Camera OCR ("পাম্প মিটারের ছবি") + 1-tap Bangla Voice ("মুখে বলুন") + 1-tap Taka presets (৳200..Full tank); removed mandatory price/missed-fill confirmation checkboxes.' },
  { id: 'V4-Service', text: 'Workshop receipt Camera OCR ("রশিদের ছবি") + inline Bangla Voice/Text ("মুখে বলুন বা লিখুন") without disclosure friction + quick repeat-last-service pill.' },
  { id: 'V4-Docs', text: '2-step Document Add: Photo -> Expiry Date (default 1yr, 1/2/5/10yr chips) & inline reminder switch; number/holder collapsed as optional.' },
  { id: 'V4-Home', text: '4-Zone Garage with plain Bangla telemetry labels ("মোট চলা", "মাইলেজ"), top-3 wear priority slice + expand toggle, wired Breakdown emergency sheet, and 1-tap reset in GaugeDetail.' },
  { id: 'V4-History', text: 'Simplified CostInsights (total, period/category chips, stacked Fuel/Service/Other bar, "+ অন্যান্য খরচ যোগ") and sticky month headers without commercial work-ledger clutter.' },
  { id: 'V4-Sheet', text: 'Pull-down sheet body handoff when scrolled to top + sticky primary footer CTAs in Privacy & Recovery sheets.' },
  { id: 'V4-Me', text: 'Profile grouped into 4 summary accordions with live status subtitles & plain-Bangla backup summary; cloud-backup sheet wired.' },
  { id: 'V4-TabBar', text: 'Long-press (360ms) or right-click central "+" pops quick-launch pills (Fuel / Service / Odometer) alongside tap QuickAdd.' },
]

export function DevPanel() {
  const [fixtures, setFixtures] = useState<{ id: string; synthetic: boolean; prototype: AppData & { settings: Settings } }[]>([])
  const [fixtureError, setFixtureError] = useState('')
  const reminderPreview = useUI((u) => u.reminderPreview)
  const odoPhotoPreview = useUI((u) => u.odoPhotoPreview)
  const sharePreview = useUI((u) => u.sharePreview)
  useEffect(() => {
    Promise.all(['/p00-fixtures.json', '/p01-fixtures.json'].map((url) => fetch(url).then((r) => r.ok ? r.json() : [])))
      .then((collections) => setFixtures(collections.flat())).catch(() => setFixtures([]))
  }, [])
  const s = useStore((st) => st.settings)
  const set = useStore((st) => st.setSetting)
  const ride = useStore((st) => st.ride)
  const startOnboarding = useStore((st) => st.startOnboarding)
  const resetDemo = useStore((st) => st.resetDemo)
  const open = useUI((u) => u.open)
  const setTab = useUI((u) => u.setTab)
  const [log, setLog] = useState<{ n: number; kind: string }[]>([])

  useEffect(() => {
    let n = 0
    const h = (e: Event) => setLog((l) => [{ n: ++n, kind: (e as CustomEvent<string>).detail }, ...l].slice(0, 6))
    window.addEventListener('mv-haptic', h)
    return () => window.removeEventListener('mv-haptic', h)
  }, [])

  return (
    <aside className="dev" aria-label="Review panel">
      <h1>MotoVault prototype v4</h1>
      <p className="dev-lead">UX/Motion evolution over v3 for review & change tracking. Runs independently on port 5274. Demo data only.</p>
      <section aria-label="P01 reminder & capture review"><h2>P01 fixture states (simulation only)</h2>
        <select aria-label="Simulated notification state" value={reminderPreview} onChange={(event) => useUI.setState({ reminderPreview: event.target.value as 'off' | 'enabled' | 'denied' | 'failed' })}>
          <option value="off">Reminder: Off</option><option value="enabled">Reminder: Enabled</option><option value="denied">Reminder: Denied</option><option value="failed">Reminder: Save failed</option>
        </select>
        <select aria-label="Simulated odometer photo state" value={odoPhotoPreview} onChange={(event) => useUI.setState({ odoPhotoPreview: event.target.value as 'clear' | 'blur' | 'multiple' | 'permission' })}>
          <option value="clear">Odo photo: Clear</option><option value="blur">Odo photo: Blur / glare</option><option value="multiple">Odo photo: Multiple</option><option value="permission">Odo photo: Denied</option>
        </select>
        <select aria-label="Simulated share preview state" value={sharePreview} onChange={(event) => useUI.setState({ sharePreview: event.target.value as 'warm' | 'cold' | 'locked' | 'duplicate' | 'unsupported' })}>
          <option value="warm">Share: App open</option><option value="cold">Share: App closed</option><option value="locked">Share: Locked</option><option value="duplicate">Share: Duplicate</option><option value="unsupported">Share: Unsupported</option>
        </select>
        <div className="dev-grid">
          <Button size="sm" kind="secondary" onClick={() => open('reminders')}>Review reminders</Button>
          <Button size="sm" kind="secondary" onClick={() => open('paste')}>Paste / Smart text</Button>
        </div>
      </section>

      {fixtures.length > 0 && <section aria-label="P00 baseline fixtures">
        <h2>P00 synthetic baseline</h2>
        <select aria-label="P00 baseline fixture" defaultValue="" onChange={(event) => {
          const fixture = fixtures.find((f) => f.id === event.target.value)
          if (!fixture?.synthetic) { setFixtureError('Choose a synthetic fixture'); return }
          useStore.setState(fixture.prototype)
          useUI.setState({ tab: 'home', sheet: null, toast: null })
          setFixtureError('')
        }}>
          <option value="" disabled>Choose fixture</option>
          {fixtures.map((f) => <option key={f.id} value={f.id}>{f.id}</option>)}
        </select>
        {fixtureError && <p role="alert">{fixtureError}</p>}
        <p>Local synthetic data only. Reference date: 24 October 2026.</p>
      </section>}

      <section>
        <h2>Try</h2>
        <div className="dev-grid">
          <Button size="sm" kind="secondary" onClick={() => open('fuel')}>Fuel</Button>
          <Button size="sm" kind="secondary" onClick={() => open('service')}>Service</Button>
          <Button size="sm" kind="secondary" onClick={() => open('odometer')}>Odometer</Button>
          <Button size="sm" kind="secondary" onClick={() => open('paste')}>Paste / Voice</Button>
          <Button size="sm" kind="secondary" onClick={() => open('docAdd')}>Add document</Button>
          <Button size="sm" kind="secondary" onClick={() => open('privacy')}>Privacy</Button>
          <Button size="sm" kind="secondary" onClick={() => open('recovery')}>Recovery</Button>
          <Button size="sm" kind="secondary" onClick={() => { setTab('home'); startOnboarding() }}>Onboarding</Button>
        </div>
        <Button size="sm" kind="ghost" full onClick={resetDemo}>Reset demo data</Button>
      </section>

      <section>
        <h2>Look and feel</h2>
        <Segmented label="Theme" value={s.theme} onChange={(v) => set('theme', v)} options={[{ value: 'system', label: 'System' }, { value: 'light', label: 'Light' }, { value: 'dark', label: 'Dark' }]} />
        <Segmented label="Digits" value={s.digits} onChange={(v) => set('digits', v)} options={[{ value: 'bn', label: '১২৩' }, { value: 'en', label: '123' }]} />
        <Segmented label="Text" value={s.textScale} onChange={(v) => set('textScale', v)} options={[{ value: 1, label: '1×' }, { value: 1.15, label: '1.15×' }, { value: 1.3, label: '1.3×' }]} />
        <div className="swatches small" role="radiogroup" aria-label="Accent">
          {BRANDS.map((b) => (
            <button key={b.id} type="button" role="radio" aria-checked={(s.brandOverride ?? ride?.brandId) === b.id} aria-label={b.name} className={(s.brandOverride ?? ride?.brandId) === b.id ? 'on' : ''}
              style={{ ['--sw-l' as string]: b.light, ['--sw-d' as string]: b.dark }} onClick={() => { haptic('tick'); set('brandOverride', b.id === ride?.brandId ? null : b.id) }} />
          ))}
        </div>
        <Switch checked={s.reduceMotion} onChange={(v) => set('reduceMotion', v)} label="Reduce motion" />
        <Switch checked={s.haptics} onChange={(v) => set('haptics', v)} label="Haptics" sub="Vibrates on phones that support it" />
        <div className="haptic-log" aria-label="Haptic log">
          {log.length === 0 ? <span>Haptic events show here</span> : log.map((l) => <i key={l.n} className={`hk ${l.kind}`}>{l.kind}</i>)}
        </div>
      </section>

      <section>
        <h2>What changed vs v3</h2>
        <ul className="dev-changes">
          {CHANGES.map((c) => <li key={c.id}><b>{c.id}</b><span>{c.text}</span></li>)}
        </ul>
      </section>
    </aside>
  )
}
