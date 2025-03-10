import Flutter
import AVFoundation

class RotateVideoCommand: Command {
    func execute(call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard let arguments = call.arguments as? [String: Any],
              let videoPath = arguments["videoPath"] as? String,
              let degrees = arguments["degrees"] as? NSNumber else {
            result(FlutterError(
                code: "INVALID_ARGUMENTS",
                message: "Missing required arguments: videoPath or degrees",
                details: nil
            ))
            return
        }
        
        DispatchQueue.global(qos: .userInitiated).async {
            do {
                let outputPath = try VideoUtils.rotateVideo(
                    videoPath: videoPath,
                    rotationDegrees: degrees.floatValue
                )
                
                DispatchQueue.main.async {
                    result(outputPath)
                }
            } catch {
                DispatchQueue.main.async {
                    result(FlutterError(
                        code: "ROTATE_ERROR",
                        message: error.localizedDescription,
                        details: nil
                    ))
                }
            }
        }
    }
} 