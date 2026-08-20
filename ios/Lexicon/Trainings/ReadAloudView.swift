import SwiftUI
import Shared

/// Read Aloud: the same speaking check as Pronunciation Check, but over whole sentences
/// written for the words in the study set rather than the words on their own.
struct ReadAloudView: View {
    @Environment(\.dismiss) private var dismiss
    @StateObject private var recognizer = SpeechRecognizer()

    @State private var sessionId = ""
    @State private var steps: [PronunciationStepResponse] = []
    @State private var index = 0
    @State private var state: AnswerState = .unanswered
    @State private var tally = SessionTally()
    @State private var unavailable: String?
    @State private var finished = false

    var body: some View {
        Group {
            if finished {
                SessionResultView(tally: tally) { dismiss() }
            } else if let message = unavailable {
                TrainingUnavailableView(title: "Nothing to read yet", message: message)
            } else if let step = steps[safe: index] {
                TrainingScaffold(step: index, total: steps.count, state: state) {
                    VStack(spacing: Spacing.medium) {
                        Text(step.expectedText)
                            .font(.title2)
                            .multilineTextAlignment(.center)
                        if !step.clueText.isEmpty {
                            Text(step.clueText)
                                .font(.callout)
                                .foregroundStyle(.secondary)
                                .multilineTextAlignment(.center)
                        }
                        Button { Speech.shared.speak(step.expectedText) } label: {
                            Label("Listen", systemImage: "speaker.wave.2")
                        }
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
                ProgressView("Writing your sentences…")
            }
        }
        .task { await start() }
    }

    private func start() async {
        guard steps.isEmpty else { return }
        let result = try? await deps.startPronunciationSentences.invoke()
        switch result {
        case let ready as PronunciationSentencesResultReady:
            sessionId = ready.session.sessionId
            steps = ready.session.steps
            if steps.isEmpty { unavailable = "No sentence came back for the words in your study set." }
        case is PronunciationSentencesResultOffline:
            unavailable = "The sentences are written on demand and the network is not answering."
        case let refused as PronunciationSentencesResultRefused:
            unavailable = refused.reason
        default:
            unavailable = "Add a few words to your study set and this will have something to read."
        }
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

#Preview("Read Aloud") {
    NavigationStack { ReadAloudView() }
}
