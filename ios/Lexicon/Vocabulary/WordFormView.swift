import SwiftUI
import Shared

/// Writing a word by hand, or correcting one that exists.
///
/// The Polish side fills itself in as the English is typed and the other way round —
/// but only over what the app itself put there, never over what was typed.
struct WordFormView: View {
    let wordId: Int64?

    @Environment(\.dismiss) private var dismiss
    @State private var text = ""
    @State private var translation = ""
    @State private var images: [String] = []
    @State private var chosenImage: String?
    @State private var problem: String?
    @State private var textWasFilledIn = false
    @State private var translationWasFilledIn = false

    var body: some View {
        Form {
            Section("English") {
                TextField("English", text: $translation)
                    .onChange(of: translation) { _, value in
                        Task { await fillPolish(from: value) }
                    }
            }
            Section("Polish") {
                TextField("Polish", text: $text)
                    .onChange(of: text) { _, _ in textWasFilledIn = false }
            }
            Section("Picture") {
                if images.isEmpty {
                    Text("Type a word to look for a picture.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                } else {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: Spacing.small) {
                            ForEach(images, id: \.self) { url in
                                AsyncImage(url: URL(string: url)) { image in
                                    image.resizable().scaledToFill()
                                } placeholder: {
                                    Color.secondary.opacity(0.2)
                                }
                                .frame(width: 96, height: 96)
                                .clipShape(RoundedRectangle(cornerRadius: Radius.small))
                                .overlay(
                                    RoundedRectangle(cornerRadius: Radius.small)
                                        .stroke(chosenImage == url ? Palette.accentDeep : .clear, lineWidth: 3)
                                )
                                .onTapGesture { chosenImage = url }
                            }
                        }
                    }
                }
            }
            if let problem {
                Text(problem).foregroundStyle(Palette.failure)
            }
        }
        .navigationTitle(wordId == nil ? "New word" : "Edit word")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                AsyncButton { await save() } label: { Text("Save") }
            }
        }
        .task { await load() }
    }

    private func load() async {
        guard let wordId else { return }
        guard let word = try? await deps.getWord.invoke(id: VocabularyId(value: wordId)) else { return }
        text = word.text
        translation = word.translation
        // The picture the word already has is the one the trainings show, so it is
        // the one that arrives already chosen.
        chosenImage = try? await deps.getPinnedImage.invoke(translation: word.translation)
        await lookUpImages(for: word.translation)
    }

    /// Fills the Polish side in, and only over a value the app put there itself.
    private func fillPolish(from english: String) async {
        guard !english.isEmpty else { return }
        await lookUpImages(for: english)
        guard text.isEmpty || textWasFilledIn else { return }
        // `try?` on a throwing call that already returns an optional gives a double
        // optional; one flatten and it reads as what it is.
        let filled = try? await deps.translateWord.invoke(text: english, toPolish: true)
        if let filled, !filled.isEmpty {
            text = filled
            textWasFilledIn = true
        }
    }

    private func lookUpImages(for query: String) async {
        images = (try? await deps.searchImageCandidates.invoke(query: query, skip: 0)) ?? []
        if chosenImage == nil { chosenImage = images.first }
    }

    private func save() async {
        do {
            if let wordId {
                _ = try await deps.updateWord.invoke(
                    id: VocabularyId(value: wordId),
                    text: text,
                    translation: translation,
                    imageUrl: chosenImage,
                    presetIds: []
                )
            } else {
                _ = try await deps.createWord.invoke(
                    text: text,
                    translation: translation,
                    imageUrl: chosenImage,
                    presetIds: []
                )
            }
            dismiss()
        } catch {
            problem = "That word could not be saved. Check both fields are filled in."
        }
    }
}
