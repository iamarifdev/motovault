import { useEffect } from 'react'
import { AnimatePresence, motion } from 'motion/react'
import { Undo2 } from 'lucide-react'
import { useUI } from '../store'
import { ease, spring } from '../lib/motion'
import { haptic } from '../lib/haptics'

/**
 * Replaces the blocking "saving" dialog and the Android-style snackbar.
 * The record is already saved when this appears; Undo reverses it. It clears itself after 5 s or on swipe.
 */
export function ToastLayer() {
  const toast = useUI((s) => s.toast)
  const hide = useUI((s) => s.hideToast)
  useEffect(() => {
    if (!toast) return
    const t = setTimeout(hide, 5000)
    return () => clearTimeout(t)
  }, [toast, hide])
  return (
    <div className="toast-layer" aria-live="polite">
      <AnimatePresence>
        {toast && (
          <motion.div key={toast.id} className={`toast${toast.tone === 'warn' ? ' warn' : ''}`} role="status"
            initial={{ opacity: 0, y: 24, scale: 0.96 }} animate={{ opacity: 1, y: 0, scale: 1, transition: spring.sheet }}
            exit={{ opacity: 0, y: 12, transition: { duration: 0.18, ease: ease.sky } }}
            drag="x" dragConstraints={{ left: 0, right: 0 }} dragElastic={0.6}
            onDragEnd={(_, i) => { if (Math.abs(i.offset.x) > 80) hide() }}>
            <span className="toast-text">{toast.text}</span>
            {toast.undo && (
              <button type="button" className="toast-undo" onClick={() => { haptic('warning'); toast.undo?.(); hide() }}>
                <Undo2 size={16} aria-hidden /> আনডু
              </button>
            )}
            <motion.span className="toast-bar" initial={{ scaleX: 1 }} animate={{ scaleX: 0 }} transition={{ duration: 5, ease: 'linear' }} />
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}
