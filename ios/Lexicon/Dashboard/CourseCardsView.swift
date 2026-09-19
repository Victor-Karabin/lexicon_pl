import SwiftUI
import Shared

struct CourseCardsView: View {

    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var scheme
    @State private var cards: [WordCard] = []
    @State private var index = 0

    var body: some View {
        Group {
            if let card = cards[safe: index] {
                let skin = TileSkin.standard(highlighted: true, scheme: scheme)
                TrainingScaffold(step: index, total: cards.count, state: .unanswered) {
                    Tile(skin: skin) {
                        if let url = card.imageUrl, let link = imageURL(url) {
                            AsyncImage(url: link) { $0.resizable().scaledToFill() } placeholder: {
                                Color.secondary.opacity(0.15)
                            }
                            .frame(height: 200)
                            .frame(maxWidth: .infinity)
                            .clipped()
                            .clipShape(RoundedRectangle(cornerRadius: Radius.small))
                        }
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(card.translation).font(.title3).foregroundStyle(skin.onTile.muted)
                                Text(card.text).font(.largeTitle.weight(.semibold)).foregroundStyle(skin.onTile)
                                if !card.transcription.isEmpty {
                                    Text("[\(card.transcription)]").font(.callout).foregroundStyle(skin.onTile.muted)
                                }
                            }
                            Spacer()
                            Button { Speech.shared.speak(card.text) } label: {
                                Image(systemName: "speaker.wave.2").foregroundStyle(skin.onTile)
                            }
                        }
                    }
                } actions: {
                    HStack {
                        if index > 0 { Button("Back") { index -= 1 } }
                        Spacer()
                        Button(index == cards.count - 1 ? "Start training" : "Next") {
                            if index == cards.count - 1 {
                                Task {
                                    try? await deps.markCourseCardsSeen.invoke()
                                    dismiss()
                                }
                            } else {
                                index += 1
                            }
                        }
                        .buttonStyle(.borderedProminent)
                    }
                }
            } else {
                ProgressView()
            }
        }
        .navigationTitle("New words")
        .navigationBarTitleDisplayMode(.inline)
        .task { await load() }
    }

    private func load() async {
        guard let course = try? await deps.getVocabularyCourse.invoke() else { return }
        cards = (try? await deps.getWordCards.invoke(ids: course.newWords as? [VocabularyId] ?? [])) ?? []
    }
}
