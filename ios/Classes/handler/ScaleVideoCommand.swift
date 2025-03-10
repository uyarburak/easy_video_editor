import Flutter
import AVFoundation

class ScaleVideoCommand: Command {
    func execute(call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard let arguments = call.arguments as? [String: Any],
              let videoPath = arguments["videoPath"] as? String,
              let width = arguments["width"] as? NSNumber,
              let height = arguments["height"] as? NSNumber else {
            result(FlutterError(
                code: "INVALID_ARGUMENTS",
                message: "Missing required arguments: videoPath, width, or height",
                details: nil
            ))
            return
        }
        
        DispatchQueue.global(qos: .userInitiated).async {
            do {
                let outputPath = try VideoUtils.scaleVideo(
                    videoPath: videoPath,
                    scaleX: width.floatValue,
                    scaleY: height.floatValue
                )
                
                DispatchQueue.main.async {
                    result(outputPath)
                }
            } catch {
                DispatchQueue.main.async {
                    result(FlutterError(
                        code: "SCALE_ERROR",
                        message: error.localizedDescription,
                        details: nil
                    ))
                }
            }
        }
    }
} 