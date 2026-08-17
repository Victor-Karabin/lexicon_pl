import AVFoundation
import Foundation

@MainActor
final class LessonAudio: ObservableObject {
    static let shared = LessonAudio()

    @Published private(set) var playing: String?

    private var player: AVAudioPlayer?
    private var inFlight: Set<String> = []

    private init() {}

    private var directory: URL? {
        let base = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first
        guard let directory = base?.appendingPathComponent("lesson_audio", isDirectory: true) else { return nil }
        try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        return directory
    }

    func play(file: String, remoteId: String?) async {
        guard let url = await path(file: file, remoteId: remoteId) else { return }
        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .spokenAudio)
            try AVAudioSession.sharedInstance().setActive(true)
            player = try AVAudioPlayer(contentsOf: url)
            player?.play()
            playing = file
        } catch {
            playing = nil
        }
    }

    func stop() {
        player?.stop()
        playing = nil
    }

    private func path(file: String, remoteId: String?) async -> URL? {
        guard let directory else { return nil }
        let target = directory.appendingPathComponent(file)
        if FileManager.default.fileExists(atPath: target.path) { return target }
        guard let remoteId, !inFlight.contains(file) else { return nil }

        inFlight.insert(file)
        defer { inFlight.remove(file) }

        let source = URL(string: "https://drive.usercontent.google.com/download?id=\(remoteId)&export=download")
        guard let source else { return nil }
        do {
            let (data, response) = try await URLSession.shared.data(from: source)
            guard (response as? HTTPURLResponse)?.statusCode == 200 else { return nil }

            let partial = target.appendingPathExtension("part")
            try data.write(to: partial)
            try? FileManager.default.removeItem(at: target)
            try FileManager.default.moveItem(at: partial, to: target)
            return target
        } catch {
            return nil
        }
    }
}
