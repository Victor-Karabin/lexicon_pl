import SwiftUI
import Shared

struct DashboardView: View {
    @StateObject private var model = DashboardModel()
    @Environment(\.colorScheme) private var scheme
    @State private var launching: ProgramTurn?
    @State private var showingCards = false
    @State private var isAdvancing = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: Spacing.medium) {
                    if let program = model.program {
                        card(program)
                    } else {
                        VStack(spacing: Spacing.large) {
                            Text("No program yet. Build one on the Plan tab and it will show up here.")
                                .multilineTextAlignment(.center)
                                .foregroundStyle(.secondary)
                        }
                        .padding(Spacing.xl)
                    }

                    if let studyTime = model.studyTime {
                        StudyTimeCard(history: studyTime)
                    }
                }
                .padding(Spacing.medium)
            }
            .navigationTitle("Dashboard")
            .navigationDestination(item: $launching) { turn in
                TrainingHost(entry: turn.entry, vocabularyIds: turn.wordIds)
                    .id(turn.id)
                    .environment(\.onTrainingFinished, { Task { await finishTurn() } })
            }
            .navigationDestination(isPresented: $showingCards) {
                ProgramCardsView(programId: model.program?.id)
            }
            .task { await model.load() }
            .refreshable { await model.load() }
        }
    }

    private func card(_ program: Program) -> some View {
        let skin = TileSkin.standard(highlighted: true, scheme: scheme)
        return Tile(skin: skin) {
            HStack(spacing: Spacing.medium) {
                ProgressRing(fraction: model.overall, skin: skin) {
                    Medallion(skin: skin) { MedallionIcon(systemName: "heart.fill", skin: skin) }
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text("Continuing").font(.caption).foregroundStyle(skin.onTile.muted)
                    Text(program.title.text()).font(.title3.weight(.semibold)).foregroundStyle(skin.onTile)
                    Text("\(Int(model.overall * 100))% through").font(.callout).foregroundStyle(skin.onTile.muted)
                }
                Spacer()
                if model.streak > 0 {
                    VStack(spacing: 0) {
                        Image(systemName: "flame.fill").foregroundStyle(skin.onTile)
                        Text("\(model.streak)").font(.headline).foregroundStyle(skin.onTile)
                        Text("day streak").font(.caption2).foregroundStyle(skin.onTile.muted)
                    }
                }
            }

            ForEach(model.metrics, id: \.type) { metric in
                metricRow(metric, skin: skin)
            }

            if model.totalTrainings > 0 {
                VStack(alignment: .leading, spacing: Spacing.small) {
                    HStack {
                        Text("Trainings today").font(.caption).foregroundStyle(skin.onTile.muted)
                        Spacer()
                        Text("\(model.doneTrainings) / \(model.totalTrainings)")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(skin.onTile)
                    }

                    HStack(spacing: Spacing.tiny) {
                        ForEach(0..<model.totalTrainings, id: \.self) { i in
                            Circle()
                                .fill(i < model.doneTrainings ? skin.onTile : skin.onTile.opacity(0.25))
                                .frame(width: 10, height: 10)
                        }
                    }
                }
            }

            if model.isDayComplete {
                Text("Today is done. Come back tomorrow.")
                    .font(.subheadline)
                    .foregroundStyle(skin.onTile.muted)
            } else if model.nothingToPractise {
                Text("Nothing to practise right now. Add words to your study set, or come back once reviews are due.")
                    .font(.subheadline)
                    .foregroundStyle(skin.onTile.muted)
            } else {
                Button {
                    Task { await start() }
                } label: {
                    Label(model.continueLabel, systemImage: model.showsCards ? "book" : "play.fill")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .tint(skin.medallion)
            }
        }
    }

    private func metricRow(_ metric: ProgressMetric, skin: TileSkin) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            HStack {
                Text(model.label(for: metric.type)).font(.caption).foregroundStyle(skin.onTile.muted)
                Spacer()
                Text(model.value(for: metric)).font(.caption.weight(.semibold)).foregroundStyle(skin.onTile)
            }
            if metric.type != .consistency {
                ProgressView(value: min(Double(metric.current), Double(metric.target)), total: Double(max(metric.target, 1)))
                    .tint(skin.onTile)
            }
        }
    }

    private func start() async {
        if model.showsCards {
            showingCards = true
            return
        }
        launching = await model.nextTraining()
    }

    private func finishTurn() async {
        guard !isAdvancing else { return }
        isAdvancing = true
        defer { isAdvancing = false }
        launching = await model.advance()
        if launching == nil { await model.load() }
    }
}

struct ProgressRing<Content: View>: View {
    let fraction: Double
    let skin: TileSkin
    @ViewBuilder var content: Content

    var body: some View {
        ZStack {
            Circle().stroke(skin.onTile.opacity(0.25), lineWidth: 5)
            Circle()
                .trim(from: 0, to: max(0, min(1, fraction)))
                .stroke(skin.onTile, style: StrokeStyle(lineWidth: 5, lineCap: .round))
                .rotationEffect(.degrees(-90))
            content
        }
        .frame(width: 72, height: 72)
    }
}

extension TrainingEntry: Hashable {
    static func == (lhs: TrainingEntry, rhs: TrainingEntry) -> Bool { lhs.id == rhs.id }
    func hash(into hasher: inout Hasher) { hasher.combine(id) }
}
