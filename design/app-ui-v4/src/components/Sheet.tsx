import { useEffect, useRef, type ReactNode } from 'react'
import { motion, useDragControls } from 'motion/react'
import { X } from 'lucide-react'
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
  closeLabel?: string
  /** Compact icon actions shown in the header, left of the close button. */
  actions?: ReactNode
}

/**
 * Bottom sheet with Sky geometry (20 dp corners, 40 x 4 handle, 60 dp toolbar gap).
 * v4 enhancement: supports downward drag-to-dismiss from the sheet body whenever scrolled to top, plus sticky footer CTAs.
 */
export function Sheet({ title, children, footer, detent = 'content', onClose, closeLabel = 'বন্ধ করুন', actions }: SheetProps) {
  const close = onClose ?? useUI.getState().close
  const controls = useDragControls()
  const sheetRef = useRef<HTMLElement>(null)
  const bodyRef = useRef<HTMLDivElement>(null)
  const startY = useRef<number | null>(null)

  useEffect(() => {
    const prev = document.activeElement as HTMLElement | null
    sheetRef.current?.focus({ preventScroll: true })
    return () => {
      if (prev && typeof prev.focus === 'function' && prev.isConnected) prev.focus({ preventScroll: true })
    }
  }, [])

  return (
    <>
      <motion.div className="scrim" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
        transition={{ duration: dur.sheetIn, ease: ease.sky }} onClick={close} />
      <motion.section
        ref={sheetRef}
        tabIndex={-1}
        className={`sheet sheet-${detent}`}
        role="dialog" aria-modal="true" aria-label={title}
        initial={{ y: '100%' }} animate={{ y: 0, transition: spring.sheet }} exit={{ y: '100%', transition: { duration: dur.sheetOut, ease: ease.sky } }}
        drag="y" dragControls={controls} dragListener={false}
        dragConstraints={{ top: 0, bottom: 0 }} dragElastic={{ top: 0.02, bottom: 0.7 }}
        onDragEnd={(_, info) => { if (info.offset.y > 110 || info.velocity.y > 650) { haptic('tick'); close() } }}
      >
        <div className="sheet-grab" onPointerDown={(e) => controls.start(e)}>
          <span className="handle" />
          <header className="sheet-head">
            <h2>{title}</h2>
            <span className="sheet-actions">{actions}<IconButton icon={X} label={closeLabel} onClick={close} /></span>
          </header>
        </div>
        <div
          ref={bodyRef}
          className="sheet-body"
          onPointerDown={(e) => {
            if ((bodyRef.current?.scrollTop ?? 0) <= 0) startY.current = e.clientY
            else startY.current = null
          }}
          onPointerMove={(e) => {
            if (startY.current !== null && e.clientY - startY.current > 10 && (bodyRef.current?.scrollTop ?? 0) <= 0) {
              startY.current = null
              controls.start(e)
            }
          }}
          onPointerUp={() => { startY.current = null }}
          onPointerCancel={() => { startY.current = null }}
        >
          {children}
        </div>
        {footer && <footer className="sheet-foot">{footer}</footer>}
      </motion.section>
    </>
  )
}
