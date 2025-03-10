import Flutter
import Foundation

enum MethodName: String {
    case trimVideo = "trimVideo"
    case mergeVideos = "mergeVideos"
    case extractAudio = "extractAudio"
    case adjustVideoSpeed = "adjustVideoSpeed"
    case removeAudio = "removeAudio"
    case scaleVideo = "scaleVideo"
    case rotateVideo = "rotateVideo"
    case generateThumbnail = "generateThumbnail"
}

class CommandHandler {
    private let handlers: [MethodName: Command]
    
    init() {
        handlers = [
            .trimVideo: TrimVideoCommand(),
            .mergeVideos: MergeVideosCommand(),
            .extractAudio: ExtractAudioCommand(),
            .adjustVideoSpeed: AdjustVideoSpeedCommand(),
            .removeAudio: RemoveAudioCommand(),
            .scaleVideo: ScaleVideoCommand(),
            .rotateVideo: RotateVideoCommand(),
            .generateThumbnail: GenerateThumbnailCommand()
        ]
    }
    
    func handleCommand(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard let methodName = MethodName(rawValue: call.method) else {
            result(FlutterMethodNotImplemented)
            return
        }
        
        if let handler = handlers[methodName] {
            handler.execute(call: call, result: result)
        } else {
            result(FlutterMethodNotImplemented)
        }
    }
} 