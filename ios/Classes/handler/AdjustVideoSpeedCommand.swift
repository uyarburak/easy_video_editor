import Flutter
import AVFoundation

class AdjustVideoSpeedCommand: Command {
    func execute(call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard let arguments = call.arguments as? [String: Any],
              let videoPath = arguments["videoPath"] as? String,
              let speed = arguments["speed"] as? NSNumber else {
            result(FlutterError(
                code: "INVALID_ARGUMENTS",
                message: "Missing required arguments: videoPath or speed",
                details: nil
            ))
            return
        }
        
        DispatchQueue.global(qos: .userInitiated).async {
            do {
                let outputPath = try VideoUtils.adjustVideoSpeed(
                    videoPath: videoPath,
                    speed: speed.floatValue
                )
                
                DispatchQueue.main.async {
                    result(outputPath)
                }
            } catch VideoError.fileNotFound {
                DispatchQueue.main.async {
                    result(FlutterError(
                        code: "FILE_NOT_FOUND",
                        message: "The video file was not found at the specified path",
                        details: nil
                    ))
                }
            } catch VideoError.invalidParameters {
                DispatchQueue.main.async {
                    result(FlutterError(
                        code: "INVALID_SPEED",
                        message: "The speed value must be greater than 0",
                        details: nil
                    ))
                }
            } catch VideoError.exportFailed(let message) {
                DispatchQueue.main.async {
                    result(FlutterError(
                        code: "EXPORT_FAILED",
                        message: message,
                        details: nil
                    ))
                }
            } catch {
                DispatchQueue.main.async {
                    result(FlutterError(
                        code: "SPEED_ADJUST_ERROR",
                        message: error.localizedDescription,
                        details: nil
                    ))
                }
            }
        }
    }
} 