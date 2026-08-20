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
                        .tabItem { Label("Home", systemImage: "square.grid.2x2") }
                    TrainingsView()
                        .tabItem { Label("Trainings", systemImage: "graduationcap") }
                    VocabularyView()
                        .tabItem { Label("Words", systemImage: "book") }
                    PlanView()
                        .tabItem { Label("Plan", systemImage: "calendar") }
                    SettingsView()
                        .tabItem { Label("Settings", systemImage: "gearshape") }
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
    @Published private(set) var settings: AppSettings = AppSettings.companion.Default

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
