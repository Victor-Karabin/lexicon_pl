import SwiftUI
import Shared

struct PresetDetailView: View {
    let preset: VocabularyPreset

    @State private var words: [Word] = []
    @State private var studySet: Set<Int64> = []
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
                    StatChip(systemName: "character.book.closed", text: "\(preset.vocabularyIds.count) words", skin: skin)
                }
                .padding(.bottom, Spacing.medium)

                ForEach(words, id: \.id.value) { word in
                    WordRow(word: word, isInStudySet: studySet.contains(word.id.value)) {
                        Task {
                            try? await deps.toggleWordInStudySet.invoke(
                                id: word.id,
                                isInStudySet: !studySet.contains(word.id.value)
                            )
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
        studySet = Set(words.filter { $0.isInStudySet }.map { $0.id.value })
    }
}
