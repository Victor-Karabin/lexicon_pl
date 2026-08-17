import SwiftUI
import Shared

struct ProgramFormView: View {
    let programId: ProgramId?

    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var scheme

    @State private var favourites = 0
    @State private var newWords = 10
    @State private var reviewWords = 10
    @State private var queue: [String] = []
    @State private var isEnrolled = false
    @State private var loaded = false

    @State private var draggedFrom: Int?
    @State private var dragOffset: CGFloat = 0
    @State private var rowHeight: CGFloat = 0

    private let minimumNewWords = 10

    var body: some View {
        Group {
            if !loaded {
                ProgressView()
            } else if favourites == 0 {
                VStack(spacing: Spacing.large) {
                    Text("Nothing starred yet. Add words to your study set in the Words tab first.")
                        .multilineTextAlignment(.center)
                        .foregroundStyle(.secondary)
                }
                .padding(Spacing.xl)
            } else {
                form
            }
        }
        .navigationTitle(programId == nil ? "New program" : "Edit program")
        .navigationBarTitleDisplayMode(.inline)
        .task { await load() }
    }

    private var form: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Spacing.medium) {
                let skin = TileSkin.standard(scheme: scheme)

                Tile(skin: skin) {
                    Text("Over the words in your study set").foregroundStyle(skin.onTile)
                    StatChip(systemName: "character.book.closed", text: "\(favourites) words", skin: skin)
                }

                slider("New words a day", value: $newWords, range: minimumNewWords...max(favourites, minimumNewWords))
                slider("Reviews a day", value: $reviewWords, range: 0...max(favourites, minimumNewWords))

                Text("Trainings").font(.subheadline.weight(.semibold))

                FlowLayout(spacing: Spacing.small) {
                    ForEach(TrainingCatalog.all.filter { $0.id != "memory_cards" && $0.id != "crossword" }) { entry in
                        Button { queue.append(entry.id) } label: {
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
                if queue.isEmpty {
                    Text("Nothing yet. Tap a training above to add a turn at it.")
                        .font(.caption).foregroundStyle(.secondary)
                } else {
                    Text("\(queue.count) turns a day, in this order")
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
            }
            .padding(Spacing.medium)
        }
        .safeAreaInset(edge: .bottom) {
            VStack(spacing: Spacing.small) {
                if programId != nil {
                    AsyncButton {
                        guard let programId else { return }
                        if isEnrolled {
                            try? await deps.leaveProgram.invoke(id: programId)
                        } else {
                            _ = try? await deps.enrolInProgram.invoke(id: programId)
                        }
                        isEnrolled.toggle()
                    } label: {
                        Text(isEnrolled ? "In progress · tap to stop" : "Start this program")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                }
                if queue.isEmpty {
                    Text("Add at least one training to save.").font(.caption).foregroundStyle(.secondary)
                }
                AsyncButton { await save() } label: { Text("Save").frame(maxWidth: .infinity) }
                    .buttonStyle(.borderedProminent)
                    .disabled(queue.isEmpty)
            }
            .padding(Spacing.medium)
            .background(.bar)
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
                Button { queue.remove(at: i) } label: { Image(systemName: "xmark") }
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
            if range.upperBound > range.lowerBound {
                Slider(
                    value: Binding(get: { Double(value.wrappedValue) }, set: { value.wrappedValue = Int($0.rounded()) }),
                    in: Double(range.lowerBound)...Double(range.upperBound),
                    step: 1
                )
            } else {
                Text("Every word in your study set.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
    }

    private func move(_ from: Int, by: Int) {
        let to = from + by
        guard queue.indices.contains(from), queue.indices.contains(to) else { return }
        withAnimation(.snappy) { queue.insert(queue.remove(at: from), at: to) }
    }

    private func load() async {
        favourites = Int((try? await deps.countFavourites.invoke()) as? Int32 ?? 0)
        if let programId, let program = try? await deps.getProgram.invoke(id: programId) {
            let plan = program.config.dailyPlan
            newWords = min(max(Int(plan.newWords), minimumNewWords), max(favourites, minimumNewWords))
            reviewWords = min(Int(plan.reviewWords), max(favourites, minimumNewWords))
            queue = plan.queue
            let active = try? await deps.observeActiveEnrolmentFirst()
            isEnrolled = active?.programId.value == programId.value
        }
        loaded = true
    }

    private func save() async {
        let draft = ProgramDraft(
            title: "My program",
            description: "Over the words in your study set",
            newWordsPerDay: Int32(newWords),
            reviewWordsPerDay: Int32(reviewWords),
            trainings: queue
        )
        if let programId {
            _ = try? await deps.updateProgram.invoke(id: programId, draft: draft)
        } else {
            _ = try? await deps.createProgram.invoke(draft: draft)
        }
        dismiss()
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
