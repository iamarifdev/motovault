import { useState } from 'react'
import { Sheet } from '../components/Sheet'
import { Button, Chip } from '../components/ui'
import { brandOf } from '../data/catalog'
import { useFmt } from '../lib/bn'
import { haptic } from '../lib/haptics'
import { useStore, useUI } from '../store'

export function BikeSwitcherSheet() {
  const ride = useStore((s) => s.ride)
  const garageBikes = useStore((s) => s.garageBikes ?? [])
  const switchBike = useStore((s) => s.switchBike)
  const toggleArchiveBike = useStore((s) => s.toggleArchiveBike)
  const close = useUI((u) => u.close)
  const setAddingBike = useUI((u) => u.setAddingBike)
  const showToast = useUI((u) => u.showToast)
  const f = useFmt()
  const [error, setError] = useState<string | null>(null)

  if (!ride) return null
  const activeId = ride.id ?? 'primary'
  const all = [{ ride: { ...ride, id: activeId } }, ...garageBikes.map((b) => ({ ride: { ...b.ride, id: b.ride.id ?? 'primary' } }))]
  const activeBikes = all.filter((b) => !b.ride.archived)
  const archivedBikes = all.filter((b) => b.ride.archived)

  return (
    <Sheet title="আপনার গ্যারেজ" closeLabel="বন্ধ করুন">
      <div className="card-stack">
        <small className="cap">সক্রিয় বাইক</small>
        {activeBikes.map(({ ride: b }) => {
          const isCurrent = b.id === activeId
          return (
            <div key={b.id} className="setting">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8 }}>
                <div>
                  <b>{b.name}</b>
                  <small className="cap" style={{ display: 'block' }}>{brandOf(b.brandId).name} · {f.d(String(b.year))} · {f.km(b.odometer)}</small>
                </div>
                {isCurrent && <Chip active onClick={() => {}}>সক্রিয়</Chip>}
              </div>
              <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
                {!isCurrent && (
                  <Button size="sm" kind="secondary" onClick={() => { haptic('tick'); switchBike(b.id); showToast(`${b.name} বেছে নেওয়া হয়েছে`); close() }}>
                    বেছে নিন
                  </Button>
                )}
                <Button size="sm" kind="ghost" onClick={() => {
                  const ok = toggleArchiveBike(b.id)
                  if (!ok) {
                    haptic('error')
                    setError('গ্যারেজে অন্তত একটি সক্রিয় বাইক থাকতে হবে।')
                  } else {
                    setError(null)
                    haptic('tick')
                    showToast(`${b.name} আর্কাইভ করা হয়েছে`)
                  }
                }}>
                  আর্কাইভ করুন
                </Button>
              </div>
            </div>
          )
        })}
      </div>

      {archivedBikes.length > 0 && (
        <div className="card-stack" style={{ marginTop: 12 }}>
          <small className="cap">আর্কাইভ করা বাইক</small>
          {archivedBikes.map(({ ride: b }) => (
            <div key={b.id} className="setting">
              <div>
                <b>{b.name}</b>
                <small className="cap" style={{ display: 'block' }}>{brandOf(b.brandId).name} · {f.d(String(b.year))} · {f.km(b.odometer)}</small>
              </div>
              <div style={{ marginTop: 8 }}>
                <Button size="sm" kind="secondary" onClick={() => { haptic('tick'); toggleArchiveBike(b.id); setError(null); showToast(`${b.name} ফিরিয়ে আনা হয়েছে`) }}>
                  ফিরিয়ে আনুন
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}

      {error && <p className="cap" style={{ color: 'var(--bad)', marginTop: 8 }}>{error}</p>}

      <div style={{ marginTop: 14 }}>
        <Button full onClick={() => { haptic('tick'); setAddingBike(true) }}>আরেকটি বাইক যোগ করুন</Button>
      </div>
    </Sheet>
  )
}
