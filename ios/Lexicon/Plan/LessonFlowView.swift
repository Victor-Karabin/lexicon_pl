import Shared
import SwiftUI

private func rich(_ text: String) -> AttributedString {
    (try? AttributedString(markdown: text, options: .init(interpretedSyntax: .inlineOnlyPreservingWhitespace))) ?? AttributedString(text)
}

private let labelWidth: CGFloat = 28

struct LessonFlowView: View {
    let lessonId: LessonId
    let lessonNumber: Int
    let hasNextLesson: Bool
    let onNextLesson: () -> Void

    @Environment(\.dismiss) private var dismiss
    @ObservedObject private var audio = LessonAudio.shared
    @State private var session: LessonSession?
    @State private var loaded = false
    @State private var transcriptOpen = false
    @State private var results: LessonResults?
    @State private var reviewing = false
    @State private var reviewProblem: String?
    @State private var openInfo: String?
    @State private var saving: Task<Void, Never>?
    @FocusState private var focused: String?

    var body: some View {
        Group {
            if let session {
                if let results {
                    resultsView(results)
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
        .task { await load() }
        .onDisappear { audio.stop() }
    }

    private var title: String {
        guard let session else { return "" }
        if results != nil { return Strings.courseLessonNumber(lessonNumber) }
        return Strings.lessonFlowPosition(session.screen.id, Int(session.position), Int(session.total))
    }

    // MARK: Screens

    private func screenView(_ session: LessonSession) -> some View {
        let screen = session.screen
        return ScrollView {
            VStack(alignment: .leading, spacing: Spacing.medium) {
                Text(screen.title).font(.title2.weight(.semibold))
                Text(rich(screen.instruction)).font(.callout)
                ForEach(screen.tracks, id: \.id) { trackPlayer($0) }

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
                if let transcript = screen.transcript, session.isTranscriptUnlocked(screen: screen) {
                    transcriptView(transcript)
                }
            }
            .padding(Spacing.medium)
        }
        .scrollDismissesKeyboard(.interactively)
        .safeAreaInset(edge: .bottom) {
            if focused == nil { footer(session) }
        }
        .id(session.index)
    }

    private func trackPlayer(_ track: LessonTrack, showLabel: Bool = true) -> some View {
        VStack(alignment: .leading, spacing: Spacing.tiny) {
            if showLabel, let label = track.label { Text(label).font(.subheadline.weight(.semibold)) }
            HStack(spacing: Spacing.small) {
                AsyncButton {
                    await audio.toggle(file: track.file, remoteId: track.remoteId)
                } label: {
                    Label(
                        audio.playing == track.file ? Strings.exercisePause : Strings.exercisePlay,
                        systemImage: audio.playing == track.file ? "pause.fill" : "play.fill"
                    )
                }
                .buttonStyle(.borderedProminent)
                if audio.playing == track.file || audio.paused == track.file {
                    AsyncButton {
                        await audio.play(file: track.file, remoteId: track.remoteId)
                    } label: {
                        Label(Strings.exerciseReplay, systemImage: "gobackward")
                    }
                    .buttonStyle(.bordered)
                }
            }
            if audio.unavailable.contains(track.file) {
                Text(Strings.exerciseAudioUnavailable).font(.caption).foregroundStyle(.secondary)
            }
        }
    }

    private func card<Content: View>(_ title: String?, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: Spacing.small) {
            if let title { Text(title).font(.subheadline.weight(.semibold)) }
            content()
        }
        .padding(Spacing.medium)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.secondary.opacity(0.1))
        .clipShape(RoundedRectangle(cornerRadius: Radius.medium))
    }

    private func referenceBody(_ screen: LessonScreenReference) -> some View {
        VStack(alignment: .leading, spacing: Spacing.medium) {
            ForEach(Array(screen.tables.enumerated()), id: \.offset) { _, table in
                tableView(table)
            }
            ForEach(Array(screen.groups.enumerated()), id: \.offset) { _, group in
                card(group.title) {
                    if let track = group.track { trackPlayer(track, showLabel: false) }
                    phraseList(group.phrases)
                }
            }
            ForEach(screen.notes, id: \.self) { note in
                Text(rich(note)).font(.callout)
            }
        }
    }

    private func phraseList(_ phrases: [LessonPhrase]) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            ForEach(Array(phrases.enumerated()), id: \.offset) { index, phrase in
                if index > 0 { Divider() }
                VStack(alignment: .leading, spacing: 2) {
                    Text(phrase.polish).font(.body.weight(.medium))
                    Text(phrase.english).font(.callout).foregroundStyle(.secondary)
                }
                .padding(.vertical, Spacing.small)
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
                let infoKey = "\(screen.id)/\(question.key)"
                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: Spacing.small) {
                        Text("\(question.label))").foregroundStyle(.secondary).frame(width: labelWidth, alignment: .leading)
                        TextField("", text: binding(question.key))
                            .keyboardType(screen.keyboard == .digits ? .numberPad : .default)
                            .autocorrectionDisabled()
                            .textInputAutocapitalization(.never)
                            .focused($focused, equals: question.key)
                            .disabled(session.isChecked)
                            .padding(.horizontal, Spacing.small)
                            .padding(.vertical, 6)
                            .overlay(RoundedRectangle(cornerRadius: Radius.small).stroke(verdictColor(verdict, idle: Color.secondary.opacity(0.4))))
                        if verdict == .correct {
                            Image(systemName: "checkmark").foregroundStyle(Palette.success)
                        }
                        if session.isChecked, question.feedback != nil { infoButton(infoKey) }
                    }
                    if let verdict, verdict != .correct {
                        expected(question.answers.first ?? "", verdict: verdict).padding(.leading, labelWidth + Spacing.small)
                    }
                    if openInfo == infoKey, let explanation = question.feedback {
                        explanationText(explanation).padding(.leading, labelWidth + Spacing.small)
                    }
                }
            }
        }
    }

    private func choiceBody(_ screen: LessonScreenChoice, _ session: LessonSession) -> some View {
        VStack(alignment: .leading, spacing: Spacing.small) {
            ForEach(screen.items, id: \.question.key) { item in
                let question = item.question
                let chosen = session.answer(screenId: screen.id, key: question.key)
                let infoKey = "\(screen.id)/\(question.key)"
                VStack(alignment: .leading, spacing: Spacing.tiny) {
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
                        if session.isChecked, question.feedback != nil { infoButton(infoKey) }
                    }
                    if openInfo == infoKey, let explanation = question.feedback {
                        explanationText(explanation)
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
                card(section.title) {
                    if let track = section.track { trackPlayer(track, showLabel: false) }
                    ForEach(Array(section.lines.enumerated()), id: \.offset) { _, line in
                        let parts = segments(line.text)
                        VStack(alignment: .leading, spacing: 2) {
                            FlowLayout(spacing: Spacing.tiny) {
                                if let speaker = line.speaker {
                                    Text("\(speaker):").fontWeight(.semibold).foregroundStyle(Palette.accentDeep)
                                }
                                ForEach(Array(parts.enumerated()), id: \.offset) { _, part in
                                    switch part {
                                    case .text(let text):
                                        Text(text)
                                    case .gap(let key):
                                        gapField(key, question: questions[key], session: session, screenId: screen.id)
                                        if session.isChecked, questions[key]?.feedback != nil { infoButton("\(screen.id)/\(key)") }
                                    }
                                }
                            }
                            ForEach(gapKeys(parts), id: \.self) { key in
                                if openInfo == "\(screen.id)/\(key)", let explanation = questions[key]?.feedback {
                                    explanationText(explanation)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private func gapField(_ key: String, question: LessonQuestion?, session: LessonSession, screenId: String) -> some View {
        let verdict = session.verdict(screenId: screenId, key: key)
        let answer = question?.answers.first ?? ""
        return VStack(spacing: 0) {
            TextField("", text: binding(key))
                .multilineTextAlignment(.center)
                .autocorrectionDisabled()
                .textInputAutocapitalization(.never)
                .focused($focused, equals: key)
                .disabled(session.isChecked)
                .overlay(alignment: .bottom) { Rectangle().fill(verdictColor(verdict, idle: .secondary)).frame(height: 2) }
            if let verdict, verdict != .correct {
                Text(answer).font(.caption).foregroundStyle(verdictColor(verdict, idle: .secondary))
            }
        }
        .frame(width: CGFloat(max(answer.count, 4)) * 11)
    }

    private func freeWritingBody(_ screen: LessonScreenFreeWriting, _ session: LessonSession) -> some View {
        VStack(alignment: .leading, spacing: Spacing.medium) {
            ForEach(Array(screen.fields.enumerated()), id: \.offset) { index, label in
                TextField(label, text: binding(String(index)), axis: .vertical)
                    .lineLimit(4, reservesSpace: true)
                    .autocorrectionDisabled()
                    .focused($focused, equals: String(index))
                    .disabled(session.isFinished || reviewing)
                    .padding(Spacing.small)
                    .overlay(RoundedRectangle(cornerRadius: Radius.small).stroke(Color.secondary.opacity(0.4)))
            }
            if reviewing {
                HStack(spacing: Spacing.small) {
                    ProgressView()
                    Text(Strings.lessonFlowReviewing).font(.callout)
                }
            }
            if let reviewProblem {
                Text(reviewProblem).font(.callout).foregroundStyle(.secondary)
            }
            if let review = session.review(screenId: screen.id) {
                card(nil) {
                    if !review.strengths.isEmpty {
                        Text(Strings.lessonFlowReviewStrengths).font(.headline)
                        ForEach(review.strengths, id: \.self) { Text(rich("• " + $0)).font(.callout) }
                    }
                    if !review.improvements.isEmpty {
                        Text(Strings.lessonFlowReviewImprovements).font(.headline).padding(.top, Spacing.small)
                        ForEach(review.improvements, id: \.self) { Text(rich("• " + $0)).font(.callout) }
                    }
                }
            }
            if session.isFinished {
                card(Strings.lessonFlowModelAnswer) {
                    ForEach(screen.model, id: \.self) { Text(rich($0)).font(.callout) }
                }
            }
        }
    }

    private func infoButton(_ key: String) -> some View {
        Button {
            openInfo = openInfo == key ? nil : key
        } label: {
            Image(systemName: "info.circle").imageScale(.large)
        }
        .buttonStyle(.borderless)
        .accessibilityLabel(Strings.lessonFlowExplain)
    }

    private func explanationText(_ text: String) -> some View {
        Text(rich(text)).font(.caption).foregroundStyle(.secondary)
    }

    private func expected(_ answer: String, verdict: AnswerVerdict) -> some View {
        Text(verdict == .almost ? Strings.lessonFlowAlmostExpected(answer) : Strings.lessonFlowExpected(answer))
            .font(.caption)
            .foregroundStyle(verdictColor(verdict, idle: .secondary))
    }

    private func verdictColor(_ verdict: AnswerVerdict?, idle: Color) -> Color {
        switch verdict {
        case nil: idle
        case .correct: Palette.success
        case .almost: Palette.warning
        default: Palette.failure
        }
    }

    private func transcriptView(_ transcript: Transcript) -> some View {
        VStack(alignment: .leading, spacing: Spacing.small) {
            Button(transcriptOpen ? Strings.lessonFlowHideTranscript : Strings.lessonFlowShowTranscript) {
                transcriptOpen.toggle()
            }
            if transcriptOpen {
                card(nil) {
                    ForEach(Array(transcript.sections.enumerated()), id: \.offset) { _, section in
                        if let title = section.title { Text(title).font(.subheadline.weight(.semibold)) }
                        ForEach(Array(section.lines.enumerated()), id: \.offset) { _, line in
                            Text(rich((line.speaker.map { "**\($0):** " } ?? "") + line.text)).font(.callout)
                        }
                    }
                }
            }
        }
    }

    private func resultsView(_ results: LessonResults) -> some View {
        let overall = results.overall
        return ScrollView {
            VStack(alignment: .leading, spacing: Spacing.medium) {
                Text(Strings.lessonFlowCompleted).font(.title2.weight(.semibold))
                Text(Strings.lessonFlowResults(Int(overall.correct), Int(overall.total), Int(overall.percent))).font(.headline)
                card(nil) {
                    ForEach(Array(results.steps.enumerated()), id: \.offset) { index, step in
                        if index > 0 { Divider() }
                        HStack {
                            Text(step.step.title)
                            Spacer()
                            Text(Strings.lessonFlowStepResult(Int(step.score.correct), Int(step.score.total))).foregroundStyle(.secondary)
                        }
                        .padding(.vertical, Spacing.tiny)
                    }
                }
            }
            .padding(Spacing.medium)
        }
        .safeAreaInset(edge: .bottom) {
            HStack(spacing: Spacing.small) {
                if hasNextLesson {
                    Button(Strings.lessonFlowNextLesson) {
                        onNextLesson()
                        dismiss()
                    }.buttonStyle(.borderedProminent)
                }
                Button(Strings.lessonFlowClose) { dismiss() }.buttonStyle(.bordered)
                Spacer()
            }
            .padding(Spacing.medium)
            .background(.bar)
        }
    }

    // MARK: Footer

    private func footer(_ session: LessonSession) -> some View {
        HStack(spacing: Spacing.small) {
            if session.canGoBack {
                Button(Strings.lessonFlowBack) { change { $0.previous() } }.buttonStyle(.bordered)
            }
            if session.needsCheck {
                Button(Strings.exerciseCheck) { check(session) }
                    .buttonStyle(.borderedProminent)
                    .disabled(!session.canCheck || reviewing)
            } else if session.isLastScreen {
                AsyncButton {
                    await next(session)
                } label: {
                    Text(Strings.lessonFlowFinish)
                }
                .buttonStyle(.borderedProminent)
            } else {
                AsyncButton {
                    await next(session)
                } label: {
                    Text(Strings.lessonFlowNext)
                }
                .buttonStyle(.borderedProminent)
            }
            Spacer()
        }
        .padding(Spacing.medium)
        .background(.bar)
    }

    // MARK: State

    private func binding(_ key: String) -> Binding<String> {
        Binding(
            get: { session.map { $0.answer(screenId: $0.screen.id, key: key) } ?? "" },
            set: { value in change { $0.withAnswer(key: key, value: value) } }
        )
    }

    private func check(_ session: LessonSession) {
        guard let writing = session.screen as? LessonScreenFreeWriting else {
            change { $0.check() }
            return
        }
        guard session.canCheck, !reviewing else { return }
        focused = nil
        reviewing = true
        reviewProblem = nil
        let answers = writing.fields.indices.map { session.answer(screenId: writing.id, key: String($0)) }
        Task {
            let outcome = try? await deps.reviewWriting.invoke(screen: writing, answers: answers)
            reviewing = false
            if let reviewed = outcome as? WritingReviewOutcomeReviewed {
                change { $0.withReview(review: reviewed.review) }
            } else {
                reviewProblem = outcome is WritingReviewOutcomeOffline ? Strings.lessonFlowReviewOffline : Strings.lessonFlowReviewUnavailable
                change { $0.check() }
            }
        }
    }

    private func next(_ session: LessonSession) async {
        guard !session.needsCheck else { return }
        let next = session.next()
        change { _ in next }
        guard session.isLastScreen, next.isAtEnd else { return }
        audio.stop()
        try? await deps.setLessonCompleted.invoke(id: lessonId, isCompleted: true)
        results = next.results
    }

    private func change(_ transform: (LessonSession) -> LessonSession) {
        guard let current = session else { return }
        let next = transform(current)
        if next.index != current.index {
            transcriptOpen = false
            openInfo = nil
            reviewProblem = nil
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
            session = LessonSession.companion.resume(script: script, progress: progress)
        }
        loaded = true
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
            result += words(rest[..<open])
            result.append(.gap(String(rest[rest.index(after: open)..<close])))
            rest = rest[rest.index(after: close)...]
        }
        return result + words(rest)
    }

    private func words(_ text: Substring) -> [Segment] {
        text.split(separator: " ").map { .text(String($0)) }
    }

    private func gapKeys(_ parts: [Segment]) -> [String] {
        parts.compactMap { part -> String? in
            if case .gap(let key) = part { return key }
            return nil
        }
    }
}
