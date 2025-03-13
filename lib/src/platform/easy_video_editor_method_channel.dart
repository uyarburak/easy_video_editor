import 'package:easy_video_editor/src/enums/enums.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'easy_video_editor_platform_interface.dart';

/// An implementation of [EasyVideoEditorPlatform] that uses method channels.
class MethodChannelEasyVideoEditor extends EasyVideoEditorPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('easy_video_editor');

  @override
  Future<String?> trimVideo(
      String videoPath, int startTimeMs, int endTimeMs) async {
    final result = await methodChannel.invokeMethod<String>('trimVideo', {
      'videoPath': videoPath,
      'startTimeMs': startTimeMs,
      'endTimeMs': endTimeMs,
    });
    return result;
  }

  @override
  Future<String?> mergeVideos(List<String> videoPaths) async {
    final result = await methodChannel.invokeMethod<String>('mergeVideos', {
      'videoPaths': videoPaths,
    });
    return result;
  }

  @override
  Future<String?> extractAudio(String videoPath) async {
    final result = await methodChannel.invokeMethod<String>('extractAudio', {
      'videoPath': videoPath,
    });
    return result;
  }

  @override
  Future<String?> adjustVideoSpeed(String videoPath, double speed) async {
    final result =
        await methodChannel.invokeMethod<String>('adjustVideoSpeed', {
      'videoPath': videoPath,
      'speed': speed,
    });
    return result;
  }

  @override
  Future<String?> removeAudio(String videoPath) async {
    final result = await methodChannel.invokeMethod<String>('removeAudio', {
      'videoPath': videoPath,
    });
    return result;
  }

  @override
  Future<String?> cropVideo(
      String videoPath, VideoAspectRatio aspectRatio) async {
    final result = await methodChannel.invokeMethod<String>('cropVideo', {
      'videoPath': videoPath,
      'aspectRatio': aspectRatio.value,
    });
    return result;
  }

  @override
  Future<String?> rotateVideo(String videoPath, int degrees) async {
    final result = await methodChannel.invokeMethod<String>('rotateVideo', {
      'videoPath': videoPath,
      'rotationDegrees': degrees,
    });
    return result;
  }

  @override
  Future<String?> generateThumbnail(
      String videoPath, int positionMs, int quality,
      {int? height, int? width}) async {
    final result =
        await methodChannel.invokeMethod<String>('generateThumbnail', {
      'videoPath': videoPath,
      'positionMs': positionMs,
      'quality': quality,
      if (height != null) 'height': height,
      if (width != null) 'width': width,
    });
    return result;
  }

  @override
  Future<String?> compressVideo(String videoPath,
      {VideoResolution resolution = VideoResolution.p720}) async {
    final result = await methodChannel.invokeMethod<String>('compressVideo', {
      'videoPath': videoPath,
      'targetHeight': resolution.height,
    });
    return result;
  }
}
