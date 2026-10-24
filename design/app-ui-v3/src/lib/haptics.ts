import { useStore } from '../store'

export type HapticKind = 'tick' | 'success' | 'warning' | 'error'

const patterns: Record<HapticKind, number | number[]> = {
  tick: 8,
  success: [12, 40, 18],
  warning: [20, 60, 20],
  error: [30, 50, 30, 50, 30],
}

/** Real vibration on phones that support it. The dev panel also logs every event so it can be reviewed on desktop. */
export function haptic(kind: HapticKind) {
  if (!useStore.getState().settings.haptics) return
  try { navigator.vibrate?.(patterns[kind]) } catch { /* unsupported */ }
  window.dispatchEvent(new CustomEvent('mv-haptic', { detail: kind }))
}
