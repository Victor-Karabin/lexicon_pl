import SwiftUI
import Shared

struct WordFormView: View {
    let wordId: Int64?

    @Environment(\.dismiss) private var dismiss
    @State private var text = ""
    @State private var translation = ""
    @State private var example = ""
    @State private var images: [String] = []

    @State private var ownImages: [String] = []
    @State private var chosenImage: String?
    @State private var problem: String?
    @State private var textWasFilledIn = false
    @State private var translationWasFilledIn = false
    @State private var memberships: [PresetMembership]?

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
            Section("Example") {
                ExampleSentenceRow(sentence: example, word: text)
                TextField(
                    "A sentence using the word",
                    text: Binding(get: { deps.editableExample(marked: example) }, set: { example = $0 }),
                    axis: .vertical
                )
                    .lineLimit(2...4)
            }

            Section("Picture") {

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: Spacing.small) {
                        AddImageTile { url in

                            ownImages = ([url] + ownImages).uniqued()
                            chosenImage = url
                        }
                        ForEach(ownImages + images, id: \.self) { url in
                            AsyncImage(url: imageURL(url)) { image in
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
                if images.isEmpty {
                    Text("Type a word to look for a picture.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
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
        example = word.example
        memberships = await presetMemberships(of: wordId)

        chosenImage = try? await deps.getPinnedImage.invoke(translation: word.translation)
        if let pinned = chosenImage, isOwnImage(pinned) { ownImages = [pinned] }
        await lookUpImages(for: word.translation)
    }

    private func fillPolish(from english: String) async {
        guard !english.isEmpty else { return }
        await lookUpImages(for: english)
        guard text.isEmpty || textWasFilledIn else { return }

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

    private func presetMemberships(of wordId: Int64) async -> [PresetMembership]? {
        (try? await deps.getWordPresetMemberships.invoke(wordId: VocabularyId(value: wordId))) as? [PresetMembership]
    }

    private func save() async {
        do {
            if let wordId {
                var known = memberships
                if known == nil { known = await presetMemberships(of: wordId) }
                guard let known else {
                    problem = "The word's presets could not be read, so nothing was saved. Try again."
                    return
                }
                _ = try await deps.updateWord.invoke(
                    id: VocabularyId(value: wordId),
                    text: text,
                    translation: translation,
                    imageUrl: chosenImage,
                    example: example,
                    presetIds: known.filter { $0.isMember }.map { $0.preset.id }
                )
            } else {
                _ = try await deps.createWord.invoke(
                    text: text,
                    translation: translation,
                    imageUrl: chosenImage,
                    example: example,
                    presetIds: []
                )
            }
            dismiss()
        } catch {
            problem = "That word could not be saved. Check both fields are filled in."
        }
    }
}

private extension Array where Element: Hashable {

    func uniqued() -> [Element] {
        var seen = Set<Element>()
        return filter { seen.insert($0).inserted }
    }
}
