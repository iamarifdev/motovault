import { motion, useDragControls } from 'motion/react'
import { X } from 'lucide-react'
import type { ReactNode } from 'react'
import { useUI } from '../store'
import { dur, ease, spring } from '../lib/motion'
import { haptic } from '../lib/haptics'
import { IconButton } from './ui'

interface SheetProps {
  title: string
  children: ReactNode
  footer?: ReactNode
  /** 'tall' fills 92% like Sky's selection sheet. 'content' hugs its content. */
  detent?: 'content' | 'tall'
  onClose?: () => void
}

/**
 * Bottom sheet with Sky geometry (20 dp corners, 40 x 4 handle, 60 dp toolbar gap).
 * New vs the Kotlin app: the handle really drags, a fast flick dismisses, and the body scrolls under a fixed header.
 */
export function Sheet({ title, children, footer, detent = 'content', onClose }: SheetProps) {
  const close = onClose ?? useUI.getState().close
  const controls = useDragControls()
  return (
    <>
      <motion.div className="scrim" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
        transition={{ duration: dur.sheetIn, ease: ease.sky }} onClick={close} />
      <motion.section
        className={`sheet sheet-${detent}`}
        role="dialog" aria-modal="true" aria-label={title}
        initial={{ y: '100%' }} animate={{ y: 0, transition: spring.sheet }} exit={{ y: '100%', transition: { duration: dur.sheetOut, ease: ease.sky } }}
        drag="y" dragControls={controls} dragListener={false}
        dragConstraints={{ top: 0, bottom: 0 }} dragElastic={{ top: 0.02, bottom: 0.7 }}
        onDragEnd={(_, info) => { if (info.offset.y > 120 || info.velocity.y > 700) { haptic('tick'); close() } }}
      >
        <div className="sheet-grab" onPointerDown={(e) => controls.start(e)}>
          <span className="handle" />
          <header className="sheet-head">
            <h2>{title}</h2>
            <IconButton icon={X} label="বন্ধ করুন" onClick={close} />
          </header>
        </div>
        <div className="sheet-body">{children}</div>
        {footer && <footer className="sheet-foot">{footer}</footer>}
      </motion.section>
    </>
  )
}
