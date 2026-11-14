import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import { animate, motion, useMotionValue } from 'motion/react'
import { ChevronDown, ChevronRight, Fuel, Gauge as GaugeIcon, Wrench } from 'lucide-react'
import { useStore, useUI } from '../store'
import { brandOf, itemOf, modelOf } from '../data/catalog'
import { describeWear, type Wear } from '../lib/wear'
import { useFmt } from '../lib/bn'
import { haptic } from '../lib/haptics'
import { CountUp, useMotionOff } from './Gauge'
import { spring } from '../lib/motion'

type Health = 'ok' | 'soon' | 'overdue'
const EMPTY: NonNullable<ReturnType<typeof useStore.getState>['garageBikes']> = []

/**
 * Showroom bike card: the bike photo is the hero, the odometer is the main number,
 * and mileage plus the next job share one row. Swipe the photo to switch bikes.
 */
export function BikeShowroom({ health, healthLabel, healthIcon: HealthIcon, estimate, econ, fuelNote, nextJob }: {
  health: Health
  healthLabel: string
  healthIcon: typeof Wrench
  estimate: number
  econ: number | null
  fuelNote: string
  nextJob: Wear | null
}) {
  const ride = useStore((s) => s.ride)!
  const garageBikes = useStore((s) => s.garageBikes) ?? EMPTY
  const switchBike = useStore((s) => s.switchBike)
  const open = useUI((s) => s.open)
  const setTab = useUI((s) => s.setTab)
  const f = useFmt()
  const brand = brandOf(ride.brandId)
  const model = modelOf(ride.modelId)
  const activeId = ride.id ?? 'primary'
  // Stable order (primary first, then by id), so a bike keeps its place when it becomes active.
  const bikes = [{ ...ride, id: activeId }, ...garageBikes.filter((b) => !b.ride.archived).map((b) => ({ ...b.ride, id: b.ride.id ?? 'primary' }))]
    .sort((a, b) => (a.id === 'primary' ? -1 : b.id === 'primary' ? 1 : a.id < b.id ? -1 : 1))
  const multi = bikes.length > 1
  const index = Math.max(0, bikes.findIndex((b) => b.id === activeId))
  const motionOff = useMotionOff()

  // Each slide is 84% of the stage, so the next and previous bikes peek in from the sides.
  const stageRef = useRef<HTMLDivElement>(null)
  const [stageW, setStageW] = useState(0)
  useLayoutEffect(() => {
    const el = stageRef.current
    if (!el) return
    const ro = new ResizeObserver(() => setStageW(el.clientWidth))
    ro.observe(el)
    setStageW(el.clientWidth)
    return () => ro.disconnect()
  }, [])
  const slideW = multi ? stageW * 0.84 : stageW
  const offsetFor = (i: number) => (stageW - slideW) / 2 - i * slideW
  const x = useMotionValue(0)
  const settling = useRef(false)
  useEffect(() => {
    if (settling.current || !stageW) return
    x.set(offsetFor(index))
  }, [index, stageW, slideW])  // eslint-disable-line react-hooks/exhaustive-deps

  const goTo = (i: number) => {
    const target = Math.max(0, Math.min(bikes.length - 1, i))
    settling.current = true
    const done = () => {
      settling.current = false
      if (target !== index) { haptic('tick'); switchBike(bikes[target].id) }
    }
    if (motionOff) { x.set(offsetFor(target)); done(); return }
    animate(x, offsetFor(target), { ...spring.sheet, onComplete: done })
  }

  return (
    <section className="showroom" aria-label="আমার বাইক ও অবস্থা">
      <div className="showroom-top">
        <button type="button" className="showroom-name" aria-label={`${ride.name}, বাইক বদলান`} onClick={() => { haptic('tick'); open('bikeSwitcher') }}>
          <b>{ride.name} <ChevronDown size={16} aria-hidden /></b>
          <small>{brand.name} · {f.d(String(ride.year))}{(ride.cc ?? model?.cc) ? ` · ${f.d(String(ride.cc ?? model?.cc))} cc` : ''}</small>
        </button>
        <span className={`hero-health-pill ${health}`}><HealthIcon size={14} strokeWidth={2.2} aria-hidden /><span>{healthLabel}</span></span>
      </div>

      <div className="showroom-stage" ref={stageRef} aria-roledescription="carousel" aria-label="আপনার বাইকগুলো">
        <motion.div className="showroom-track" style={{ x }} drag={multi ? 'x' : false}
          dragConstraints={{ left: offsetFor(bikes.length - 1) - 40, right: offsetFor(0) + 40 }} dragElastic={0.18} dragMomentum={false}
          onDragEnd={(_, info) => {
            const moved = info.offset.x + info.velocity.x * 0.2
            goTo(index + (moved < -slideW * 0.22 ? 1 : moved > slideW * 0.22 ? -1 : 0))
          }}>
          {bikes.map((b, i) => {
            const m = modelOf(b.modelId)
            const br = brandOf(b.brandId)
            return (
              <div key={b.id} className={`showroom-slide${i === index ? ' on' : ''}`} style={{ width: slideW || '100%' }} aria-hidden={i !== index}
                onClick={() => { if (i !== index) goTo(i) }}>
                {m?.photo
                  ? <img src={m.photo} alt={i === index ? `${br.name} ${b.name}` : ''} draggable={false} />
                  : <div className="hero-bike-fallback"><GaugeIcon size={40} aria-hidden /><span>{br.name} {b.name}</span></div>}
              </div>
            )
          })}
        </motion.div>
      </div>

      {multi && (
        <div className="showroom-dots" role="tablist" aria-label="বাইক বদলান">
          {bikes.map((b) => (
            <button key={b.id} type="button" role="tab" aria-selected={b.id === activeId} aria-label={b.name} className={b.id === activeId ? 'on' : ''}
              onClick={() => goTo(bikes.findIndex((x) => x.id === b.id))} />
          ))}
        </div>
      )}

      <button type="button" className="showroom-odo" onClick={() => { haptic('tick'); open('odometer') }}>
        <span className="showroom-odo-val"><CountUp value={ride.odometer} format={(n) => f.n(n)} /> <small>{f.digits === 'bn' ? 'কিমি' : 'km'}</small></span>
        <span className="showroom-odo-sub">আজ আনুমানিক {f.n(estimate)} · আপডেট <ChevronRight size={14} aria-hidden /></span>
      </button>

      <div className="showroom-stats">
        <button type="button" onClick={() => { haptic('tick'); setTab('history') }}>
          <Fuel size={18} aria-hidden />
          <span><b>{econ === null ? '--' : f.d(econ.toFixed(1))} {f.digits === 'bn' ? 'কিমি/লি' : 'km/L'}</b><small>{fuelNote}</small></span>
        </button>
        <button type="button" onClick={() => { haptic('tick'); if (nextJob) open('gaugeDetail', { id: nextJob.rule.id }); else open('service') }}>
          <Wrench size={18} aria-hidden />
          {nextJob
            ? <span><b>{itemOf(nextJob.rule.item)?.short ?? nextJob.rule.name}</b><small className={nextJob.status}>{describeWear(nextJob, f)}</small></span>
            : <span><b>পরের কাজ</b><small>এখন কিছু বাকি নেই</small></span>}
        </button>
      </div>
    </section>
  )
}
