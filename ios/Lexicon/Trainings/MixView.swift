import SwiftUI
import Shared

/// A little of every training, in one session.
///
/// The shared session decides which step is which; this dispatches on the type and
/// hands each one to the same UI the standalone training uses.
struct MixView: View {
    let vocabularyIds: [Int64]

    @Environment(\.dismiss) private var dismiss
    @State private var steps: [MixStep] = []
    @State private var sessionId = ""
    @State private var index = 0
    @State private var typed = ""
    @State private var state: AnswerState = .unanswered
    @State private var tally = SessionTally()
    @State private var finished = false

    var body: some View {
        Group {
            if finished {
                SessionResultView(tally: tally) { dismiss() }
            } else if let step = steps[safe: index] {
                TrainingScaffold(step: index, total: steps.count, state: state) {
                    content(for: step)
                } actions: {
                    HStack {
                        if !state.isAnswered {
                            Button("Skip") { Task { await submit(step, skipped: true) } }
                            Spacer()
                            AsyncButton { await submit(step, skipped: false) } label: { Text("Check") }
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

    @ViewBuilder
    private func content(for step: MixStep) -> some View {
        VStack(spacing: Spacing.medium) {
            switch step {
            case let dictation as MixStepDictation:
                Button { Speech.shared.speak(dictation.step.expectedText) } label: {
                    Label("Listen again", systemImage: "speaker.wave.2.fill")
                }
                Text(dictation.step.translationText).font(.title3.weight(.semibold))
                answerField
            case let puzzle as MixStepDictationPuzzle:
                Button { Speech.shared.speak(puzzle.step.expectedText) } label: {
                    Label("Listen again", systemImage: "speaker.wave.2.fill")
                }
                Text(puzzle.step.translationText).font(.title3.weight(.semibold))
                answerField
            case let puzzle as MixStepPuzzle:
                Text(puzzle.step.clueText).font(.title3.weight(.semibold))
                answerField
            case let test as MixStepImageTest:
                if let url = test.step.imageUrl, let link = URL(string: url) {
                    AsyncImage(url: link) { $0.resizable().scaledToFit() } placeholder: {
                        Color.secondary.opacity(0.15)
                    }
                    .frame(height: 180)
                }
                Text(test.step.clueText).font(.title3.weight(.semibold))
                answerField
            case let tf as MixStepTrueOrFalse:
                Text(tf.step.word).font(.largeTitle.bold())
                Text(tf.step.displayedTranslation).font(.title3).foregroundStyle(.secondary)
                Text("Type “true” or “false”").font(.caption).foregroundStyle(.secondary)
                answerField
            case let say as MixStepPronunciation:
                Text(say.step.expectedText).font(.largeTitle.bold())
                Text(say.step.clueText).font(.title3).foregroundStyle(.secondary)
                Button { Speech.shared.speak(say.step.expectedText) } label: {
                    Label("Listen", systemImage: "speaker.wave.2")
                }
            default:
                EmptyView()
            }
        }
    }

    private var answerField: some View {
        TextField("Your answer", text: $typed)
            .textFieldStyle(.roundedBorder)
            .autocorrectionDisabled()
            .textInputAutocapitalization(.never)
            .disabled(state.isAnswered)
    }

    private func start() async {
        let response = try? await deps.startMix.invoke(
            request: StartMixSessionRequest(
                stepCount: nil,
                trainingTypes: Set(MixTrainingType.entries),
                vocabularyIds: vocabularyIds.map { KotlinLong(value: $0) }
            )
        )
        guard let response else { return }
        sessionId = response.sessionId
        steps = response.steps
    }

    /// Every kind of step in a Mix ends up in the same place: the training's own
    /// submit use case, so the learning record cannot tell it apart from the
    /// standalone training.
    private func submit(_ step: MixStep, skipped: Bool) async {
        switch step {
        case let dictation as MixStepDictation:
            let response = try? await deps.submitDictation.invoke(
                request: SubmitDictationAnswerRequest(
                    sessionId: sessionId, stepIndex: Int32(index),
                    vocabularyItemId: dictation.step.vocabularyItemId,
                    expectedText: dictation.step.expectedText, submittedText: typed,
                    tipUsed: false, skipped: skipped
                )
            )
            record(response?.outcome.name, expected: dictation.step.expectedText)
        case let puzzle as MixStepDictationPuzzle:
            let response = try? await deps.submitDictationPuzzle.invoke(
                request: SubmitDictationPuzzleAnswerRequest(
                    sessionId: sessionId, stepIndex: Int32(index),
                    vocabularyItemId: puzzle.step.vocabularyItemId,
                    expectedText: puzzle.step.expectedText, submittedText: typed,
                    tipUsed: false, skipped: skipped
                )
            )
            record(response?.outcome.name, expected: puzzle.step.expectedText)
        case let puzzle as MixStepPuzzle:
            let response = try? await deps.submitPuzzle.invoke(
                request: SubmitPuzzleAnswerRequest(
                    sessionId: sessionId, stepIndex: Int32(index),
                    vocabularyItemId: puzzle.step.vocabularyItemId,
                    expectedText: puzzle.step.expectedText, submittedText: typed,
                    tipUsed: false, skipped: skipped
                )
            )
            record(response?.outcome.name, expected: puzzle.step.expectedText)
        case let test as MixStepImageTest:
            let response = try? await deps.submitImageTest.invoke(
                request: SubmitImageTestAnswerRequest(
                    sessionId: sessionId, stepIndex: Int32(index),
                    vocabularyItemId: test.step.vocabularyItemId,
                    correctOption: test.step.correctOption,
                    selectedOption: skipped ? nil : typed, skipped: skipped
                )
            )
            record(response?.outcome.name, expected: test.step.correctOption)
        case let tf as MixStepTrueOrFalse:
            let saidTrue = typed.lowercased().hasPrefix("t")
            let response = try? await deps.submitTrueOrFalse.invoke(
                request: SubmitTrueOrFalseAnswerRequest(
                    sessionId: sessionId, stepIndex: Int32(index),
                    vocabularyItemId: tf.step.vocabularyItemId,
                    isDisplayedTranslationCorrect: tf.step.isDisplayedTranslationCorrect,
                    userAnsweredTrue: saidTrue
                )
            )
            record(response?.outcome.name, expected: tf.step.isDisplayedTranslationCorrect ? "true" : "false")
        case let say as MixStepPronunciation:
            let response = try? await deps.submitPronunciation.invoke(
                request: SubmitPronunciationResultRequest(
                    sessionId: sessionId, stepIndex: Int32(index),
                    vocabularyItemId: say.step.vocabularyItemId,
                    expectedText: say.step.expectedText, recognizedText: typed,
                    confidence: nil, tipUsed: false, skipped: skipped
                )
            )
            record(response?.outcome.name, expected: say.step.expectedText)
        default:
            break
        }
    }

    private func record(_ outcome: String?, expected: String) {
        switch outcome {
        case "CORRECT": state = .correct; tally.correct += 1
        case "SKIPPED": state = .skipped(expected: expected); tally.skipped += 1
        default: state = .incorrect(expected: expected); tally.incorrect += 1
        }
    }

    private func advance() {
        if index + 1 >= steps.count {
            finished = true
            return
        }
        index += 1
        typed = ""
        state = .unanswered
    }
}
