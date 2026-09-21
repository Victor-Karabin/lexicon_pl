import SwiftUI
import Shared

struct SettingsView: View {
    @EnvironmentObject private var store: SettingsStore
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: Spacing.medium) {
                    let skin = TileSkin.standard(scheme: scheme)

                    Tile(skin: skin) {
                        heading("paintpalette", Strings.settingsAppearance, skin)
                        ForEach(themeModes, id: \.self) { mode in
                            Button {
                                Task { try? await deps.updateThemeMode.invoke(themeMode: mode) }
                            } label: {
                                HStack {
                                    Image(systemName: mode == store.settings.themeMode ? "largecircle.fill.circle" : "circle")
                                    Text(label(for: mode))
                                    Spacer()
                                }
                                .foregroundStyle(skin.onTile)
                            }
                        }
                    }

                    Tile(skin: skin) {
                        heading("figure.strengthtraining.traditional", Strings.settingsTraining, skin)
                        HStack {
                            Text(Strings.settingsStepCount).foregroundStyle(skin.onTile)
                            Spacer()
                            Text("\(store.settings.stepCount)").bold().foregroundStyle(skin.onTile)
                        }
                        Slider(
                            value: Binding(
                                get: { Double(store.settings.stepCount) },
                                set: { value in
                                    Task { try? await deps.updateStepCount.invoke(stepCount: Int32(value.rounded())) }
                                }
                            ),
                            in: Double(deps.minStepCount)...Double(deps.maxStepCount),
                            step: 1
                        )
                        Text(Strings.settingsStepCountNote)
                            .font(.caption)
                            .foregroundStyle(skin.onTile.muted)
                    }
                }
                .padding(Spacing.medium)
            }
            .navigationTitle(Strings.tabSettings)
        }
    }

    private var themeModes: [ThemeMode] { [.system, .light, .dark] }

    private func label(for mode: ThemeMode) -> String {
        switch mode {
        case .light: return Strings.settingsThemeLight
        case .dark: return Strings.settingsThemeDark
        default: return Strings.settingsThemeSystem
        }
    }

    @ViewBuilder
    private func heading(_ icon: String, _ title: String, _ skin: TileSkin) -> some View {
        HStack(spacing: Spacing.medium) {
            Medallion(skin: skin, size: 36) { MedallionIcon(systemName: icon, skin: skin) }
            Text(title).font(.headline).foregroundStyle(skin.onTile)
        }
    }
}
