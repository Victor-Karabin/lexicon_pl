import PhotosUI
import SwiftUI
import UIKit

private let ownImageDirectory = "word_images"

func isOwnImage(_ url: String) -> Bool { url.hasPrefix("file:") }

struct AddImageTile: View {
    let onPicked: (String) -> Void

    @State private var libraryItem: PhotosPickerItem?
    @State private var isTakingPhoto = false
    @State private var isChoosing = false

    private var hasCamera: Bool { UIImagePickerController.isSourceTypeAvailable(.camera) }

    var body: some View {
        Menu {
            Button {
                isChoosing = true
            } label: {
                Label("Choose a photo", systemImage: "photo.on.rectangle")
            }
            if hasCamera {
                Button {
                    isTakingPhoto = true
                } label: {
                    Label("Take a photo", systemImage: "camera")
                }
            }
        } label: {
            RoundedRectangle(cornerRadius: Radius.small)
                .fill(Color.secondary.opacity(0.15))
                .frame(width: 96, height: 96)
                .overlay(Image(systemName: "plus").foregroundStyle(.secondary))
                .accessibilityLabel("Add a picture of your own")
        }
        .photosPicker(isPresented: $isChoosing, selection: $libraryItem, matching: .images)
        .onChange(of: libraryItem) { _, item in
            guard let item else { return }
            Task {
                if let data = try? await item.loadTransferable(type: Data.self),
                   let url = writeOwnImage(data) {
                    onPicked(url)
                }
                libraryItem = nil
            }
        }
        .fullScreenCover(isPresented: $isTakingPhoto) {
            CameraPicker { image in
                isTakingPhoto = false
                guard let data = image?.jpegData(compressionQuality: 0.9), let url = writeOwnImage(data) else { return }
                onPicked(url)
            }
            .ignoresSafeArea()
        }
    }
}

private func writeOwnImage(_ data: Data) -> String? {
    let directory = URL.documentsDirectory.appendingPathComponent(ownImageDirectory, isDirectory: true)
    try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
    let file = directory.appendingPathComponent("\(UUID().uuidString).jpg")
    return (try? data.write(to: file)) == nil ? nil : file.absoluteString
}

private struct CameraPicker: UIViewControllerRepresentable {
    let onTaken: (UIImage?) -> Void

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let controller = UIImagePickerController()
        controller.sourceType = .camera
        controller.delegate = context.coordinator
        return controller
    }

    func updateUIViewController(_ controller: UIImagePickerController, context: Context) {}

    func makeCoordinator() -> Coordinator { Coordinator(onTaken: onTaken) }

    final class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        private let onTaken: (UIImage?) -> Void

        init(onTaken: @escaping (UIImage?) -> Void) { self.onTaken = onTaken }

        func imagePickerController(
            _ picker: UIImagePickerController,
            didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]
        ) {
            onTaken(info[.originalImage] as? UIImage)
        }

        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            onTaken(nil)
        }
    }
}
