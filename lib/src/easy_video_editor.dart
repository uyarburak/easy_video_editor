import 'platform/easy_video_editor_platform_interface.dart';

/// Main class for video editing operations
class EasyVideoEditor {
  /// Trims a video to the specified start and end times.
  Future<String?> trimVideo(String videoPath, int startTime, int endTime) {
    return EasyVideoEditorPlatform.instance
        .trimVideo(videoPath, startTime, endTime);
  }

  /// Merges multiple videos into a single video.
  Future<String?> mergeVideos(List<String> videoPaths) {
    return EasyVideoEditorPlatform.instance.mergeVideos(videoPaths);
  }

  /// Extracts audio from a video file.
  Future<String?> extractAudio(String videoPath) {
    return EasyVideoEditorPlatform.instance.extractAudio(videoPath);
  }

  /// Adjusts the playback speed of a video.
  Future<String?> adjustVideoSpeed(String videoPath, double speed) {
    return EasyVideoEditorPlatform.instance.adjustVideoSpeed(videoPath, speed);
  }

  /// Removes audio from a video file.
  Future<String?> removeAudio(String videoPath) {
    return EasyVideoEditorPlatform.instance.removeAudio(videoPath);
  }

  /// Scales a video to the specified dimensions.
  Future<String?> scaleVideo(String videoPath, int width, int height) {
    return EasyVideoEditorPlatform.instance
        .scaleVideo(videoPath, width, height);
  }

  /// Rotates a video by the specified degrees.
  Future<String?> rotateVideo(String videoPath, int degrees) {
    return EasyVideoEditorPlatform.instance.rotateVideo(videoPath, degrees);
  }

  /// Generates a thumbnail from a video at the specified time.
  Future<String?> generateThumbnail(String videoPath, int timeMs, int quality) {
    return EasyVideoEditorPlatform.instance
        .generateThumbnail(videoPath, timeMs, quality);
  }
}
