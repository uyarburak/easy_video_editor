import '../enums/video_operation_type.dart';

/// Represents a video operation with its parameters
class VideoOperation {
  /// The type of operation
  final VideoOperationType type;

  /// Parameters for the operation
  final Map<String, dynamic> params;

  /// Creates a new video operation
  ///
  /// [type] The type of operation to perform
  /// [params] Parameters required for the operation
  const VideoOperation(this.type, this.params);
}
