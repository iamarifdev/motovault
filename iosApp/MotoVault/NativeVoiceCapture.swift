import AVFoundation
import Speech
import ComposeApp

final class NativeVoiceCapture: NSObject, IosVoiceCaptureBridge {
    static func capabilityReport() -> String {
        let locales = SFSpeechRecognizer.supportedLocales().map { $0.identifier.replacingOccurrences(of: "_", with: "-").lowercased() }
        let native = NativeVoiceCapture()
        let values: [String: Any] = ["requestedLocale": "bn-BD", "localeListed": locales.contains("bn-bd"),
            "banglaLocales": locales.filter { $0.hasPrefix("bn-") }.sorted(),
            "localAvailable": native.supportsLocal(), "providerAvailable": native.available(),
            "authorizationStatus": SFSpeechRecognizer.authorizationStatus().rawValue]
        guard let data = try? JSONSerialization.data(withJSONObject: values, options: [.prettyPrinted, .sortedKeys]),
              let result = String(data: data, encoding: .utf8) else { return "{}" }
        return result
    }
    private let engine = AVAudioEngine()
    private var request: SFSpeechAudioBufferRecognitionRequest?
    private var task: SFSpeechRecognitionTask?
    private var listener: VoiceCaptureListener?
    private var generation = 0
    private var tapped = false
    private var timeout: DispatchWorkItem?

    private func recognizer() -> SFSpeechRecognizer? {
        let supported = SFSpeechRecognizer.supportedLocales().contains {
            $0.identifier.replacingOccurrences(of: "_", with: "-").lowercased() == "bn-bd"
        }
        return supported ? SFSpeechRecognizer(locale: Locale(identifier: "bn-BD")) : nil
    }
    func supportsLocal() -> Bool { recognizer()?.supportsOnDeviceRecognition == true }
    func available() -> Bool { recognizer()?.isAvailable == true }

    func start(localOnly: Bool, listener: VoiceCaptureListener) {
        cancel()
        self.listener = listener
        guard let recognizer = recognizer(), recognizer.isAvailable else { fail(.unavailable); return }
        guard !localOnly || recognizer.supportsOnDeviceRecognition else { fail(.localUnavailable); return }
        let token = generation
        listener.state(value: .requesting)
        SFSpeechRecognizer.requestAuthorization { [weak self] authorization in
            DispatchQueue.main.async {
                guard let self, token == self.generation else { return }
                guard authorization == .authorized else { self.fail(.denied); return }
                AVAudioSession.sharedInstance().requestRecordPermission { [weak self] allowed in
                    DispatchQueue.main.async {
                        guard let self, token == self.generation else { return }
                        guard allowed else { self.fail(.denied); return }
                        self.listen(recognizer, localOnly: localOnly, token: token)
                    }
                }
            }
        }
    }

    private func listen(_ recognizer: SFSpeechRecognizer, localOnly: Bool, token: Int) {
        do {
            let audio = AVAudioSession.sharedInstance()
            try audio.setCategory(.record, mode: .measurement)
            try audio.setActive(true)
            let request = SFSpeechAudioBufferRecognitionRequest()
            request.shouldReportPartialResults = false
            request.requiresOnDeviceRecognition = localOnly
            self.request = request
            let input = engine.inputNode
            input.installTap(onBus: 0, bufferSize: 1024, format: input.outputFormat(forBus: 0)) { buffer, _ in
                request.append(buffer)
            }
            tapped = true
            task = recognizer.recognitionTask(with: request) { [weak self] result, error in
                DispatchQueue.main.async {
                    guard let self, self.generation == token else { return }
                    if let result, result.isFinal {
                        let text = result.bestTranscription.formattedString
                        let receiver = self.listener
                        self.cancel()
                        if text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || text.utf16.count > 10_000 {
                            receiver?.state(value: .noSpeech)
                        } else { receiver?.transcript(text: text); receiver?.state(value: .idle) }
                    } else if error != nil { self.fail(.failed) }
                }
            }
            engine.prepare()
            try engine.start()
            listener?.state(value: .listening)
            let timeout = DispatchWorkItem { [weak self] in if self?.generation == token { self?.fail(.failed) } }
            self.timeout = timeout
            DispatchQueue.main.asyncAfter(deadline: .now() + 60, execute: timeout)
        } catch { fail(.failed) }
    }

    func stop() {
        guard request != nil else { return }
        stopAudio()
        request?.endAudio()
        listener?.state(value: .processing)
    }
    func cancel() {
        generation += 1
        timeout?.cancel(); timeout = nil
        stopAudio()
        request?.endAudio(); request = nil
        task?.cancel(); task = nil
        listener = nil
    }
    private func stopAudio() {
        engine.stop()
        if tapped { engine.inputNode.removeTap(onBus: 0); tapped = false }
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }
    private func fail(_ state: VoiceCaptureState) {
        let receiver = listener
        cancel()
        receiver?.state(value: state)
    }
}
