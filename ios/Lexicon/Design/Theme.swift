import SwiftUI

enum Palette {
    static let primary = Color(red: 0.71, green: 0.76, blue: 1.0)
    static let onPrimary = Color(red: 0.16, green: 0.11, blue: 0.36)
    static let accentDeep = Color(red: 0.24, green: 0.35, blue: 1.0)

    static let success = Color(red: 0.18, green: 0.49, blue: 0.20)
    static let failure = Color(red: 0.78, green: 0.16, blue: 0.16)
    static let warning = Color(red: 0.90, green: 0.32, blue: 0.0)
}

enum Spacing {
    static let tiny: CGFloat = 4
    static let small: CGFloat = 8
    static let medium: CGFloat = 16
    static let large: CGFloat = 24
    static let xl: CGFloat = 32
}

enum Radius {
    static let small: CGFloat = 8
    static let medium: CGFloat = 16
}

extension Color {
    var muted: Color { opacity(0.75) }
}
