import SwiftUI
import Shared

/// Turn the cards over and find the pairs: a picture and the word it is of.
struct MemoryCardsView: View {
    let vocabularyIds: [Int64]

    @Environment(\.dismiss) private var dismiss
    @State private var steps: [MemoryCardsStepResponse] = []
    @State private var sessionId = ""
    @State private var index = 0
    @State private var cards: [MemoryCard] = []
    @State private var faceUp: [Int] = []
    @State private var matched: Set<Int> = []
    @State private var attempts = 0
    @State private var tally = SessionTally()
    @State private var finished = false

    var body: some View {
        Group {
            if finished {
                SessionResultView(tally: tally) { dismiss() }
            } else if steps.isEmpty {
                ProgressView()
            } else {
                TrainingScaffold(step: index, total: steps.count, state: .unanswered) {
                    LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 3), spacing: Spacing.small) {
                        ForEach(cards.indices, id: \.self) { i in
                            card(at: i)
                        }
                    }
                } actions: { EmptyView() }
            }
        }
        .task { await start() }
    }

    private func card(at i: Int) -> some View {
        let isUp = faceUp.contains(i) || matched.contains(i)
        return Button {
            Task { await turn(i) }
        } label: {
            ZStack {
                RoundedRectangle(cornerRadius: Radius.small)
                    .fill(matched.contains(i) ? Palette.success.opacity(0.3) : Color.secondary.opacity(0.15))
                if isUp {
                    switch cards[i].face {
                    case .word(let text):
                        Text(text).font(.callout).padding(4).multilineTextAlignment(.center)
                    case .picture(let url, let fallback):
                        if let link = URL(string: url) {
                            AsyncImage(url: link) { image in
                                image.resizable().scaledToFill()
                            } placeholder: {
                                Text(fallback).font(.caption)
                            }
                            .clipped()
                        } else {
                            Text(fallback).font(.caption)
                        }
                    }
                } else {
                    Image(systemName: "questionmark").foregroundStyle(.secondary)
                }
            }
            .frame(height: 96)
            .clipShape(RoundedRectangle(cornerRadius: Radius.small))
        }
        .buttonStyle(.plain)
        .disabled(isUp)
    }

    private func start() async {
        let response = try? await deps.startMemoryCards.invoke(
            request: StartMemoryCardsSessionRequest(
                stepCount: nil,
                pairsPerStep: StartMemoryCardsSessionRequest.companion.DEFAULT_PAIRS_PER_STEP,
                vocabularyIds: vocabularyIds.map { KotlinLong(value: $0) }
            )
        )
        guard let response else { return }
        sessionId = response.sessionId
        steps = response.steps
        deal()
    }

    private func deal() {
        guard let step = steps[safe: index] else { return }
        cards = step.pairs.flatMap { pair -> [MemoryCard] in
            [
                MemoryCard(pairId: pair.vocabularyItemId, face: .word(pair.text)),
                MemoryCard(pairId: pair.vocabularyItemId, face: .picture(pair.imageUrl ?? "", pair.imageFallbackText)),
            ]
        }.shuffled()
        faceUp = []
        matched = []
        attempts = 0
    }

    private func turn(_ i: Int) async {
        guard faceUp.count < 2 else { return }
        faceUp.append(i)
        guard faceUp.count == 2 else { return }

        let (a, b) = (faceUp[0], faceUp[1])
        if cards[a].pairId == cards[b].pairId {
            matched.insert(a)
            matched.insert(b)
            faceUp = []
            if matched.count == cards.count { await finishStep() }
        } else {
            attempts += 1
            try? await Task.sleep(nanoseconds: 700_000_000)
            faceUp = []
        }
    }

    private func finishStep() async {
        guard let step = steps[safe: index] else { return }
        let response = try? await deps.submitMemoryCards.invoke(
            request: SubmitMemoryCardsStepResultRequest(
                sessionId: sessionId,
                stepIndex: Int32(index),
                vocabularyItemIds: step.pairs.map { KotlinLong(value: $0.vocabularyItemId) },
                incorrectAttempts: Int32(attempts),
                skipped: false
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

struct MemoryCard {
    enum Face {
        case word(String)
        case picture(String, String)
    }

    let pairId: Int64
    let face: Face
}
