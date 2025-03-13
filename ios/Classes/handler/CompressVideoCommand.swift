import Flutter
import AVFoundation

class CompressVideoCommand: Command {
    func execute(call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard let arguments = call.arguments as? [String: Any],
              let videoPath = arguments["videoPath"] as? String,
              let targetHeight = arguments["targetHeight"] as? Int else {
            result(FlutterError(
                code: "INVALID_ARGUMENTS",
                message: "Missing required arguments: videoPath or targetHeight",
                details: nil
            ))
            return
        }
        
        DispatchQueue.global(qos: .userInitiated).async {
            do {
                let outputPath = try VideoUtils.compressVideo(
                    videoPath: videoPath,
                    targetHeight: targetHeight
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
                        code: "COMPRESS_ERROR",
                        message: error.localizedDescription,
                        details: nil
                    ))
                }
            }
        }
    }
}
