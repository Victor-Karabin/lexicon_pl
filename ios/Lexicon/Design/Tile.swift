import SwiftUI

struct TileSkin {
    let sweep: [Color]
    let onTile: Color
    let medallion: Color
    let onMedallion: Color

    static func standard(highlighted: Bool = false, scheme: ColorScheme) -> TileSkin {
        let dark = scheme == .dark
        if highlighted {
            return TileSkin(
                sweep: dark
                    ? [Color(red: 0.29, green: 0.24, blue: 0.44), Color(red: 0.40, green: 0.27, blue: 0.36)]
                    : [Color(red: 0.87, green: 0.87, blue: 1.0), Color(red: 1.0, green: 0.87, blue: 0.93)],
                onTile: dark ? .white : Color(red: 0.13, green: 0.10, blue: 0.24),
                medallion: dark ? Palette.primary : Palette.accentDeep,
                onMedallion: dark ? Palette.onPrimary : .white
            )
        }
        return TileSkin(
            sweep: dark
                ? [Color(white: 0.16), Color(red: 0.22, green: 0.20, blue: 0.28)]
                : [Color(white: 0.95), Color(red: 0.90, green: 0.90, blue: 0.96)],
            onTile: dark ? .white : .black,
            medallion: dark ? Color(red: 0.35, green: 0.29, blue: 0.55) : Color(red: 0.85, green: 0.85, blue: 1.0),
            onMedallion: dark ? .white : Palette.accentDeep
        )
    }

    static func accent(_ accent: Color, onAccent: Color, scheme: ColorScheme) -> TileSkin {
        let dark = scheme == .dark
        return TileSkin(
            sweep: [dark ? Color(white: 0.16) : Color(white: 0.95), accent.opacity(0.22)],
            onTile: dark ? .white : .black,
            medallion: accent,
            onMedallion: onAccent
        )
    }
}

struct Tile<Content: View>: View {
    let skin: TileSkin
    var padding: CGFloat = Spacing.medium
    @ViewBuilder var content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: padding) {
            content
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(padding)
        .background(
            LinearGradient(colors: skin.sweep, startPoint: .topLeading, endPoint: .bottomTrailing)
        )
        .clipShape(RoundedRectangle(cornerRadius: Radius.medium, style: .continuous))
    }
}

struct Medallion<Content: View>: View {
    let skin: TileSkin
    var size: CGFloat = 52
    @ViewBuilder var content: Content

    var body: some View {
        ZStack {
            Circle().fill(skin.medallion)
            content
        }
        .frame(width: size, height: size)
    }
}

struct MedallionText: View {
    let text: String
    let skin: TileSkin

    var body: some View {
        Text(text).font(.title3.bold()).foregroundStyle(skin.onMedallion)
    }
}

struct MedallionIcon: View {
    let systemName: String
    let skin: TileSkin

    var body: some View {
        Image(systemName: systemName).font(.title3).foregroundStyle(skin.onMedallion)
    }
}

struct StatChip: View {
    let systemName: String
    let text: String
    let skin: TileSkin

    var body: some View {
        HStack(spacing: Spacing.tiny) {
            Image(systemName: systemName).font(.caption2)

            Text(text).font(.caption).lineLimit(1).fixedSize(horizontal: true, vertical: false)
        }
        .foregroundStyle(skin.onTile)
        .padding(.horizontal, Spacing.small)
        .padding(.vertical, Spacing.tiny)
        .background(skin.onTile.opacity(0.1))
        .clipShape(RoundedRectangle(cornerRadius: Radius.small, style: .continuous))
    }
}
