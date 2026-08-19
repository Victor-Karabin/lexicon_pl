import SwiftUI
import Shared

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
                            NavigationLink {
                                WordFormView(wordId: word.id.value)
                            } label: {
                                HStack(spacing: Spacing.small) {
                                    if model.isSelecting {
                                        Image(systemName: model.isSelected(word) ? "checkmark.circle.fill" : "circle")
                                            .foregroundStyle(model.isSelected(word) ? Palette.accentDeep : .secondary)
                                    }
                                    WordRow(word: word, isInStudySet: model.isInStudySet(word)) {
                                        Task { await model.toggleInStudySet(word) }
                                    }
                                }
                            }
                            .buttonStyle(.plain)

                            .onLongPressGesture { model.startSelecting(word) }
                            .simultaneousGesture(TapGesture().onEnded {
                                if model.isSelecting { model.toggleSelected(word) }
                            })
                            .swipeActions(edge: .trailing) {
                                Button(role: .destructive) {
                                    Task { await model.delete(word) }
                                } label: {
                                    Label("Delete", systemImage: "trash")
                                }
                            }
                            Divider()
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
                    if model.isSelecting {
                        Button(role: .destructive) {
                            Task { await model.deleteSelected() }
                        } label: {
                            Label("Delete", systemImage: "trash")
                        }
                    } else {
                        Menu {
                            NavigationLink { WordFormView(wordId: nil) } label: { Label("Add word", systemImage: "text.badge.plus") }
                            NavigationLink { PresetFormView() } label: { Label("Add preset", systemImage: "folder.badge.plus") }
                        } label: {
                            Image(systemName: "plus")
                        }
                    }
                }
                ToolbarItem(placement: .topBarLeading) {
                    if model.isSelecting {
                        Button("Stop selecting") { model.stopSelecting() }
                    }
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
    let isInStudySet: Bool
    let onStudySet: () -> Void

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
            Button(action: onStudySet) {
                Image(systemName: isInStudySet ? "heart.fill" : "heart")
            }
            .buttonStyle(.plain)
            .padding(.leading, Spacing.small)
        }
        .padding(.vertical, Spacing.small)
    }
}

#Preview("Word rows") {
    LightDark(title: "Word rows") {
        VStack(spacing: 0) {
            WordRow(
                word: PresetWord(
                    id: VocabularyId(value: 1),
                    text: "woda",
                    translation: "water",
                    transcription: "ˈvɔda",
                    isInStudySet: true,
                    cefr: CefrLevel.a1
                ),
                isInStudySet: true,
                onStudySet: {}
            )
            Divider()

            WordRow(
                word: PresetWord(
                    id: VocabularyId(value: 2),
                    text: "dzień dobry",
                    translation: "good morning",
                    transcription: "",
                    isInStudySet: false,
                    cefr: CefrLevel.a1
                ),
                isInStudySet: false,
                onStudySet: {}
            )
        }
    }
}
