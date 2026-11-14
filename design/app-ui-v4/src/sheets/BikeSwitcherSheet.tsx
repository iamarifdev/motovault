import { Archive, ArchiveRestore, Bike, Check, Plus } from 'lucide-react'
import { Sheet } from '../components/Sheet'
import { brandOf } from '../data/catalog'
import { useFmt } from '../lib/bn'
import { haptic } from '../lib/haptics'
import { useStore, useUI } from '../store'

const EMPTY_BIKES: NonNullable<ReturnType<typeof useStore.getState>['garageBikes']> = []

/** Garage: tap a bike to ride it. The current bike is marked; archive sits behind a quiet icon with Undo. */
export function BikeSwitcherSheet() {
  const ride = useStore((s) => s.ride)
  const garageBikes = useStore((s) => s.garageBikes) ?? EMPTY_BIKES
  const switchBike = useStore((s) => s.switchBike)
  const toggleArchiveBike = useStore((s) => s.toggleArchiveBike)
  const close = useUI((u) => u.close)
  const setAddingBike = useUI((u) => u.setAddingBike)
  const showToast = useUI((u) => u.showToast)
  const f = useFmt()

  if (!ride) return null
  const activeId = ride.id ?? 'primary'
  const all = [{ ...ride, id: activeId }, ...garageBikes.map((b) => ({ ...b.ride, id: b.ride.id ?? 'primary' }))]
  const activeBikes = all.filter((b) => !b.archived).sort((a, b) => (a.id === activeId ? -1 : b.id === activeId ? 1 : 0))
  const archivedBikes = all.filter((b) => b.archived)
  const canArchive = activeBikes.length > 1

  const archive = (id: string, name: string) => {
    if (!toggleArchiveBike(id)) { haptic('error'); return }
    haptic('tick')
    showToast(`${name} আর্কাইভ করা হয়েছে`, () => toggleArchiveBike(id))
  }

  return (
    <Sheet title="আপনার গ্যারেজ">
      <ul className="garage" aria-label="সক্রিয় বাইক">
        {activeBikes.map((b) => {
          const brand = brandOf(b.brandId)
          const current = b.id === activeId
          return (
            <li key={b.id} className={`garage-row${current ? ' current' : ''}`} style={{ '--bike': brand.light } as React.CSSProperties}>
              <button type="button" className="garage-main" aria-current={current || undefined} disabled={current}
                onClick={() => { haptic('tick'); switchBike(b.id); showToast(`${b.name} বেছে নেওয়া হয়েছে`); close() }}>
                <span className="garage-avatar"><Bike size={22} aria-hidden /></span>
                <span className="garage-text">
                  <b>{b.name}</b>
                  <small>{brand.name} · {f.d(String(b.year))} · {f.km(b.odometer)}</small>
                </span>
                {current
                  ? <span className="garage-badge"><Check size={14} aria-hidden /> এখন চালাচ্ছেন</span>
                  : <span className="garage-switch">বেছে নিন</span>}
              </button>
              {canArchive && (
                <button type="button" className="garage-icon" aria-label={`${b.name} আর্কাইভ করুন`} onClick={() => archive(b.id, b.name)}>
                  <Archive size={18} aria-hidden />
                </button>
              )}
            </li>
          )
        })}
        <li>
          <button type="button" className="garage-add" onClick={() => { haptic('tick'); setAddingBike(true) }}>
            <span className="garage-avatar"><Plus size={22} aria-hidden /></span>
            <span>আরেকটি বাইক যোগ করুন</span>
          </button>
        </li>
      </ul>

      {archivedBikes.length > 0 && (
        <section className="garage-archived" aria-label="আর্কাইভ করা বাইক">
          <h3 className="cap">আর্কাইভ করা</h3>
          <ul className="garage">
            {archivedBikes.map((b) => (
              <li key={b.id} className="garage-row muted">
                <span className="garage-main">
                  <span className="garage-avatar"><Bike size={22} aria-hidden /></span>
                  <span className="garage-text"><b>{b.name}</b><small>{brandOf(b.brandId).name} · {f.km(b.odometer)}</small></span>
                </span>
                <button type="button" className="garage-icon" aria-label={`${b.name} ফিরিয়ে আনুন`}
                  onClick={() => { haptic('tick'); toggleArchiveBike(b.id); showToast(`${b.name} ফিরিয়ে আনা হয়েছে`) }}>
                  <ArchiveRestore size={18} aria-hidden />
                </button>
              </li>
            ))}
          </ul>
        </section>
      )}
    </Sheet>
  )
}
