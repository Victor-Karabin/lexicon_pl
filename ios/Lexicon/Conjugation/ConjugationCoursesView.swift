import SwiftUI
import Shared

/// The verbs the learner picked, as courses they can carry on with or drop.
struct ConjugationCoursesView: View {
    @State private var courses: [ConjugationCourse] = []
    @State private var loading = true
    @State private var choosing = false

    var body: some View {
        Group {
            if loading {
                ProgressView()
            } else if courses.isEmpty {
                empty
            } else {
                List {
                    ForEach(courses, id: \.id) { course in
                        NavigationLink {
                            ConjugationTrainingView(courseId: course.id)
                        } label: {
                            row(course)
                        }
                    }
                    .onDelete { offsets in Task { await delete(offsets) } }
                }
                .listStyle(.insetGrouped)
            }
        }
        .navigationTitle("Verb Conjugation")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button { choosing = true } label: { Image(systemName: "plus") }
            }
        }
        .sheet(isPresented: $choosing) {
            NavigationStack {
                VerbSelectionView { await reload() }
            }
        }
        .task { await reload() }
    }

    private var empty: some View {
        VStack(spacing: Spacing.medium) {
            Image(systemName: "textformat.abc").font(.system(size: 44)).foregroundStyle(.secondary)
            Text("No course yet").font(.title3.weight(.semibold))
            Text("Pick the verbs you want to drill and they become a course you can come back to.")
                .font(.callout).foregroundStyle(.secondary).multilineTextAlignment(.center)
            Button("Choose verbs") { choosing = true }.buttonStyle(.borderedProminent)
        }
        .padding(Spacing.xl)
    }

    private func row(_ course: ConjugationCourse) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(course.title).font(.headline)
            Text("\(course.progress.mastered) of \(course.progress.total) forms mastered")
                .font(.caption).foregroundStyle(.secondary)
            ProgressView(value: Double(course.progress.fraction))
        }
        .padding(.vertical, 4)
    }

    private func reload() async {
        courses = (try? await deps.loadConjugationCourses.invoke()) as? [ConjugationCourse] ?? []
        loading = false
    }

    private func delete(_ offsets: IndexSet) async {
        for index in offsets {
            try? await deps.deleteConjugationCourse.invoke(courseId: courses[index].id)
        }
        await reload()
    }
}

#Preview("Conjugation courses") {
    NavigationStack { ConjugationCoursesView() }
}
