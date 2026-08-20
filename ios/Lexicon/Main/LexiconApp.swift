import SwiftUI
import Shared

@main
struct LexiconApp: App {
    init() {

        IosKoinKt.doInitKoinIos(pexelsApiKey: Keys.pexels, pixabayApiKey: Keys.pixabay)
    }

    var body: some Scene {
        WindowGroup {
            RootView()
        }
    }
}
