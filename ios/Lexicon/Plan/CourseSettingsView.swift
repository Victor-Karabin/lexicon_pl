import SwiftUI
import Shared

struct CourseSettingsView: View {
    @Environment(\.colorScheme) private var scheme

    @State private var newWords = 10
    @State private var reviews = 15
    @State private var queue: [String] = []
    @State private var loaded = false
    @State private var keptLastTraining = false

    @State private var draggedFrom: Int?
    @State private var dragOffset: CGFloat = 0
    @State private var rowHeight: CGFloat = 0

    private let maxWordsADay = 50

    var body: some View {
        Group {
            if loaded {
                form
            } else {
                ProgressView()
            }
        }
        .navigationTitle("Vocabulary course")
        .navigationBarTitleDisplayMode(.inline)
        .task { await load() }
        .onDisappear { Task { await save() } }
    }

    private var form: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Spacing.medium) {
                let skin = TileSkin.standard(scheme: scheme)

                Text("Words you mark To learn come first, favourites before the rest. Reviews are picked at random from the words you know.")
                    .font(.callout)
                    .foregroundStyle(.secondary)

                slider("New words a day", value: $newWords, range: 1...maxWordsADay)
                slider("Reviews a day", value: $reviews, range: 0...maxWordsADay)

                Text("Trainings").font(.subheadline.weight(.semibold))

                FlowLayout(spacing: Spacing.small) {
                    ForEach(TrainingCatalog.all) { entry in
                        Button {
                            queue.append(entry.id)
                            keptLastTraining = false
                        } label: {
                            Label(entry.name, systemImage: entry.symbol)
                                .font(.caption)
                                .padding(.horizontal, Spacing.small)
                                .padding(.vertical, Spacing.tiny)
                                .overlay(Capsule().stroke(Color.secondary.opacity(0.4)))
                        }
                        .buttonStyle(.plain)
                    }
                }

                Text("Queue").font(.subheadline.weight(.semibold))
                Text(
                    keptLastTraining
                        ? "The queue needs at least one training, so the last one stays."
                        : "\(queue.count) trainings, in this order, over and over"
                )
                .font(.caption).foregroundStyle(.secondary)

                ForEach(queue.indices, id: \.self) { i in
                    queueRow(i, skin: skin)
                        .background(
                            GeometryReader { proxy in
                                Color.clear.preference(key: RowHeightKey.self, value: proxy.size.height)
                            }
                        )
                        .offset(y: rowShift(i))
                        .zIndex(draggedFrom == i ? 1 : 0)
                        .gesture(reorder(i))
                }
                .onPreferenceChange(RowHeightKey.self) { rowHeight = $0 }
            }
            .padding(Spacing.medium)
        }
    }

    private func queueRow(_ i: Int, skin: TileSkin) -> some View {

        Tile(skin: skin, padding: Spacing.small) {
            HStack(spacing: Spacing.medium) {
                Image(systemName: "line.3.horizontal")
                    .foregroundStyle(draggedFrom == i ? skin.onTile : skin.onTile.muted)
                Medallion(skin: skin, size: 32) { MedallionText(text: "\(i + 1)", skin: skin) }
                Text(TrainingCatalog.entry(id: queue[i])?.name ?? queue[i]).foregroundStyle(skin.onTile)
                Spacer()
                Button { remove(i) } label: { Image(systemName: "xmark") }
            }
            .foregroundStyle(skin.onTile)
        }

        .accessibilityAction(named: "Move earlier") { move(i, by: -1) }
        .accessibilityAction(named: "Move later") { move(i, by: 1) }
    }

    private var step: CGFloat { rowHeight + Spacing.medium }

    private func landing(_ from: Int) -> Int {
        guard step > 0, !queue.isEmpty else { return from }
        return min(max(from + Int((dragOffset / step).rounded()), 0), queue.count - 1)
    }

    private func rowShift(_ i: Int) -> CGFloat {
        guard let from = draggedFrom else { return 0 }
        if i == from { return dragOffset }
        let to = landing(from)
        if to > from, i > from, i <= to { return -step }
        if to < from, i >= to, i < from { return step }
        return 0
    }

    private func reorder(_ i: Int) -> some Gesture {
        LongPressGesture(minimumDuration: 0.3)
            .sequenced(before: DragGesture())
            .onChanged { value in
                guard case .second(true, let drag?) = value else { return }
                if draggedFrom == nil { draggedFrom = i }
                dragOffset = drag.translation.height
            }
            .onEnded { _ in
                if let from = draggedFrom {
                    let to = landing(from)
                    if to != from { move(from, by: to - from) }
                }
                draggedFrom = nil
                dragOffset = 0
            }
    }

    @ViewBuilder
    private func slider(_ title: String, value: Binding<Int>, range: ClosedRange<Int>) -> some View {
        VStack(alignment: .leading) {
            HStack {
                Text(title)
                Spacer()
                Text("\(value.wrappedValue)").bold()
            }
            Slider(
                value: Binding(get: { Double(value.wrappedValue) }, set: { value.wrappedValue = Int($0.rounded()) }),
                in: Double(range.lowerBound)...Double(range.upperBound),
                step: 1
            )
        }
    }

    private func move(_ from: Int, by: Int) {
        let to = from + by
        guard queue.indices.contains(from), queue.indices.contains(to) else { return }
        withAnimation(.snappy) { queue.insert(queue.remove(at: from), at: to) }
    }

    private func remove(_ i: Int) {
        guard queue.indices.contains(i) else { return }
        guard queue.count > 1 else {
            keptLastTraining = true
            return
        }
        withAnimation(.snappy) { _ = queue.remove(at: i) }
    }

    private func load() async {
        guard let course = try? await deps.getVocabularyCourse.invoke() else { return }
        newWords = min(max(Int(course.settings.newWordsADay), 1), maxWordsADay)
        reviews = min(max(Int(course.settings.reviewsADay), 0), maxWordsADay)
        queue = course.settings.queue
        loaded = true
    }

    private func save() async {
        guard loaded else { return }
        let settings = CourseSettings(
            newWordsADay: Int32(newWords),
            reviewsADay: Int32(reviews),
            queue: queue
        )
        try? await deps.updateCourseSettings.invoke(settings: settings)
    }
}

struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let width = proposal.width ?? .infinity
        var x: CGFloat = 0, y: CGFloat = 0, lineHeight: CGFloat = 0
        for view in subviews {
            let size = view.sizeThatFits(.unspecified)
            if x + size.width > width {
                x = 0
                y += lineHeight + spacing
                lineHeight = 0
            }
            x += size.width + spacing
            lineHeight = max(lineHeight, size.height)
        }
        return CGSize(width: width, height: y + lineHeight)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX, y = bounds.minY, lineHeight: CGFloat = 0
        for view in subviews {
            let size = view.sizeThatFits(.unspecified)
            if x + size.width > bounds.maxX {
                x = bounds.minX
                y += lineHeight + spacing
                lineHeight = 0
            }
            view.place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(size))
            x += size.width + spacing
            lineHeight = max(lineHeight, size.height)
        }
    }
}

private struct RowHeightKey: PreferenceKey {
    static var defaultValue: CGFloat = 0

    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = max(value, nextValue())
    }
}
