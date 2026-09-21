import SwiftUI
import Shared

@MainActor
final class DashboardModel: ObservableObject {
    @Published private(set) var course: VocabularyCourse?
    @Published private(set) var metrics: [ProgressMetric] = []
    @Published private(set) var streak: Int = 0
    @Published private(set) var nothingToPractise = false
    @Published private(set) var studyTime: StudyTimeHistory?
    @Published private(set) var wordsToReview = 0

    private var watcher: Cancellable?
    private var turns = 0

    init() {
        watcher = deps.watchVocabularyCourse { [weak self] course in
            Task { await self?.show(course) }
        }
    }

    deinit { watcher?.cancel() }

    var totalTrainings: Int { Int(course?.totalTrainings ?? 0) }
    var doneTrainings: Int { Int(course?.completedInRound ?? 0) }
    var round: Int { Int(course?.round ?? 0) + 1 }
    var roundFraction: Double { Double(course?.roundFraction ?? 0) }
    var showsCards: Bool { course?.showCardsNext ?? false }

    var known: Int { Int(metric(.vocabulary)?.current ?? 0) }
    var learning: Int { Int((metric(.vocabulary)?.target ?? 0) - (metric(.vocabulary)?.current ?? 0)) }
    var accuracy: ProgressMetric? { metric(.accuracy) }

    var continueLabel: String {
        if showsCards, let count = course?.newWords.count {
            return Strings.dashboardMeetWords(Int(count))
        }
        return Strings.dashboardContinue
    }

    func load() async {
        if let course = try? await deps.getVocabularyCourse.invoke() {
            await show(course)
        }
    }

    private func show(_ course: VocabularyCourse) async {
        self.course = course
        studyTime = try? await deps.getDailyStudyTime.invoke()
        wordsToReview = Int((try? await deps.countWordsToReview.invoke()) as? Int32 ?? 0)
        metrics = (try? await deps.getCourseProgress.invoke())?.metrics as? [ProgressMetric] ?? []
        streak = Int((try? await deps.getStudyStreak.invoke()) as? Int32 ?? 0)
        nothingToPractise = false
    }

    func nextTraining() async -> CourseTurn? {
        turn(for: try? await deps.nextCourseTraining.next())
    }

    func advance() async -> CourseTurn? {
        turn(for: try? await deps.nextCourseTraining.advance())
    }

    func reset() async -> CourseStep {
        try? await deps.resetCourseQueue.invoke()
        await load()
        if showsCards { return .cards }
        if let turn = await nextTraining() { return .training(turn) }
        return .nothing
    }

    private func turn(for launch: CourseLaunch?) -> CourseTurn? {
        guard let launch, let entry = TrainingCatalog.entry(id: launch.training.id) else {
            nothingToPractise = true
            return nil
        }
        turns += 1
        let wordIds = (launch.wordIds as? [VocabularyId] ?? []).map { $0.value }
        return CourseTurn(id: turns, entry: entry, wordIds: wordIds)
    }

    private func metric(_ type: ProgressMetricType) -> ProgressMetric? {
        metrics.first { $0.type == type }
    }
}

struct CourseTurn: Identifiable, Hashable {
    let id: Int
    let entry: TrainingEntry
    let wordIds: [Int64]
}

enum CourseStep {
    case training(CourseTurn)
    case cards
    case nothing
}
