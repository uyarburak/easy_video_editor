import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'easy_video_editor_method_channel.dart';

abstract class EasyVideoEditorPlatform extends PlatformInterface {
  /// Constructs a EasyVideoEditorPlatform.
  EasyVideoEditorPlatform() : super(token: _token);

  static final Object _token = Object();

  static EasyVideoEditorPlatform _instance = MethodChannelEasyVideoEditor();

  /// The default instance of [EasyVideoEditorPlatform] to use.
  ///
  /// Defaults to [MethodChannelEasyVideoEditor].
  static EasyVideoEditorPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [EasyVideoEditorPlatform] when
  /// they register themselves.
  static set instance(EasyVideoEditorPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> trimVideo(String videoPath, int startTimeMs, int endTimeMs) {
    throw UnimplementedError('trimVideo() has not been implemented.');
  }

  Future<String?> mergeVideos(List<String> videoPaths) {
    throw UnimplementedError('mergeVideos() has not been implemented.');
  }

  Future<String?> extractAudio(String videoPath) {
    throw UnimplementedError('extractAudio() has not been implemented.');
  }

  Future<String?> adjustVideoSpeed(String videoPath, double speed) {
    throw UnimplementedError('adjustVideoSpeed() has not been implemented.');
  }

  Future<String?> removeAudio(String videoPath) {
    throw UnimplementedError('removeAudio() has not been implemented.');
  }

  Future<String?> scaleVideo(String videoPath, int width, int height) {
    throw UnimplementedError('scaleVideo() has not been implemented.');
  }

  Future<String?> rotateVideo(String videoPath, int degrees) {
    throw UnimplementedError('rotateVideo() has not been implemented.');
  }

  Future<String?> generateThumbnail(String videoPath, int timeMs, int quality) {
    throw UnimplementedError('generateThumbnail() has not been implemented.');
  }
}
