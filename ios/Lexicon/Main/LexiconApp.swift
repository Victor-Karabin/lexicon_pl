import SwiftUI
import Shared

@main
struct LexiconApp: App {
    init() {
        // The equivalent of LexiconApplication on Android: Koin has to be up before
        // any view resolves a use case from it. The keys come from the generated
        // file, and are blank when local.properties has none.
        IosKoinKt.doInitKoinIos(pexelsApiKey: Keys.pexels, pixabayApiKey: Keys.pixabay)
    }

    var body: some Scene {
        WindowGroup {
            RootView()
        }
    }
}
