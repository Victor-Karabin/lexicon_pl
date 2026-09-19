import SwiftUI
import UIKit

let cardImageAspect: CGFloat = 1.6

private let aspectTolerance: CGFloat = 0.03
private let maxImageSide: CGFloat = 2048

struct CropWindow {
    let imageSize: CGSize
    var aspect: CGFloat = cardImageAspect

    private var isWider: Bool { imageSize.width / imageSize.height > aspect }

    var width: CGFloat { isWider ? (imageSize.height * aspect).rounded() : imageSize.width }
    var height: CGFloat { isWider ? imageSize.height : (imageSize.width / aspect).rounded() }
    var slack: CGFloat { isWider ? imageSize.width - width : imageSize.height - height }
    var movesHorizontally: Bool { isWider }

    var needsPositioning: Bool {
        guard imageSize.width > 0, imageSize.height > 0 else { return false }
        return abs(imageSize.width / imageSize.height - aspect) / aspect > aspectTolerance
    }

    func rect(at focus: CGFloat) -> CGRect {
        let offset = (slack * min(max(focus, 0), 1)).rounded()
        return isWider
            ? CGRect(x: offset, y: 0, width: width, height: height)
            : CGRect(x: 0, y: offset, width: width, height: height)
    }

    func focus(from start: CGFloat, dragged: CGFloat, frame: CGFloat) -> CGFloat {
        guard slack > 0, frame > 0 else { return start }
        let moved = dragged * (isWider ? width : height) / frame
        return min(max(start - moved / slack, 0), 1)
    }
}

func uprightImage(_ image: UIImage) -> UIImage {
    let side = max(image.size.width, image.size.height)
    let shrink = side > maxImageSide ? maxImageSide / side : 1
    let size = CGSize(width: (image.size.width * shrink).rounded(), height: (image.size.height * shrink).rounded())
    let format = UIGraphicsImageRendererFormat()
    format.scale = 1
    return UIGraphicsImageRenderer(size: size, format: format).image { _ in
        image.draw(in: CGRect(origin: .zero, size: size))
    }
}

struct PendingCrop: Identifiable {
    let id = UUID()
    let image: UIImage
}

struct ImageCropView: View {
    let image: UIImage
    let onFinished: (UIImage?) -> Void

    @State private var focus: CGFloat = 0.5
    @State private var dragStart: CGFloat?

    private var window: CropWindow { CropWindow(imageSize: image.size) }

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: Spacing.medium) {
                Text("Drag the photo so the part you want sits in the frame.")
                    .font(.callout)
                    .foregroundStyle(.secondary)

                GeometryReader { proxy in
                    let frame = proxy.size
                    let scale = frame.width / window.width
                    let crop = window.rect(at: focus)
                    Image(uiImage: image)
                        .resizable()
                        .frame(width: image.size.width * scale, height: image.size.height * scale)
                        .offset(x: -crop.minX * scale, y: -crop.minY * scale)
                        .frame(width: frame.width, height: frame.height, alignment: .topLeading)
                        .clipped()
                        .contentShape(Rectangle())
                        .gesture(
                            DragGesture()
                                .onChanged { drag in
                                    let start = dragStart ?? focus
                                    dragStart = start
                                    focus = window.movesHorizontally
                                        ? window.focus(from: start, dragged: drag.translation.width, frame: frame.width)
                                        : window.focus(from: start, dragged: drag.translation.height, frame: frame.height)
                                }
                                .onEnded { _ in dragStart = nil }
                        )
                }
                .aspectRatio(cardImageAspect, contentMode: .fit)
                .clipShape(RoundedRectangle(cornerRadius: Radius.small))

                Spacer()
            }
            .padding(Spacing.medium)
            .navigationTitle("Position the picture")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { onFinished(nil) }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Use") { onFinished(cropped()) }
                }
            }
        }
    }

    private func cropped() -> UIImage? {
        guard let cgImage = image.cgImage?.cropping(to: window.rect(at: focus)) else { return nil }
        return UIImage(cgImage: cgImage)
    }
}
