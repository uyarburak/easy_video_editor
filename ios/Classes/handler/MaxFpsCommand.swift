import Flutter
import Foundation
import AVFoundation

class MaxFpsCommand: Command {
    func execute(call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard let args = call.arguments as? [String: Any],
              let videoPath = args["videoPath"] as? String,
              let maxFps = args["maxFps"] as? Int else {
            result(FlutterError(code: "INVALID_ARGUMENTS",
                              message: "Missing required arguments: videoPath or maxFps",
                              details: nil))
            return
        }
        
        // Create a new operation ID for cancellation support
        let operationId = OperationManager.shared.generateOperationId()
        
        // Create a dispatch queue for video processing
        let queue = DispatchQueue(label: "com.example.easy_video_editor.maxFps", qos: .userInitiated)
        
        queue.async {
            do {
                let outputPath = try VideoUtils.setMaxFps(videoPath: videoPath, maxFps: maxFps)
                DispatchQueue.main.async {
                    result(outputPath)
                }
            } catch {
                DispatchQueue.main.async {
                    result(FlutterError(code: "MAX_FPS_ERROR",
                                      message: error.localizedDescription,
                                      details: nil))
                }
            }
            
            // Clean up operation
            OperationManager.shared.cancelOperation(operationId)
        }
    }
} 