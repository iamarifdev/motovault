import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { AppData, DocKind, DocRenewal, FuelLog, FuelType, ItemKey, LogEntry, ServiceLine, ServiceLog, Settings, Tab, VaultDoc } from './types'
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
  updateFuel: (id: string, v: Omit<FuelLog, 'id' | 'type'>) => void
  addService: (v: Omit<ServiceLog, 'id' | 'type' | 'total' | 'resets'>) => string
  setOdometer: (odo: number) => void
  removeLog: (id: string) => { entry: LogEntry; index: number } | null
  restoreLog: (entry: LogEntry, index: number) => void
  addDoc: (v: Omit<VaultDoc, 'id'>) => string
  setDocReminder: (id: string, on: boolean) => void
  renewDoc: (docId: string, v: { newExpiry: string; fee: number | null; receiptName?: string; notes?: string }) => void
  toggleDocStep: (docId: string, stepId: string) => void
  completeOnboarding: (v: { brandId: string; modelId: string; name: string; odometer: number; lastServiceKnown: boolean; lastServiceKm?: number; lastServiceDate?: string }) => void
  switchBike: (bikeId: string) => void
  toggleArchiveBike: (bikeId: string) => boolean
  startOnboarding: () => void
  resetDemo: () => void
}

const id = () => Math.random().toString(36).slice(2, 9)
export const lineTotal = (l: ServiceLine) => (l.price === null ? 0 : l.price * l.qty)
const bikeIdOf = (r: { id?: string } | null) => r?.id ?? 'primary'

