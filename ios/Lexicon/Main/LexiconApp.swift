import SwiftUI
import Shared

@main
struct LexiconApp: App {
    init() {
        // The equivalent of LexiconApplication on Android: Koin has to be up before
        // any view resolves a use case from it.
        IosKoinKt.doInitKoinIos()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
        }
    }
}
