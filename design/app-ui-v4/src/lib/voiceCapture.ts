type RecognitionInstance = {
  lang: string
  interimResults: boolean
  onresult: (e: { results: ArrayLike<ArrayLike<{ transcript: string }>> }) => void
  onend: () => void
  onerror: () => void
  start: () => void
  stop: () => void
}

type RecognitionCtor = new () => RecognitionInstance

function getSpeechCtor(): RecognitionCtor | undefined {
  const win = window as unknown as {
    SpeechRecognition?: RecognitionCtor
    webkitSpeechRecognition?: RecognitionCtor
  }
  return win.SpeechRecognition ?? win.webkitSpeechRecognition
}

export interface VoiceHandle {
  stop: () => void
}

export type VoiceFailure = 'unavailable' | 'failed'

/**
 * Starts Bangla speech recognition. When the device has no recognizer it reports
 * 'unavailable' and never makes up a transcript; the rider types instead.
 */
export function startBanglaVoice({
  onResult,
  onEnd,
  onError,
}: {
  onResult: (transcript: string) => void
  onEnd: () => void
  onError?: (reason: VoiceFailure) => void
}): VoiceHandle {
  const SR = getSpeechCtor()
  if (!SR) {
    const timer = window.setTimeout(() => {
      onError?.('unavailable')
      onEnd()
    }, 0)
    return { stop: () => { window.clearTimeout(timer); onEnd() } }
  }

  let finished = false
  const r = new SR()
  r.lang = 'bn-BD'
  r.interimResults = false
  r.onresult = (ev) => {
    if (finished) return
    const transcript = ev.results?.[0]?.[0]?.transcript ?? ''
    if (transcript) onResult(transcript)
  }
  r.onend = () => {
    if (finished) return
    finished = true
    onEnd()
  }
  r.onerror = () => {
    if (finished) return
    finished = true
    onError?.('failed')
    onEnd()
  }
  r.start()
  return {
    stop: () => {
      if (finished) return
      finished = true
      r.onresult = () => {}
      r.stop()
      onEnd()
    },
  }
}

export const VOICE_UNAVAILABLE = 'এই ফোনে মুখে বলার সুবিধা নেই। সংখ্যাটি টাইপ করুন।'
