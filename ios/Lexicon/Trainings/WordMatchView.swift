import SwiftUI
import Shared

struct WordMatchView: View {
    let vocabularyIds: [Int64]

    @Environment(\.dismiss) private var dismiss
    @State private var steps: [WordMatchStepResponse] = []
    @State private var sessionId = ""
    @State private var index = 0
    @State private var shuffledTranslations: [String] = []
    @State private var chosenWord: String?
    @State private var matched: Set<String> = []
    @State private var wrong: String?
    @State private var attempts = 0
    @State private var tally = SessionTally()
    @State private var finished = false

    var body: some View {
        Group {
            if finished {
                SessionResultView(tally: tally) { dismiss() }
            } else if let step = steps[safe: index] {
                TrainingScaffold(step: index, total: steps.count, state: .unanswered) {
                    HStack(alignment: .top, spacing: Spacing.small) {
                        VStack(spacing: Spacing.small) {
                            ForEach(step.pairs, id: \.vocabularyItemId) { pair in
                                tile(pair.word, isMatched: matched.contains(pair.word), isChosen: chosenWord == pair.word) {
                                    chosenWord = pair.word
                                }
                            }
                        }
                        VStack(spacing: Spacing.small) {
                            ForEach(shuffledTranslations, id: \.self) { translation in
                                tile(translation, isMatched: matched.contains(translation), isChosen: false) {
                                    Task { await choose(translation: translation, step: step) }
                                }
                            }
                        }
                    }
                } actions: { EmptyView() }
            } else {
                ProgressView()
            }
        }
        .task { await start() }
    }

    private func tile(_ text: String, isMatched: Bool, isChosen: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(text)
                .font(.callout)
                .frame(maxWidth: .infinity, minHeight: 44)
                .background(
                    isMatched ? Palette.success.opacity(0.3)
                        : isChosen ? Palette.primary.opacity(0.5)
                        : wrong == text ? Palette.failure.opacity(0.3)
                        : Color.secondary.opacity(0.15)
                )
                .clipShape(RoundedRectangle(cornerRadius: Radius.small))
        }
        .buttonStyle(.plain)
        .disabled(isMatched)
    }

    private func start() async {
        let response = try? await deps.startWordMatch.invoke(
            request: StartWordMatchSessionRequest(stepCount: nil, vocabularyIds: vocabularyIds.map { KotlinLong(value: $0) })
        )
        guard let response else { return }
        sessionId = response.sessionId
        steps = response.steps
        deal()
    }

    private func deal() {
        guard let step = steps[safe: index] else { return }
        shuffledTranslations = step.pairs.map { $0.translation }.shuffled()
        matched = []
        chosenWord = nil
        attempts = 0
    }

    private func choose(translation: String, step: WordMatchStepResponse) async {
        guard let word = chosenWord else { return }
        let isPair = step.pairs.contains { $0.word == word && $0.translation == translation }
        if isPair {
            matched.insert(word)
            matched.insert(translation)
            chosenWord = nil
            if matched.count == step.pairs.count * 2 {
                await finishStep(step)
            }
        } else {
            attempts += 1
            wrong = translation
            try? await Task.sleep(nanoseconds: 400_000_000)
            wrong = nil
            chosenWord = nil
        }
    }

    private func finishStep(_ step: WordMatchStepResponse) async {
        let response = try? await deps.submitWordMatch.invoke(
            request: SubmitWordMatchStepResultRequest(
                sessionId: sessionId,
                stepIndex: Int32(index),
                vocabularyItemIds: step.pairs.map { KotlinLong(value: $0.vocabularyItemId) },
                incorrectAttempts: Int32(attempts)
            )
        )
        if response?.outcome == .correct { tally.correct += 1 } else { tally.incorrect += 1 }
        if index + 1 >= steps.count {
            finished = true
        } else {
            index += 1
            deal()
        }
    }
}
