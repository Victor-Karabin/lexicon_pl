import SwiftUI
import Shared

struct WordSearchView: View {
    @Environment(\.dismiss) private var dismiss

    @State private var puzzle: FillwordPuzzle?
    @State private var emptyStudySet = false
    @State private var found: Set<String> = []
    @State private var revealed: Set<String> = []
    @State private var dragFrom: FillwordCell?
    @State private var dragTo: FillwordCell?
    @State private var checked = false
    @State private var finished = false

    private var words: [FillwordWord] { puzzle?.words as? [FillwordWord] ?? [] }
    private var remaining: [FillwordWord] { words.filter { !found.contains($0.word) } }

    private var tally: SessionTally {
        var tally = SessionTally()
        tally.correct = found.count
        tally.incorrect = revealed.count
        return tally
    }

    var body: some View {
        Group {
            if finished {
                SessionResultView(tally: tally) { dismiss() }
            } else if emptyStudySet {
                TrainingUnavailableView()
            } else if let puzzle {
                ScrollView {
                    VStack(spacing: Spacing.large) {
                        grid(puzzle)
                        legend(puzzle)
                    }
                    .padding(Spacing.medium)
                }
                .safeAreaInset(edge: .bottom) { actions }
            } else {
                ProgressView()
            }
        }
        .task { await start() }
    }

    private var actions: some View {
        HStack {
            Spacer()
            if remaining.isEmpty || checked {
                Button("Finish") { finished = true }.buttonStyle(.borderedProminent)
            } else {
                Button("Check") { reveal() }.buttonStyle(.borderedProminent)
            }
        }
        .padding(Spacing.medium)
    }

    private func grid(_ puzzle: FillwordPuzzle) -> some View {
        let size = Int(puzzle.size)
        return GeometryReader { proxy in
            let side = proxy.size.width / CGFloat(size)
            VStack(spacing: 0) {
                ForEach(0..<size, id: \.self) { row in
                    HStack(spacing: 0) {
                        ForEach(0..<size, id: \.self) { column in
                            letter(puzzle, row: row, column: column, side: side)
                        }
                    }
                }
            }
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { value in
                        dragFrom = dragFrom ?? cell(at: value.startLocation, side: side, size: size)
                        dragTo = cell(at: value.location, side: side, size: size)
                    }
                    .onEnded { _ in finishDrag(puzzle) }
            )
        }
        .aspectRatio(1, contentMode: .fit)
    }

    private func letter(_ puzzle: FillwordPuzzle, row: Int, column: Int, side: CGFloat) -> some View {
        let cell = FillwordCell(row: Int32(row), column: Int32(column))
        return Text(puzzle.letterAt(cell: cell).uppercased())
            .font(.system(size: max(11, side * 0.42), weight: .medium, design: .rounded))
            .frame(width: side, height: side)
            .background(background(for: cell, puzzle: puzzle))
            .foregroundStyle(isTaken(cell, puzzle: puzzle) ? Color.white : Color.primary)
    }

    private func background(for cell: FillwordCell, puzzle: FillwordPuzzle) -> some View {
        Rectangle().fill(colour(for: cell, puzzle: puzzle))
    }

    private func colour(for cell: FillwordCell, puzzle: FillwordPuzzle) -> Color {
        if isIn(revealed, cell: cell, puzzle: puzzle) { return Palette.failure }
        if isIn(found, cell: cell, puzzle: puzzle) { return Palette.success }
        if isDragging(cell) { return Color.accentColor.opacity(0.35) }
        return Color.secondary.opacity(0.08)
    }

    private func isTaken(_ cell: FillwordCell, puzzle: FillwordPuzzle) -> Bool {
        isIn(found, cell: cell, puzzle: puzzle) || isIn(revealed, cell: cell, puzzle: puzzle)
    }

    private func isIn(_ names: Set<String>, cell: FillwordCell, puzzle: FillwordPuzzle) -> Bool {
        words.contains { names.contains($0.word) && ($0.cells as? [FillwordCell] ?? []).contains(cell) }
    }

    private func isDragging(_ cell: FillwordCell) -> Bool {
        guard let puzzle, let from = dragFrom, let to = dragTo else { return false }
        return (puzzle.runBetween(from: from, to: to) as? [FillwordCell] ?? []).contains(cell)
    }

    private func cell(at point: CGPoint, side: CGFloat, size: Int) -> FillwordCell? {
        let column = Int(point.x / side)
        let row = Int(point.y / side)
        guard row >= 0, row < size, column >= 0, column < size else { return nil }
        return FillwordCell(row: Int32(row), column: Int32(column))
    }

    private func finishDrag(_ puzzle: FillwordPuzzle) {
        defer { dragFrom = nil; dragTo = nil }
        guard let from = dragFrom, let to = dragTo else { return }
        guard let hit = puzzle.wordAlong(from: from, to: to) else { return }
        found.insert(hit.word)
    }

    private func legend(_ puzzle: FillwordPuzzle) -> some View {
        FlowLayout(spacing: Spacing.small) {
            ForEach(words, id: \.word) { word in
                Text(puzzle.translationOf(word: word))
                    .font(.callout)
                    .strikethrough(found.contains(word.word))
                    .foregroundStyle(colour(for: word))
                    .padding(.horizontal, Spacing.small)
                    .padding(.vertical, 4)
                    .background(Capsule().fill(Color.secondary.opacity(0.12)))
            }
        }
    }

    private func colour(for word: FillwordWord) -> Color {
        if found.contains(word.word) { return Palette.success }
        if revealed.contains(word.word) { return Palette.failure }
        return .primary
    }

    /// Shows what was missed rather than leaving the learner stuck, and marks it wrong.
    private func reveal() {
        revealed = Set(remaining.map(\.word))
        checked = true
    }

    private func start() async {
        guard puzzle == nil else { return }
        let result = try? await deps.startFillword.invoke()
        switch result {
        case let ready as FillwordSessionResultReady: puzzle = ready.puzzle
        default: emptyStudySet = true
        }
    }
}

#Preview("Word Search") {
    NavigationStack { WordSearchView() }
}
