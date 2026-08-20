import SwiftUI
import Shared

/// Read and Write, and Read and Choose. The same passage with the same gaps; the only
/// difference is whether the answers are typed or picked from a bank, which is what the
/// shared session already decides.
struct PassageView: View {
    let withWordBank: Bool

    @Environment(\.dismiss) private var dismiss

    @State private var sessionId = ""
    @State private var passage: Passage?
    @State private var bank: [String] = []
    @State private var answers: [String] = []
    @State private var results: [PassageGapResult] = []
    @State private var unavailable: String?
    @State private var finished = false

    private var gaps: [PassageSegmentGap] { passage?.gaps as? [PassageSegmentGap] ?? [] }
    private var isChecked: Bool { !results.isEmpty }

    private var tally: SessionTally {
        var tally = SessionTally()
        tally.correct = results.filter(\.isCorrect).count
        tally.incorrect = results.count - tally.correct
        return tally
    }

    var body: some View {
        Group {
            if finished {
                SessionResultView(tally: tally) { dismiss() }
            } else if let message = unavailable {
                TrainingUnavailableView(title: "Nothing to read yet", message: message)
            } else if let passage {
                ScrollView {
                    VStack(alignment: .leading, spacing: Spacing.large) {
                        text(passage)
                        if withWordBank && !isChecked { wordBank }
                        if isChecked { review }
                    }
                    .padding(Spacing.medium)
                }
                .safeAreaInset(edge: .bottom) { actions }
            } else {
                ProgressView("Writing your sentences…")
            }
        }
        .task { await start() }
    }

    private var actions: some View {
        HStack {
            Spacer()
            if isChecked {
                Button("Finish") { finished = true }.buttonStyle(.borderedProminent)
            } else {
                AsyncButton { await check() } label: { Text("Check") }
                    .buttonStyle(.borderedProminent)
                    .disabled(answers.contains(where: \.isEmpty))
            }
        }
        .padding(Spacing.medium)
    }

    private func text(_ passage: Passage) -> some View {
        VStack(alignment: .leading, spacing: Spacing.medium) {
            ForEach(Array((passage.sentences as? [PassageSentence] ?? []).enumerated()), id: \.offset) { _, sentence in
                sentenceView(sentence)
            }
        }
    }

    private func sentenceView(_ sentence: PassageSentence) -> some View {
        FlowLayout(spacing: 4) {
            ForEach(Array((sentence.segments as? [PassageSegment] ?? []).enumerated()), id: \.offset) { _, segment in
                segmentView(segment)
            }
        }
    }

    @ViewBuilder
    private func segmentView(_ segment: PassageSegment) -> some View {
        if let text = segment as? PassageSegmentText {
            Text(text.text).font(.body)
        } else if let gap = segment as? PassageSegmentGap, let index = indexOf(gap) {
            gapView(index)
        }
    }

    private func gapView(_ index: Int) -> some View {
        Group {
            if withWordBank {
                Text(answers[index].isEmpty ? "____" : answers[index])
                    .font(.body.weight(.medium))
                    .foregroundStyle(gapColour(index))
                    .padding(.horizontal, Spacing.small)
                    .padding(.vertical, 2)
                    .background(RoundedRectangle(cornerRadius: 6).fill(Color.secondary.opacity(0.12)))
                    .onTapGesture { if !isChecked { answers[index] = "" } }
            } else {
                TextField("____", text: binding(index))
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .fixedSize()
                    .padding(.horizontal, Spacing.small)
                    .padding(.vertical, 2)
                    .background(RoundedRectangle(cornerRadius: 6).fill(Color.secondary.opacity(0.12)))
                    .foregroundStyle(gapColour(index))
                    .disabled(isChecked)
            }
        }
    }

    private func gapColour(_ index: Int) -> Color {
        guard index < results.count else { return .primary }
        return results[index].isCorrect ? Palette.success : Palette.failure
    }

    private var wordBank: some View {
        FlowLayout(spacing: Spacing.small) {
            ForEach(bank, id: \.self) { word in
                Button(word) { place(word) }
                    .font(.callout)
                    .padding(.horizontal, Spacing.medium)
                    .padding(.vertical, Spacing.small)
                    .background(Capsule().fill(Color.accentColor.opacity(usedUp(word) ? 0.08 : 0.18)))
                    .foregroundStyle(usedUp(word) ? Color.secondary : Color.accentColor)
                    .disabled(usedUp(word))
            }
        }
    }

    private var review: some View {
        VStack(alignment: .leading, spacing: Spacing.small) {
            ForEach(Array(results.enumerated()), id: \.offset) { _, result in
                HStack(alignment: .firstTextBaseline, spacing: Spacing.small) {
                    Image(systemName: result.isCorrect ? "checkmark.circle.fill" : "xmark.circle.fill")
                        .foregroundStyle(result.isCorrect ? Palette.success : Palette.failure)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(result.expected).font(.callout.weight(.medium))
                        if !result.translation.isEmpty {
                            Text(result.translation).font(.caption).foregroundStyle(.secondary)
                        }
                        if !result.isCorrect && !result.submitted.isEmpty {
                            Text("You wrote \(result.submitted)").font(.caption).foregroundStyle(.secondary)
                        }
                    }
                }
            }
        }
    }

    private func binding(_ index: Int) -> Binding<String> {
        Binding(get: { answers[index] }, set: { answers[index] = $0 })
    }

    private func indexOf(_ gap: PassageSegmentGap) -> Int? {
        gaps.firstIndex { $0 === gap }
    }

    private func usedUp(_ word: String) -> Bool {
        let offered = bank.filter { $0 == word }.count
        return answers.filter { $0 == word }.count >= offered
    }

    private func place(_ word: String) {
        guard let next = answers.firstIndex(where: \.isEmpty) else { return }
        answers[next] = word
    }

    private func start() async {
        guard passage == nil else { return }
        let request = StartPassageSessionRequest(withWordBank: withWordBank, stepCount: nil)
        let result = try? await deps.startPassage.invoke(request: request)
        switch result {
        case let ready as PassageSessionResultReady:
            sessionId = ready.sessionId
            passage = ready.passage
            bank = ready.bank as? [String] ?? []
            answers = Array(repeating: "", count: (ready.passage.gaps as? [PassageSegmentGap] ?? []).count)
        case is PassageSessionResultOffline:
            unavailable = "The sentences are written on demand and the network is not answering."
        case let refused as PassageSessionResultRefused:
            unavailable = refused.reason
        default:
            unavailable = "Add a few words to your study set and this will have something to write about."
        }
    }

    private func check() async {
        let request = SubmitPassageAnswersRequest(
            sessionId: sessionId,
            expected: gaps.map(\.answer),
            answers: answers,
            words: gaps.map(\.word)
        )
        guard let response = try? await deps.submitPassage.invoke(request: request) else { return }
        results = response.results as? [PassageGapResult] ?? []
    }
}

#Preview("Read and Write") {
    NavigationStack { PassageView(withWordBank: false) }
}