const pick = (s: Store): AppData => ({
  ride: s.ride, rules: s.rules, logs: s.logs, docs: s.docs, garageBikes: s.garageBikes ?? [], lastPrice: s.lastPrice,
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

      updateFuel: (entryId, v) => set((s) => ({
        logs: s.logs.map((entry) => entry.id === entryId && entry.type === 'fuel' ? { ...entry, ...v } : entry),
        ride: s.ride && { ...s.ride, odometer: Math.max(s.ride.odometer, v.odo), odometerAt: v.odo >= s.ride.odometer ? v.date : s.ride.odometerAt },
        lastPrice: { ...s.lastPrice, [v.fuel]: v.price },
        places: unique(v.place, s.places),
      })),

      addService: (v) => {
        const keys = v.lines.map((l) => l.item).filter((k): k is ItemKey => k !== 'custom')
        const resets: string[] = []
        const subtotal = v.lines.reduce((a, l) => a + lineTotal(l), 0)
        const entry: ServiceLog = { ...v, id: id(), type: 'service', total: Math.max(0, subtotal + v.labour + v.misc - v.discount), resets }
        const isPaid = (v.visitState ?? 'paid') === 'paid'
        set((s) => {
          const rules = isPaid ? s.rules.map((r) => {
            const hit = keys.some((k) => itemOf(k)?.ruleName === itemOf(r.item)?.ruleName && itemOf(k)?.ruleName)
            if (hit) { resets.push(r.id); return { ...r, lastKm: v.odo, lastDate: v.date, baseline: 'confirmed' as const } }
            return r
          }) : s.rules
          const prices = { ...s.lastItemPrice }
          v.lines.forEach((l) => { if (l.item !== 'custom' && l.price !== null) prices[l.item] = l.price })
          return {
            rules, logs: [entry, ...s.logs], lastItemPrice: prices,
            ride: isPaid && s.ride ? { ...s.ride, odometer: Math.max(s.ride.odometer, v.odo), odometerAt: v.date } : s.ride,
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

      /** The document expiry, the append-only renewal event and the checklist reset as one save. */
      renewDoc: (docId, v) => set((s) => ({
        docs: s.docs.map((d) => {
          if (d.id !== docId) return d
          const revision = (d.revision ?? 0) + 1
          const renewal: DocRenewal = {
            id: id(), kind: d.kind, previousExpiry: d.expiry, newExpiry: v.newExpiry, renewedOn: todayISO(),
            fee: v.fee, receiptName: v.receiptName, notes: v.notes?.trim() || undefined, revision,
          }
          return { ...d, expiry: v.newExpiry, snoozedUntil: undefined, revision, renewals: [renewal, ...(d.renewals ?? [])], checkedSteps: [] }
        }),
      })),
      toggleDocStep: (docId, stepId) => set((s) => ({
        docs: s.docs.map((d) => d.id !== docId ? d : {
          ...d,
          checkedSteps: d.checkedSteps?.includes(stepId) ? d.checkedSteps.filter((step) => step !== stepId) : [...(d.checkedSteps ?? []), stepId],
        }),
      })),

      completeOnboarding: ({ brandId, modelId, name, odometer, lastServiceKnown, lastServiceKm, lastServiceDate }) => {
        const today = todayISO()
        const s = get()
        const rules = seedData(today).rules.map((r) => ({
          ...r,
          lastKm: lastServiceKnown ? (lastServiceKm ?? 0) : 0,
          lastDate: lastServiceKnown ? (lastServiceDate ?? today) : today,
          baseline: lastServiceKnown ? 'estimate' as const : 'unknown' as const,
        }))
        const nextRide = {
          id: s.ride ? `bike-${id()}` : 'primary', archived: false,
          brandId, modelId, name: name || modelOf(modelId)?.name || '', year: new Date().getFullYear() - 2,
          odometer, odometerAt: today, startedAt: today,
        }
        if (!s.ride) {
          const base = emptyData()
          set({ ...base, garageBikes: [], rules, ride: nextRide })
          return
        }
        const currentBikeDocs = s.docs.filter((d) => d.kind !== 'licence')
        const ownerDocs = s.docs.filter((d) => d.kind === 'licence')
        const archivedCurrent = { ride: { ...s.ride, id: bikeIdOf(s.ride) }, rules: s.rules, logs: s.logs, docs: currentBikeDocs }
        set({
          ride: nextRide, rules, logs: [], docs: ownerDocs,
          garageBikes: [...(s.garageBikes ?? []), archivedCurrent],
        })
      },
      switchBike: (bikeId) => set((s) => {
        if (!s.ride || bikeIdOf(s.ride) === bikeId) return s
        const pool = s.garageBikes ?? []
        const target = pool.find((b) => bikeIdOf(b.ride) === bikeId)
        if (!target || target.ride.archived) return s
        const ownerDocs = s.docs.filter((d) => d.kind === 'licence')
        const prev = { ride: { ...s.ride, id: bikeIdOf(s.ride) }, rules: s.rules, logs: s.logs, docs: s.docs.filter((d) => d.kind !== 'licence') }
        return {
          ride: target.ride, rules: target.rules, logs: target.logs,
          docs: [...ownerDocs, ...target.docs.filter((d) => d.kind !== 'licence')],
          garageBikes: [prev, ...pool.filter((b) => bikeIdOf(b.ride) !== bikeId)],
        }
      }),
      toggleArchiveBike: (bikeId) => {
        const s = get()
        if (!s.ride) return false
        const all = [{ ride: { ...s.ride, id: bikeIdOf(s.ride) }, rules: s.rules, logs: s.logs, docs: s.docs.filter((d) => d.kind !== 'licence') }, ...(s.garageBikes ?? [])]
        const target = all.find((b) => bikeIdOf(b.ride) === bikeId)
        if (!target) return false
        const nextArchived = !target.ride.archived
        const remainingActive = all.filter((b) => bikeIdOf(b.ride) !== bikeId ? !b.ride.archived : !nextArchived)
        if (remainingActive.length === 0) return false
        const ownerDocs = s.docs.filter((d) => d.kind === 'licence')
        const updated = all.map((b) => bikeIdOf(b.ride) === bikeId ? { ...b, ride: { ...b.ride, archived: nextArchived } } : b)
        const active = bikeIdOf(s.ride) === bikeId && nextArchived ? remainingActive[0] : (updated.find((b) => bikeIdOf(b.ride) === bikeIdOf(s.ride)) ?? remainingActive[0])
        set({
          ride: active.ride, rules: active.rules, logs: active.logs,
          docs: [...ownerDocs, ...active.docs.filter((d) => d.kind !== 'licence')],
          garageBikes: updated.filter((b) => bikeIdOf(b.ride) !== bikeIdOf(active.ride)),
        })
        return true
      },
      startOnboarding: () => set({ ...emptyData() }),
      resetDemo: () => set({ ...seedData(todayISO()), settings: defaultSettings }),
    }),
    { name: 'motovault-v3', version: 1, partialize: (s) => ({ ...pick(s), settings: s.settings }) },
  ),
)

export type SheetName = 'quickAdd' | 'fuel' | 'service' | 'odometer' | 'docAdd' | 'docView' | 'renewal' | 'paste' | 'rule' | 'gaugeDetail' | 'reminders' | 'privacy' | 'recovery' | 'bikeSwitcher' | 'breakdown' | 'work-ledger' | 'cloud-backup'
export interface SheetState { name: SheetName; props?: Record<string, unknown> }
export interface Toast { id: number; text: string; undo?: () => void; tone?: 'ok' | 'warn' }

interface UI {
  reminderPreview: 'off' | 'enabled' | 'denied' | 'failed'
  previewBrand: string | null
  addingBike: boolean
  setPreviewBrand: (id: string | null) => void
  setAddingBike: (on: boolean) => void
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
  reminderPreview: 'off',
  previewBrand: null, setPreviewBrand: (previewBrand) => set({ previewBrand }),
  addingBike: false, setAddingBike: (addingBike) => set({ addingBike, sheet: null }),
  tab: 'home', sheet: null, toast: null,
  setTab: (tab) => set({ tab, addingBike: false }),
  open: (name, props) => set({ sheet: { name, props }, toast: null }),
  close: () => set({ sheet: null }),
  showToast: (text, undo, tone) => set({ toast: { id: ++toastId, text, undo, tone } }),
  hideToast: () => set({ toast: null }),
}))

export type { DocKind, FuelType }
