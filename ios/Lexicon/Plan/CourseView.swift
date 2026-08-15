import SwiftUI
import Shared

/// A course's lessons, locked until the one before is done.
struct CourseView: View {
    let course: Course

    @Environment(\.colorScheme) private var scheme

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Spacing.medium) {
                let skin = TileSkin.standard(highlighted: true, scheme: scheme)
                let done = course.lessons.filter { $0.isCompleted }.count
                Tile(skin: skin) {
                    HStack(spacing: Spacing.medium) {
                        Medallion(skin: skin) { MedallionText(text: course.level, skin: skin) }
                        Text("\(done) of \(course.lessons.count) lessons done")
                            .foregroundStyle(skin.onTile.muted)
                        Spacer()
                    }
                    ProgressView(value: Double(done), total: Double(max(course.lessons.count, 1))).tint(skin.onTile)
                }

                ForEach(course.lessons, id: \.id.value) { lesson in
                    NavigationLink {
                        LessonView(lessonId: lesson.id, title: lesson.title)
                    } label: {
                        HStack(spacing: Spacing.medium) {
                            Image(systemName: lesson.isCompleted ? "checkmark.circle.fill" : lesson.isUnlocked ? "circle" : "lock.fill")
                                .foregroundStyle(lesson.isCompleted ? Palette.success : .secondary)
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Lesson \(lesson.number)").font(.caption).foregroundStyle(.secondary)
                                Text(lesson.title).font(.body.weight(.medium))
                                Text("\(lesson.wordCount) words").font(.caption).foregroundStyle(.secondary)
                            }
                            Spacer()
                        }
                        .opacity(lesson.isUnlocked ? 1 : 0.45)
                        .padding(.vertical, Spacing.small)
                    }
                    .buttonStyle(.plain)
                    .disabled(!lesson.isUnlocked)
                    Divider()
                }
            }
            .padding(Spacing.medium)
        }
        .navigationTitle(course.title.text())
        .navigationBarTitleDisplayMode(.inline)
    }
}

/// One lesson: its words, and a way to train over exactly those.
struct LessonView: View {
    let lessonId: LessonId
    let title: String

    @State private var lesson: Lesson?
    @State private var words: [PresetWord] = []

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Spacing.medium) {
                Text(title).font(.title2.weight(.semibold))

                if let lesson {
                    HStack(spacing: Spacing.small) {
                        NavigationLink {
                            TrainingHost(
                                entry: TrainingCatalog.entry(id: "mix")!,
                                vocabularyIds: lesson.vocabularyIds.map { $0.value }
                            )
                        } label: {
                            Label("Train this lesson", systemImage: "play.fill")
                        }
                        .buttonStyle(.borderedProminent)

                        AsyncButton {
                            try? await deps.setLessonCompleted.invoke(id: lessonId, isCompleted: !lesson.isCompleted)
                            await load()
                        } label: {
                            Label(lesson.isCompleted ? "Done" : "Mark as done", systemImage: "checkmark.circle")
                        }
                        .buttonStyle(.bordered)
                    }
                }

                Text("New words").font(.subheadline.weight(.semibold))
                ForEach(words, id: \.id.value) { word in
                    WordRow(word: word, isFavourite: word.isFavourite) {
                        Task {
                            try? await deps.toggleWordFavourite.invoke(id: word.id, isFavourite: !word.isFavourite)
                            await load()
                        }
                    }
                    Divider()
                }
            }
            .padding(Spacing.medium)
        }
        .navigationBarTitleDisplayMode(.inline)
        .task { await load() }
    }

    private func load() async {
        lesson = try? await deps.getLesson.invoke(id: lessonId)
        words = (try? await deps.getLessonVocabulary.invoke(id: lessonId)) ?? []
    }
}
