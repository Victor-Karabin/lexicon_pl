import SwiftUI

struct TrainingsView: View {
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(spacing: Spacing.small) {
                    ForEach(TrainingCatalog.all) { entry in
                        NavigationLink {
                            TrainingHost(entry: entry, vocabularyIds: [])
                        } label: {
                            tile(entry)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(Spacing.medium)
            }
            .navigationTitle(Strings.tabTrainings)
        }
    }

    private func tile(_ entry: TrainingEntry) -> some View {
        let skin = TileSkin.standard(scheme: scheme)
        return Tile(skin: skin) {
            HStack(spacing: Spacing.medium) {
                Medallion(skin: skin) { MedallionIcon(systemName: entry.symbol, skin: skin) }
                VStack(alignment: .leading, spacing: 2) {
                    Text(entry.name).font(.headline).foregroundStyle(skin.onTile)
                    Text(entry.blurb).font(.caption).foregroundStyle(skin.onTile.muted)
                }
                Spacer()
                Image(systemName: "chevron.right").foregroundStyle(skin.onTile.muted)
            }
        }
    }
}

struct TrainingEntry: Identifiable {
    let id: String
    let name: String
    let blurb: String
    let symbol: String
}

enum TrainingCatalog {
    static let all: [TrainingEntry] = [
        .init(id: "dictation", name: Strings.dictationTitle, blurb: Strings.trainingDictationBlurb, symbol: "headphones"),
        .init(id: "dictation_puzzle", name: Strings.dictationPuzzleTitle, blurb: Strings.trainingDictationPuzzleBlurb, symbol: "keyboard"),
        .init(id: "puzzle", name: Strings.puzzleTitle, blurb: Strings.trainingPuzzleBlurb, symbol: "puzzlepiece"),
        .init(id: "image_test", name: Strings.imageTestTitle, blurb: Strings.trainingImageTestBlurb, symbol: "photo"),
        .init(id: "word_match", name: Strings.wordMatchTitle, blurb: Strings.trainingWordMatchBlurb, symbol: "link"),
        .init(id: "true_or_false", name: Strings.trueOrFalseTitle, blurb: Strings.trainingTrueOrFalseBlurb, symbol: "questionmark"),
        .init(id: "pronunciation_check", name: Strings.pronunciationTitle, blurb: Strings.trainingPronunciationBlurb, symbol: "waveform"),
        .init(id: "pronunciation_sentences", name: Strings.pronunciationSentencesTitle, blurb: Strings.trainingPronunciationSentencesBlurb, symbol: "mic"),
        .init(id: "memory_cards", name: Strings.memoryCardsTitle, blurb: Strings.trainingMemoryCardsBlurb, symbol: "square.stack"),
        .init(id: "crossword", name: Strings.crosswordTitle, blurb: Strings.trainingCrosswordBlurb, symbol: "grid"),
        .init(id: "word_card", name: Strings.wordCardTitle, blurb: Strings.trainingWordCardBlurb, symbol: "creditcard"),
        .init(id: "passage_write", name: Strings.passageWriteTitle, blurb: Strings.trainingPassageWriteBlurb, symbol: "text.alignleft"),
        .init(id: "passage_bank", name: Strings.passageBankTitle, blurb: Strings.trainingPassageBankBlurb, symbol: "list.bullet.rectangle"),
        .init(id: "fillword", name: Strings.fillwordTitle, blurb: Strings.trainingFillwordBlurb, symbol: "square.grid.3x3"),
        .init(id: "mix", name: Strings.mixTitle, blurb: Strings.trainingMixBlurb, symbol: "sparkles"),
    ].sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }

    static func entry(id: String) -> TrainingEntry? { all.first { $0.id == id } }
}

struct TrainingHost: View {
    let entry: TrainingEntry
    let vocabularyIds: [Int64]

    var body: some View {
        Group {
            switch entry.id {
            case "dictation": DictationView(vocabularyIds: vocabularyIds, fromAudio: true)
            case "dictation_puzzle": PuzzleView(vocabularyIds: vocabularyIds, fromAudio: true)
            case "puzzle": PuzzleView(vocabularyIds: vocabularyIds, fromAudio: false)
            case "image_test": ImageTestView(vocabularyIds: vocabularyIds)
            case "word_match": WordMatchView(vocabularyIds: vocabularyIds)
            case "true_or_false": TrueOrFalseView(vocabularyIds: vocabularyIds)
            case "pronunciation_check": PronunciationView(vocabularyIds: vocabularyIds)
            case "memory_cards": MemoryCardsView(vocabularyIds: vocabularyIds)
            case "crossword": CrosswordView(vocabularyIds: vocabularyIds)
            case "word_card": WordCardView(vocabularyIds: vocabularyIds)
            case "pronunciation_sentences": ReadAloudView()
            case "passage_write": PassageView(withWordBank: false)
            case "passage_bank": PassageView(withWordBank: true)
            case "fillword": WordSearchView()
            case "mix": MixView(vocabularyIds: vocabularyIds)
            default: Text(Strings.trainingUnknown)
            }
        }
        .navigationTitle(entry.name)
        .navigationBarTitleDisplayMode(.inline)
    }
}

#Preview("Trainings") {
    TrainingsView()
}

#Preview("Trainings · dark") {
    TrainingsView().preferredColorScheme(.dark)
}
