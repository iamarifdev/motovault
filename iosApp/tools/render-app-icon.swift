import CoreGraphics
import ImageIO
import Foundation

// Exact existing Glyph("mark") geometry, on the established light canvas.
let size = 1024
let context = CGContext(data: nil, width: size, height: size, bitsPerComponent: 8, bytesPerRow: size * 4, space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue)!
context.setFillColor(CGColor(red: 245/255, green: 244/255, blue: 239/255, alpha: 1))
context.fill(CGRect(x: 0, y: 0, width: size, height: size))
let scale = 25.0
let points: [(Double, Double)] = [(0,0),(12,5),(24,0),(24,16),(12,24),(0,16)]
for (index, point) in points.enumerated() {
    let pixel = CGPoint(x: 212 + point.0 * scale, y: 812 - point.1 * scale)
    if index == 0 { context.move(to: pixel) } else { context.addLine(to: pixel) }
}
context.closePath()
context.setFillColor(CGColor(red: 170/255, green: 71/255, blue: 0, alpha: 1))
context.fillPath()
let url = URL(fileURLWithPath: "iosApp/MotoVault/Assets.xcassets/AppIcon.appiconset/AppIcon.png")
let destination = CGImageDestinationCreateWithURL(url as CFURL, "public.png" as CFString, 1, nil)!
CGImageDestinationAddImage(destination, context.makeImage()!, nil)
precondition(CGImageDestinationFinalize(destination))
