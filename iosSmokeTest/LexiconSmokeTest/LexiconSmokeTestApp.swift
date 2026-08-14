import SwiftUI
import Shared

@main
struct LexiconSmokeTestApp: App {
    init() {
        IosKoinKt.doInitKoinIos()
    }

    var body: some Scene {
        WindowGroup { ContentView() }
    }
}

struct ContentView: View {
    @State private var result = "running…"

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Lexicon shared code on iOS")
                .font(.headline)
            Text(result)
                .font(.system(.footnote, design: .monospaced))
        }
        .padding()
        .task {
            do {
                result = try await SharedSmokeTest.shared.run()
            } catch {
                result = "FAIL threw: \(error)"
            }
            NSLog("SMOKE_TEST_RESULT_BEGIN\n%@\nSMOKE_TEST_RESULT_END", result)
        }
    }
}
