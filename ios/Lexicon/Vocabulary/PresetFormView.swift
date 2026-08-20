import SwiftUI
import Shared

struct PresetFormView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var scheme

    @State private var title = ""
    @State private var description = ""
    @State private var icon = "restaurant"
    @State private var colour = "#EF6C00"
    @State private var problem: String?

    private let icons = [
        "restaurant", "local_pizza", "trending_up", "record_voice_over", "home",
        "directions_car", "school", "work", "favorite", "pets", "sports_soccer",
        "local_hospital", "shopping_cart", "watch", "wb_sunny", "family_restroom",
        "language", "music_note", "palette", "computer",
    ]
    private let colours = [
        "#EF6C00", "#2E7D32", "#1565C0", "#6A1B9A", "#C62828", "#00838F",
        "#4E342E", "#37474F", "#AD1457", "#F9A825",
    ]

    var body: some View {
        Form {
            Section("Name") {
                TextField("Name", text: $title)
            }
            Section("Description (optional)") {
                TextField("Description", text: $description)
            }
            Section("Icon") {
                FlowLayout(spacing: Spacing.small) {
                    ForEach(icons, id: \.self) { name in
                        Button { icon = name } label: {
                            Image(systemName: symbol(for: name))
                                .frame(width: 40, height: 40)
                                .background(icon == name ? accent.opacity(0.35) : Color.secondary.opacity(0.12))
                                .clipShape(RoundedRectangle(cornerRadius: Radius.small))
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            Section("Colour") {
                FlowLayout(spacing: Spacing.small) {
                    ForEach(colours, id: \.self) { hex in
                        Button { colour = hex } label: {
                            Circle()
                                .fill(Color(hex: hex))
                                .frame(width: 36, height: 36)
                                .overlay(Circle().stroke(.primary, lineWidth: colour == hex ? 3 : 0))
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            if let problem {
                Text(problem).foregroundStyle(Palette.failure)
            }
        }
        .navigationTitle("New preset")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                AsyncButton { await save() } label: { Text("Save") }
            }
        }
    }

    private var accent: Color { Color(hex: colour) }

    private func symbol(for name: String) -> String {
        VocabularyPreset.symbolName(forIcon: name)
    }

    private func save() async {
        guard !title.trimmingCharacters(in: .whitespaces).isEmpty else {
            problem = "Give the preset a name."
            return
        }
        _ = try? await deps.createPreset.invoke(
            title: title,
            description: description,
            icon: icon,
            color: colour,
            wordIds: []
        )
        dismiss()
    }
}

extension Color {
    init(hex: String) {
        let cleaned = hex.replacingOccurrences(of: "#", with: "")
        let value = UInt32(cleaned, radix: 16) ?? 0
        self.init(
            red: Double((value >> 16) & 0xFF) / 255,
            green: Double((value >> 8) & 0xFF) / 255,
            blue: Double(value & 0xFF) / 255
        )
    }
}
