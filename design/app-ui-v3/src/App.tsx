import { useEffect, useLayoutEffect, useRef, useState, type CSSProperties } from 'react'
import { AnimatePresence, MotionConfig } from 'motion/react'
import { useStore, useUI } from './store'
import { brandOf } from './data/catalog'
import { Home } from './screens/Home'
import { Vault } from './screens/Vault'
import { History } from './screens/History'
import { Me } from './screens/Me'
import { Onboarding } from './screens/Onboarding'
import { TabBar } from './components/TabBar'
import { ToastLayer } from './components/Toast'
import { DevPanel } from './components/DevPanel'
import { QuickAddSheet } from './sheets/QuickAdd'
import { FuelSheet } from './sheets/FuelSheet'
import { ServiceSheet } from './sheets/ServiceSheet'
import { OdometerSheet } from './sheets/OdometerSheet'
import { DocAddSheet } from './sheets/DocAddSheet'
import { DocViewSheet } from './sheets/DocViewSheet'
import { PasteSheet } from './sheets/PasteSheet'
import type { ItemKey, Tab } from './types'
import type { ParsedLine } from './lib/smartParse'

const PHONE_W = 440
const PHONE_H = 956

function useSystemDark() {
  const [dark, setDark] = useState(() => window.matchMedia('(prefers-color-scheme: dark)').matches)
  useEffect(() => {
    const q = window.matchMedia('(prefers-color-scheme: dark)')
    const h = () => setDark(q.matches)
    q.addEventListener('change', h)
    return () => q.removeEventListener('change', h)
  }, [])
  return dark
}

function useStageScale() {
  const [state, setState] = useState({ scale: 1, narrow: false })
  useLayoutEffect(() => {
    const calc = () => {
      const narrow = window.innerWidth < 820
      if (narrow) return setState({ scale: 1, narrow })
      const s = Math.min(1, (window.innerHeight - 32) / PHONE_H, (window.innerWidth - 420) / PHONE_W)
      setState({ scale: Math.max(0.5, s), narrow })
    }
    calc()
    window.addEventListener('resize', calc)
    return () => window.removeEventListener('resize', calc)
  }, [])
  return state
}

function renderSheet(name: string, props: Record<string, unknown> = {}) {
  switch (name) {
    case 'quickAdd': return <QuickAddSheet key="quickAdd" />
    case 'fuel': return <FuelSheet key="fuel" amount={props.amount as number | undefined} />
    case 'service': return <ServiceSheet key="service" item={props.item as ItemKey | undefined} rows={props.rows as ParsedLine[] | undefined} />
    case 'odometer': return <OdometerSheet key="odometer" />
    case 'docAdd': return <DocAddSheet key="docAdd" />
    case 'docView': return <DocViewSheet key={`docView-${props.id}`} id={props.id as string} />
    case 'paste': return <PasteSheet key="paste" />
    default: return null
  }
}

const PANELS: { id: Tab; node: () => JSX.Element }[] = [
  { id: 'home', node: () => <Home /> },
  { id: 'vault', node: () => <Vault /> },
  { id: 'history', node: () => <History /> },
  { id: 'me', node: () => <Me /> },
]

function Screen() {
  const ride = useStore((s) => s.ride)
  const tab = useUI((s) => s.tab)
  const sheet = useUI((s) => s.sheet)
  const close = useUI((s) => s.close)

  useEffect(() => {
    const h = (e: KeyboardEvent) => { if (e.key === 'Escape' && useUI.getState().sheet) close() }
    window.addEventListener('keydown', h)
    return () => window.removeEventListener('keydown', h)
  }, [close])

  if (!ride) return <Onboarding />
  return (
    <>
      <main className="panels">
        {PANELS.map((p) => (
          <div key={p.id} className={`panel${tab === p.id ? ' on' : ''}`} role="tabpanel" aria-hidden={tab !== p.id} {...(tab === p.id ? {} : { inert: '' as unknown as boolean })}>
            {p.node()}
          </div>
        ))}
      </main>
      <TabBar />
      <AnimatePresence>{sheet && renderSheet(sheet.name, sheet.props)}</AnimatePresence>
    </>
  )
}

export default function App() {
  const settings = useStore((s) => s.settings)
  const ride = useStore((s) => s.ride)
  const previewBrand = useUI((s) => s.previewBrand)
  const systemDark = useSystemDark()
  const { scale, narrow } = useStageScale()
  const rootRef = useRef<HTMLDivElement>(null)

  const theme = settings.theme === 'system' ? (systemDark ? 'dark' : 'light') : settings.theme
  const brand = brandOf(settings.brandOverride ?? previewBrand ?? ride?.brandId ?? 'yamaha')
  const vars = {
    '--brand': theme === 'dark' ? brand.dark : brand.light,
    '--on-brand': theme === 'dark' ? '#101215' : '#FFFFFF',
    '--scale': settings.textScale,
  } as CSSProperties

  return (
    <MotionConfig reducedMotion={settings.reduceMotion ? 'always' : 'user'}>
      <div ref={rootRef} className={`stage mv${narrow ? ' narrow' : ''}`} data-theme={theme} data-reduce={settings.reduceMotion ? '' : undefined} style={vars}>
        {!narrow && <DevPanel />}
        <div className="phone-slot" style={narrow ? undefined : { width: PHONE_W * scale, height: PHONE_H * scale }}>
          <div className="phone" style={narrow ? undefined : { width: PHONE_W, height: PHONE_H, transform: `scale(${scale})` }}>
            <div className="screen">
              {!narrow && <div className="statusbar" aria-hidden><span>9:41</span><i className="island" /><span>●●●</span></div>}
              <div className="content"><Screen /></div>
              <ToastLayer />
              {!narrow && <i className="home-bar" aria-hidden />}
            </div>
          </div>
        </div>
      </div>
    </MotionConfig>
  )
}
