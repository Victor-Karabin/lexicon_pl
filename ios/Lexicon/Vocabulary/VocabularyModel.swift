import SwiftUI
import Shared

@MainActor
final class VocabularyModel: ObservableObject {
    @Published var query: String = ""
    @Published private(set) var levels: Set<CefrLevel> = []
    @Published private(set) var presets: [VocabularyPreset] = []
    @Published private(set) var words: [Word] = []
    @Published private(set) var studySet: Set<Int64> = []
    @Published private(set) var isLoadingMoreWords = false
    @Published private(set) var hasMoreWords = true

    private var watcher: Cancellable?

    let allLevels: [CefrLevel] = [.a1, .a2, .b1, .b2, .c1, .c2]

    init() {
        watcher = deps.watchStudySetWordIds { [weak self] ids in
            self?.studySet = Set(ids.compactMap { ($0 as? VocabularyId)?.value })
        }
    }

    deinit { watcher?.cancel() }

    func load() async {
        presets = (try? await deps.getPresets.invoke()) ?? []
        await search()
    }

    /// Paged the way Android is: a level can be hundreds of words, and drawing them all
    /// before showing the first is what made this slow.
    func search() async {
        guard !query.isEmpty || !levels.isEmpty else {
            words = []
            hasMoreWords = true
            return
        }
        let first = await page(skip: 0)
        words = first
        hasMoreWords = first.count >= Int(SearchVocabularyUseCaseCompanion.shared.PAGE)
    }

    func moreWords() async {
        guard hasMoreWords, !isLoadingMoreWords, !words.isEmpty else { return }
        isLoadingMoreWords = true
        defer { isLoadingMoreWords = false }

        let more = await page(skip: words.count)
        let known = Set(words.map(\.id.value))
        words += more.filter { !known.contains($0.id.value) }
        hasMoreWords = more.count >= Int(SearchVocabularyUseCaseCompanion.shared.PAGE)
    }

    private func page(skip: Int) async -> [Word] {
        (try? await deps.searchVocabulary.invoke(
            query: query,
            levels: levels,
            limit: SearchVocabularyUseCaseCompanion.shared.PAGE,
            skip: Int32(skip)
        )) ?? []
    }

    func toggleLevel(_ level: CefrLevel) async {
        if levels.contains(level) { levels.remove(level) } else { levels.insert(level) }
        await search()
    }

    func isInStudySet(_ word: Word) -> Bool { studySet.contains(word.id.value) }

    func toggleInStudySet(_ preset: VocabularyPreset) async {
        let wanted = preset.studySetState != PresetStudySetState.all
        try? await deps.setPresetInStudySet.invoke(id: preset.id, isInStudySet: wanted)
        presets = (try? await deps.getPresets.invoke()) ?? presets
    }

    func toggleInStudySet(_ word: Word) async {
        try? await deps.toggleWordInStudySet.invoke(id: word.id, isInStudySet: !isInStudySet(word))
    }

    func name(of level: CefrLevel) -> String { level.name }

    @Published private(set) var selected: Set<Int64> = []
    @Published private(set) var isSelecting = false

    func isSelected(_ word: Word) -> Bool { selected.contains(word.id.value) }

    func startSelecting(_ word: Word) {
        isSelecting = true
        selected = [word.id.value]
    }

    func toggleSelected(_ word: Word) {
        if selected.contains(word.id.value) {
            selected.remove(word.id.value)
        } else {
            selected.insert(word.id.value)
        }

        if selected.isEmpty { isSelecting = false }
    }

    func stopSelecting() {
        isSelecting = false
        selected = []
    }

    func delete(_ word: Word) async {
        try? await deps.deleteWord.invoke(id: word.id)
        await search()
    }

    func deleteSelected() async {
        for id in selected {
            try? await deps.deleteWord.invoke(id: VocabularyId(value: id))
        }
        stopSelecting()
        await search()
    }
}

extension VocabularyPreset {

    var accentColor: Color {
        guard let hex = color?.replacingOccurrences(of: "#", with: ""),
              let value = UInt32(hex, radix: 16) else { return Palette.accentDeep }
        return Color(
            red: Double((value >> 16) & 0xFF) / 255,
            green: Double((value >> 8) & 0xFF) / 255,
            blue: Double(value & 0xFF) / 255
        )
    }

    var onAccentColor: Color {
        guard let hex = color?.replacingOccurrences(of: "#", with: ""),
              let value = UInt32(hex, radix: 16) else { return .white }
        let r = Double((value >> 16) & 0xFF) / 255
        let g = Double((value >> 8) & 0xFF) / 255
        let b = Double(value & 0xFF) / 255
        let luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b
        return luminance > 0.34 ? .black : .white
    }

    var symbolName: String { VocabularyPreset.symbolName(forIcon: icon) }

    static func symbolName(forIcon icon: String?) -> String {
        switch icon {
        case "restaurant", "local_pizza", "bakery_dining", "set_meal", "egg": return "fork.knife"
        case "trending_up": return "chart.line.uptrend.xyaxis"
        case "record_voice_over", "campaign": return "person.wave.2"
        case "home", "hotel", "bed", "chair": return "house"
        case "directions_car", "directions_bus", "train", "subway", "flight", "sailing": return "car"
        case "school", "auto_stories", "menu_book": return "graduationcap"
        case "work", "business_center", "engineering": return "briefcase"
        case "favorite", "heart_broken": return "heart"
        case "pets", "emoji_nature", "forest", "park", "nature": return "leaf"
        case "sports_soccer", "sports_basketball", "sports_tennis", "fitness_center": return "sportscourt"
        case "local_hospital", "health_and_safety", "medication": return "cross.case"
        case "shopping_cart", "storefront", "payments", "attach_money", "savings": return "cart"
        case "watch", "schedule": return "clock"
        case "wb_sunny", "cloud", "thunderstorm", "ac_unit": return "sun.max"
        case "family_restroom", "groups", "child_care", "elderly": return "person.3"
        case "language", "translate": return "character.book.closed"
        case "music_note", "library_music", "piano": return "music.note"
        case "palette", "brush": return "paintpalette"
        case "computer", "smartphone", "code": return "desktopcomputer"
        default: return "square.grid.2x2"
        }
    }
}
