import SwiftUI
import Shared

struct VocabularyView: View {
    @StateObject private var model = VocabularyModel()
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(spacing: Spacing.small) {
                    filters

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
                        Text("No results")
                            .foregroundStyle(.secondary)
                            .padding(.top, Spacing.xl)
                    } else {
                        ForEach(model.words, id: \.id.value) { word in
                            Group {
                                if model.isSelecting {
                                    Button { model.toggleSelected(word) } label: { wordRow(word) }
                                        .buttonStyle(.plain)
                                } else {
                                    NavigationLink {
                                        WordFormView(wordId: word.id.value)
                                    } label: {
                                        wordRow(word)
                                    }
                                    .buttonStyle(.plain)
                                    .onLongPressGesture { model.startSelecting(word) }
                                }
                            }
                            .onAppear {
                                if word.id.value == model.words.last?.id.value {
                                    Task { await model.moreWords() }
                                }
                            }
                            Divider()
                        }

                        if model.isLoadingMoreWords {
                            ProgressView().padding(Spacing.medium)
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

    private func wordRow(_ word: Word) -> some View {
        HStack(spacing: Spacing.small) {
            if model.isSelecting {
                Image(systemName: model.isSelected(word) ? "checkmark.circle.fill" : "circle")
                    .foregroundStyle(model.isSelected(word) ? Palette.accentDeep : .secondary)
            }
            WordRow(word: word, status: model.status(of: word)) {
                Task { await model.cycleStatus(word) }
            }
        }
    }

    private var filters: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Spacing.small) {
                Button {
                    Task { await model.toggleToLearnOnly() }
                } label: {
                    Text("To learn")
                        .font(.callout)
                        .padding(.horizontal, Spacing.medium)
                        .padding(.vertical, Spacing.small)
                        .background(model.toLearnOnly ? Palette.primary.opacity(0.35) : Color.clear)
                        .overlay(Capsule().stroke(Color.secondary.opacity(0.4)))
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)

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
            StatChip(systemName: "character.book.closed", text: "\(preset.wordCount) words", skin: skin)
        }
    }
}

/// Matches Android: filled for a preset wholly in the study set, broken for part of one,
/// outlined for none, and red for anything but none.
struct WordRow: View {
    let word: Word
    let status: WordStatus
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
                Image(systemName: status.symbolName)
                    .foregroundStyle(status.tint)
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
                word: Word(
                    id: VocabularyId(value: 1),
                    text: "woda",
                    translation: "water",
                    transcription: "ˈvɔda",
                    status: WordStatus.toLearn,
                    cefr: CefrLevel.a1,
                    example: "Piję **wodę** codziennie."
                ),
                status: WordStatus.toLearn,
                onStudySet: {}
            )
            Divider()

            WordRow(
                word: Word(
                    id: VocabularyId(value: 2),
                    text: "dzień dobry",
                    translation: "good morning",
                    transcription: "",
                    status: WordStatus.undefined,
                    cefr: CefrLevel.a1,
                    example: ""
                ),
                status: WordStatus.undefined,
                onStudySet: {}
            )
        }
    }
}

extension WordStatus {
    var symbolName: String {
        switch self {
        case .toLearn: return "graduationcap"
        case .favourite: return "heart.fill"
        case .known: return "checkmark.circle.fill"
        default: return "circle"
        }
    }

    var tint: Color {
        switch self {
        case .toLearn: return Palette.accentDeep
        case .favourite: return Palette.failure
        case .known: return Palette.success
        default: return .secondary
        }
    }
}
