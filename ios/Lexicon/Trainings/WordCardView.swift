import SwiftUI
import Shared

struct WordCardView: View {
    let vocabularyIds: [Int64]

    @Environment(\.dismiss) private var dismiss
    @State private var steps: [WordCardStep] = []
    @State private var sessionId = ""
    @State private var index = 0
    @State private var seen: Set<Int> = []
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        Group {
            if let step = steps[safe: index] {
                let skin = TileSkin.standard(highlighted: true, scheme: scheme)
                TrainingScaffold(step: index, total: steps.count, state: .unanswered) {
                    Tile(skin: skin) {
                        if let url = step.imageUrl, let link = URL(string: url) {
                            AsyncImage(url: link) { image in
                                image.resizable().scaledToFill()
                            } placeholder: {
                                Color.secondary.opacity(0.15)
                            }
                            .frame(height: 200)
                            .frame(maxWidth: .infinity)
                            .clipped()
                            .clipShape(RoundedRectangle(cornerRadius: Radius.small))
                        }
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(step.translation).font(.title3).foregroundStyle(skin.onTile.muted)
                                Text(step.text).font(.largeTitle.weight(.semibold)).foregroundStyle(skin.onTile)
                                if !step.transcription.isEmpty {
                                    Text("[\(step.transcription)]").font(.callout).foregroundStyle(skin.onTile.muted)
                                }
                            }
                            Spacer()
                            Button { Speech.shared.speak(step.text) } label: {
                                Image(systemName: "speaker.wave.2").foregroundStyle(skin.onTile)
                            }
                        }
                    }
                } actions: {
                    HStack {
                        if index > 0 {
                            Button("Back") { index -= 1 }
                        }
                        Spacer()
                        Button(index == steps.count - 1 ? "Done" : "Next") {
                            if index == steps.count - 1 {
                                dismiss()
                            } else {
                                index += 1
                                Task { await record() }
                            }
                        }
                        .buttonStyle(.borderedProminent)
                    }
                }
            } else {
                ProgressView()
            }
        }
        .task {
            await start()
            await record()
        }
    }

    private func start() async {
        let response = try? await deps.startWordCard.invoke(
            request: StartWordCardSessionRequest(vocabularyIds: vocabularyIds.map { KotlinLong(value: $0) })
        )
        guard let response else { return }
        sessionId = response.sessionId
        steps = response.steps
    }

    private func record() async {
        guard let step = steps[safe: index], seen.insert(index).inserted else { return }
        try? await deps.recordWordCardSeen.invoke(
            request: RecordWordCardSeenRequest(
                sessionId: sessionId,
                stepIndex: Int32(step.stepIndex),
                vocabularyItemId: step.vocabularyItemId,
                text: step.text
            )
        )
    }
}
