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

    private var settle: Task<Void, Never>?

    var current: Word? { words.indices.contains(index) ? words[index] : nil }

    var isFinished: Bool { !loading && current == nil }

    func load() async {
        waiting = Int((try? await deps.countWordsToReview.invoke()) as? Int32 ?? 0)
        words = (try? await deps.getWordsToReview.invoke(limit: Int32(deps.wordsToReviewBatch))) as? [Word] ?? []
        loading = false
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
        guard index >= words.count else { return }

        words = (try? await deps.getWordsToReview.invoke(limit: Int32(deps.wordsToReviewBatch))) as? [Word] ?? []
        index = 0
    }
}

struct ReviewWordsView: View {
    @StateObject private var model = ReviewWordsModel()
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        Group {
            if model.loading {
                ProgressView()
            } else if model.isFinished {
                VStack(spacing: Spacing.large) {
                    Text("Nothing left to sort. You went through \(model.reviewed) words.")
                        .multilineTextAlignment(.center)
                        .foregroundStyle(.secondary)
                    Button("Done") { dismiss() }.buttonStyle(.borderedProminent)
                }
                .padding(Spacing.xl)
            } else if let word = model.current {
                ScrollView {
                    VStack(spacing: Spacing.medium) {
                        Text("\(model.waiting) still to sort")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .frame(maxWidth: .infinity, alignment: .leading)

                        card(word)
                        choices
                        Button(role: .destructive) { model.delete() } label: {
                            Label("Delete this word", systemImage: "trash").frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.bordered)
                    }
                    .padding(Spacing.medium)
                }
            }
        }
        .navigationTitle("Review words")
        .navigationBarTitleDisplayMode(.inline)
        .task { await model.load() }
    }

    private func card(_ word: Word) -> some View {
        let skin = TileSkin.standard(highlighted: true, scheme: scheme)
        return Tile(skin: skin) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(word.translation).font(.title3).foregroundStyle(skin.onTile.muted)
                    Text(word.text).font(.largeTitle.weight(.semibold)).foregroundStyle(skin.onTile)
                    if !word.transcription.isEmpty {
                        Text("[\(word.transcription)]").font(.callout).foregroundStyle(skin.onTile.muted)
                    }
                }
                Spacer()
                Button { Speech.shared.speak(word.text) } label: {
                    Image(systemName: "speaker.wave.2").foregroundStyle(skin.onTile)
                }
            }

            ExampleSentenceRow(sentence: word.example, word: word.text, tint: skin.onTile.muted)
        }
    }

    private var choices: some View {
        HStack(spacing: Spacing.small) {
            choice("To learn", systemImage: "graduationcap", status: .toLearn, tint: Palette.accentDeep)
            choice("Favourite", systemImage: "heart.fill", status: .favourite, tint: Palette.failure)
            choice("Known", systemImage: "checkmark.circle.fill", status: .known, tint: Palette.success)
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
        .buttonStyle(.bordered)
        .tint(tint)
        .background(model.chosen == status ? tint.opacity(0.25) : Color.clear)
        .clipShape(RoundedRectangle(cornerRadius: Radius.small))
    }
}
