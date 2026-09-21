import SwiftUI
import Shared

struct CourseCardsView: View {

    @Environment(\.dismiss) private var dismiss
    @State private var cards: [WordCard] = []
    @State private var index = 0

    var body: some View {
        Group {
            if let card = cards[safe: index] {
                TrainingScaffold(step: index, total: cards.count, state: .unanswered) {
                    WordCardFace(
                        text: card.text,
                        translation: card.translation,
                        transcription: card.transcription,
                        imageUrl: card.imageUrl,
                        example: card.example
                    )
                } actions: {
                    HStack {
                        if index > 0 { Button(Strings.cardsBack) { index -= 1 } }
                        Spacer()
                        Button(index == cards.count - 1 ? Strings.cardsStartTraining : Strings.cardsNext) {
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
        .navigationTitle(Strings.cardsTitle)
        .navigationBarTitleDisplayMode(.inline)
        .task { await load() }
    }

    private func load() async {
        guard let course = try? await deps.getVocabularyCourse.invoke() else { return }
        cards = (try? await deps.getWordCards.invoke(ids: course.newWords as? [VocabularyId] ?? [])) ?? []
    }
}
