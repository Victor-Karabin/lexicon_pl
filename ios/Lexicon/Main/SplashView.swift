import SwiftUI
import Shared

/// The catalogues are imported before anything can be studied, and the import says
/// what it is doing rather than showing a spinner over an empty app.
struct SplashView: View {
    let onFinished: () -> Void

    @State private var status: CatalogSyncStatus?
    @State private var watcher: Cancellable?

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
                step("Vocabulary", status?.vocabulary)
                step("Presets", status?.presets)
                step("Course", status?.course)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(Spacing.medium)

            if isBlocked {
                Text("The bundled vocabulary could not be loaded, so there is nothing to study yet.")
                    .font(.callout)
                    .foregroundStyle(Palette.failure)
                    .multilineTextAlignment(.center)
                Button("Try again") { start() }
            }
            Spacer()
        }
        .padding(Spacing.xl)
        .onAppear { start() }
        .onDisappear { watcher?.cancel() }
    }

    @ViewBuilder
    private func step(_ name: String, _ state: SyncStepStatus?) -> some View {
        HStack(spacing: Spacing.small) {
            switch state {
            case is SyncStepStatusComplete:
                Image(systemName: "checkmark").foregroundStyle(Palette.success)
            case let failed as SyncStepStatusFailed:
                Image(systemName: "xmark").foregroundStyle(Palette.failure)
                    .accessibilityLabel(failed.reason)
            case is SyncStepStatusInProgress:
                ProgressView().controlSize(.small)
            default:
                Image(systemName: "circle").foregroundStyle(.secondary)
            }
            VStack(alignment: .leading, spacing: 0) {
                Text(name).font(.callout)
                if let complete = state as? SyncStepStatusComplete {
                    Text("\(complete.total) ready").font(.caption).foregroundStyle(.secondary)
                }
                // The reason is what says which of the three actually broke, so it
                // belongs on screen rather than only in a log nobody reads.
                if let failed = state as? SyncStepStatusFailed {
                    Text(failed.reason).font(.caption).foregroundStyle(Palette.failure)
                }
            }
        }
    }

    private var isBlocked: Bool {
        guard let status else { return false }
        return [status.vocabulary, status.presets, status.course]
            .contains { ($0 as? SyncStepStatusFailed)?.canContinue == false }
    }

    private var isFinished: Bool {
        guard let status else { return false }
        return [status.vocabulary, status.presets, status.course].allSatisfy {
            $0 is SyncStepStatusComplete || $0 is SyncStepStatusFailed
        }
    }

    private func start() {
        watcher?.cancel()
        status = nil
        watcher = deps.watchCatalogSync { value in
            status = value
            if isFinished && !isBlocked {
                watcher?.cancel()
                onFinished()
            }
        }
    }
}
