import Flutter
import AVFoundation

class MergeVideosCommand: Command {
    func execute(call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard let arguments = call.arguments as? [String: Any],
              let videoPaths = arguments["videoPaths"] as? [String] else {
            result(FlutterError(
                code: "INVALID_ARGUMENTS",
                message: "Missing required argument: videoPaths",
                details: nil
            ))
            return
        }
        
        DispatchQueue.global(qos: .userInitiated).async {
            do {
                let outputPath = try VideoUtils.mergeVideos(videoPaths: videoPaths)
                
                DispatchQueue.main.async {
                    result(outputPath)
                }
            } catch {
                DispatchQueue.main.async {
                    result(FlutterError(
                        code: "MERGE_ERROR",
                        message: error.localizedDescription,
                        details: nil
                    ))
                }
            }
        }
    }
} 