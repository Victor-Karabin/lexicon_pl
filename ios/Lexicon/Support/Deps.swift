import Foundation
import Shared

let deps = IosDependencies.shared

extension VocabularyId {
    var raw: Int64 { value }
}

extension LocalizedText {

    func text(_ languageTag: String = "en") -> String {
        values[languageTag] ?? values["en"] ?? values.values.first ?? ""
    }
}
