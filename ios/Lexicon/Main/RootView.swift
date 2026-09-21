import SwiftUI
import Shared

struct RootView: View {
    @StateObject private var settings = SettingsStore()
    @State private var hasSynced = false

    var body: some View {
        Group {
            if hasSynced {
                TabView {
                    DashboardView()
                        .tabItem { Label(Strings.tabHome, systemImage: "square.grid.2x2") }
                    TrainingsView()
                        .tabItem { Label(Strings.tabTrainings, systemImage: "graduationcap") }
                    VocabularyView()
                        .tabItem { Label(Strings.tabWords, systemImage: "book") }
                    PlanView()
                        .tabItem { Label(Strings.tabPlan, systemImage: "calendar") }
                    SettingsView()
                        .tabItem { Label(Strings.tabSettings, systemImage: "gearshape") }
                }
            } else {
                SplashView { hasSynced = true }
            }
        }
        .environmentObject(settings)
        .preferredColorScheme(settings.preferredColorScheme)
    }
}

@MainActor
final class SettingsStore: ObservableObject {
    @Published private(set) var settings: AppSettings = deps.defaultSettings

    private var watcher: Cancellable?

    init() {
        watcher = deps.watchSettings { [weak self] value in
            self?.settings = value
        }
    }

    deinit { watcher?.cancel() }

    var preferredColorScheme: ColorScheme? {
        switch settings.themeMode {
        case .light: return .light
        case .dark: return .dark
        default: return nil
        }
    }
}
