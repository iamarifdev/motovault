import Foundation
import AVFoundation
import AppKit
import CoreVideo
import ImageIO

// Native macOS encoder/probe. No optional ffmpeg installation is required.
let arguments = CommandLine.arguments
if arguments.count > 2 && arguments[1] == "scan-toggle" {
    let source = CGImageSourceCreateWithURL(URL(fileURLWithPath: arguments[2]) as CFURL, nil)!
    let image = CGImageSourceCreateImageAtIndex(source, 0, nil)!
    let data = image.dataProvider!.data! as Data
    print("Image \(image.width)x\(image.height), \(image.bitsPerPixel) bits, alpha \(image.alphaInfo.rawValue)")
    let x = Int(Double(image.width) * 0.90)
    var first: Int? = nil
    for y in 0..<image.height {
        let offset = y * image.bytesPerRow + x * 4
        let pixel = Array(data[offset..<(offset + 4)])
        let expected = [115,126,139]
        let gray = (0..<4).contains { omitted in
            let rgb = (0..<4).filter { $0 != omitted }.map { Int(pixel[$0]) }.sorted()
            return zip(rgb,expected).allSatisfy { abs($0.0-$0.1) <= 5 }
        }
        if gray && first == nil { first = y }
        if !gray, let start = first {
            if y-start>30 { print("Gray pill span \(start)..\(y), normalized center \(Double(start+y)/2/Double(image.height))") }
            first = nil
        }
    }
} else if arguments.count > 2 && arguments[1] == "inspect" {
    for path in arguments.dropFirst(2) {
        let asset = AVURLAsset(url: URL(fileURLWithPath: path))
        let tracks = asset.tracks(withMediaType: .video)
        guard let track = tracks.first else { fatalError("No video track: \(path)") }
        let duration = CMTimeGetSeconds(asset.duration)
        guard duration.isFinite && duration > 0 else { fatalError("Invalid duration: \(path)") }
        // A decoded frame checks more than file existence or container metadata.
        let image = try AVAssetImageGenerator(asset: asset).copyCGImage(at: .zero, actualTime: nil)
        let poster = NSBitmapImageRep(cgImage: image).representation(using: .png, properties: [:])!
        try poster.write(to: URL(fileURLWithPath: path).deletingPathExtension().appendingPathExtension("poster.png"))
        let row: [String: Any] = ["file": path, "durationSeconds": duration,
            "width": image.width, "height": image.height, "nominalFrameRate": track.nominalFrameRate,
            "decodedFrame": true]
        let json = try JSONSerialization.data(withJSONObject: row, options: [.sortedKeys])
        print(String(data: json, encoding: .utf8)!)
    }
} else {
    guard arguments.count == 2 else { fatalError("Usage: encode-motion.swift capture.json | inspect movie...") }
    let input = URL(fileURLWithPath: arguments[1])
    let root = input.deletingLastPathComponent()
    let payload = try JSONSerialization.jsonObject(with: Data(contentsOf: input)) as! [String: Any]
    let frames = payload["frames"] as! [[String: Any]]
    let labels = Array(Set(frames.compactMap { $0["label"] as? String })).sorted()
    for label in labels {
        let sequence = frames.filter { $0["label"] as? String == label }
        let output = root.appendingPathComponent(label + ".mp4")
        if FileManager.default.fileExists(atPath: output.path) { try FileManager.default.removeItem(at: output) }
        let first = NSImage(contentsOf: root.appendingPathComponent(sequence[0]["file"] as! String))!
        var rect = CGRect(origin: .zero, size: first.size)
        let firstImage = first.cgImage(forProposedRect: &rect, context: nil, hints: nil)!
        let width = firstImage.width, height = firstImage.height
        let writer = try AVAssetWriter(outputURL: output, fileType: .mp4)
        let channel = AVAssetWriterInput(mediaType: .video, outputSettings: [AVVideoCodecKey: AVVideoCodecType.h264,
            AVVideoWidthKey: width, AVVideoHeightKey: height])
        let adaptor = AVAssetWriterInputPixelBufferAdaptor(assetWriterInput: channel,
            sourcePixelBufferAttributes: [kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32ARGB,
                kCVPixelBufferWidthKey as String: width, kCVPixelBufferHeightKey as String: height])
        writer.add(channel)
        guard writer.startWriting() else { fatalError("Writer failed: \(String(describing: writer.error))") }
        writer.startSession(atSourceTime: .zero)
        let baseline = (sequence[0]["atMs"] as! NSNumber).doubleValue
        for frame in sequence {
            while !channel.isReadyForMoreMediaData { Thread.sleep(forTimeInterval: 0.002) }
            let nsImage = NSImage(contentsOf: root.appendingPathComponent(frame["file"] as! String))!
            let cg = nsImage.cgImage(forProposedRect: nil, context: nil, hints: nil)!
            var buffer: CVPixelBuffer?
            CVPixelBufferPoolCreatePixelBuffer(kCFAllocatorDefault, adaptor.pixelBufferPool!, &buffer)
            guard let pixels = buffer else { fatalError("No pixel buffer") }
            CVPixelBufferLockBaseAddress(pixels, [])
            let context = CGContext(data: CVPixelBufferGetBaseAddress(pixels), width: width, height: height,
                bitsPerComponent: 8, bytesPerRow: CVPixelBufferGetBytesPerRow(pixels),
                space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.noneSkipFirst.rawValue)!
            context.draw(cg, in: CGRect(x: 0, y: 0, width: width, height: height))
            CVPixelBufferUnlockBaseAddress(pixels, [])
            let seconds = ((frame["atMs"] as! NSNumber).doubleValue - baseline) / 1000
            guard adaptor.append(pixels, withPresentationTime: CMTime(seconds: seconds, preferredTimescale: 1000)) else {
                fatalError("Frame append failed: \(String(describing: writer.error))")
            }
        }
        channel.markAsFinished()
        let finished = DispatchSemaphore(value: 0)
        writer.finishWriting { finished.signal() }
        finished.wait()
        guard writer.status == .completed else { fatalError("Finalize failed: \(String(describing: writer.error))") }
        print("Encoded \(label): \(sequence.count) actual screenshot samples; \(output.path)")
    }
}
