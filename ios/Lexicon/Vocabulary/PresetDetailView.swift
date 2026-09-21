import SwiftUI
import Shared

struct PresetDetailView: View {
    let preset: VocabularyPreset

    @State private var words: [Word] = []
    @State private var statuses: [Int64: WordStatus] = [:]
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 0) {
                let skin = TileSkin.accent(preset.accentColor, onAccent: preset.onAccentColor, scheme: scheme)
                Tile(skin: skin) {
                    HStack(spacing: Spacing.medium) {
                        Medallion(skin: skin) { MedallionIcon(systemName: preset.symbolName, skin: skin) }
                        Text(preset.description_.text())
                            .font(.caption)
                            .foregroundStyle(skin.onTile.muted)
                        Spacer()
                    }
                    StatChip(systemName: "character.book.closed", text: "\(preset.wordCount) \(Strings.presetsWordCountLabel(Int(preset.wordCount)))", skin: skin)
                }
                .padding(.bottom, Spacing.medium)

                ForEach(words, id: \.id.value) { word in
                    WordRow(word: word, status: statuses[word.id.value] ?? word.status) {
                        Task {
                            try? await deps.setWordStatus.invoke(id: word.id, status: (statuses[word.id.value] ?? word.status).next())
                            await load()
                        }
                    }
                    Divider()
                }
            }
            .padding(Spacing.medium)
        }
        .navigationTitle(preset.title.text())
        .navigationBarTitleDisplayMode(.inline)
        .task { await load() }
    }

    private func load() async {
        words = (try? await deps.getPresetVocabulary.invoke(id: preset.id)) ?? []
        statuses = Dictionary(uniqueKeysWithValues: words.map { ($0.id.value, $0.status) })
    }
}
