import SwiftUI
import Shared

/// The five tabs, in the order Android has them.
///
/// Two of the labels are shorter than the screens they open. Five tabs on a 375pt
/// phone leaves each about seventy points, and "Dashboard" and "Vocabulary" fill it
/// edge to edge before Dynamic Type is turned up at all. The screens keep their full
/// names in their navigation titles — it is only the tab that is abbreviated, which
/// is the ordinary way round on iOS.
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

/// The theme setting, which every screen is drawn under.
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
