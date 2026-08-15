import SwiftUI
import Shared

/// What the learner should see first: the program they are on, and how it is going.
struct DashboardView: View {
    @StateObject private var model = DashboardModel()
    @Environment(\.colorScheme) private var scheme
    @State private var launching: TrainingEntry?
    @State private var showingCards = false

    var body: some View {
        NavigationStack {
            ScrollView {
                if let program = model.program {
                    card(program)
                        .padding(Spacing.medium)
                } else {
                    VStack(spacing: Spacing.large) {
                        Text("No program yet. Build one on the Plan tab and it will show up here.")
                            .multilineTextAlignment(.center)
                            .foregroundStyle(.secondary)
                    }
                    .padding(Spacing.xl)
                }
            }
            .navigationTitle("Dashboard")
            .navigationDestination(item: $launching) { entry in
                TrainingHost(entry: entry, vocabularyIds: model.sessionWordIds)
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
                    // One pip per turn: a count is a fact, pips are a thing to fill in.
                    HStack(spacing: Spacing.tiny) {
                        ForEach(0..<model.totalTrainings, id: \.self) { i in
                            Circle()
                                .fill(i < model.doneTrainings ? skin.onTile : skin.onTile.opacity(0.25))
                                .frame(width: 10, height: 10)
                        }
                    }
                }
            }

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
        guard let next = await model.nextTraining() else { return }
        launching = TrainingCatalog.entry(id: next)
    }
}

/// How far through the program, drawn around whatever sits inside it.
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
