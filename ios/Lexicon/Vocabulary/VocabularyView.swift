import SwiftUI
import Shared

/// The study set and the shipped presets: search, filter by level, star what is
/// worth learning.
struct VocabularyView: View {
    @StateObject private var model = VocabularyModel()
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(spacing: Spacing.small) {
                    levelFilters

                    if model.query.isEmpty && model.levels.isEmpty {
                        ForEach(model.presets, id: \.id.value) { preset in
                            NavigationLink {
                                PresetDetailView(preset: preset)
                            } label: {
                                PresetTile(preset: preset, model: model)
                            }
                            .buttonStyle(.plain)
                        }
                    } else if model.words.isEmpty {
                        Text("No words match “\(model.query)”.")
                            .foregroundStyle(.secondary)
                            .padding(.top, Spacing.xl)
                    } else {
                        ForEach(model.words, id: \.id.value) { word in
                            WordRow(word: word, isFavourite: model.isFavourite(word)) {
                                Task { await model.toggleFavourite(word) }
                            }
                        }
                    }
                }
                .padding(Spacing.medium)
            }
            .searchable(text: $model.query, prompt: "Search words")
            .onChange(of: model.query) { Task { await model.search() } }
            .navigationTitle("Vocabulary")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    NavigationLink { WordFormView(wordId: nil) } label: { Image(systemName: "plus") }
                }
            }
            .task { await model.load() }
        }
    }

    private var levelFilters: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Spacing.small) {
                ForEach(model.allLevels, id: \.self) { level in
                    let on = model.levels.contains(level)
                    Button {
                        Task { await model.toggleLevel(level) }
                    } label: {
                        Text(model.name(of: level))
                            .font(.callout)
                            .padding(.horizontal, Spacing.medium)
                            .padding(.vertical, Spacing.small)
                            .background(on ? Palette.primary.opacity(0.35) : Color.clear)
                            .overlay(
                                Capsule().stroke(Color.secondary.opacity(0.4))
                            )
                            .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }
}

private struct PresetTile: View {
    let preset: VocabularyPreset
    @ObservedObject var model: VocabularyModel
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        let skin = TileSkin.accent(preset.accentColor, onAccent: preset.onAccentColor, scheme: scheme)
        Tile(skin: skin) {
            HStack(spacing: Spacing.medium) {
                Medallion(skin: skin) { MedallionIcon(systemName: preset.symbolName, skin: skin) }
                VStack(alignment: .leading, spacing: 2) {
                    Text(preset.title.text()).font(.headline).foregroundStyle(skin.onTile)
                    Text(preset.description_.text())
                        .font(.caption)
                        .foregroundStyle(skin.onTile.muted)
                        .lineLimit(2)
                }
                Spacer()
            }
            StatChip(systemName: "character.book.closed", text: "\(preset.vocabularyIds.count) words", skin: skin)
        }
    }
}

struct WordRow: View {
    let word: PresetWord
    let isFavourite: Bool
    let onFavourite: () -> Void

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(word.text).font(.body.weight(.semibold))
                Text(word.translation).font(.caption).foregroundStyle(.secondary)
                if !word.transcription.isEmpty {
                    Text("[\(word.transcription)]").font(.caption2).foregroundStyle(.secondary)
                }
            }
            Spacer()
            Button { Speech.shared.speak(word.text) } label: {
                Image(systemName: "speaker.wave.2")
            }
            .buttonStyle(.plain)
            Button(action: onFavourite) {
                Image(systemName: isFavourite ? "heart.fill" : "heart")
            }
            .buttonStyle(.plain)
            .padding(.leading, Spacing.small)
        }
        .padding(.vertical, Spacing.small)
    }
}
