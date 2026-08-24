import SwiftUI
import Shared

/// The shared model parses the sentence and says which span is the word, so both platforms
/// bold the same characters without either of them reasoning about Polish inflection.
struct ExampleSentenceRow: View {
    let sentence: String
    let word: String
    var tint: Color = .secondary

    private var parsed: ExampleSentence { ExampleSentence.companion.of(sentence: sentence, word: word) }

    var body: some View {
        let example = parsed
        if !example.isBlank {
            HStack(spacing: Spacing.small) {
                Text(attributed(example))
                    .font(.callout)
                    .foregroundStyle(tint)
                Spacer(minLength: 0)
                Button { Speech.shared.speak(example.text) } label: {
                    Image(systemName: "speaker.wave.2").foregroundStyle(tint)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Play the example")
            }
        }
    }

    private func attributed(_ example: ExampleSentence) -> AttributedString {
        var text = AttributedString(example.text)
        for span in example.emphasis {
            let start = Int(span.first)
            let end = Int(span.last)
            guard start >= 0, end >= start, end < example.text.count else { continue }

            let from = text.index(text.startIndex, offsetByCharacters: start)
            let to = text.index(text.startIndex, offsetByCharacters: end + 1)
            text[from..<to].font = .callout.weight(.bold)
        }
        return text
    }
}
