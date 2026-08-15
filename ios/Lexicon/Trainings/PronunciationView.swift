import SwiftUI
import Speech
import Shared

/// Say the word out loud and be heard.
struct PronunciationView: View {
    let vocabularyIds: [Int64]

    @Environment(\.dismiss) private var dismiss
    @StateObject private var recognizer = SpeechRecognizer()
    @State private var steps: [PronunciationStepResponse] = []
    @State private var sessionId = ""
    @State private var index = 0
    @State private var state: AnswerState = .unanswered
    @State private var tally = SessionTally()
    @State private var finished = false

    var body: some View {
        Group {
            if finished {
                SessionResultView(tally: tally) { dismiss() }
            } else if let step = steps[safe: index] {
                TrainingScaffold(step: index, total: steps.count, state: state) {
                    VStack(spacing: Spacing.small) {
                        Text(step.expectedText).font(.largeTitle.bold())
                        if !step.transcription.isEmpty {
                            Text("IPA: /\(step.transcription)/").font(.callout).foregroundStyle(.secondary)
                        }
                        Text(step.clueText).font(.title3).foregroundStyle(.secondary)
                        Button { Speech.shared.speak(step.expectedText) } label: {
                            Label("Listen", systemImage: "speaker.wave.2")
                        }
                        .padding(.top, Spacing.small)

                        if !recognizer.heard.isEmpty {
                            Text("Heard: \(recognizer.heard)").font(.callout)
                        }
                        if let error = recognizer.error {
                            Text(error).font(.callout).foregroundStyle(Palette.failure).multilineTextAlignment(.center)
                        }
                    }
                } actions: {
                    HStack(spacing: Spacing.small) {
                        if !state.isAnswered {
                            Button("Skip") { Task { await submit(skipped: true) } }
                            Spacer()
                            Button(recognizer.isRecording ? "Listening…" : "Record") {
                                Task {
                                    if recognizer.isRecording {
                                        recognizer.stop()
                                        await submit(skipped: false)
                                    } else {
                                        await recognizer.start()
                                    }
                                }
                            }
                            .buttonStyle(.borderedProminent)
                        } else {
                            Spacer()
                            Button("Next") { advance() }.buttonStyle(.borderedProminent)
                        }
                    }
                }
            } else {
                ProgressView()
            }
        }
        .task { await start() }
    }

    private func start() async {
        let response = try? await deps.startPronunciation.invoke(
            request: StartPronunciationSessionRequest(stepCount: nil, vocabularyIds: vocabularyIds.map { KotlinLong(value: $0) })
        )
        guard let response else { return }
        sessionId = response.sessionId
        steps = response.steps
    }

    private func submit(skipped: Bool) async {
        guard let step = steps[safe: index] else { return }
        let response = try? await deps.submitPronunciation.invoke(
            request: SubmitPronunciationResultRequest(
                sessionId: sessionId,
                stepIndex: Int32(index),
                vocabularyItemId: step.vocabularyItemId,
                expectedText: step.expectedText,
                recognizedText: skipped ? "" : recognizer.heard,
                confidence: nil,
                tipUsed: false,
                skipped: skipped
            )
        )
        guard let response else { return }
        switch response.outcome {
        case .correct: state = .correct; tally.correct += 1
        case .skipped: state = .skipped(expected: response.expectedText); tally.skipped += 1
        default: state = .incorrect(expected: response.expectedText); tally.incorrect += 1
        }
    }

    private func advance() {
        if index + 1 >= steps.count {
            finished = true
            return
        }
        index += 1
        state = .unanswered
        recognizer.reset()
    }
}

/// Polish speech, turned into text so the shared use case can judge it.
@MainActor
final class SpeechRecognizer: ObservableObject {
    @Published var heard = ""
    @Published var isRecording = false
    @Published var error: String?

    private let recognizer = SFSpeechRecognizer(locale: Locale(identifier: "pl-PL"))
    private let engine = AVAudioEngine()
    private var request: SFSpeechAudioBufferRecognitionRequest?
    private var task: SFSpeechRecognitionTask?

    func start() async {
        error = nil
        heard = ""

        guard await authorised() else {
            error = "Speech recognition isn't available until it is allowed in Settings."
            return
        }
        guard let recognizer, recognizer.isAvailable else {
            error = "Polish speech recognition isn't available on this device."
            return
        }

        let request = SFSpeechAudioBufferRecognitionRequest()
        request.shouldReportPartialResults = true
        self.request = request

        let input = engine.inputNode
        input.removeTap(onBus: 0)
        input.installTap(onBus: 0, bufferSize: 1024, format: input.outputFormat(forBus: 0)) { buffer, _ in
            request.append(buffer)
        }

        do {
            try AVAudioSession.sharedInstance().setCategory(.record, mode: .measurement, options: .duckOthers)
            try AVAudioSession.sharedInstance().setActive(true, options: .notifyOthersOnDeactivation)
            engine.prepare()
            try engine.start()
            isRecording = true
        } catch {
            self.error = "The microphone could not be started."
            return
        }

        task = recognizer.recognitionTask(with: request) { [weak self] result, _ in
            guard let result else { return }
            Task { @MainActor in self?.heard = result.bestTranscription.formattedString }
        }
    }

    func stop() {
        engine.stop()
        engine.inputNode.removeTap(onBus: 0)
        request?.endAudio()
        task?.finish()
        isRecording = false
    }

    func reset() {
        heard = ""
        error = nil
    }

    private func authorised() async -> Bool {
        await withCheckedContinuation { continuation in
            SFSpeechRecognizer.requestAuthorization { status in
                continuation.resume(returning: status == .authorized)
            }
        }
    }
}
