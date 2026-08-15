import SwiftUI
import Shared

/// Is the translation on screen the right one?
struct TrueOrFalseView: View {
    let vocabularyIds: [Int64]

    @Environment(\.dismiss) private var dismiss
    @State private var steps: [TrueOrFalseStepResponse] = []
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
                        Text(step.word).font(.largeTitle.bold())
                        Text(step.displayedTranslation).font(.title2).foregroundStyle(.secondary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, Spacing.xl)
                } actions: {
                    HStack(spacing: Spacing.medium) {
                        if !state.isAnswered {
                            AsyncButton { await submit(answeredTrue: true) } label: {
                                Text("True").frame(maxWidth: .infinity)
                            }
                            .buttonStyle(.borderedProminent)
                            AsyncButton { await submit(answeredTrue: false) } label: {
                                Text("False").frame(maxWidth: .infinity)
                            }
                            .buttonStyle(.bordered)
                        } else {
                            Button("Next") { advance() }
                                .buttonStyle(.borderedProminent)
                                .frame(maxWidth: .infinity)
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
        let response = try? await deps.startTrueOrFalse.invoke(
            request: StartTrueOrFalseSessionRequest(
                poolSize: StartTrueOrFalseSessionRequest.companion.DEFAULT_POOL_SIZE,
                correctProbability: StartTrueOrFalseSessionRequest.companion.DEFAULT_CORRECT_PROBABILITY,
                vocabularyIds: vocabularyIds.map { KotlinLong(value: $0) }
            )
        )
        guard let response else { return }
        sessionId = response.sessionId
        steps = response.steps
    }

    private func submit(answeredTrue: Bool) async {
        guard let step = steps[safe: index] else { return }
        let response = try? await deps.submitTrueOrFalse.invoke(
            request: SubmitTrueOrFalseAnswerRequest(
                sessionId: sessionId,
                stepIndex: Int32(index),
                vocabularyItemId: step.vocabularyItemId,
                isDisplayedTranslationCorrect: step.isDisplayedTranslationCorrect,
                userAnsweredTrue: answeredTrue
            )
        )
        guard let response else { return }
        if response.outcome == .correct {
            state = .correct
            tally.correct += 1
        } else {
            state = .incorrect(expected: step.isDisplayedTranslationCorrect ? "True" : "False")
            tally.incorrect += 1
        }
    }

    private func advance() {
        if index + 1 >= steps.count {
            finished = true
            return
        }
        index += 1
        state = .unanswered
    }
}
