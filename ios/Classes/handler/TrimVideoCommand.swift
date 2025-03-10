import Flutter
import AVFoundation

class TrimVideoCommand: Command {
    func execute(call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard let arguments = call.arguments as? [String: Any],
              let videoPath = arguments["videoPath"] as? String,
              let startTime = arguments["startTimeMs"] as? NSNumber,
              let endTime = arguments["endTimeMs"] as? NSNumber else {
            result(FlutterError(
                code: "INVALID_ARGUMENTS",
                message: "Missing required arguments: videoPath, startTimeMs, or endTimeMs",
                details: nil
            ))
            return
        }
        
        // Convert to Int64 to ensure proper handling of large millisecond values
        let startTimeMs = startTime.int64Value
        let endTimeMs = endTime.int64Value
        
        DispatchQueue.global(qos: .userInitiated).async {
            do {
                let outputPath = try VideoUtils.trimVideo(
                    videoPath: videoPath,
                    startTimeMs: startTimeMs,
                    endTimeMs: endTimeMs
                )
                
                DispatchQueue.main.async {
                    result(outputPath)
                }
            } catch VideoError.invalidPath {
                DispatchQueue.main.async {
                    result(FlutterError(
                        code: "INVALID_PATH",
                        message: "The provided video path is invalid",
                        details: nil
                    ))
                }
            } catch VideoError.invalidTimeRange {
                DispatchQueue.main.async {
                    result(FlutterError(
                        code: "INVALID_TIME_RANGE",
                        message: "The provided time range is invalid. Make sure start time is less than end time and within video duration",
                        details: nil
                    ))
                }
            } catch VideoError.invalidAsset {
                DispatchQueue.main.async {
                    result(FlutterError(
                        code: "INVALID_ASSET",
                        message: "Could not create video asset from the provided path",
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
                        code: "TRIM_ERROR",
                        message: error.localizedDescription,
                        details: nil
                    ))
                }
            }
        }
    }
} 