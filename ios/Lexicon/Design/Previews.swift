import SwiftUI

/// Previews come in pairs here, the way `@LightDarkPreview` pairs them on Android.
///
/// A tile is built from a `TileSkin` that reads the colour scheme, so a preview in
/// one scheme only proves half of it — and the half that usually breaks is the one
/// nobody looked at.
struct LightDark<Content: View>: View {
    let title: String
    @ViewBuilder var content: Content

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Spacing.large) {
                pane("light", scheme: .light)
                pane("dark", scheme: .dark)
            }
            .padding(Spacing.medium)
        }
    }

    private func pane(_ name: String, scheme: ColorScheme) -> some View {
        VStack(alignment: .leading, spacing: Spacing.small) {
            Text("\(title) · \(name)")
                .font(.caption)
                .foregroundStyle(.secondary)
            content
                .environment(\.colorScheme, scheme)
                .padding(Spacing.small)
                .background(scheme == .dark ? Color.black : Color.white)
                .clipShape(RoundedRectangle(cornerRadius: Radius.medium))
        }
    }
}

// MARK: - the design system

#Preview("Tiles") {
    LightDark(title: "Tiles") {
        TilePreviewBody()
    }
}

/// Both coats, the medallion in each of its three forms, and the chips — the whole
/// vocabulary a screen is built from, on one page.
private struct TilePreviewBody: View {
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        VStack(spacing: Spacing.medium) {
            tile(TileSkin.standard(highlighted: true, scheme: scheme), title: "Under way")
            tile(TileSkin.standard(scheme: scheme), title: "Waiting")
            tile(
                TileSkin.accent(Color(hex: "#EF6C00"), onAccent: .white, scheme: scheme),
                title: "A preset's own colour"
            )
        }
    }

    private func tile(_ skin: TileSkin, title: String) -> some View {
        Tile(skin: skin) {
            HStack(spacing: Spacing.medium) {
                Medallion(skin: skin) { MedallionIcon(systemName: "heart.fill", skin: skin) }
                Medallion(skin: skin, size: 36) { MedallionText(text: "A1", skin: skin) }
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).font(.headline).foregroundStyle(skin.onTile)
                    Text("Secondary text sits at three quarters.")
                        .font(.caption)
                        .foregroundStyle(skin.onTile.muted)
                }
                Spacer()
            }
            HStack(spacing: Spacing.small) {
                StatChip(systemName: "character.book.closed", text: "1000 words", skin: skin)
                StatChip(systemName: "book", text: "10 new a day", skin: skin)
            }
            ProgressView(value: 0.4)
                .tint(skin.onTile)
        }
    }
}

// MARK: - the training frame

#Preview("Answer states") {
    LightDark(title: "Answer states") {
        VStack(spacing: Spacing.medium) {
            ForEach(
                [
                    AnswerState.correct,
                    .incorrect(expected: "chleb"),
                    .skipped(expected: "woda"),
                ],
                id: \.label
            ) { state in
                VStack(spacing: Spacing.tiny) {
                    Text(state.label).foregroundStyle(state.tint).font(.headline)
                    if let expected = state.expected {
                        Text("Expected: \(expected)").font(.callout).foregroundStyle(.secondary)
                    }
                }
            }
        }
    }
}

#Preview("Session result") {
    SessionResultView(
        tally: SessionTally(correct: 7, incorrect: 2, skipped: 1, tipsUsed: 1),
        onDone: {}
    )
}

#Preview("Session result · dark") {
    SessionResultView(
        tally: SessionTally(correct: 7, incorrect: 2, skipped: 1, tipsUsed: 1),
        onDone: {}
    )
    .preferredColorScheme(.dark)
}

// MARK: - the progress ring

#Preview("Progress ring") {
    LightDark(title: "Progress ring") {
        RingPreviewBody()
    }
}

private struct RingPreviewBody: View {
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        let skin = TileSkin.standard(highlighted: true, scheme: scheme)
        Tile(skin: skin) {
            HStack(spacing: Spacing.large) {
                ForEach([0.0, 0.35, 1.0], id: \.self) { fraction in
                    ProgressRing(fraction: fraction, skin: skin) {
                        Medallion(skin: skin) { MedallionIcon(systemName: "heart.fill", skin: skin) }
                    }
                }
            }
        }
    }
}
