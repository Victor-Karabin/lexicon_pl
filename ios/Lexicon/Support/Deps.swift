import Foundation
import Shared

/// Everything resolved from Koin, in one place.
///
/// `IosDependencies` is the shared framework's own accessor object — naming the use
/// cases there rather than casting from `Any` here is what keeps the Swift side
/// type-checked.
let deps = IosDependencies.shared

/// Kotlin `Set<VocabularyId>` and friends arrive as `Set<AnyHashable>`; these turn
/// them back into something Swift can compare.
extension VocabularyId {
    var raw: Int64 { value }
}

extension LocalizedText {
    /// The Kotlin `resolve` extension is not visible to Swift, so this is it.
    func text(_ languageTag: String = "en") -> String {
        values[languageTag] ?? values["en"] ?? values.values.first ?? ""
    }
}
