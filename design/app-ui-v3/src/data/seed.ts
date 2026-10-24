import type { AppData, FuelLog, ServiceLog } from '../types'
import { addDays } from '../lib/dates'

export const emptyData = (): AppData => ({
  ride: null, rules: [], logs: [], docs: [],
  lastPrice: { octane: 120, petrol: 115 }, lastItemPrice: {}, workshops: [], places: [],
})

/** Demo garage, dated relative to today so the urgency states always show: one overdue, two soon, two fine. */
export function seedData(today: string): AppData {
  const d = (n: number) => addDays(today, n)
  const fuel = (id: string, day: number, odo: number, liters: number, full: boolean, place: string): FuelLog => ({
    id, type: 'fuel', date: d(day), odo, liters, full, fuel: 'octane', price: 120, amount: Math.round((liters * 120) / 10) * 10, place,
  })
  const logs: (FuelLog | ServiceLog)[] = [
    fuel('f7', -11, 12340, 4.9, true, 'পদ্মা পাম্প, মিরপুর'),
    fuel('f6', -17, 12120, 5.2, true, 'পদ্মা পাম্প, মিরপুর'),
    fuel('f5', -27, 11890, 5.0, true, 'যমুনা ফিলিং স্টেশন'),
    fuel('f4', -33, 11700, 3.8, false, 'যমুনা ফিলিং স্টেশন'),
    fuel('f3', -41, 11500, 4.8, true, 'পদ্মা পাম্প, মিরপুর'),
    fuel('f2', -52, 11290, 4.7, true, 'পদ্মা পাম্প, মিরপুর'),
    fuel('f1', -64, 11080, 5.0, true, 'যমুনা ফিলিং স্টেশন'),
    {
      id: 's1', type: 'service', date: d(-45), odo: 11350, workshop: 'রহমান মোটরস, মিরপুর ১০', notes: '',
      lines: [
        { item: 'engine_oil', name: 'ইঞ্জিন অয়েল', qty: 1, price: 900, detail: '10W-40' },
        { item: 'oil_filter', name: 'অয়েল ফিল্টার', qty: 1, price: 250 },
      ],
      labour: 200, misc: 0, discount: 0, total: 1350, resets: ['r-oil'],
    },
  ]
  return {
    ride: { brandId: 'yamaha', modelId: 'yamaha-r15-v4', name: 'R15 V4', year: 2023, odometer: 12340, odometerAt: d(-11), startedAt: d(-90) },
    rules: [
      { id: 'r-oil', item: 'engine_oil', name: 'ইঞ্জিন অয়েল ও ফিল্টার', intervalKm: 3000, intervalDays: 180, lastKm: 11350, lastDate: d(-45) },
      { id: 'r-chain', item: 'chain_sprocket', name: 'চেইন পরিষ্কার ও লুব্রিকেন্ট', intervalKm: 500, intervalDays: 30, lastKm: 11520, lastDate: d(-41) },
      { id: 'r-air', item: 'air_filter', name: 'এয়ার ফিল্টার', intervalKm: 5000, intervalDays: 180, lastKm: 9900, lastDate: d(-140) },
      { id: 'r-spark', item: 'spark_plug', name: 'স্পার্ক প্লাগ', intervalKm: 8000, intervalDays: 365, lastKm: 8000, lastDate: d(-200) },
      { id: 'r-brakes', item: 'brake_pad', name: 'ব্রেক প্যাড', intervalKm: 10000, intervalDays: 365, lastKm: 7200, lastDate: d(-300) },
    ],
    logs,
    docs: [
      { id: 'd1', kind: 'registration', number: 'ঢাকা মেট্রো-ল ৬২-৪৫১৮', holder: 'মো. রাফিদ হাসান', expiry: d(400), reminder: true },
      { id: 'd2', kind: 'licence', number: 'DL-1987-654321', holder: 'মো. রাফিদ হাসান', expiry: d(24), reminder: false },
      { id: 'd3', kind: 'tax', number: 'TT-2025-88120', holder: 'মো. রাফিদ হাসান', expiry: d(-5), reminder: false },
      { id: 'd4', kind: 'insurance', number: 'INS-554-2291', holder: 'মো. রাফিদ হাসান', expiry: d(200), reminder: true },
    ],
    lastPrice: { octane: 120, petrol: 115 },
    lastItemPrice: { engine_oil: 900, oil_filter: 250, chain_sprocket: 350, air_filter: 400, spark_plug: 300, brake_pad: 650 },
    workshops: ['রহমান মোটরস, মিরপুর ১০', 'স্টার সার্ভিস সেন্টার'],
    places: ['পদ্মা পাম্প, মিরপুর', 'যমুনা ফিলিং স্টেশন'],
  }
}
