import SwiftUI

/// A button whose action is asynchronous, which most of them are here: nearly every
/// tap resolves into a suspend function on the shared framework.
struct AsyncButton<Label: View>: View {
    let action: () async -> Void
    @ViewBuilder var label: Label

    @State private var isRunning = false

    var body: some View {
        Button {
            guard !isRunning else { return }
            isRunning = true
            Task {
                await action()
                isRunning = false
            }
        } label: {
            label
        }
        .disabled(isRunning)
    }
}
