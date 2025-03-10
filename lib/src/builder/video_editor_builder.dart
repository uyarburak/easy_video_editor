import 'dart:io';

import '../easy_video_editor.dart';
import '../enums/video_operation_type.dart';
import '../models/video_operation.dart';

/// A builder class for chaining video operations
class VideoEditorBuilder {
  final EasyVideoEditor _editor;
  String _videoPath;
  final List<VideoOperation> _operations = [];

  /// Creates a new video editor builder
  ///
  /// [videoPath] is the path to the input video file
  VideoEditorBuilder(String videoPath)
      : _editor = EasyVideoEditor(),
        _videoPath = videoPath;

  /// Adds trim operation
  ///
  /// [startTimeMs] Start time in milliseconds
  /// [endTimeMs] End time in milliseconds
  VideoEditorBuilder trim(int startTimeMs, int endTimeMs) {
    _operations.add(VideoOperation(
      VideoOperationType.trim,
      {'startTimeMs': startTimeMs, 'endTimeMs': endTimeMs},
    ));
    return this;
  }

  /// Adds merge operation
  ///
  /// [otherVideoPaths] List of video paths to merge with current video
  VideoEditorBuilder merge(List<String> otherVideoPaths) {
    _operations.add(VideoOperation(
      VideoOperationType.merge,
      {'paths': otherVideoPaths},
    ));
    return this;
  }

  /// Adds speed adjustment operation
  ///
  /// [speed] Speed multiplier (e.g., 0.5 for half speed, 2.0 for double speed)
  VideoEditorBuilder speed(double speed) {
    _operations.add(VideoOperation(
      VideoOperationType.speed,
      {'speed': speed},
    ));
    return this;
  }

  /// Adds audio removal operation
  VideoEditorBuilder removeAudio() {
    _operations.add(VideoOperation(VideoOperationType.removeAudio, {}));
    return this;
  }

  /// Adds scale operation
  ///
  /// [width] Target width in pixels
  /// [height] Target height in pixels
  VideoEditorBuilder scale(int width, int height) {
    _operations.add(VideoOperation(
      VideoOperationType.scale,
      {'width': width, 'height': height},
    ));
    return this;
  }

  /// Adds rotation operation
  ///
  /// [degrees] Rotation angle in degrees (should be 90, 180, or 270)
  VideoEditorBuilder rotate(int degrees) {
    _operations.add(VideoOperation(
      VideoOperationType.rotate,
      {'degrees': degrees},
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
          operation.params['startTimeMs'],
          operation.params['endTimeMs'],
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
          operation.params['speed'],
        );

      case VideoOperationType.removeAudio:
        return await _editor.removeAudio(inputPath);

      case VideoOperationType.scale:
        return await _editor.scaleVideo(
          inputPath,
          operation.params['width'],
          operation.params['height'],
        );

      case VideoOperationType.rotate:
        return await _editor.rotateVideo(
          inputPath,
          operation.params['degrees'],
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
  Future<String?> generateThumbnail(int timeMs, int quality) async {
    return await _editor.generateThumbnail(_videoPath, timeMs, quality);
  }
}
