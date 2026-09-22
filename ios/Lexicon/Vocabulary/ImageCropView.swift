import Shared
import SwiftUI
import UIKit

private let maxImageSide: CGFloat = 2048
private let scrimOpacity = 0.6
private let frameStroke: CGFloat = 2
private let maxPreviewHeight: CGFloat = 480

func cropWindow(for size: CGSize) -> CropWindow {
    CropWindow(
        imageWidth: Int32(size.width.rounded()),
        imageHeight: Int32(size.height.rounded()),
        aspect: CropWindowKt.CARD_IMAGE_ASPECT
    )
}

private extension CropRect {
    var cgRect: CGRect { CGRect(x: Int(left), y: Int(top), width: Int(width), height: Int(height)) }
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

    @State private var focus: Float = 0.5
    @State private var dragStart: Float?

    private var window: CropWindow { cropWindow(for: image.size) }

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: Spacing.medium) {
                Text(Strings.createWordImagePositionHint)
                    .font(.callout)
                    .foregroundStyle(.secondary)

                GeometryReader { proxy in
                    let shown = proxy.size
                    let scale = shown.width / image.size.width
                    let crop = window.rectAt(focus: focus).cgRect
                    let frame = CGRect(x: crop.minX * scale, y: crop.minY * scale, width: crop.width * scale, height: crop.height * scale)
                    ZStack(alignment: .topLeading) {
                        Image(uiImage: image)
                            .resizable()
                            .frame(width: shown.width, height: shown.height)
                        Path { path in
                            path.addRect(CGRect(origin: .zero, size: shown))
                            path.addRect(frame)
                        }
                        .fill(Color.black.opacity(scrimOpacity), style: FillStyle(eoFill: true))
                        Rectangle()
                            .stroke(Palette.accentDeep, lineWidth: frameStroke)
                            .frame(width: frame.width, height: frame.height)
                            .offset(x: frame.minX, y: frame.minY)
                    }
                    .contentShape(Rectangle())
                    .gesture(
                        DragGesture()
                            .onChanged { drag in
                                let start = dragStart ?? focus
                                dragStart = start
                                focus = window.movesHorizontally
                                    ? window.focusAfterFrameDrag(focus: start, dragPixels: Float(drag.translation.width), shownImagePixels: Float(shown.width))
                                    : window.focusAfterFrameDrag(focus: start, dragPixels: Float(drag.translation.height), shownImagePixels: Float(shown.height))
                            }
                            .onEnded { _ in dragStart = nil }
                    )
                }
                .aspectRatio(image.size.width / image.size.height, contentMode: .fit)
                .frame(maxWidth: .infinity, maxHeight: maxPreviewHeight)
                .clipShape(RoundedRectangle(cornerRadius: Radius.small))

                Spacer()
            }
            .padding(Spacing.medium)
            .navigationTitle(Strings.createWordImagePosition)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(Strings.actionCancel) { onFinished(nil) }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(Strings.createWordImageUse) { onFinished(cropped()) }
                }
            }
        }
    }

    private func cropped() -> UIImage? {
        guard let cgImage = image.cgImage?.cropping(to: window.rectAt(focus: focus).cgRect) else { return nil }
        return UIImage(cgImage: cgImage)
    }
}
