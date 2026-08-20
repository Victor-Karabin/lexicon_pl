import SwiftUI
import Shared

/// One verb's whole table, asked at once. Every person's row is answered from the same
/// bank of endings or forms, which is what makes the pattern visible rather than six
/// unrelated questions.
struct ConjugationTrainingView: View {
    let courseId: String

    @Environment(\.dismiss) private var dismiss

    @State private var table: ConjugationTable?
    @State private var answers: [String: String] = [:]
    @State private var correctness: [String: Bool] = [:]
    @State private var picking: GrammaticalPerson?
    @State private var finished = false
    @State private var loading = true

    private var steps: [ConjugationStep] { table?.steps as? [ConjugationStep] ?? [] }
    private var isChecked: Bool { !correctness.isEmpty }
    private var isAnswered: Bool { steps.allSatisfy { answers[key($0.variant.person)] != nil } }

    var body: some View {
        Group {
            if loading {
                ProgressView()
            } else if finished || table == nil {
                TrainingUnavailableView(
                    title: "Nothing left to drill",
                    message: "Every form in this course is mastered. Add another course to keep going."
                )
            } else if let table {
                ScrollView {
                    VStack(spacing: Spacing.large) {
                        header(table)
                        rows(table)
                        if !isChecked { bank(table) }
                    }
                    .padding(Spacing.medium)
                }
                .safeAreaInset(edge: .bottom) { actions }
            }
        }
        .navigationTitle("Conjugation")
        .navigationBarTitleDisplayMode(.inline)
        .task { await load() }
    }

    private func header(_ table: ConjugationTable) -> some View {
        VStack(spacing: Spacing.small) {
            if let url = table.imageUrl, let link = URL(string: url) {
                AsyncImage(url: link) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    Color.secondary.opacity(0.1)
                }
                .frame(height: 160)
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            Text(table.infinitive).font(.largeTitle.bold())
            if let translation = table.translation, !translation.isEmpty {
                Text(translation).font(.title3).foregroundStyle(.secondary)
            }
            if let ipa = table.transcription, !ipa.isEmpty {
                Text("/\(ipa)/").font(.callout).foregroundStyle(.secondary)
            }
            Button { Speech.shared.speak(table.infinitive) } label: {
                Label("Listen", systemImage: "speaker.wave.2")
            }
        }
    }

    private func rows(_ table: ConjugationTable) -> some View {
        VStack(spacing: Spacing.small) {
            ForEach(steps, id: \.variant.person) { step in
                row(step, table: table)
            }
        }
    }

    private func row(_ step: ConjugationStep, table: ConjugationTable) -> some View {
        let person = step.variant.person
        let answer = answers[key(person)]
        return HStack(spacing: Spacing.small) {
            Text(person.label)
                .font(.body)
                .frame(width: 110, alignment: .leading)
                .foregroundStyle(.secondary)

            Button {
                if !isChecked { picking = person }
            } label: {
                HStack(spacing: 0) {
                    if step.mode == .ending && !step.stem.isEmpty {
                        Text(step.stem).font(.body.weight(.medium))
                    }
                    Text(answer ?? "___")
                        .font(.body.weight(.medium))
                        .foregroundStyle(tint(person))
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, Spacing.small)
                .padding(.vertical, Spacing.small)
                .background(
                    RoundedRectangle(cornerRadius: 8)
                        .fill(picking == person ? Color.accentColor.opacity(0.15) : Color.secondary.opacity(0.08))
                )
            }
            .buttonStyle(.plain)
            .disabled(isChecked)

            if let right = correctness[key(person)] {
                Image(systemName: right ? "checkmark.circle.fill" : "xmark.circle.fill")
                    .foregroundStyle(right ? Palette.success : Palette.failure)
            }
        }
    }

    private func tint(_ person: GrammaticalPerson) -> Color {
        guard let right = correctness[key(person)] else { return .primary }
        return right ? Palette.success : Palette.failure
    }

    private func bank(_ table: ConjugationTable) -> some View {
        VStack(alignment: .leading, spacing: Spacing.small) {
            Text(picking.map { "Pick for \($0.label)" } ?? "Tap a row, then pick its ending")
                .font(.caption)
                .foregroundStyle(.secondary)
            FlowLayout(spacing: Spacing.small) {
                ForEach(table.bank as? [String] ?? [], id: \.self) { option in
                    Button(option) { place(option) }
                        .font(.callout.weight(.medium))
                        .padding(.horizontal, Spacing.medium)
                        .padding(.vertical, Spacing.small)
                        .background(Capsule().fill(Color.accentColor.opacity(0.15)))
                        .disabled(picking == nil)
                }
            }
        }
    }

    private var actions: some View {
        HStack {
            Spacer()
            if isChecked {
                AsyncButton { await load() } label: { Text("Next verb") }
                    .buttonStyle(.borderedProminent)
            } else {
                AsyncButton { await check() } label: { Text("Check") }
                    .buttonStyle(.borderedProminent)
                    .disabled(!isAnswered)
            }
        }
        .padding(Spacing.medium)
    }

    private func key(_ person: GrammaticalPerson) -> String { person.name }

    private func place(_ option: String) {
        guard let person = picking else { return }
        answers[key(person)] = option
        let remaining = steps.first { answers[key($0.variant.person)] == nil }
        picking = remaining?.variant.person
    }

    private func load() async {
        loading = true
        answers = [:]
        correctness = [:]
        picking = nil
        table = try? await deps.nextConjugationTable.invoke(courseId: courseId)
        picking = steps.first?.variant.person
        finished = table == nil
        loading = false
    }

    private func check() async {
        guard let table else { return }
        var byPerson: [GrammaticalPerson: String] = [:]
        for step in steps {
            byPerson[step.variant.person] = answers[key(step.variant.person)]
        }
        let request = SubmitConjugationAnswerRequest(courseId: courseId, table: table, answers: byPerson)
        guard let response = try? await deps.submitConjugationAnswer.invoke(request: request) else { return }
        var marked: [String: Bool] = [:]
        for (person, right) in response.correctness {
            marked[key(person)] = right.boolValue
        }
        correctness = marked
    }
}

#Preview("Conjugation") {
    NavigationStack { ConjugationTrainingView(courseId: "preview") }
}
