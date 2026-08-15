import SwiftUI
import Shared

/// Programs and courses: the things there are to work through.
struct PlanView: View {
    @StateObject private var model = PlanModel()
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: Spacing.small) {
                    Text("Programs").font(.subheadline.weight(.semibold)).foregroundStyle(.secondary)

                    ForEach(model.programs, id: \.id.value) { program in
                        NavigationLink {
                            ProgramFormView(programId: program.id)
                        } label: {
                            programTile(program)
                        }
                        .buttonStyle(.plain)
                    }

                    // Only until they have one: an offer that never goes away reads as
                    // an unfinished setup step rather than an option.
                    if model.programs.isEmpty {
                        NavigationLink {
                            ProgramFormView(programId: nil)
                        } label: {
                            createTile
                        }
                        .buttonStyle(.plain)
                    }

                    Text("Courses").font(.subheadline.weight(.semibold)).foregroundStyle(.secondary)
                        .padding(.top, Spacing.small)

                    ForEach(model.courses, id: \.id.value) { course in
                        NavigationLink {
                            CourseView(course: course)
                        } label: {
                            courseTile(course)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(Spacing.medium)
            }
            .navigationTitle("Plan")
        }
    }

    private func programTile(_ program: Program) -> some View {
        let active = model.activeProgramId == program.id.value
        let skin = TileSkin.standard(highlighted: active, scheme: scheme)
        return Tile(skin: skin) {
            HStack(spacing: Spacing.medium) {
                Medallion(skin: skin) { MedallionIcon(systemName: "heart.fill", skin: skin) }
                VStack(alignment: .leading, spacing: 2) {
                    Text(program.title.text()).font(.headline).foregroundStyle(skin.onTile)
                    Text(program.description_.text()).font(.caption).foregroundStyle(skin.onTile.muted)
                }
                Spacer()
                Image(systemName: active ? "play.fill" : "chevron.right").foregroundStyle(skin.onTile.muted)
            }
            // Flowed rather than in a row: three chips fit across a wide phone and do
            // not fit a narrow one at a large text size, and a row has no way to say so.
            FlowLayout(spacing: Spacing.small) {
                if let words = program.config.goals.first(where: { $0.type == .vocabulary })?.target {
                    StatChip(systemName: "character.book.closed", text: "\(words) words", skin: skin)
                }
                if program.config.dailyPlan.newWords > 0 {
                    StatChip(systemName: "book", text: "\(program.config.dailyPlan.newWords) new a day", skin: skin)
                }
                if !program.config.dailyPlan.queue.isEmpty {
                    StatChip(
                        systemName: "figure.strengthtraining.traditional",
                        text: "\(program.config.dailyPlan.queue.count) trainings",
                        skin: skin
                    )
                }
            }
        }
    }

    private var createTile: some View {
        let skin = TileSkin.standard(scheme: scheme)
        return Tile(skin: skin) {
            HStack(spacing: Spacing.medium) {
                Medallion(skin: skin) { MedallionIcon(systemName: "plus", skin: skin) }
                VStack(alignment: .leading, spacing: 2) {
                    Text("Build your own").font(.headline).foregroundStyle(skin.onTile)
                    Text("A daily plan over the words in your study set")
                        .font(.caption).foregroundStyle(skin.onTile.muted)
                }
                Spacer()
                Image(systemName: "chevron.right").foregroundStyle(skin.onTile.muted)
            }
        }
    }

    private func courseTile(_ course: Course) -> some View {
        let skin = TileSkin.standard(scheme: scheme)
        let done = course.lessons.filter { $0.isCompleted }.count
        return Tile(skin: skin) {
            HStack(spacing: Spacing.medium) {
                Medallion(skin: skin) { MedallionText(text: course.level, skin: skin) }
                VStack(alignment: .leading, spacing: 2) {
                    Text(course.title.text()).font(.headline).foregroundStyle(skin.onTile)
                    Text("\(done) of \(course.lessons.count) lessons done")
                        .font(.caption).foregroundStyle(skin.onTile.muted)
                }
                Spacer()
                Image(systemName: "chevron.right").foregroundStyle(skin.onTile.muted)
            }
            ProgressView(value: Double(done), total: Double(max(course.lessons.count, 1))).tint(skin.onTile)
        }
    }
}

@MainActor
final class PlanModel: ObservableObject {
    @Published private(set) var programs: [Program] = []
    @Published private(set) var courses: [Course] = []
    @Published private(set) var activeProgramId: String?

    private var programWatcher: Cancellable?
    private var courseWatcher: Cancellable?
    private var enrolmentWatcher: Cancellable?

    init() {
        programWatcher = deps.watchPrograms { [weak self] value in self?.programs = value }
        courseWatcher = deps.watchCourses { [weak self] value in self?.courses = value }
        enrolmentWatcher = deps.watchActiveEnrolment { [weak self] enrolment in
            self?.activeProgramId = enrolment?.programId.value
        }
    }

    deinit {
        programWatcher?.cancel()
        courseWatcher?.cancel()
        enrolmentWatcher?.cancel()
    }
}
