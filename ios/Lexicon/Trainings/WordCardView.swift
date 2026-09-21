import SwiftUI
import Shared

struct WordCardView: View {
    let vocabularyIds: [Int64]

    @Environment(\.dismiss) private var dismiss
    @Environment(\.onTrainingFinished) private var onTrainingFinished
    @State private var steps: [WordCardStep] = []
    @State private var sessionId = ""
    @State private var index = 0
    @State private var seen: Set<Int> = []

    var body: some View {
        Group {
            if let step = steps[safe: index] {
                TrainingScaffold(step: index, total: steps.count, state: .unanswered) {
                    WordCardFace(
                        text: step.text,
                        translation: step.translation,
                        transcription: step.transcription,
                        imageUrl: step.imageUrl,
                        example: step.example
                    )
                } actions: {
                    HStack {
                        if index > 0 {
                            Button(Strings.cardsBack) { index -= 1 }
                        }
                        Spacer()
                        Button(index == steps.count - 1 ? Strings.actionDone : Strings.actionNext) {
                            if index == steps.count - 1 {
                                if let onTrainingFinished { onTrainingFinished() } else { dismiss() }
                            } else {
                                index += 1
                                Task { await record() }
                            }
                        }
                        .buttonStyle(.borderedProminent)
                    }
                }
            } else {
                ProgressView()
            }
        }
        .task {
            await start()
            await record()
        }
    }

    private func start() async {
        let response = try? await deps.startWordCard.invoke(
            request: StartWordCardSessionRequest(vocabularyIds: vocabularyIds.map { KotlinLong(value: $0) })
        )
        guard let response else { return }
        sessionId = response.sessionId
        steps = response.steps
    }

    private func record() async {
        guard let step = steps[safe: index], seen.insert(index).inserted else { return }
        try? await deps.recordWordCardSeen.invoke(
            request: RecordWordCardSeenRequest(
                sessionId: sessionId,
                stepIndex: Int32(step.stepIndex),
                vocabularyItemId: step.vocabularyItemId,
                text: step.text
            )
        )
    }
}
