import { motion } from 'motion/react'
import type { ReactNode } from 'react'
import type { LucideIcon } from 'lucide-react'
import { spring } from '../lib/motion'
import { haptic } from '../lib/haptics'

interface ButtonProps {
  children: ReactNode
  onClick?: () => void
  kind?: 'primary' | 'secondary' | 'ghost' | 'danger'
  icon?: LucideIcon
  full?: boolean
  disabled?: boolean
  size?: 'md' | 'sm'
  label?: string
}

export function Button({ children, onClick, kind = 'primary', icon: Icon, full, disabled, size = 'md', label }: ButtonProps) {
  return (
    <motion.button
      type="button"
      className={`btn btn-${kind} btn-${size}${full ? ' full' : ''}`}
      disabled={disabled}
      aria-label={label}
      whileTap={disabled ? undefined : { scale: 0.97 }}
      transition={spring.press}
      onClick={() => { if (!disabled) { haptic('tick'); onClick?.() } }}
    >
      {Icon && <Icon size={size === 'sm' ? 16 : 20} strokeWidth={2} aria-hidden />}
      <span>{children}</span>
    </motion.button>
  )
}

export function IconButton({ icon: Icon, label, onClick, tone }: { icon: LucideIcon; label: string; onClick?: () => void; tone?: 'plain' | 'tint' }) {
  return (
    <motion.button type="button" className={`icon-btn${tone === 'tint' ? ' tint' : ''}`} aria-label={label} whileTap={{ scale: 0.92 }} transition={spring.press}
      onClick={() => { haptic('tick'); onClick?.() }}>
      <Icon size={22} strokeWidth={2} aria-hidden />
    </motion.button>
  )
}

export function Chip({ active, onClick, icon: Icon, children, hint }: { active?: boolean; onClick?: () => void; icon?: LucideIcon; children: ReactNode; hint?: string }) {
  return (
    <motion.button type="button" className={`chip${active ? ' on' : ''}`} aria-pressed={active} whileTap={{ scale: 0.95 }} transition={spring.press}
      onClick={() => { haptic('tick'); onClick?.() }}>
      {Icon && <Icon size={16} strokeWidth={2} aria-hidden />}
      <span>{children}</span>
      {hint && <small>{hint}</small>}
    </motion.button>
  )
}

export function Segmented<T extends string | number>({ value, options, onChange, label }: { value: T; options: { value: T; label: string }[]; onChange: (v: T) => void; label: string }) {
  return (
    <div className="seg" role="radiogroup" aria-label={label}>
      {options.map((o) => (
        <button key={String(o.value)} type="button" role="radio" aria-checked={o.value === value} className={o.value === value ? 'on' : ''}
          onClick={() => { haptic('tick'); onChange(o.value) }}>
          {o.value === value && <motion.span layoutId={`seg-${label}`} className="seg-pill" transition={spring.marker} />}
          <span>{o.label}</span>
        </button>
      ))}
    </div>
  )
}

export function Switch({ checked, onChange, label, sub }: { checked: boolean; onChange: (v: boolean) => void; label: string; sub?: string }) {
  return (
    <button type="button" role="switch" aria-checked={checked} className="switch-row" onClick={() => { haptic('tick'); onChange(!checked) }}>
      <span className="switch-text"><b>{label}</b>{sub && <small>{sub}</small>}</span>
      <span className={`switch${checked ? ' on' : ''}`}><motion.i layout transition={spring.press} /></span>
    </button>
  )
}

export function SectionTitle({ children, action }: { children: ReactNode; action?: ReactNode }) {
  return <div className="section-title"><h2>{children}</h2>{action}</div>
}

export function Field({ label, children, error, hint }: { label: string; children: ReactNode; error?: string | null; hint?: string }) {
  return (
    <label className="field">
      <span className="field-label">{label}</span>
      {children}
      {error ? <span className="field-msg err" role="alert">{error}</span> : hint ? <span className="field-msg">{hint}</span> : null}
    </label>
  )
}
