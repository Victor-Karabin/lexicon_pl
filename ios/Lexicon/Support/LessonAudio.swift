import AVFoundation
import Foundation

@MainActor
final class LessonAudio: NSObject, ObservableObject, AVAudioPlayerDelegate {
    static let shared = LessonAudio()

    @Published private(set) var playing: String?
    @Published private(set) var paused: String?
    @Published private(set) var unavailable: Set<String> = []

    private var player: AVAudioPlayer?
    private var inFlight: Set<String> = []

    private override init() {}

    private var directory: URL? {
        let base = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first
        guard let directory = base?.appendingPathComponent("lesson_audio", isDirectory: true) else { return nil }
        try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        return directory
    }

    func play(file: String, remoteId: String?) async {
        guard let url = await path(file: file, remoteId: remoteId) else {
            unavailable.insert(file)
            return
        }
        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .spokenAudio)
            try AVAudioSession.sharedInstance().setActive(true)
            player = try AVAudioPlayer(contentsOf: url)
            player?.delegate = self
            player?.play()
            paused = nil
            playing = file
        } catch {
            playing = nil
            paused = nil
            unavailable.insert(file)
        }
    }

    func toggle(file: String, remoteId: String?) async {
        if playing == file {
            player?.pause()
            paused = file
            playing = nil
        } else if paused == file, let player {
            player.play()
            paused = nil
            playing = file
        } else {
            await play(file: file, remoteId: remoteId)
        }
    }

    nonisolated func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) {
        Task { @MainActor in
            self.playing = nil
            self.paused = nil
        }
    }

    func stop() {
        player?.stop()
        playing = nil
        paused = nil
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
