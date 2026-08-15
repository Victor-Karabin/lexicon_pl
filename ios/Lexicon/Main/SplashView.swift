import SwiftUI
import Shared

/// The catalogues are imported before anything can be studied, and the import says
/// what it is doing rather than showing a spinner over an empty app.
struct SplashView: View {
    let onFinished: () -> Void

    @State private var lines: [String] = []
    @State private var failed = false

    var body: some View {
        VStack(spacing: Spacing.large) {
            Spacer()
            Image(systemName: "book.closed.fill")
                .font(.system(size: 56))
                .foregroundStyle(Palette.primary)
            Text("Lexicon").font(.largeTitle.bold())
            Text("Polish vocabulary, one session at a time")
                .font(.subheadline)
                .foregroundStyle(.secondary)

            VStack(alignment: .leading, spacing: Spacing.small) {
                ForEach(lines, id: \.self) { line in
                    Label(line, systemImage: "checkmark")
                        .font(.callout)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(Spacing.medium)

            if failed {
                Text("The bundled vocabulary could not be loaded, so there is nothing to study yet.")
                    .font(.callout)
                    .foregroundStyle(Palette.failure)
                    .multilineTextAlignment(.center)
                Button("Try again") { Task { await sync() } }
            }
            Spacer()
        }
        .padding(Spacing.xl)
        .task { await sync() }
    }

    private func sync() async {
        failed = false
        lines = []
        do {
            let outcome = try await deps.syncCatalog.invoke()
            lines = ["Vocabulary ready", "Presets ready", "Course ready"]
            _ = outcome
            onFinished()
        } catch {
            failed = true
        }
    }
}
