import PhotosUI
import SwiftUI
import UIKit

/// Where the learner's own pictures live, inside the app's own storage.
private let ownImageDirectory = "word_images"

/// A picture out of the app's own files, rather than one off the web.
func isOwnImage(_ url: String) -> Bool { url.hasPrefix("file:") }

/// The tile that adds a picture of the learner's own, first in the row and always there.
///
/// Two sources behind one tile: whether a picture comes from the library or the camera
/// is a detail of getting one, not a choice worth two tiles in a row of pictures.
///
/// Both hand back a `file://` string, the same shape of thing as a searched picture's
/// URL, so nothing downstream has to know where a picture came from. A picked photo is
/// copied into the app's files rather than kept as the item it arrived as: that item is
/// the library's, and the word is meant to keep its picture.
struct AddImageTile: View {
    let onPicked: (String) -> Void

    @State private var libraryItem: PhotosPickerItem?
    @State private var isTakingPhoto = false
    @State private var isChoosing = false

    /// A simulator has no camera, and offering one that cannot open is worse than not
    /// offering it.
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

/// Null when the picture cannot be written — a full disk, and little else.
private func writeOwnImage(_ data: Data) -> String? {
    let directory = URL.documentsDirectory.appendingPathComponent(ownImageDirectory, isDirectory: true)
    try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
    let file = directory.appendingPathComponent("\(UUID().uuidString).jpg")
    return (try? data.write(to: file)) == nil ? nil : file.absoluteString
}

/// The system camera, which SwiftUI has no view of its own for.
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
