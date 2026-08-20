import SwiftUI
import Shared

struct DictationView: View {
    let vocabularyIds: [Int64]
    let fromAudio: Bool

    @Environment(\.dismiss) private var dismiss
    @State private var steps: [DictationStepResponse] = []
    @State private var sessionId = ""
    @State private var index = 0
    @State private var typed = ""
    @State private var state: AnswerState = .unanswered
    @State private var tipUsed = false
    @State private var tally = SessionTally()
    @State private var finished = false

    var body: some View {
        Group {
            if finished {
                SessionResultView(tally: tally) { dismiss() }
            } else if let step = steps[safe: index] {
                TrainingScaffold(step: index, total: steps.count, state: state) {
                    VStack(spacing: Spacing.medium) {
                        Button {
                            Speech.shared.speak(step.expectedText)
                        } label: {
                            Label("Listen again", systemImage: "speaker.wave.2.fill").font(.title3)
                        }
                        Text(step.translationText).font(.title2.weight(.semibold))
                        if tipUsed {
                            Text("Hint: \(String(step.expectedText.prefix(1)))…")
                                .font(.callout)
                                .foregroundStyle(.secondary)
                        }
                        TextField("Type what you heard", text: $typed)
                            .textFieldStyle(.roundedBorder)
                            .autocorrectionDisabled()
                            .textInputAutocapitalization(.never)
                            .disabled(state.isAnswered)
                    }
                } actions: {
                    HStack(spacing: Spacing.small) {
                        if !state.isAnswered {
                            Button("Tip") { tipUsed = true }
                            Button("Skip") { Task { await submit(skipped: true) } }
                            Spacer()
                            AsyncButton { await submit(skipped: false) } label: { Text("Check") }
                                .buttonStyle(.borderedProminent)
                                .disabled(typed.isEmpty)
                        } else {
                            Spacer()
                            Button("Next") { advance() }.buttonStyle(.borderedProminent)
                        }
                    }
                }
                .onChange(of: index) { speakCurrent() }
                .onAppear { speakCurrent() }
            } else {
                ProgressView()
            }
        }
        .task { await start() }
    }

    private func start() async {
        let response = try? await deps.startDictation.invoke(
            request: StartDictationSessionRequest(stepCount: nil, vocabularyIds: vocabularyIds.map { KotlinLong(value: $0) })
        )
        guard let response else { return }
        sessionId = response.sessionId
        steps = response.steps
    }

    private func speakCurrent() {
        guard fromAudio, let step = steps[safe: index] else { return }
        Speech.shared.speak(step.expectedText)
    }

    private func submit(skipped: Bool) async {
        guard let step = steps[safe: index] else { return }
        let response = try? await deps.submitDictation.invoke(
            request: SubmitDictationAnswerRequest(
                sessionId: sessionId,
                stepIndex: Int32(index),
                vocabularyItemId: step.vocabularyItemId,
                expectedText: step.expectedText,
                submittedText: typed,
                tipUsed: tipUsed,
                skipped: skipped
            )
        )
        guard let response else { return }
        switch response.outcome {
        case .correct:
            state = .correct
            tally.correct += 1
        case .skipped:
            state = .skipped(expected: response.expectedText)
            tally.skipped += 1
        default:
            state = .incorrect(expected: response.expectedText)
            tally.incorrect += 1
        }
        if tipUsed { tally.tipsUsed += 1 }
    }

    private func advance() {
        if index + 1 >= steps.count {
            finished = true
            return
        }
        index += 1
        typed = ""
        tipUsed = false
        state = .unanswered
    }
}

extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}
