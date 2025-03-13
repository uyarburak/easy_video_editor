import Flutter
import Foundation

enum MethodName: String {
    case trimVideo = "trimVideo"
    case mergeVideos = "mergeVideos"
    case extractAudio = "extractAudio"
    case adjustVideoSpeed = "adjustVideoSpeed"
    case removeAudio = "removeAudio"
    case cropVideo = "cropVideo"
    case rotateVideo = "rotateVideo"
    case generateThumbnail = "generateThumbnail"
    case compressVideo = "compressVideo"
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
            .cropVideo: CropVideoCommand(),
            .rotateVideo: RotateVideoCommand(),
            .generateThumbnail: GenerateThumbnailCommand(),
            .compressVideo: CompressVideoCommand()
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