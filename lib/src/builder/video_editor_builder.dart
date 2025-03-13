import 'dart:io';

import '../easy_video_editor.dart';
import '../enums/enums.dart';
import '../models/models.dart';

/// A builder class for chaining video operations
class VideoEditorBuilder {
  final EasyVideoEditor _editor;
  String _videoPath;
  final List<VideoOperation> _operations = [];

  /// Creates a new video editor builder
  ///
  /// [videoPath] is the path to the input video file
  VideoEditorBuilder({required String videoPath})
      : _editor = EasyVideoEditor(),
        _videoPath = videoPath;

  /// Adds trim operation
  ///
  /// [startTimeMs] Start time in milliseconds
  /// [endTimeMs] End time in milliseconds
  VideoEditorBuilder trim({required int startTimeMs, required int endTimeMs}) {
    _operations.add(VideoOperation(
      VideoOperationType.trim,
      {'startTimeMs': startTimeMs, 'endTimeMs': endTimeMs},
    ));
    return this;
  }

  /// Adds merge operation
  ///
  /// [otherVideoPaths] List of video paths to merge with current video
  VideoEditorBuilder merge({required List<String> otherVideoPaths}) {
    _operations.add(VideoOperation(
      VideoOperationType.merge,
      {'paths': otherVideoPaths},
    ));
    return this;
  }

  /// Adds speed adjustment operation
  ///
  /// [speed] Speed multiplier (e.g., 0.5 for half speed, 2.0 for double speed)
  VideoEditorBuilder speed({required double speed}) {
    _operations.add(VideoOperation(
      VideoOperationType.speed,
      {'speed': speed},
    ));
    return this;
  }

  /// Adds audio removal operation
  VideoEditorBuilder removeAudio() {
    _operations.add(const VideoOperation(VideoOperationType.removeAudio, {}));
    return this;
  }

  /// Adds crop operation
  ///
  /// [aspectRatio] Target aspect ratio from predefined ratios
  VideoEditorBuilder crop({required VideoAspectRatio aspectRatio}) {
    _operations.add(VideoOperation(
      VideoOperationType.crop,
      {'aspectRatio': aspectRatio},
    ));
    return this;
  }

  /// Adds rotation operation
  ///
  /// [degree] Rotation angle (90, 180, or 270 degrees)
  VideoEditorBuilder rotate({required RotationDegree degree}) {
    _operations.add(VideoOperation(
      VideoOperationType.rotate,
      {'degrees': degree.value},
    ));
    return this;
  }

  /// Adds compression operation
  ///
  /// [resolution] Target resolution for the video (e.g., VideoResolution.p720 for 720p).
  /// If not specified, defaults to 720p while maintaining aspect ratio.
  VideoEditorBuilder compress(
      {VideoResolution resolution = VideoResolution.p720}) {
    _operations.add(VideoOperation(
      VideoOperationType.compress,
      {'resolution': resolution},
    ));
    return this;
  }

  /// Executes all operations in sequence and returns the final video path
  Future<String?> export() async {
    String? currentPath = _videoPath;
    String? previousPath;

    for (final operation in _operations) {
      if (currentPath == null) break;

      try {
        // Store the current path before executing the operation
        previousPath = currentPath;

        // Execute the operation
        currentPath = await _executeOperation(operation, currentPath);

        // If this is not the input video and the operation was successful,
        // delete the intermediate file
        if (previousPath != _videoPath && currentPath != null) {
          try {
            final file = File(previousPath);
            if (await file.exists()) {
              await file.delete();
            }
          } catch (e) {
            print('Warning: Could not delete intermediate file: $previousPath');
          }
        }
      } catch (e) {
        print('Error executing operation ${operation.type.name}: $e');
        return null;
      }
    }

    _videoPath = currentPath ?? _videoPath;
    return currentPath;
  }

  Future<String?> _executeOperation(
      VideoOperation operation, String inputPath) async {
    switch (operation.type) {
      case VideoOperationType.trim:
        return await _editor.trimVideo(
          inputPath,
          operation.params['startTimeMs'] as int,
          operation.params['endTimeMs'] as int,
        );

      case VideoOperationType.merge:
        final paths = [
          inputPath,
          ...(operation.params['paths'] as List<String>)
        ];
        return await _editor.mergeVideos(paths);

      case VideoOperationType.speed:
        return await _editor.adjustVideoSpeed(
          inputPath,
          operation.params['speed'] as double,
        );

      case VideoOperationType.removeAudio:
        return await _editor.removeAudio(inputPath);

      case VideoOperationType.crop:
        return await _editor.cropVideo(
          inputPath,
          operation.params['aspectRatio'] as VideoAspectRatio,
        );

      case VideoOperationType.rotate:
        return await _editor.rotateVideo(
          inputPath,
          operation.params['degrees'] as int,
        );

      case VideoOperationType.compress:
        return await _editor.compressVideo(
          inputPath,
          resolution: operation.params['resolution'] as VideoResolution,
        );
    }
  }

  /// Gets the current video path
  String get currentPath => _videoPath;

  /// Extracts audio from the current video
  Future<String?> extractAudio() async {
    return await _editor.extractAudio(_videoPath);
  }

  /// Generates a thumbnail from the current video
  ///
  /// [timeMs] Time position in milliseconds
  /// [quality] Quality of the thumbnail (0-100)
  Future<String?> generateThumbnail(
      {required int positionMs,
      required int quality,
      int? height,
      int? width}) async {
    return await _editor.generateThumbnail(_videoPath, positionMs, quality,
        height: height, width: width);
  }
}
