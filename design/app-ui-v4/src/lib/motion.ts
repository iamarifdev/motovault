import { skyMotion } from '../tokens.generated'

/**
 * Motion tokens. Page, sheet and expansion timings come straight from Sky (tokens.generated.ts).
 * The springs are MotoVault additions for gestures, where a fixed curve feels dead.
 */
export const ease = {
  sky: skyMotion.page.curve as unknown as [number, number, number, number],
  expand: skyMotion.expansion.curve as unknown as [number, number, number, number],
}

export const dur = {
  instant: 0.1,
  quick: 0.18,
  expand: skyMotion.expansion.enter,
  page: skyMotion.page.enter,
  sheetIn: skyMotion.sheet.enter,
  sheetOut: skyMotion.sheet.exit,
  sweep: 0.6,
}

export const spring = {
  sheet: { type: 'spring', stiffness: 420, damping: 40, mass: 0.9 } as const,
  press: { type: 'spring', stiffness: 600, damping: 32 } as const,
  list: { type: 'spring', stiffness: 500, damping: 38 } as const,
  marker: { type: 'spring', stiffness: 520, damping: 36 } as const,
}

export const stagger = (i: number, step = 0.05) => i * step

export const fadeUp = {
  initial: { opacity: 0, y: 10 },
  animate: { opacity: 1, y: 0 },
  exit: { opacity: 0, y: 6 },
}
