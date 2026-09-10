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
    @State private var nextOffset: Int32 = 0
    @State private var hasMore = true
    @State private var loadingMore = false
    @State private var generation = 0

    private let searchSettleNanoseconds: UInt64 = 300_000_000

    var body: some View {
        Group {
            if loading {
                ProgressView()
            } else {
                List {
                    ForEach(verbs, id: \.infinitive) { verb in
                        row(verb)
                            .onAppear {
                                if verb.infinitive == verbs.last?.infinitive {
                                    Task { await loadMore() }
                                }
                            }
                    }
                    if loadingMore {
                        ProgressView().frame(maxWidth: .infinity)
                    }
                }
                .listStyle(.plain)
            }
        }
        .searchable(text: $query, prompt: "Search verb")
        .task(id: query) {
            if !loading {
                try? await Task.sleep(nanoseconds: searchSettleNanoseconds)
                guard !Task.isCancelled else { return }
            }
            await loadFirstPage()
        }
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

    private func loadFirstPage() async {
        generation += 1
        let current = generation
        let page = try? await deps.loadConjugationVerbs.page(query: query, offset: 0)
        guard current == generation else { return }
        verbs = page?.verbs as? [VerbConjugation] ?? []
        nextOffset = page?.nextOffset ?? 0
        hasMore = !(page?.isLast ?? true)
        loadingMore = false
        loading = false
    }

    private func loadMore() async {
        guard hasMore, !loadingMore, !loading else { return }
        let current = generation
        loadingMore = true
        let page = try? await deps.loadConjugationVerbs.page(query: query, offset: nextOffset)
        guard current == generation else { return }
        loadingMore = false
        guard let page else { return }
        let known = Set(verbs.map(\.infinitive))
        verbs += (page.verbs as? [VerbConjugation] ?? []).filter { !known.contains($0.infinitive) }
        nextOffset = page.nextOffset
        hasMore = !page.isLast
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
