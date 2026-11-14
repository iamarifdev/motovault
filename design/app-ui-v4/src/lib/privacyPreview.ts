import { create } from 'zustand'
import { useUI } from '../store'
import type { VaultDoc } from '../types'

/** Ephemeral demonstration permission. Never persist a successful unlock or handover. */
export const usePrivacyPreview = create<{
  mode: 'locked' | 'owner' | 'inspection'
  selected: VaultDoc | null
  generation: number
  lock: () => void
  unlock: () => void
  inspect: (doc: VaultDoc) => void
}>((set) => ({
  mode: 'locked', selected: null, generation: 0,
  lock: () => { useUI.getState().close(); useUI.getState().hideToast(); set((s) => ({ mode: 'locked', selected: null, generation: s.generation + 1 })) },
  unlock: () => set({ mode: 'owner', selected: null }),
  inspect: (doc) => { useUI.getState().close(); useUI.getState().hideToast(); set({ mode: 'inspection', selected: { ...doc } }) },
}))
