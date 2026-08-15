import SwiftUI

/// Every training, as a tile apiece.
///
/// A name on its own says little — "Puzzle" and "Dictation Puzzle" are not obviously
/// different things — so each tile carries what the training actually asks of you.
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
            .navigationTitle("Trainings")
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

/// The same list the Android catalogue holds, in the same order.
enum TrainingCatalog {
    static let all: [TrainingEntry] = [
        .init(id: "dictation", name: "Dictation", blurb: "Hear a word and type it", symbol: "headphones"),
        .init(id: "dictation_puzzle", name: "Dictation Puzzle", blurb: "Hear a word and build it from letters", symbol: "keyboard"),
        .init(id: "puzzle", name: "Puzzle", blurb: "Put the letters in the right order", symbol: "puzzlepiece"),
        .init(id: "image_test", name: "Image Test", blurb: "Pick the word that matches the picture", symbol: "photo"),
        .init(id: "word_match", name: "Word Match", blurb: "Pair each word with its translation", symbol: "link"),
        .init(id: "true_or_false", name: "True or False", blurb: "Say whether the translation is right", symbol: "questionmark"),
        .init(id: "pronunciation_check", name: "Pronunciation Check", blurb: "Say the word out loud and be heard", symbol: "waveform"),
        .init(id: "memory_cards", name: "Memory Cards", blurb: "Turn the cards over and find the pairs", symbol: "square.stack"),
        .init(id: "crossword", name: "Crossword", blurb: "Fill the grid from the clues", symbol: "grid"),
        .init(id: "word_card", name: "Word Card", blurb: "Read the word, its picture and how it sounds", symbol: "creditcard"),
        .init(id: "mix", name: "Mix", blurb: "A little of every training", symbol: "sparkles"),
    ]

    static func entry(id: String) -> TrainingEntry? { all.first { $0.id == id } }
}

/// Sends a training id to the screen that runs it.
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
            case "mix": MixView(vocabularyIds: vocabularyIds)
            default: Text("Unknown training")
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
