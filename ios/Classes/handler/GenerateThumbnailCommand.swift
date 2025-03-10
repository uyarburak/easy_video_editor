import Flutter
import AVFoundation

class GenerateThumbnailCommand: Command {
    func execute(call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard let arguments = call.arguments as? [String: Any],
              let videoPath = arguments["videoPath"] as? String,
              let timeMs = arguments["timeMs"] as? NSNumber,
              let quality = arguments["quality"] as? NSNumber else {
            result(FlutterError(
                code: "INVALID_ARGUMENTS",
                message: "Missing required arguments: videoPath, timeMs, or quality",
                details: nil
            ))
            return
        }
        
        DispatchQueue.global(qos: .userInitiated).async {
            do {
                let outputPath = try VideoUtils.generateThumbnail(
                    videoPath: videoPath,
                    timeMs: timeMs.int64Value,
                    quality: quality.intValue
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
            } catch VideoError.invalidTimeRange {
                DispatchQueue.main.async {
                    result(FlutterError(
                        code: "INVALID_TIME",
                        message: "The specified time is invalid or outside the video duration",
                        details: nil
                    ))
                }
            } catch VideoError.thumbnailGenerationFailed {
                DispatchQueue.main.async {
                    result(FlutterError(
                        code: "THUMBNAIL_GENERATION_FAILED",
                        message: "Failed to generate thumbnail from the video",
                        details: nil
                    ))
                }
            } catch {
                DispatchQueue.main.async {
                    result(FlutterError(
                        code: "THUMBNAIL_ERROR",
                        message: error.localizedDescription,
                        details: nil
                    ))
                }
            }
        }
    }
} 