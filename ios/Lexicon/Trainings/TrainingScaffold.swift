import SwiftUI

enum AnswerState: Equatable {
    case unanswered
    case correct
    case incorrect(expected: String)
    case skipped(expected: String)

    var isAnswered: Bool { self != .unanswered }

    var tint: Color {
        switch self {
        case .correct: return Palette.success
        case .incorrect: return Palette.failure
        case .skipped: return Palette.warning
        case .unanswered: return .secondary
        }
    }

    var label: String {
        switch self {
        case .correct: return "✓ Correct"
        case .incorrect: return "✗ Incorrect"
        case .skipped: return "Skipped"
        case .unanswered: return ""
        }
    }

    var expected: String? {
        switch self {
        case .incorrect(let expected), .skipped(let expected): return expected
        default: return nil
        }
    }
}

struct SessionTally {
    var correct = 0
    var incorrect = 0
    var skipped = 0
    var tipsUsed = 0

    var total: Int { correct + incorrect + skipped }
    var accuracy: Int { total == 0 ? 0 : Int((Double(correct) * 100 / Double(total)).rounded()) }
}

struct TrainingScaffold<Content: View, Actions: View>: View {
    let step: Int
    let total: Int
    let state: AnswerState
    @ViewBuilder var content: Content
    @ViewBuilder var actions: Actions

    var body: some View {
        VStack(spacing: Spacing.medium) {
            if total > 0 {
                ProgressView(value: Double(step + 1), total: Double(total))
                Text("\(step + 1) / \(total)")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }

            ScrollView {
                VStack(spacing: Spacing.medium) {
                    content

                    if state.isAnswered {
                        VStack(spacing: Spacing.tiny) {
                            Text(state.label).foregroundStyle(state.tint).font(.headline)
                            if let expected = state.expected {
                                Text("Expected: \(expected)").font(.callout).foregroundStyle(.secondary)
                            }
                        }
                    }
                }
                .frame(maxWidth: .infinity)
            }

            actions
        }
        .padding(Spacing.medium)
    }
}

struct SessionResultView: View {
    let tally: SessionTally
    let onDone: () -> Void

    @Environment(\.colorScheme) private var scheme

    var body: some View {
        VStack(spacing: Spacing.large) {
            let skin = TileSkin.standard(highlighted: true, scheme: scheme)
            Tile(skin: skin) {
                Text("\(tally.accuracy)%").font(.system(size: 44, weight: .bold)).foregroundStyle(skin.onTile)
                Text("answered correctly").font(.callout).foregroundStyle(skin.onTile.muted)
                FlowLayout(spacing: Spacing.small) {
                    StatChip(systemName: "checkmark", text: "\(tally.correct)", skin: skin)
                    if tally.incorrect > 0 {
                        StatChip(systemName: "xmark", text: "\(tally.incorrect)", skin: skin)
                    }
                    if tally.skipped > 0 {
                        StatChip(systemName: "arrow.right", text: "\(tally.skipped)", skin: skin)
                    }
                }
            }
            Spacer()
            Button("Done", action: onDone)
                .buttonStyle(.borderedProminent)
                .frame(maxWidth: .infinity)
        }
        .padding(Spacing.medium)
    }
}

/// Shown when a training drew nothing it could ask. The Android app gained the same
/// screen when seven of its trainings were found sitting on a spinner instead.
struct TrainingUnavailableView: View {
    var title = "Nothing to practise here"
    var message = "This training could not build a round from your study set. Try another training, or add a few more words."

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(spacing: Spacing.medium) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 44))
                .foregroundStyle(.secondary)
            Text(title).font(.title3.weight(.semibold)).multilineTextAlignment(.center)
            Text(message)
                .font(.callout)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            Button("Go back") { dismiss() }
                .buttonStyle(.borderedProminent)
                .padding(.top, Spacing.small)
        }
        .padding(Spacing.xl)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
