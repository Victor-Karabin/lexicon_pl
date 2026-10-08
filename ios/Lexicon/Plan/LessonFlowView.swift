import Shared
import SwiftUI

private let polishLetters = ["ą", "ć", "ę", "ł", "ń", "ó", "ś", "ź", "ż"]

private func rich(_ text: String) -> AttributedString {
    (try? AttributedString(markdown: text)) ?? AttributedString(text)
}

struct LessonFlowView: View {
    let lessonId: LessonId

    @Environment(\.dismiss) private var dismiss
    @ObservedObject private var audio = LessonAudio.shared
    @State private var session: LessonSession?
    @State private var loaded = false
    @State private var transcriptOpen = false
    @State private var completed = false
    @State private var saving: Task<Void, Never>?
    @FocusState private var focused: String?

    var body: some View {
        Group {
            if let session {
                if completed {
                    completedView(session)
                } else {
                    screenView(session)
                }
            } else if loaded {
                TrainingUnavailableView(message: Strings.lessonNotFound)
            } else {
                ProgressView()
            }
        }
        .navigationTitle(title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .keyboard) {
                if acceptsLetters {
                    HStack(spacing: 0) {
                        ForEach(polishLetters, id: \.self) { letter in
                            Button { insert(letter) } label: {
                                Text(letter).frame(maxWidth: .infinity, minHeight: 36)
                            }
                        }
                    }
                    .frame(maxWidth: .infinity)
                }
            }
        }
        .task { await load() }
        .onDisappear { audio.stop() }
    }

    private var title: String {
        guard let session else { return "" }
        if completed { return session.script.title }
        return Strings.lessonFlowPosition(session.screen.id, Int(session.position), Int(session.total))
    }

    private var acceptsLetters: Bool {
        guard let session, focused != nil, !session.isChecked else { return false }
        let screen = session.screen
        if screen is LessonScreenFreeWriting { return !session.isFinished }
        if let write = screen as? LessonScreenWrite { return write.keyboard == .text }
        return screen is LessonScreenGapFill
    }

    // MARK: Screens

    private func screenView(_ session: LessonSession) -> some View {
        let screen = session.screen
        return ScrollView {
            VStack(alignment: .leading, spacing: Spacing.medium) {
                Text(screen.title).font(.title2.weight(.semibold))
                Text(rich(screen.instruction)).font(.callout)
                tracks(screen.tracks)

                if let reference = screen as? LessonScreenReference {
                    referenceBody(reference)
                } else if let write = screen as? LessonScreenWrite {
                    writeBody(write, session)
                } else if let choice = screen as? LessonScreenChoice {
                    choiceBody(choice, session)
                } else if let gaps = screen as? LessonScreenGapFill {
                    gapBody(gaps, session)
                } else if let writing = screen as? LessonScreenFreeWriting {
                    freeWritingBody(writing, session)
                }

                if session.isChecked {
                    let score = session.screenScore(screen: screen)
                    Text(Strings.exerciseScore(Int(score.correct), Int(score.total))).font(.headline)
                }
                if let transcript = screen.transcript {
                    transcriptView(transcript, unlocked: session.isTranscriptUnlocked(screen: screen))
                }
                if session.endsStep, session.isFinished, let step = session.step {
                    stepScoreView(session.stepScore(step: step))
                }
                if let failed = session.stepToRetry, failed != session.step {
                    Text(Strings.lessonFlowRetryEarlier(failed.title, Int(session.script.passMark * 100)))
                }
            }
            .padding(Spacing.medium)
        }
        .scrollDismissesKeyboard(.interactively)
        .safeAreaInset(edge: .bottom) { footer(session) }
        .id(session.index)
    }

    private func tracks(_ tracks: [LessonTrack]) -> some View {
        VStack(alignment: .leading, spacing: Spacing.small) {
            HStack(spacing: Spacing.small) {
                ForEach(tracks, id: \.id) { track in
                    VStack(alignment: .leading, spacing: Spacing.tiny) {
                        if let label = track.label { Text(label).font(.caption.weight(.semibold)) }
                        AsyncButton {
                            await audio.toggle(file: track.file, remoteId: track.remoteId)
                        } label: {
                            Label(
                                audio.playing == track.file ? Strings.exercisePause : Strings.exercisePlay,
                                systemImage: audio.playing == track.file ? "pause.fill" : "play.fill"
                            )
                        }
                        .buttonStyle(.borderedProminent)
                    }
                }
            }
            if tracks.contains(where: { audio.unavailable.contains($0.file) }) {
                Text(Strings.exerciseAudioUnavailable).font(.caption).foregroundStyle(.secondary)
            }
        }
    }

    private func referenceBody(_ screen: LessonScreenReference) -> some View {
        VStack(alignment: .leading, spacing: Spacing.medium) {
            ForEach(Array(screen.tables.enumerated()), id: \.offset) { _, table in
                tableView(table)
            }
            ForEach(screen.notes, id: \.self) { note in
                Text(rich(note)).font(.callout)
            }
        }
    }

    private func tableView(_ table: LessonTable) -> some View {
        Grid(alignment: .topLeading, horizontalSpacing: Spacing.small, verticalSpacing: Spacing.small) {
            if table.header.contains(where: { !$0.isEmpty }) {
                GridRow {
                    ForEach(Array(table.header.enumerated()), id: \.offset) { _, cell in
                        cellText(cell).font(.subheadline.weight(.semibold)).foregroundStyle(Palette.accentDeep)
                    }
                }
            }
            ForEach(Array(table.rows.enumerated()), id: \.offset) { _, row in
                GridRow {
                    ForEach(Array(row.enumerated()), id: \.offset) { _, cell in
                        cellText(cell).font(.callout)
                    }
                }
            }
        }
        .padding(Spacing.small)
        .frame(maxWidth: .infinity, alignment: .leading)
        .overlay(RoundedRectangle(cornerRadius: Radius.small).stroke(Color.secondary.opacity(0.3)))
    }

    private func cellText(_ cell: String) -> some View {
        Text(rich(cell))
            .fixedSize(horizontal: false, vertical: true)
            .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func writeBody(_ screen: LessonScreenWrite, _ session: LessonSession) -> some View {
        VStack(alignment: .leading, spacing: Spacing.small) {
            ForEach(screen.questions, id: \.key) { question in
                let verdict = session.verdict(screenId: screen.id, key: question.key)
                HStack {
                    Text("\(question.label))").foregroundStyle(.secondary).frame(width: 28, alignment: .leading)
                    TextField("", text: binding(question.key))
                        .textFieldStyle(.roundedBorder)
                        .keyboardType(screen.keyboard == .digits ? .numberPad : .default)
                        .autocorrectionDisabled()
                        .textInputAutocapitalization(.never)
                        .focused($focused, equals: question.key)
                        .disabled(session.isChecked)
                    if verdict == .correct {
                        Image(systemName: "checkmark").foregroundStyle(Palette.success)
                    }
                }
                feedback(label: nil, verdict: verdict, expected: question.answers.first ?? "", explanation: question.feedback)
            }
        }
    }

    private func choiceBody(_ screen: LessonScreenChoice, _ session: LessonSession) -> some View {
        VStack(alignment: .leading, spacing: Spacing.medium) {
            ForEach(screen.items, id: \.question.key) { item in
                let question = item.question
                let chosen = session.answer(screenId: screen.id, key: question.key)
                VStack(alignment: .leading, spacing: Spacing.tiny) {
                    Text("\(question.label))").foregroundStyle(.secondary)
                    HStack(spacing: Spacing.small) {
                        ForEach(item.options, id: \.self) { option in
                            Button {
                                change { $0.withAnswer(key: question.key, value: option) }
                            } label: {
                                Text(option)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, Spacing.small)
                                    .background(optionColor(option, chosen: chosen, answer: question.answers.first, checked: session.isChecked))
                                    .clipShape(RoundedRectangle(cornerRadius: Radius.small))
                            }
                            .buttonStyle(.plain)
                            .allowsHitTesting(!session.isChecked)
                        }
                    }
                    if session.isChecked, let explanation = question.feedback {
                        Text(rich(explanation)).font(.caption).foregroundStyle(.secondary)
                    }
                }
            }
        }
    }

    private func optionColor(_ option: String, chosen: String, answer: String?, checked: Bool) -> Color {
        if checked {
            if option == answer { return Palette.success.opacity(0.45) }
            if option == chosen { return Palette.failure.opacity(0.45) }
            return Color.secondary.opacity(0.12)
        }
        return option == chosen ? Palette.primary.opacity(0.45) : Color.secondary.opacity(0.12)
    }

    private func gapBody(_ screen: LessonScreenGapFill, _ session: LessonSession) -> some View {
        let questions = Dictionary(uniqueKeysWithValues: screen.questions.map { ($0.key, $0) })
        return VStack(alignment: .leading, spacing: Spacing.medium) {
            ForEach(Array(screen.sections.enumerated()), id: \.offset) { _, section in
                if let title = section.title { Text(title).font(.subheadline.weight(.semibold)) }
                ForEach(Array(section.lines.enumerated()), id: \.offset) { _, line in
                    VStack(alignment: .leading, spacing: Spacing.tiny) {
                        if let speaker = line.speaker {
                            Text(speaker).font(.caption.weight(.semibold)).foregroundStyle(Palette.accentDeep)
                        }
                        FlowLayout(spacing: Spacing.tiny) {
                            ForEach(Array(segments(line.text).enumerated()), id: \.offset) { _, segment in
                                switch segment {
                                case .text(let text):
                                    Text(text)
                                case .gap(let key):
                                    gapField(key, expected: questions[key]?.answers.first ?? "", session: session, screenId: screen.id)
                                }
                            }
                        }
                    }
                }
                ForEach(gapKeys(section), id: \.self) { key in
                    if let question = questions[key] {
                        feedback(
                            label: question.label,
                            verdict: session.verdict(screenId: screen.id, key: key),
                            expected: question.answers.first ?? "",
                            explanation: firstExplanation(question, in: section, questions: questions)
                        )
                    }
                }
            }
        }
    }

    private func firstExplanation(_ question: LessonQuestion, in section: GapSection, questions: [String: LessonQuestion]) -> String? {
        guard let explanation = question.feedback else { return nil }
        let earlier = gapKeys(section).prefix { $0 != question.key }
        return earlier.contains { questions[$0]?.feedback == explanation } ? nil : explanation
    }

    private func gapField(_ key: String, expected: String, session: LessonSession, screenId: String) -> some View {
        let verdict = session.verdict(screenId: screenId, key: key)
        let underline: Color = switch verdict {
        case nil: .secondary
        case .correct: Palette.success
        case .almost: Palette.warning
        default: Palette.failure
        }
        return TextField("", text: binding(key))
            .multilineTextAlignment(.center)
            .autocorrectionDisabled()
            .textInputAutocapitalization(.never)
            .focused($focused, equals: key)
            .disabled(session.isChecked)
            .frame(width: CGFloat(max(expected.count, 4)) * 11)
            .overlay(alignment: .bottom) { Rectangle().fill(underline).frame(height: 2) }
    }

    private func freeWritingBody(_ screen: LessonScreenFreeWriting, _ session: LessonSession) -> some View {
        VStack(alignment: .leading, spacing: Spacing.medium) {
            ForEach(Array(screen.fields.enumerated()), id: \.offset) { index, label in
                VStack(alignment: .leading, spacing: Spacing.tiny) {
                    Text(label).font(.subheadline.weight(.semibold))
                    TextEditor(text: binding(String(index)))
                        .autocorrectionDisabled()
                        .frame(minHeight: 110)
                        .focused($focused, equals: String(index))
                        .overlay(RoundedRectangle(cornerRadius: Radius.small).stroke(Color.secondary.opacity(0.3)))
                }
            }
            if session.isFinished {
                VStack(alignment: .leading, spacing: Spacing.small) {
                    Text(Strings.lessonFlowModelAnswer).font(.headline)
                    ForEach(screen.model, id: \.self) { Text(rich($0)).font(.callout) }
                    Text(Strings.lessonFlowChecklist).font(.headline).padding(.top, Spacing.small)
                    ForEach(screen.checklist, id: \.self) { Text(rich("• " + $0)).font(.callout) }
                }
                .padding(Spacing.medium)
                .background(Color.secondary.opacity(0.1))
                .clipShape(RoundedRectangle(cornerRadius: Radius.medium))
            }
        }
    }

    @ViewBuilder
    private func feedback(label: String?, verdict: AnswerVerdict?, expected: String, explanation: String?) -> some View {
        if let verdict {
            VStack(alignment: .leading, spacing: 2) {
                if verdict != .correct {
                    let prefix = label.map { "\($0)) " } ?? ""
                    Text(prefix + (verdict == .almost ? Strings.lessonFlowAlmostExpected(expected) : Strings.lessonFlowExpected(expected)))
                        .font(.callout)
                        .foregroundStyle(verdict == .almost ? Palette.warning : Palette.failure)
                }
                if let explanation {
                    Text(rich(explanation)).font(.caption).foregroundStyle(.secondary)
                }
            }
            .padding(.leading, 28)
        }
    }

    @ViewBuilder
    private func transcriptView(_ transcript: Transcript, unlocked: Bool) -> some View {
        if !unlocked {
            Text(Strings.lessonFlowTranscriptLocked).font(.caption).foregroundStyle(.secondary)
        } else {
            VStack(alignment: .leading, spacing: Spacing.small) {
                Button(transcriptOpen ? Strings.lessonFlowHideTranscript : Strings.lessonFlowShowTranscript) {
                    transcriptOpen.toggle()
                }
                if transcriptOpen {
                    VStack(alignment: .leading, spacing: Spacing.small) {
                        ForEach(Array(transcript.sections.enumerated()), id: \.offset) { _, section in
                            if let title = section.title { Text(title).font(.subheadline.weight(.semibold)) }
                            ForEach(Array(section.lines.enumerated()), id: \.offset) { _, line in
                                Text(rich((line.speaker.map { "**\($0):** " } ?? "") + line.text)).font(.callout)
                            }
                        }
                    }
                    .padding(Spacing.medium)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color.secondary.opacity(0.1))
                    .clipShape(RoundedRectangle(cornerRadius: Radius.medium))
                }
            }
        }
    }

    private func stepScoreView(_ score: StepScore) -> some View {
        VStack(alignment: .leading, spacing: Spacing.small) {
            Text(Strings.lessonFlowStepScore(score.step.title, Int(score.percent), Int(score.correct), Int(score.total)))
                .font(.headline)
                .foregroundStyle(score.passed ? Palette.success : Palette.failure)
            Text(score.passed ? Strings.lessonFlowStepPassed : Strings.lessonFlowStepFailed(Int(score.passMark * 100)))
                .font(.callout)
        }
        .padding(Spacing.medium)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.secondary.opacity(0.1))
        .clipShape(RoundedRectangle(cornerRadius: Radius.medium))
    }

    private func completedView(_ session: LessonSession) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Spacing.small) {
                Text(Strings.lessonFlowCompleted).font(.title2.weight(.semibold))
                Text(Strings.lessonFlowCompletedBody(session.script.title)).font(.callout)
                ForEach(session.script.vocabulary, id: \.polish) { phrase in
                    HStack {
                        Text(phrase.polish).font(.body.weight(.medium))
                        Spacer()
                        Text(phrase.english).font(.callout).foregroundStyle(.secondary)
                    }
                    Divider()
                }
            }
            .padding(Spacing.medium)
        }
        .safeAreaInset(edge: .bottom) {
            Button(Strings.lessonFlowDone) { dismiss() }
                .buttonStyle(.borderedProminent)
                .padding(Spacing.medium)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(.bar)
        }
    }

    // MARK: Footer

    private func footer(_ session: LessonSession) -> some View {
        HStack(spacing: Spacing.small) {
            if session.canGoBack {
                Button(Strings.lessonFlowBack) { change { $0.previous() } }.buttonStyle(.bordered)
            }
            footerAction(session)
            Spacer()
        }
        .padding(Spacing.medium)
        .background(.bar)
    }

    @ViewBuilder
    private func footerAction(_ session: LessonSession) -> some View {
        let screen = session.screen
        if screen.isGraded && !session.isChecked {
            Button(Strings.exerciseCheck) { change { $0.check() } }
                .buttonStyle(.borderedProminent)
                .disabled(!session.canCheck)
        } else if let writing = screen as? LessonScreenFreeWriting, !session.isFinished {
            Button(Strings.lessonFlowShowModel) { change { $0.finish() } }
                .buttonStyle(.borderedProminent)
                .disabled(!writing.fields.indices.contains { !session.answer(screenId: screen.id, key: String($0)).isEmpty })
        } else if !session.isFinished {
            Button(Strings.lessonFlowDone) { change { $0.finish().next() } }.buttonStyle(.borderedProminent)
        } else if let step = session.stepToRetry {
            Button(Strings.lessonFlowRetry) { change { $0.retryStep(step: step) } }.buttonStyle(.borderedProminent)
            Button(Strings.lessonFlowContinueAnyway) {
                if session.isLastScreen { dismiss() } else { change { $0.next() } }
            }
        } else if session.isLastScreen {
            AsyncButton {
                await finishLesson(session)
            } label: {
                Text(Strings.lessonFlowFinish)
            }
            .buttonStyle(.borderedProminent)
            .disabled(!session.isLessonComplete)
        } else {
            Button(Strings.lessonFlowNext) { change { $0.next() } }.buttonStyle(.borderedProminent)
        }
    }

    // MARK: State

    private func binding(_ key: String) -> Binding<String> {
        Binding(
            get: { session.map { $0.answer(screenId: $0.screen.id, key: key) } ?? "" },
            set: { value in change { $0.withAnswer(key: key, value: value) } }
        )
    }

    private func insert(_ letter: String) {
        guard let key = focused, let session else { return }
        let current = session.answer(screenId: session.screen.id, key: key)
        change { $0.withAnswer(key: key, value: current + letter) }
    }

    private func change(_ transform: (LessonSession) -> LessonSession) {
        guard let current = session else { return }
        let next = transform(current)
        if next.index != current.index {
            transcriptOpen = false
            focused = nil
            audio.stop()
        }
        session = next
        let previous = saving
        let progress = next.progress
        saving = Task {
            await previous?.value
            try? await deps.saveLessonProgress.invoke(id: lessonId, progress: progress)
        }
    }

    private func load() async {
        guard !loaded else { return }
        if let script = try? await deps.getLessonScript.invoke(id: lessonId) {
            let progress = try? await deps.getLessonProgress.invoke(id: lessonId)
            session = LessonSession.companion.start(script: script, progress: progress)
        }
        loaded = true
    }

    private func finishLesson(_ session: LessonSession) async {
        guard session.isLessonComplete else { return }
        audio.stop()
        try? await deps.setLessonCompleted.invoke(id: lessonId, isCompleted: true)
        completed = true
    }

    // MARK: Gaps

    private enum Segment {
        case text(String)
        case gap(String)
    }

    private func segments(_ text: String) -> [Segment] {
        var result: [Segment] = []
        var rest = Substring(text)
        while let open = rest.firstIndex(of: "["), let close = rest[open...].firstIndex(of: "]") {
            let before = rest[..<open].trimmingCharacters(in: .whitespaces)
            if !before.isEmpty { result.append(.text(before)) }
            result.append(.gap(String(rest[rest.index(after: open)..<close])))
            rest = rest[rest.index(after: close)...]
        }
        let tail = rest.trimmingCharacters(in: .whitespaces)
        if !tail.isEmpty { result.append(.text(tail)) }
        return result
    }

    private func gapKeys(_ section: GapSection) -> [String] {
        section.lines.flatMap { line in
            segments(line.text).compactMap { segment -> String? in
                if case .gap(let key) = segment { return key }
                return nil
            }
        }
    }
}
