import SwiftUI
import Shared

@MainActor
final class DashboardModel: ObservableObject {
    @Published private(set) var program: Program?
    @Published private(set) var metrics: [ProgressMetric] = []
    @Published private(set) var overall: Double = 0
    @Published private(set) var streak: Int = 0
    @Published private(set) var day: ProgramDay?
    @Published private(set) var nothingToPractise = false

    private var watcher: Cancellable?
    private var turns = 0

    init() {
        watcher = deps.watchActiveEnrolment { [weak self] enrolment in
            Task { await self?.load(enrolment: enrolment) }
        }
    }

    deinit { watcher?.cancel() }

    var totalTrainings: Int { Int(day?.totalTrainings ?? 0) }
    var doneTrainings: Int { Int(day?.completedTrainings ?? 0) }
    var showsCards: Bool { day?.showCardsNext ?? false }

    var continueLabel: String {
        if showsCards, let count = day?.newWords.count {
            return "Meet \(count) new words"
        }
        return "Continue"
    }

    func load() async {
        let enrolment = try? await deps.observeActiveEnrolmentFirst()
        await load(enrolment: enrolment)
    }

    private func load(enrolment: ProgramEnrolment?) async {
        guard let enrolment, let program = try? await deps.getProgram.invoke(id: enrolment.programId) else {
            self.program = nil
            return
        }
        self.program = program
        if let progress = try? await deps.getProgramProgress.invoke(program: program) {
            metrics = progress.metrics
            overall = progress.overall
        }
        streak = Int((try? await deps.getStudyStreak.invoke()) as? Int32 ?? 0)
        day = try? await deps.getProgramDay.invoke(id: program.id)
        nothingToPractise = false
    }

    var isDayComplete: Bool { day?.isComplete ?? false }

    func nextTraining() async -> ProgramTurn? {
        guard let program else { return nil }
        let launch = try? await deps.nextProgramTraining.next(id: program.id)
        return await turn(for: launch, in: program)
    }

    func advance() async -> ProgramTurn? {
        guard let program else { return nil }
        let launch = try? await deps.nextProgramTraining.advance(id: program.id)
        return await turn(for: launch, in: program)
    }

    private func turn(for launch: ProgramLaunch?, in program: Program) async -> ProgramTurn? {
        guard let launch, let entry = TrainingCatalog.entry(id: launch.training.id) else {
            day = try? await deps.getProgramDay.invoke(id: program.id)
            nothingToPractise = !isDayComplete
            return nil
        }
        turns += 1
        let wordIds = (launch.wordIds as? [VocabularyId] ?? []).map { $0.value }
        return ProgramTurn(id: turns, entry: entry, wordIds: wordIds)
    }

    func label(for type: ProgressMetricType) -> String {
        switch type {
        case .vocabulary: return "Words mastered"
        case .milestones: return "Milestones"
        case .accuracy: return "Correct answers"
        case .consistency: return "Days studied"
        default: return "Study time"
        }
    }

    func value(for metric: ProgressMetric) -> String {
        switch metric.type {
        case .consistency: return "\(metric.current)"
        case .accuracy: return "\(metric.current)%"
        default: return "\(metric.current) / \(metric.target)"
        }
    }
}

struct ProgramTurn: Identifiable, Hashable {
    let id: Int
    let entry: TrainingEntry
    let wordIds: [Int64]
}

extension IosDependencies {

    func observeActiveEnrolmentFirst() async throws -> ProgramEnrolment? {
        try await withCheckedThrowingContinuation { continuation in
            var handle: Cancellable?
            var resumed = false
            handle = watchActiveEnrolment { enrolment in
                guard !resumed else { return }
                resumed = true
                continuation.resume(returning: enrolment)
                handle?.cancel()
            }
        }
    }
}
