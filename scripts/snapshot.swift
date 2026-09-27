import Cocoa
import WebKit

guard CommandLine.arguments.count >= 3 else {
    print("Usage: swift snapshot.swift <html_file> <output_png> [width] [height]")
    exit(1)
}

let htmlPath = CommandLine.arguments[1]
let outputPath = CommandLine.arguments[2]
let width: CGFloat = CommandLine.arguments.count > 3 ? CGFloat(Double(CommandLine.arguments[3]) ?? 1200) : 1200
let height: CGFloat = CommandLine.arguments.count > 4 ? CGFloat(Double(CommandLine.arguments[4]) ?? 800) : 800

let app = NSApplication.shared
app.setActivationPolicy(.accessory)

class Delegate: NSObject, WKNavigationDelegate {
    let outputUrl: URL
    let webView: WKWebView

    init(outputUrl: URL, width: CGFloat, height: CGFloat) {
        self.outputUrl = outputUrl
        let config = WKWebViewConfiguration()
        self.webView = WKWebView(frame: CGRect(x: 0, y: 0, width: width, height: height), configuration: config)
        super.init()
        self.webView.navigationDelegate = self
    }

    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
            let snapConfig = WKSnapshotConfiguration()
            self.webView.takeSnapshot(with: snapConfig) { image, error in
                guard let image = image else {
                    print("Snapshot failed: \(String(describing: error))")
                    exit(1)
                }
                guard let tiffData = image.tiffRepresentation,
                      let bitmap = NSBitmapImageRep(data: tiffData),
                      let pngData = bitmap.representation(using: .png, properties: [:]) else {
                    print("Failed to encode PNG")
                    exit(1)
                }
                do {
                    try pngData.write(to: self.outputUrl)
                    print("SAVED: \(self.outputUrl.path)")
                    exit(0)
                } catch {
                    print("Write error: \(error)")
                    exit(1)
                }
            }
        }
    }

    func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
        print("Navigation failed: \(error)")
        exit(1)
    }
}

let fileUrl = URL(fileURLWithPath: (htmlPath as NSString).expandingTildeInPath)
let outUrl = URL(fileURLWithPath: (outputPath as NSString).expandingTildeInPath)

let delegate = Delegate(outputUrl: outUrl, width: width, height: height)
delegate.webView.loadFileURL(fileUrl, allowingReadAccessTo: fileUrl.deletingLastPathComponent().deletingLastPathComponent())

app.run()
