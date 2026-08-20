import SwiftUI
import Shared

struct CrosswordView: View {
    let vocabularyIds: [Int64]

    @Environment(\.dismiss) private var dismiss
    @State private var session: CrosswordSessionResponse?
    @State private var answers: [Int64: String] = [:]
    @State private var focused: Int64?
    @State private var results: [Int64: StepOutcome] = [:]
    @State private var tally = SessionTally()
    @State private var finished = false

    var body: some View {
        Group {
            if finished {
                SessionResultView(tally: tally) { dismiss() }
            } else if let session {
                ScrollView {
                    VStack(spacing: Spacing.medium) {
                        grid(session)
                        clues(session)
                    }
                    .padding(Spacing.medium)
                }
                .safeAreaInset(edge: .bottom) {
                    AsyncButton { await check(session) } label: {
                        Text("Check").frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .padding(Spacing.medium)
                }
            } else {
                ProgressView()
            }
        }
        .task { await start() }
    }

    private func grid(_ session: CrosswordSessionResponse) -> some View {
        let rows = Int(session.rowCount)
        let cols = Int(session.colCount)
        return VStack(spacing: 2) {
            ForEach(0..<rows, id: \.self) { row in
                HStack(spacing: 2) {
                    ForEach(0..<cols, id: \.self) { col in
                        cell(session, row: row, col: col)
                    }
                }
            }
        }
    }

    private func cell(_ session: CrosswordSessionResponse, row: Int, col: Int) -> some View {
        let letter = letterAt(session, row: row, col: col)
        return ZStack {
            RoundedRectangle(cornerRadius: 2)
                .fill(letter == nil ? Color.clear : Color.secondary.opacity(0.15))
            if let letter {
                Text(letter).font(.caption.monospaced())
            }
        }
        .frame(width: 30, height: 30)
    }

    private func letterAt(_ session: CrosswordSessionResponse, row: Int, col: Int) -> String? {
        for placement in session.words {
            let typed = Array(answers[placement.vocabularyItemId] ?? "")
            let length = placement.expectedText.count
            for offset in 0..<length {
                let r = placement.direction == .down ? Int(placement.row) + offset : Int(placement.row)
                let c = placement.direction == .across ? Int(placement.col) + offset : Int(placement.col)
                if r == row && c == col {
                    return offset < typed.count ? String(typed[offset]) : ""
                }
            }
        }
        return nil
    }

    private func clues(_ session: CrosswordSessionResponse) -> some View {
        VStack(alignment: .leading, spacing: Spacing.small) {
            ForEach(session.words, id: \.vocabularyItemId) { placement in
                VStack(alignment: .leading, spacing: 2) {
                    Text("\(placement.direction == .across ? "Across" : "Down") · \(placement.expectedText.count) letters")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    HStack {
                        Text(placement.clueText).font(.callout)
                        Spacer()
                        if let outcome = results[placement.vocabularyItemId] {
                            Image(systemName: outcome == .correct ? "checkmark" : "xmark")
                                .foregroundStyle(outcome == .correct ? Palette.success : Palette.failure)
                        }
                    }
                    TextField("", text: Binding(
                        get: { answers[placement.vocabularyItemId] ?? "" },
                        set: { answers[placement.vocabularyItemId] = $0.lowercased() }
                    ))
                    .textFieldStyle(.roundedBorder)
                    .autocorrectionDisabled()
                    .textInputAutocapitalization(.never)
                }
            }
        }
    }

    private func start() async {
        session = try? await deps.startCrossword.invoke(
            request: StartCrosswordSessionRequest(
                wordCount: StartCrosswordSessionRequest.companion.DEFAULT_WORD_COUNT,
                vocabularyIds: vocabularyIds.map { KotlinLong(value: $0) }
            )
        )
    }

    private func check(_ session: CrosswordSessionResponse) async {
        let submissions = session.words.map { placement in
            CrosswordWordSubmission(
                vocabularyItemId: placement.vocabularyItemId,
                expectedText: placement.expectedText,
                submittedText: answers[placement.vocabularyItemId] ?? "",
                tipUsed: false
            )
        }
        let response = try? await deps.submitCrossword.invoke(
            request: SubmitCrosswordRequest(sessionId: session.sessionId, words: submissions)
        )
        guard let response else { return }
        for result in response.wordResults {
            results[result.vocabularyItemId] = result.outcome
            if result.outcome == .correct { tally.correct += 1 } else { tally.incorrect += 1 }
        }
        finished = true
    }
}
