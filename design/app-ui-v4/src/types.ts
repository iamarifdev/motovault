export type Theme = 'system' | 'light' | 'dark'
export type Digits = 'bn' | 'en'
export type FuelType = 'octane' | 'petrol'
export type Tab = 'home' | 'vault' | 'history' | 'me'

export type ItemKey =
  | 'engine_oil' | 'oil_filter' | 'air_filter' | 'spark_plug' | 'chain_sprocket'
  | 'brake_pad' | 'coolant' | 'tyre' | 'battery' | 'general_service'

export interface Settings {
  theme: Theme
  digits: Digits
  haptics: boolean
  reduceMotion: boolean
  textScale: 1 | 1.15 | 1.3
  gps: boolean
  brandOverride: string | null
  serviceReminders: boolean
  docReminders: boolean
  reminderLeadDays?: number
  reminderMinute?: number
  privacyEnabled?: boolean
  privacyTimeoutSeconds?: number
  /** Work ledger is off by default; turning it off keeps saved shifts. */
  workMode?: boolean
  /** UI language. Saved before as an untyped key, so older data already has it. */
  language?: 'bn' | 'en'
}

export interface Ride {
  id?: string
  archived?: boolean
  brandId: string
  modelId: string
  name: string
  year: number
  cc?: number
  variantLabel?: string
  specs?: string[]
  odometer: number
  odometerAt: string
  startedAt: string
}

export interface GarageBike {
  ride: Ride
  rules: Rule[]
  logs: LogEntry[]
  docs: VaultDoc[]
  extraExpenses?: ExtraExpense[]
}

export interface Rule {
  id: string
  item: ItemKey
  name: string
  intervalKm: number
  intervalDays: number
  lastKm: number
  lastDate: string
  baseline?: 'confirmed' | 'estimate' | 'unknown'
  source?: string
  action?: 'inspect' | 'clean' | 'lubricate' | 'adjust' | 'replace'
  sourceRevision?: string
  sourcePage?: string
  applicability?: string
  reminder?: boolean
  snoozedUntil?: string
}

export interface FuelLog {
  id: string
  type: 'fuel'
  date: string
  odo: number
  amount: number
  liters: number
  price: number
  full: boolean
  fuel: FuelType
  place: string
  /** The rider checked this price per litre (Compose FuelEntryAdapter requires it for a new price). */
  priceConfirmed?: boolean
  /** A fill between this one and the previous record was not saved, so the mileage cycle restarts here. */
  missedFill?: boolean
}

export interface ServiceLine {
  item: ItemKey | 'custom'
  name: string
  qty: number
  price: number | null
  detail?: string
}

export interface ServiceLog {
  id: string
  type: 'service'
  date: string
  odo: number
  lines: ServiceLine[]
  labour: number
  misc: number
  discount: number
  total: number
  workshop: string
  notes: string
  resets: string[]
  visitState?: 'paid' | 'quote'
  repairCase?: string
  warrantyMonths?: number
  workshopPhone?: string
}

export type LogEntry = FuelLog | ServiceLog

export type DocKind = 'registration' | 'licence' | 'tax' | 'insurance' | 'fitness'

export interface DocRenewal {
  id: string
  kind: DocKind
  previousExpiry: string
  newExpiry: string
  renewedOn: string
  fee: number | null
  receiptName?: string
  notes?: string
  revision: number
}

export interface VaultDoc {
  id: string
  kind: DocKind
  number: string
  holder: string
  expiry: string
  reminder: boolean
  snoozedUntil?: string
  warningDays?: number
  revision?: number
  renewals?: DocRenewal[]
  checkedSteps?: string[]
}

export interface ExtraExpense {
  id: number
  category: number
  poisha: number
  date: string
  note: string
}

/** One ride-share or delivery shift (P14 work ledger). Money in poisha, like Compose. */
export interface WorkShift {
  id: string
  date: string
  grossPoisha: number
  deductionPoisha: number
  workKm: number
  dayKm: number
}

export interface AppData {
  ride: Ride | null
  rules: Rule[]
  logs: LogEntry[]
  docs: VaultDoc[]
  extraExpenses?: ExtraExpense[]
  garageBikes?: GarageBike[]
  workShifts?: WorkShift[]
  lastPrice: Record<FuelType, number>
  lastItemPrice: Partial<Record<ItemKey, number>>
  workshops: string[]
  places: string[]
}
