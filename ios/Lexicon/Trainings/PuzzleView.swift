import SwiftUI
import Shared

struct PuzzleView: View {
    let vocabularyIds: [Int64]
    let fromAudio: Bool

    @Environment(\.dismiss) private var dismiss
    @State private var expected: [String] = []
    @State private var clues: [String] = []
    @State private var images: [String?] = []
    @State private var wordIds: [Int64] = []
    @State private var sessionId = ""
    @State private var index = 0
    @State private var tiles: [Character] = []
    @State private var picked: [Int] = []
    @State private var state: AnswerState = .unanswered
    @State private var tally = SessionTally()
    @State private var finished = false

    var body: some View {
        Group {
            if finished {
                SessionResultView(tally: tally) { dismiss() }
            } else if index < expected.count {
                TrainingScaffold(step: index, total: expected.count, state: state) {
                    VStack(spacing: Spacing.medium) {
                        if fromAudio {
                            Button {
                                Speech.shared.speak(expected[index])
                            } label: {
                                Label("Listen again", systemImage: "speaker.wave.2.fill").font(.title3)
                            }
                        } else if let url = images[safe: index] ?? nil, let link = URL(string: url) {
                            AsyncImage(url: link) { image in
                                image.resizable().scaledToFit()
                            } placeholder: {
                                Color.secondary.opacity(0.15)
                            }
                            .frame(height: 180)
                            .clipShape(RoundedRectangle(cornerRadius: Radius.medium))
                        }
                        Text(clues[index]).font(.title3.weight(.semibold))

                        Text(assembled.isEmpty ? " " : assembled)
                            .font(.title2.monospaced())
                            .padding(Spacing.small)

                        letterTiles
                    }
                } actions: {
                    HStack(spacing: Spacing.small) {
                        if !state.isAnswered {
                            Button("Undo") { undo() }.disabled(picked.isEmpty)
                            Button("Skip") { Task { await submit(skipped: true) } }
                            Spacer()
                            AsyncButton { await submit(skipped: false) } label: { Text("Check") }
                                .buttonStyle(.borderedProminent)
                                .disabled(picked.count != tiles.count)
                        } else {
                            Spacer()
                            Button("Next") { advance() }.buttonStyle(.borderedProminent)
                        }
                    }
                }
                .onAppear { if fromAudio { Speech.shared.speak(expected[index]) } }
            } else {
                ProgressView()
            }
        }
        .task { await start() }
    }

    private var assembled: String { String(picked.map { tiles[$0] }) }

    private var letterTiles: some View {
        LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 6), spacing: Spacing.small) {
            ForEach(tiles.indices, id: \.self) { i in
                Button {
                    guard !picked.contains(i), !state.isAnswered else { return }
                    picked.append(i)
                } label: {
                    Text(String(tiles[i]))
                        .font(.title3)
                        .frame(width: 40, height: 44)
                        .background(picked.contains(i) ? Color.secondary.opacity(0.15) : Palette.primary.opacity(0.35))
                        .clipShape(RoundedRectangle(cornerRadius: Radius.small))
                }
                .buttonStyle(.plain)
                .disabled(picked.contains(i))
            }
        }
    }

    private func start() async {
        if fromAudio {
            let response = try? await deps.startDictationPuzzle.invoke(
                request: StartDictationPuzzleSessionRequest(stepCount: nil, vocabularyIds: vocabularyIds.map { KotlinLong(value: $0) })
            )
            guard let response else { return }
            sessionId = response.sessionId
            expected = response.steps.map { $0.expectedText }
            clues = response.steps.map { $0.translationText }
            images = response.steps.map { _ in nil }
            wordIds = response.steps.map { $0.vocabularyItemId }
        } else {
            let response = try? await deps.startPuzzle.invoke(
                request: StartPuzzleSessionRequest(stepCount: nil, vocabularyIds: vocabularyIds.map { KotlinLong(value: $0) })
            )
            guard let response else { return }
            sessionId = response.sessionId
            expected = response.steps.map { $0.expectedText }
            clues = response.steps.map { $0.clueText }
            images = response.steps.map { $0.imageUrl }
            wordIds = response.steps.map { $0.vocabularyItemId }
        }
        deal()
    }

    private func deal() {
        guard index < expected.count else { return }
        tiles = Array(expected[index]).shuffled()
        picked = []
    }

    private func undo() { if !picked.isEmpty { picked.removeLast() } }

    private func submit(skipped: Bool) async {
        guard index < expected.count else { return }
        let outcome: String
        if fromAudio {
            let response = try? await deps.submitDictationPuzzle.invoke(
                request: SubmitDictationPuzzleAnswerRequest(
                    sessionId: sessionId,
                    stepIndex: Int32(index),
                    vocabularyItemId: wordIds[index],
                    expectedText: expected[index],
                    submittedText: assembled,
                    tipUsed: false,
                    skipped: skipped
                )
            )
            outcome = response?.outcome.name ?? "INCORRECT"
        } else {
            let response = try? await deps.submitPuzzle.invoke(
                request: SubmitPuzzleAnswerRequest(
                    sessionId: sessionId,
                    stepIndex: Int32(index),
                    vocabularyItemId: wordIds[index],
                    expectedText: expected[index],
                    submittedText: assembled,
                    tipUsed: false,
                    skipped: skipped
                )
            )
            outcome = response?.outcome.name ?? "INCORRECT"
        }
        switch outcome {
        case "CORRECT": state = .correct; tally.correct += 1
        case "SKIPPED": state = .skipped(expected: expected[index]); tally.skipped += 1
        default: state = .incorrect(expected: expected[index]); tally.incorrect += 1
        }
    }

    private func advance() {
        if index + 1 >= expected.count {
            finished = true
            return
        }
        index += 1
        state = .unanswered
        deal()
    }
}
