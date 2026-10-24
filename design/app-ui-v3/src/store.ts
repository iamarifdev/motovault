import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { AppData, DocKind, FuelLog, FuelType, ItemKey, LogEntry, ServiceLine, ServiceLog, Settings, Tab, VaultDoc } from './types'
import { emptyData, seedData } from './data/seed'
import { itemOf, modelOf } from './data/catalog'
import { todayISO } from './lib/dates'

const defaultSettings: Settings = {
  theme: 'system', digits: 'bn', haptics: true, reduceMotion: false, textScale: 1, gps: false,
  brandOverride: null, serviceReminders: true, docReminders: true,
}

interface Store extends AppData {
  settings: Settings
  setSetting: <K extends keyof Settings>(key: K, value: Settings[K]) => void
  addFuel: (v: Omit<FuelLog, 'id' | 'type'>) => string
  addService: (v: Omit<ServiceLog, 'id' | 'type' | 'total' | 'resets'>) => string
  setOdometer: (odo: number) => void
  removeLog: (id: string) => { entry: LogEntry; index: number } | null
  restoreLog: (entry: LogEntry, index: number) => void
  addDoc: (v: Omit<VaultDoc, 'id'>) => string
  setDocReminder: (id: string, on: boolean) => void
  completeOnboarding: (v: { brandId: string; modelId: string; name: string; odometer: number; lastServiceKnown: boolean }) => void
  startOnboarding: () => void
  resetDemo: () => void
}

const id = () => Math.random().toString(36).slice(2, 9)
export const lineTotal = (l: ServiceLine) => (l.price === null ? 0 : l.price * l.qty)

const pick = (s: Store): AppData => ({
  ride: s.ride, rules: s.rules, logs: s.logs, docs: s.docs, lastPrice: s.lastPrice,
  lastItemPrice: s.lastItemPrice, workshops: s.workshops, places: s.places,
})

/** Optimistic saves keep a snapshot so Undo can put everything back, including reminder baselines and the odometer. */
export const snapshotData = (): AppData => pick(useStore.getState())
export const restoreData = (snap: AppData) => useStore.setState(snap)

const unique = (first: string, rest: string[]) => (first.trim() ? [first.trim(), ...rest.filter((r) => r !== first.trim())].slice(0, 6) : rest)

export const useStore = create<Store>()(
  persist(
    (set, get) => ({
      ...seedData(todayISO()),
      settings: defaultSettings,
      setSetting: (key, value) => set((s) => ({ settings: { ...s.settings, [key]: value } })),

      addFuel: (v) => {
        const entry: FuelLog = { ...v, id: id(), type: 'fuel' }
        set((s) => ({
          logs: [entry, ...s.logs],
          ride: s.ride && { ...s.ride, odometer: Math.max(s.ride.odometer, v.odo), odometerAt: v.date },
          lastPrice: { ...s.lastPrice, [v.fuel]: v.price },
          places: unique(v.place, s.places),
        }))
        return entry.id
      },

      addService: (v) => {
        const keys = v.lines.map((l) => l.item).filter((k): k is ItemKey => k !== 'custom')
        const resets: string[] = []
        const subtotal = v.lines.reduce((a, l) => a + lineTotal(l), 0)
        const entry: ServiceLog = { ...v, id: id(), type: 'service', total: Math.max(0, subtotal + v.labour + v.misc - v.discount), resets }
        set((s) => {
          const rules = s.rules.map((r) => {
            const hit = keys.some((k) => itemOf(k)?.ruleName === itemOf(r.item)?.ruleName && itemOf(k)?.ruleName)
            if (hit) { resets.push(r.id); return { ...r, lastKm: v.odo, lastDate: v.date } }
            return r
          })
          const prices = { ...s.lastItemPrice }
          v.lines.forEach((l) => { if (l.item !== 'custom' && l.price !== null) prices[l.item] = l.price })
          return {
            rules, logs: [entry, ...s.logs], lastItemPrice: prices,
            ride: s.ride && { ...s.ride, odometer: Math.max(s.ride.odometer, v.odo), odometerAt: v.date },
            workshops: unique(v.workshop, s.workshops),
          }
        })
        return entry.id
      },

      setOdometer: (odo) => set((s) => ({ ride: s.ride && { ...s.ride, odometer: odo, odometerAt: todayISO() } })),

      removeLog: (logId) => {
        const index = get().logs.findIndex((l) => l.id === logId)
        if (index < 0) return null
        const entry = get().logs[index]
        set((s) => ({ logs: s.logs.filter((l) => l.id !== logId) }))
        return { entry, index }
      },
      restoreLog: (entry, index) => set((s) => { const logs = [...s.logs]; logs.splice(index, 0, entry); return { logs } }),

      addDoc: (v) => { const doc = { ...v, id: id() }; set((s) => ({ docs: [doc, ...s.docs.filter((d) => d.kind !== v.kind)] })); return doc.id },
      setDocReminder: (docId, on) => set((s) => ({ docs: s.docs.map((d) => (d.id === docId ? { ...d, reminder: on } : d)) })),

      completeOnboarding: ({ brandId, modelId, name, odometer, lastServiceKnown }) => {
        const today = todayISO()
        const base = emptyData()
        // "Last service unknown" starts every reminder from today instead of blocking setup.
        const rules = seedData(today).rules.map((r) => ({ ...r, lastKm: lastServiceKnown ? Math.max(0, odometer - r.intervalKm * 0.4) : odometer, lastDate: today }))
        set({
          ...base, rules,
          ride: { brandId, modelId, name: name || modelOf(modelId)?.name || '', year: new Date().getFullYear() - 2, odometer, odometerAt: today, startedAt: today },
        })
      },
      startOnboarding: () => set({ ...emptyData() }),
      resetDemo: () => set({ ...seedData(todayISO()), settings: defaultSettings }),
    }),
    { name: 'motovault-v3', version: 1, partialize: (s) => ({ ...pick(s), settings: s.settings }) },
  ),
)

export type SheetName = 'quickAdd' | 'fuel' | 'service' | 'odometer' | 'docAdd' | 'docView' | 'paste' | 'rule'
export interface SheetState { name: SheetName; props?: Record<string, unknown> }
export interface Toast { id: number; text: string; undo?: () => void; tone?: 'ok' | 'warn' }

interface UI {
  previewBrand: string | null
  setPreviewBrand: (id: string | null) => void
  tab: Tab
  sheet: SheetState | null
  toast: Toast | null
  setTab: (t: Tab) => void
  open: (name: SheetName, props?: Record<string, unknown>) => void
  close: () => void
  showToast: (text: string, undo?: () => void, tone?: Toast['tone']) => void
  hideToast: () => void
}

let toastId = 0
export const useUI = create<UI>((set) => ({
  previewBrand: null, setPreviewBrand: (previewBrand) => set({ previewBrand }),
  tab: 'home', sheet: null, toast: null,
  setTab: (tab) => set({ tab }),
  open: (name, props) => set({ sheet: { name, props }, toast: null }),
  close: () => set({ sheet: null }),
  showToast: (text, undo, tone) => set({ toast: { id: ++toastId, text, undo, tone } }),
  hideToast: () => set({ toast: null }),
}))

export type { DocKind, FuelType }
