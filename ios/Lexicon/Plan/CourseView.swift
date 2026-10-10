import SwiftUI
import Shared

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
                        Medallion(skin: skin) { MedallionText(text: course.level?.name ?? "", skin: skin) }
                        Text(Strings.courseProgress(done, course.lessons.count))
                            .foregroundStyle(skin.onTile.muted)
                        Spacer()
                    }
                    ProgressView(value: Double(done), total: Double(max(course.lessons.count, 1))).tint(skin.onTile)
                }

                ForEach(course.lessons, id: \.id.value) { lesson in
                    NavigationLink {
                        LessonView(lesson: lesson, lessons: course.lessons)
                    } label: {
                        HStack(spacing: Spacing.medium) {
                            Image(systemName: lesson.isCompleted ? "checkmark.circle.fill" : "circle")
                                .foregroundStyle(lesson.isCompleted ? Palette.success : .secondary)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(Strings.courseLessonNumber(Int(lesson.number))).font(.body.weight(.medium))
                                Text("\(lesson.wordCount) \(Strings.presetsWordCountLabel(Int(lesson.wordCount)))").font(.caption).foregroundStyle(.secondary)
                            }
                            Spacer()
                        }
                        .padding(.vertical, Spacing.small)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    Divider()
                }
            }
            .padding(Spacing.medium)
        }
        .navigationTitle(course.title.text())
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct LessonView: View {
    let lessons: [LessonSummary]

    @State private var current: LessonSummary
    @State private var lesson: Lesson?
    @State private var words: [Word] = []
    @State private var hasScript = false
    @State private var isScriptStarted = false

    init(lesson: LessonSummary, lessons: [LessonSummary]) {
        self.lessons = lessons
        _current = State(initialValue: lesson)
    }

    private var nextLesson: LessonSummary? {
        lessons.sorted { $0.number < $1.number }.first { $0.number > current.number }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Spacing.medium) {
                if hasScript {
                    NavigationLink {
                        LessonFlowView(
                            lessonId: current.id,
                            lessonNumber: Int(current.number),
                            hasNextLesson: nextLesson != nil,
                            onNextLesson: openNextLesson
                        )
                        .onDisappear { Task { await load() } }
                    } label: {
                        Label(isScriptStarted ? Strings.lessonFlowContinue : Strings.lessonFlowStart, systemImage: "play.fill")
                    }
                    .buttonStyle(.borderedProminent)
                }

                if let lesson, !lesson.audio.isEmpty {
                    Text(Strings.lessonRecordings).font(.subheadline.weight(.semibold))
                    FlowLayout(spacing: Spacing.small) {
                        ForEach(lesson.audio, id: \.file) { track in
                            AsyncButton {
                                await LessonAudio.shared.play(file: track.file, remoteId: track.remoteId)
                            } label: {
                                Label(track.label, systemImage: "play.circle")
                                    .font(.caption)
                                    .padding(.horizontal, Spacing.small)
                                    .padding(.vertical, Spacing.tiny)
                                    .overlay(Capsule().stroke(Color.secondary.opacity(0.4)))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }

                Text(Strings.lessonWords).font(.subheadline.weight(.semibold))
                ForEach(words, id: \.id.value) { word in
                    WordRow(word: word, status: word.status) {
                        Task {
                            try? await deps.setWordStatus.invoke(id: word.id, status: word.status.next())
                            await load()
                        }
                    }
                    Divider()
                }
            }
            .padding(Spacing.medium)
        }
        .navigationTitle(Strings.courseLessonNumber(Int(current.number)))
        .navigationBarTitleDisplayMode(.inline)
        .task(id: current.id.value) { await load() }
    }

    private func openNextLesson() {
        guard let next = nextLesson else { return }
        current = next
    }

    private func load() async {
        let id = current.id
        let script = try? await deps.getLessonScript.invoke(id: id)
        hasScript = script != nil
        if let script {
            let progress = try? await deps.getLessonProgress.invoke(id: id)
            isScriptStarted = LessonSession.companion.isInProgress(script: script, progress: progress)
        } else {
            isScriptStarted = false
        }
        lesson = try? await deps.getLesson.invoke(id: id)
        words = (try? await deps.getLessonVocabulary.invoke(id: id)) ?? []
    }
}
