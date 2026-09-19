import SwiftUI
import Shared

struct PlanView: View {
    @StateObject private var model = PlanModel()
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: Spacing.small) {
                    if let course = model.course {
                        NavigationLink {
                            CourseSettingsView()
                        } label: {
                            courseSettingsTile(course)
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

                    NavigationLink {
                        ConjugationCoursesView()
                    } label: {
                        conjugationTile
                    }
                    .buttonStyle(.plain)
                }
                .padding(Spacing.medium)
            }
            .navigationTitle("Plan")
        }
    }

    private var conjugationTile: some View {
        let skin = TileSkin.standard(highlighted: false, scheme: scheme)
        return Tile(skin: skin) {
            HStack(spacing: Spacing.medium) {
                Medallion(skin: skin) { MedallionIcon(systemName: "textformat.abc", skin: skin) }
                VStack(alignment: .leading, spacing: 2) {
                    Text("Verb Conjugation").font(.headline).foregroundStyle(skin.onTile)
                    Text("Pick verbs and learn how they change by person")
                        .font(.subheadline).foregroundStyle(skin.onTile.muted)
                }
                Spacer()
                Image(systemName: "chevron.right").foregroundStyle(skin.onTile.muted)
            }
        }
    }

    private func courseSettingsTile(_ course: VocabularyCourse) -> some View {
        let skin = TileSkin.standard(highlighted: true, scheme: scheme)
        return Tile(skin: skin) {
            HStack(spacing: Spacing.medium) {
                Medallion(skin: skin) { MedallionIcon(systemName: "heart.fill", skin: skin) }
                VStack(alignment: .leading, spacing: 2) {
                    Text("Vocabulary course").font(.headline).foregroundStyle(skin.onTile)
                    Text("Words you mark To learn come first, favourites before the rest.")
                        .font(.caption).foregroundStyle(skin.onTile.muted)
                }
                Spacer()
                Image(systemName: "slider.horizontal.3").foregroundStyle(skin.onTile.muted)
            }

            FlowLayout(spacing: Spacing.small) {
                StatChip(systemName: "book", text: "\(course.settings.newWordsADay) new a day", skin: skin)
                StatChip(systemName: "character.book.closed", text: "\(course.settings.reviewsADay) reviews a day", skin: skin)
                StatChip(
                    systemName: "figure.strengthtraining.traditional",
                    text: "\(course.totalTrainings) trainings",
                    skin: skin
                )
            }
        }
    }

    private func courseTile(_ course: Course) -> some View {
        let skin = TileSkin.standard(scheme: scheme)
        let done = course.lessons.filter { $0.isCompleted }.count
        return Tile(skin: skin) {
            HStack(spacing: Spacing.medium) {
                Medallion(skin: skin) { MedallionText(text: course.level?.name ?? "", skin: skin) }
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
    @Published private(set) var course: VocabularyCourse?
    @Published private(set) var courses: [Course] = []

    private var vocabularyWatcher: Cancellable?
    private var courseWatcher: Cancellable?

    init() {
        vocabularyWatcher = deps.watchVocabularyCourse { [weak self] value in self?.course = value }
        courseWatcher = deps.watchCourses { [weak self] value in self?.courses = value }
    }

    deinit {
        vocabularyWatcher?.cancel()
        courseWatcher?.cancel()
    }
}
