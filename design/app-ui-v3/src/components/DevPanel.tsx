import { useEffect, useState } from 'react'
import { BRANDS } from '../data/catalog'
import { useStore, useUI } from '../store'
import { Button, Segmented, Switch } from './ui'
import { haptic } from '../lib/haptics'
import type { AppData, Settings } from '../types'

const CHANGES: { id: string; text: string }[] = [
  { id: 'Tokens', text: 'Colours, radii, spacing, nav geometry and motion timings are generated from Sky tokens.json, not copied by hand.' },
  { id: 'P0-2', text: 'Number input accepts Bengali and Latin digits everywhere; digits display setting is separate (Me tab).' },
  { id: 'P0-3', text: 'Fuel: "full tank" is on by default, so mileage is calculated without finding a checkbox.' },
  { id: 'P1-5', text: 'Overdue now says how far overdue ("৩২০ কিমি পেরিয়েছে"), never "0 km left".' },
  { id: 'P1-6', text: 'No OS keyboard for numbers: in-app keypad with Bengali digits, a visible Save above it, nothing hidden.' },
  { id: 'Home', text: 'Next-up card and quick actions come first; the bike photo moved down; gauges sorted by urgency with icon + words.' },
  { id: 'Nav', text: 'Centre "+" opens quick-add from any tab. Theme and settings moved into the "প্রোফাইল" tab.' },
  { id: 'Input', text: 'Fuel by taka (litres derived), remembered price, date chips, estimated odometer, recent pumps.' },
  { id: 'Service', text: 'Tap parts instead of dropdowns, last price remembered, "same as last time", oil grade chips, extras hidden.' },
  { id: 'Smart', text: 'Paste, type or speak: parsed live, handles Bengali digits. Share-to-app and odometer photo are mock-ups.' },
  { id: 'Motion', text: 'Springs for sheets and presses, drag-to-dismiss, gauge sweep, count-up, list layout animation, reduced-motion switch.' },
  { id: 'Save', text: 'Saves are instant with Undo in a toast. No blocking "saving" dialog.' },
  { id: 'Type', text: 'Variable Anek Bangla with real weights, no letter-spacing on Bangla, 12 px minimum, five roles.' },
  { id: 'Colour', text: 'Photos are transparent cut-outs on a brand-tinted well in both themes; status always has icon + words; danger is never the brand colour.' },
  { id: 'Docs', text: 'Scan, review, save, then ask for the renewal reminder while the date is fresh.' },
]

export function DevPanel() {
  const [fixtures, setFixtures] = useState<{ id: string; synthetic: boolean; prototype: AppData & { settings: Settings } }[]>([])
  const [fixtureError, setFixtureError] = useState('')
  const reminderPreview = useUI((u) => u.reminderPreview)
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
      <h1>MotoVault prototype v3</h1>
      <p className="dev-lead">Web mock-up for review. Nothing here changes the Kotlin app. Demo data only.</p>
      <section aria-label="P01 reminder review"><h2>P01 reminder states (simulation only)</h2>
        <select aria-label="Simulated notification state" value={reminderPreview} onChange={(event) => useUI.setState({ reminderPreview: event.target.value as 'off' | 'enabled' | 'denied' | 'failed' })}>
          <option value="off">Off</option><option value="enabled">Enabled</option><option value="denied">Denied</option><option value="failed">Save failed</option>
        </select><Button size="sm" kind="secondary" onClick={() => open('reminders')}>Review reminders</Button>
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
          <Button size="sm" kind="secondary" onClick={() => open('docAdd')}>Add document</Button>
          <Button size="sm" kind="secondary" onClick={() => open('paste')}>Paste</Button>
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
        <h2>What changed vs the current app</h2>
        <ul className="dev-changes">
          {CHANGES.map((c) => <li key={c.id}><b>{c.id}</b><span>{c.text}</span></li>)}
        </ul>
      </section>
    </aside>
  )
}
