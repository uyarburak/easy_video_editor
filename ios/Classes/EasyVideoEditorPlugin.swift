import Flutter
import UIKit

public class EasyVideoEditorPlugin: NSObject, FlutterPlugin {
  private let commandHandler = CommandHandler()
  
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "easy_video_editor", binaryMessenger: registrar.messenger())
    let instance = EasyVideoEditorPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
     commandHandler.handleCommand(call, result: result)
  }
}
