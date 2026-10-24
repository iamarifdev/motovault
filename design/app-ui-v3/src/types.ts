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
}

export interface Ride {
  brandId: string
  modelId: string
  name: string
  year: number
  odometer: number
  odometerAt: string
  startedAt: string
}

export interface Rule {
  id: string
  item: ItemKey
  name: string
  intervalKm: number
  intervalDays: number
  lastKm: number
  lastDate: string
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
}

export type LogEntry = FuelLog | ServiceLog

export type DocKind = 'registration' | 'licence' | 'tax' | 'insurance' | 'fitness'

export interface VaultDoc {
  id: string
  kind: DocKind
  number: string
  holder: string
  expiry: string
  reminder: boolean
}

export interface AppData {
  ride: Ride | null
  rules: Rule[]
  logs: LogEntry[]
  docs: VaultDoc[]
  lastPrice: Record<FuelType, number>
  lastItemPrice: Partial<Record<ItemKey, number>>
  workshops: string[]
  places: string[]
}
