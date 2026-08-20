import SwiftUI
import Shared

/// Picking the verbs a course will drill. Every opening starts from nothing chosen, so
/// building a second course over different verbs does not mean clearing the first.
struct VerbSelectionView: View {
    let onCreated: () async -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var verbs: [VerbConjugation] = []
    @State private var query = ""
    @State private var chosen: Set<String> = []
    @State private var loading = true
    @State private var saving = false

    var body: some View {
        Group {
            if loading {
                ProgressView()
            } else {
                List {
                    ForEach(verbs, id: \.infinitive) { verb in
                        row(verb)
                    }
                }
                .listStyle(.plain)
            }
        }
        .searchable(text: $query, prompt: "Search verb")
        .onChange(of: query) { _, _ in Task { await load() } }
        .navigationTitle("Choose verbs")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Cancel") { dismiss() }
            }
            ToolbarItem(placement: .confirmationAction) {
                AsyncButton { await create() } label: { Text("Create") }
                    .disabled(chosen.isEmpty || saving)
            }
        }
        .safeAreaInset(edge: .bottom) {
            if !chosen.isEmpty {
                Text("\(chosen.count) chosen")
                    .font(.callout).foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity)
                    .padding(Spacing.small)
                    .background(.bar)
            }
        }
        .task { await load() }
    }

    private func row(_ verb: VerbConjugation) -> some View {
        Button {
            if chosen.contains(verb.infinitive) { chosen.remove(verb.infinitive) } else { chosen.insert(verb.infinitive) }
        } label: {
            HStack(spacing: Spacing.medium) {
                Image(systemName: chosen.contains(verb.infinitive) ? "checkmark.circle.fill" : "circle")
                    .foregroundStyle(chosen.contains(verb.infinitive) ? Color.accentColor : Color.secondary)
                VStack(alignment: .leading, spacing: 2) {
                    Text(verb.infinitive).font(.body.weight(.medium))
                    if let translation = verb.translation, !translation.isEmpty {
                        Text(translation).font(.caption).foregroundStyle(.secondary)
                    }
                    if !verb.isComplete {
                        Text("\(verb.persons.count) of 6 forms").font(.caption2).foregroundStyle(.secondary)
                    }
                }
                Spacer()
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private func load() async {
        verbs = (try? await deps.loadConjugationVerbs.invoke(query: query)) as? [VerbConjugation] ?? []
        loading = false
    }

    private func create() async {
        saving = true
        _ = try? await deps.createConjugationCourse.invoke(infinitives: Array(chosen))
        await onCreated()
        saving = false
        dismiss()
    }
}

#Preview("Choose verbs") {
    NavigationStack { VerbSelectionView {} }
}
