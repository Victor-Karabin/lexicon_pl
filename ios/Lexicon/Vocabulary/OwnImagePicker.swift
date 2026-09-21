import PhotosUI
import Shared
import SwiftUI
import UIKit

private let ownImageDirectory = "word_images"

func isOwnImage(_ url: String) -> Bool { url.hasPrefix("file:") }

func imageURL(_ url: String) -> URL? {
    guard isOwnImage(url), let stored = URL(string: url) else { return URL(string: url) }
    return URL.documentsDirectory
        .appendingPathComponent(ownImageDirectory, isDirectory: true)
        .appendingPathComponent(stored.lastPathComponent)
}

struct AddImageTile: View {
    let onPicked: (String) -> Void

    @State private var libraryItem: PhotosPickerItem?
    @State private var isTakingPhoto = false
    @State private var isChoosing = false
    @State private var takenPhoto: UIImage?
    @State private var toPosition: PendingCrop?

    private var hasCamera: Bool { UIImagePickerController.isSourceTypeAvailable(.camera) }

    var body: some View {
        Menu {
            Button {
                isChoosing = true
            } label: {
                Label(Strings.createWordImageFromLibrary, systemImage: "photo.on.rectangle")
            }
            if hasCamera {
                Button {
                    isTakingPhoto = true
                } label: {
                    Label(Strings.createWordImageFromCamera, systemImage: "camera")
                }
            }
        } label: {
            RoundedRectangle(cornerRadius: Radius.small)
                .fill(Color.secondary.opacity(0.15))
                .frame(width: 96, height: 96)
                .overlay(Image(systemName: "plus").foregroundStyle(.secondary))
                .accessibilityLabel(Strings.createWordImageAdd)
        }
        .photosPicker(isPresented: $isChoosing, selection: $libraryItem, matching: .images)
        .onChange(of: libraryItem) { _, item in
            guard let item else { return }
            Task {
                if let data = try? await item.loadTransferable(type: Data.self), let image = UIImage(data: data) {
                    place(image)
                }
                libraryItem = nil
            }
        }
        .fullScreenCover(isPresented: $isTakingPhoto, onDismiss: placeTakenPhoto) {
            CameraPicker { image in
                takenPhoto = image
                isTakingPhoto = false
            }
            .ignoresSafeArea()
        }
        .sheet(item: $toPosition) { pending in
            ImageCropView(image: pending.image) { cropped in
                toPosition = nil
                if let cropped { keep(cropped) }
            }
        }
    }

    private func placeTakenPhoto() {
        guard let photo = takenPhoto else { return }
        takenPhoto = nil
        place(photo)
    }

    private func place(_ image: UIImage) {
        let upright = uprightImage(image)
        if cropWindow(for: upright.size).needsPositioning {
            toPosition = PendingCrop(image: upright)
        } else {
            keep(upright)
        }
    }

    private func keep(_ image: UIImage) {
        guard let url = storeOwnImage(image) else { return }
        onPicked(url)
    }
}

func storeOwnImage(_ image: UIImage) -> String? {
    guard let data = image.jpegData(compressionQuality: 0.9) else { return nil }
    let directory = URL.documentsDirectory.appendingPathComponent(ownImageDirectory, isDirectory: true)
    try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
    let file = directory.appendingPathComponent("\(UUID().uuidString).jpg")
    return (try? data.write(to: file)) == nil ? nil : file.absoluteString
}

func loadPicture(_ url: String) async -> UIImage? {
    guard let location = imageURL(url) else { return nil }
    let data = location.isFileURL
        ? try? Data(contentsOf: location)
        : try? await URLSession.shared.data(from: location).0
    return data.flatMap(UIImage.init(data:)).map(uprightImage)
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
