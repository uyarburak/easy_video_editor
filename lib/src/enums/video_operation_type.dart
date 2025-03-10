/// Types of video operations supported by the editor
enum VideoOperationType {
  /// Trim video to specified duration
  trim,

  /// Merge multiple videos
  merge,

  /// Adjust video speed
  speed,

  /// Remove audio track
  removeAudio,

  /// Scale video dimensions
  scale,

  /// Rotate video
  rotate,
}
