import SwiftUI
import Shared

private let settleNanoseconds: UInt64 = 350_000_000

@MainActor
final class ReviewWordsModel: ObservableObject {
    @Published private(set) var words: [Word] = []
    @Published private(set) var index = 0
    @Published private(set) var reviewed = 0
    @Published private(set) var waiting = 0
    @Published private(set) var loading = true
    @Published private(set) var chosen: WordStatus?
    @Published private(set) var pictures: [Int64: String] = [:]

    private var settle: Task<Void, Never>?
    private var picturesAsked: Set<Int64> = []
    private let picturesAhead = 2

    var current: Word? { words.indices.contains(index) ? words[index] : nil }

    var isFinished: Bool { !loading && current == nil }

    var currentPicture: String? { current.flatMap { pictures[$0.id.value] } }

    func load() async {
        waiting = Int((try? await deps.countWordsToReview.invoke()) as? Int32 ?? 0)
        words = (try? await deps.getWordsToReview.invoke(limit: Int32(deps.wordsToReviewBatch))) as? [Word] ?? []
        loading = false
        await loadPictures()
    }

    func refreshCurrent() async {
        guard let id = current?.id,
              let word = try? await deps.getWord.invoke(id: id) else { return }
        let cards = (try? await deps.getWordCards.invoke(ids: [id])) as? [WordCard] ?? []
        words = words.map { $0.id.value == id.value ? word : $0 }
        pictures[id.value] = cards.first?.imageUrl
    }

    func choose(_ status: WordStatus) {
        guard let word = current else { return }
        chosen = status

        settle?.cancel()
        settle = Task { [weak self] in
            try? await Task.sleep(nanoseconds: settleNanoseconds)
            guard !Task.isCancelled else { return }
            try? await deps.setWordStatus.invoke(id: word.id, status: status)
            await self?.advance()
        }
    }

    func delete() {
        guard let word = current else { return }
        chosen = nil

        settle?.cancel()
        settle = Task { [weak self] in
            try? await Task.sleep(nanoseconds: settleNanoseconds)
            guard !Task.isCancelled else { return }
            try? await deps.deleteWord.invoke(id: word.id)
            await self?.advance()
        }
    }

    private func advance() async {
        index += 1
        reviewed += 1
        waiting = max(waiting - 1, 0)
        chosen = nil
        if index >= words.count {
            words = (try? await deps.getWordsToReview.invoke(limit: Int32(deps.wordsToReviewBatch))) as? [Word] ?? []
            index = 0
        }
        await loadPictures()
    }

    private func loadPictures() async {
        let wanted = words.dropFirst(index).prefix(picturesAhead).map(\.id).filter { picturesAsked.insert($0.value).inserted }
        guard !wanted.isEmpty else { return }
        let cards = (try? await deps.getWordCards.invoke(ids: wanted)) as? [WordCard] ?? []
        for card in cards {
            if let url = card.imageUrl { pictures[card.id.value] = url }
        }
    }
}

struct ReviewWordsView: View {
    @StateObject private var model = ReviewWordsModel()
    @State private var editing: Int64?
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        Group {
            if model.loading {
                ProgressView()
            } else if model.isFinished {
                VStack(spacing: Spacing.large) {
                    Text(Strings.reviewWordsDone(model.reviewed))
                        .multilineTextAlignment(.center)
                        .foregroundStyle(.secondary)
                    Button(Strings.reviewWordsClose) { dismiss() }.buttonStyle(.borderedProminent)
                }
                .padding(Spacing.xl)
            } else if let word = model.current {
                ScrollView {
                    VStack(spacing: Spacing.medium) {
                        Text(Strings.reviewWordsWaiting(model.waiting))
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .frame(maxWidth: .infinity, alignment: .leading)

                        WordCardFace(
                            text: word.text,
                            translation: word.translation,
                            transcription: word.transcription,
                            imageUrl: model.currentPicture,
                            example: word.example,
                            onEdit: { editing = word.id.value }
                        )
                    }
                    .padding(Spacing.medium)
                }
                .safeAreaInset(edge: .bottom) {
                    VStack(spacing: Spacing.medium) {
                        choices
                        Button(role: .destructive) { model.delete() } label: {
                            Label(Strings.reviewWordsDelete, systemImage: "trash").frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.bordered)
                    }
                    .padding(Spacing.medium)
                    .background(.bar)
                }
            }
        }
        .navigationTitle(Strings.reviewWordsTitle)
        .navigationBarTitleDisplayMode(.inline)
        .navigationDestination(item: $editing) { WordFormView(wordId: $0) }
        .task {
            if model.loading { await model.load() } else { await model.refreshCurrent() }
        }
    }

    private var choices: some View {
        HStack(spacing: Spacing.small) {
            choice(Strings.reviewWordsToLearn, systemImage: "graduationcap", status: .toLearn, tint: Palette.accentDeep)
            choice(Strings.reviewWordsFavourite, systemImage: "heart.fill", status: .favourite, tint: Palette.failure)
            choice(Strings.reviewWordsKnown, systemImage: "checkmark.circle.fill", status: .known, tint: Palette.success)
        }
    }

    private func choice(
        _ label: String,
        systemImage: String,
        status: WordStatus,
        tint: Color
    ) -> some View {
        Button { model.choose(status) } label: {
            VStack(spacing: Spacing.tiny) {
                Image(systemName: systemImage)
                Text(label).font(.caption)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, Spacing.small)
        }
        .buttonStyle(ChoiceButtonStyle(tint: tint, isChosen: model.chosen == status))
    }
}

private struct ChoiceButtonStyle: ButtonStyle {
    let tint: Color
    let isChosen: Bool

    func makeBody(configuration: Configuration) -> some View {
        let isFilled = isChosen || configuration.isPressed
        let shape = RoundedRectangle(cornerRadius: Radius.small)
        return configuration.label
            .foregroundStyle(isFilled ? Color(uiColor: .systemBackground) : tint)
            .background(shape.fill(isFilled ? tint : Color.clear))
            .overlay(shape.stroke(tint, lineWidth: 1))
    }
}
