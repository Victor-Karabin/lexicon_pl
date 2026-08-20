import SwiftUI
import Shared

struct ImageTestView: View {
    let vocabularyIds: [Int64]

    @Environment(\.dismiss) private var dismiss
    @State private var steps: [ImageTestStepResponse] = []
    @State private var sessionId = ""
    @State private var index = 0
    @State private var chosen: String?
    @State private var state: AnswerState = .unanswered
    @State private var tally = SessionTally()
    @State private var finished = false

    var body: some View {
        Group {
            if finished {
                SessionResultView(tally: tally) { dismiss() }
            } else if let step = steps[safe: index] {
                TrainingScaffold(step: index, total: steps.count, state: state) {
                    VStack(spacing: Spacing.medium) {
                        if let url = step.imageUrl, let link = URL(string: url) {
                            AsyncImage(url: link) { image in
                                image.resizable().scaledToFit()
                            } placeholder: {
                                Color.secondary.opacity(0.15)
                            }
                            .frame(height: 200)
                            .clipShape(RoundedRectangle(cornerRadius: Radius.medium))
                        } else {
                            Text(step.clueText).font(.title2.weight(.semibold))
                        }

                        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: Spacing.small) {
                            ForEach(step.options, id: \.self) { option in
                                Button {
                                    guard !state.isAnswered else { return }
                                    chosen = option
                                    Task { await submit(skipped: false) }
                                } label: {
                                    Text(option)
                                        .frame(maxWidth: .infinity, minHeight: 48)
                                        .background(background(for: option, step: step))
                                        .clipShape(RoundedRectangle(cornerRadius: Radius.small))
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                } actions: {
                    HStack {
                        if !state.isAnswered {
                            Button("Skip") { Task { await submit(skipped: true) } }
                            Spacer()
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

    private func background(for option: String, step: ImageTestStepResponse) -> Color {
        guard state.isAnswered else { return Palette.primary.opacity(0.25) }
        if option == step.correctOption { return Palette.success.opacity(0.35) }
        if option == chosen { return Palette.failure.opacity(0.35) }
        return Color.secondary.opacity(0.15)
    }

    private func start() async {
        let response = try? await deps.startImageTest.invoke(
            request: StartImageTestSessionRequest(
                stepCount: nil,
                optionCount: StartImageTestSessionRequest.companion.DEFAULT_OPTION_COUNT,
                vocabularyIds: vocabularyIds.map { KotlinLong(value: $0) }
            )
        )
        guard let response else { return }
        sessionId = response.sessionId
        steps = response.steps
    }

    private func submit(skipped: Bool) async {
        guard let step = steps[safe: index] else { return }
        let response = try? await deps.submitImageTest.invoke(
            request: SubmitImageTestAnswerRequest(
                sessionId: sessionId,
                stepIndex: Int32(index),
                vocabularyItemId: step.vocabularyItemId,
                correctOption: step.correctOption,
                selectedOption: skipped ? nil : chosen,
                skipped: skipped
            )
        )
        guard let response else { return }
        switch response.outcome {
        case .correct: state = .correct; tally.correct += 1
        case .skipped: state = .skipped(expected: response.correctOption); tally.skipped += 1
        default: state = .incorrect(expected: response.correctOption); tally.incorrect += 1
        }
    }

    private func advance() {
        if index + 1 >= steps.count {
            finished = true
            return
        }
        index += 1
        chosen = nil
        state = .unanswered
    }
}
