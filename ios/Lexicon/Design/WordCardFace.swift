import SwiftUI

struct WordCardFace: View {
    let text: String
    let translation: String
    let transcription: String
    let imageUrl: String?
    let example: String
    var onEdit: (() -> Void)?

    @Environment(\.colorScheme) private var scheme

    var body: some View {
        let skin = TileSkin.standard(highlighted: true, scheme: scheme)
        Tile(skin: skin) {
            if let url = imageUrl, let link = imageURL(url) {
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
                    Text(translation).font(.title3).foregroundStyle(skin.onTile.muted)
                    Text(text).font(.largeTitle.weight(.semibold)).foregroundStyle(skin.onTile)
                    if !transcription.isEmpty {
                        Text("[\(transcription)]").font(.callout).foregroundStyle(skin.onTile.muted)
                    }
                }
                Spacer()
                Button { Speech.shared.speak(text) } label: {
                    Image(systemName: "speaker.wave.2").foregroundStyle(skin.onTile)
                }
                if let onEdit {
                    Button(action: onEdit) {
                        Image(systemName: "pencil").foregroundStyle(skin.onTile)
                    }
                    .accessibilityLabel(Strings.cardsEdit)
                }
            }
            ExampleSentenceRow(sentence: example, word: text, tint: skin.onTile.muted)
        }
    }
}
