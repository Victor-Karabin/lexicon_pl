import AVFoundation

/// Reading a Polish word out loud.
///
/// The Android app hands this to a platform service behind an interface because its
/// ViewModels live in shared-ish code; here the view models are Swift, so the
/// synthesizer is used directly.
final class Speech {
    static let shared = Speech()

    private let synthesizer = AVSpeechSynthesizer()

    private init() {
        // Spoken words have to be audible even with the ringer switch off, which is
        // where a learner practising quietly usually has it.
        try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .spokenAudio, options: [.duckOthers])
    }

    func speak(_ text: String, language: String = "pl-PL") {
        guard !text.isEmpty else { return }
        try? AVAudioSession.sharedInstance().setActive(true)
        synthesizer.stopSpeaking(at: .immediate)
        let utterance = AVSpeechUtterance(string: text)
        utterance.voice = AVSpeechSynthesisVoice(language: language)
        utterance.rate = AVSpeechUtteranceDefaultSpeechRate * 0.9
        synthesizer.speak(utterance)
    }
}
