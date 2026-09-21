import SwiftUI
import Shared

struct SplashView: View {
    let onFinished: () -> Void

    @State private var status: CatalogSeedStatus?
    @State private var watcher: Cancellable?

    var body: some View {
        VStack(spacing: Spacing.large) {
            Spacer()
            Image(systemName: "book.closed.fill")
                .font(.system(size: 56))
                .foregroundStyle(Palette.primary)
            Text(Strings.splashTitle).font(.largeTitle.bold())
            Text(Strings.splashTagline)
                .font(.subheadline)
                .foregroundStyle(.secondary)

            VStack(alignment: .leading, spacing: Spacing.small) {
                step(Strings.syncStepVocabulary, status?.vocabulary)
                step(Strings.syncStepPresets, status?.presets)
                step(Strings.syncStepCourse, status?.course)
                step(Strings.syncStepVerbs, status?.verbs)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(Spacing.medium)

            if isBlocked {
                Text(Strings.syncBlocked)
                    .font(.callout)
                    .foregroundStyle(Palette.failure)
                    .multilineTextAlignment(.center)
                Button(Strings.syncRetry) { start() }
            }
            Spacer()
        }
        .padding(Spacing.xl)
        .onAppear { start() }
        .onDisappear { watcher?.cancel() }
    }

    @ViewBuilder
    private func step(_ name: String, _ state: SeedStepStatus?) -> some View {
        HStack(spacing: Spacing.small) {
            switch state {
            case is SeedStepStatusComplete:
                Image(systemName: "checkmark").foregroundStyle(Palette.success)
            case let failed as SeedStepStatusFailed:
                Image(systemName: "xmark").foregroundStyle(Palette.failure)
                    .accessibilityLabel(failed.reason)
            case is SeedStepStatusInProgress:
                ProgressView().controlSize(.small)
            default:
                Image(systemName: "circle").foregroundStyle(.secondary)
            }
            VStack(alignment: .leading, spacing: 0) {
                Text(name).font(.callout)
                if let complete = state as? SeedStepStatusComplete {
                    Text(Strings.syncUpToDate(String(complete.total))).font(.caption).foregroundStyle(.secondary)
                }

                if let failed = state as? SeedStepStatusFailed {
                    Text(failed.reason).font(.caption).foregroundStyle(Palette.failure)
                }
            }
        }
    }

    private var isBlocked: Bool {
        guard let status else { return false }
        return [status.vocabulary, status.presets, status.course, status.verbs]
            .contains { ($0 as? SeedStepStatusFailed)?.canContinue == false }
    }

    private var isFinished: Bool {
        guard let status else { return false }
        return [status.vocabulary, status.presets, status.course, status.verbs].allSatisfy {
            $0 is SeedStepStatusComplete || $0 is SeedStepStatusFailed
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
