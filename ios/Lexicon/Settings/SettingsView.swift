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
                        heading("paintpalette", "Appearance", skin)
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
                        heading("figure.strengthtraining.traditional", "Training", skin)
                        HStack {
                            Text("Steps per session").foregroundStyle(skin.onTile)
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
                            in: Double(AppSettings.companion.MIN_STEP_COUNT)...Double(AppSettings.companion.MAX_STEP_COUNT),
                            step: 1
                        )
                        Text("Applies to new training sessions only.")
                            .font(.caption)
                            .foregroundStyle(skin.onTile.muted)
                    }
                }
                .padding(Spacing.medium)
            }
            .navigationTitle("Settings")
        }
    }

    private var themeModes: [ThemeMode] { [.system, .light, .dark] }

    private func label(for mode: ThemeMode) -> String {
        switch mode {
        case .light: return "Light"
        case .dark: return "Dark"
        default: return "System"
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
