import 'enums/enums.dart';
import 'platform/platform.dart';

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

  /// Crops a video to the specified aspect ratio.
  ///
  /// [videoPath] is the path to the video file.
  /// [aspectRatio] is the target aspect ratio in format 'width:height' (e.g., '16:9', '1:1').
  Future<String?> cropVideo(String videoPath, VideoAspectRatio aspectRatio) {
    return EasyVideoEditorPlatform.instance.cropVideo(videoPath, aspectRatio);
  }

  /// Rotates a video by the specified degrees.
  Future<String?> rotateVideo(String videoPath, int degrees) {
    return EasyVideoEditorPlatform.instance.rotateVideo(videoPath, degrees);
  }

  /// Generates a thumbnail from a video at the specified position.
  ///
  /// [videoPath] is the path to the video file.
  /// [positionMs] is the position in milliseconds where the thumbnail should be taken.
  /// [quality] is the quality of the generated thumbnail (0-100).
  /// [height] optional height of the thumbnail in pixels.
  /// [width] optional width of the thumbnail in pixels.
  Future<String?> generateThumbnail(
      String videoPath, int positionMs, int quality,
      {int? height, int? width}) {
    return EasyVideoEditorPlatform.instance.generateThumbnail(
        videoPath, positionMs, quality,
        height: height, width: width);
  }

  /// Compresses a video by adjusting its resolution and bitrate.
  ///
  /// [videoPath] is the path to the video file.
  /// [resolution] target resolution for the video (e.g., VideoResolution.p720 for 720p).
  /// If not specified, defaults to 720p while maintaining aspect ratio.
  Future<String?> compressVideo(String videoPath, {VideoResolution resolution = VideoResolution.p720}) {
    return EasyVideoEditorPlatform.instance.compressVideo(
      videoPath,
      resolution: resolution,
    );
  }
}
