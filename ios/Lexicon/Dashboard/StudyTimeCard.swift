import SwiftUI
import Shared

private let plotHeight: CGFloat = 72
private let barGap: CGFloat = 4
private let barCorner: CGFloat = 4
private let baselineThickness: CGFloat = 1
private let quietDayHeight: CGFloat = 3
private let quietDayOpacity: Double = 0.18
private let baselineOpacity: Double = 0.25
private let minutesPerHour = 60
private let secondsPerDay: Double = 86_400

/// The week of study time Android draws on its dashboard: one bar a day, the quiet days
/// left as a stub so the week still reads as seven days rather than as gaps.
struct StudyTimeCard: View {
    let history: StudyTimeHistory

    @Environment(\.colorScheme) private var scheme

    private var barColour: Color { scheme == .dark ? Palette.primary : Palette.accentDeep }

    var body: some View {
        let skin = TileSkin.standard(scheme: scheme)
        return Tile(skin: skin) {
            HStack(spacing: Spacing.medium) {
                Medallion(skin: skin) { MedallionIcon(systemName: "clock", skin: skin) }
                VStack(alignment: .leading, spacing: 2) {
                    Text("Study time this week")
                        .font(.caption)
                        .foregroundStyle(skin.onTile.muted)
                    if history.isEmpty {
                        Text("Nothing studied this week yet. Finish a training and the time shows up here.")
                            .font(.subheadline)
                            .foregroundStyle(skin.onTile.muted)
                    } else {
                        Text(durationText(minutes: Int(history.totalMinutes)))
                            .font(.title3.weight(.semibold))
                            .foregroundStyle(skin.onTile)
                        if let best = history.busiest {
                            Text("Best day \(weekdayName(of: best)) · \(durationText(minutes: Int(best.studiedMinutes)))")
                                .font(.caption)
                                .foregroundStyle(skin.onTile.muted)
                        }
                    }
                }
                Spacer()
            }

            if !history.isEmpty {
                StudyTimeChart(history: history, skin: skin, bars: barColour)
            }
        }
    }
}

private struct StudyTimeChart: View {
    let history: StudyTimeHistory
    let skin: TileSkin
    let bars: Color

    private var days: [DailyStudyTime] { history.days as? [DailyStudyTime] ?? [] }

    private var today: Int64? { days.last?.epochDay }

    var body: some View {
        VStack(spacing: Spacing.tiny) {
            GeometryReader { proxy in
                let count = CGFloat(max(days.count, 1))
                let barWidth = max((proxy.size.width - barGap * (count - 1)) / count, 1)
                let plot = max(proxy.size.height - baselineThickness, 1)

                ZStack(alignment: .bottomLeading) {
                    Rectangle()
                        .fill(skin.onTile.opacity(baselineOpacity))
                        .frame(height: baselineThickness)

                    HStack(alignment: .bottom, spacing: barGap) {
                        ForEach(days, id: \.epochDay) { day in
                            bar(for: day, plot: plot).frame(width: barWidth)
                        }
                    }
                    .padding(.bottom, baselineThickness)
                }
            }
            .frame(height: plotHeight)

            HStack(spacing: barGap) {
                ForEach(days, id: \.epochDay) { day in
                    Text(weekdayName(of: day))
                        .font(.caption2)
                        .fontWeight(day.epochDay == today ? .semibold : .regular)
                        .foregroundStyle(day.epochDay == today ? skin.onTile : skin.onTile.muted)
                        .lineLimit(1)
                        .frame(maxWidth: .infinity)
                }
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Study time by day: \(spokenDays)")
    }

    private func bar(for day: DailyStudyTime, plot: CGFloat) -> some View {
        let tall = day.wasStudied
            ? max(CGFloat(history.shareOfBusiest(day: day)) * plot, quietDayHeight)
            : quietDayHeight

        return UnevenRoundedRectangle(topLeadingRadius: barCorner, topTrailingRadius: barCorner)
            .fill(day.wasStudied ? bars : skin.onTile.opacity(quietDayOpacity))
            .frame(height: tall)
    }

    private var spokenDays: String {
        days
            .map { day in
                let spent = day.wasStudied ? durationText(minutes: Int(day.studiedMinutes)) : "nothing"
                return "\(weekdayName(of: day)) \(spent)"
            }
            .joined(separator: ", ")
    }
}

private func durationText(minutes: Int) -> String {
    guard minutes >= minutesPerHour else { return "\(minutes) min" }
    return "\(minutes / minutesPerHour) h \(minutes % minutesPerHour) min"
}

/// An epoch day names a calendar date rather than a moment, so it is read back in UTC —
/// a local time zone would slide the name onto the day before in the Americas.
private let weekdayFormatter: DateFormatter = {
    let formatter = DateFormatter()
    formatter.timeZone = TimeZone(secondsFromGMT: 0)
    formatter.setLocalizedDateFormatFromTemplate("E")
    return formatter
}()

private func weekdayName(of day: DailyStudyTime) -> String {
    weekdayFormatter.string(from: Date(timeIntervalSince1970: Double(day.epochDay) * secondsPerDay))
}
