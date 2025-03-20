# Easy Video Editor

A powerful Flutter plugin for video editing operations with a simple, chainable API - without FFmpeg dependency. Perform common video editing tasks like trimming, merging, speed adjustment, and more with ease by leveraging native platform capabilities.

[![pub package](https://img.shields.io/pub/v/easy_video_editor.svg)](https://pub.dev/packages/easy_video_editor)
[![likes](https://img.shields.io/pub/likes/easy_video_editor)](https://pub.dev/packages/easy_video_editor/score)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## Features

- 🎬 **Video Trimming**: Cut videos at specified start and end times
- 🔄 **Video Merging**: Combine multiple videos into one
- ⚡ **Speed Adjustment**: Change video playback speed
- 🔊 **Audio Operations**:
  - Extract audio from video
  - Remove audio from video
- 📐 **Video Transformations**:
  - Scale video to specific dimensions
  - Rotate video by specified degrees
  - Crop video to specific aspect ratios
- 🗜️ **Video Compression**:
  - Compress videos to standard resolutions (360p to 4K)
  - Maintains aspect ratio while resizing
- 🖼️ **Thumbnail Generation**: Create thumbnails from video frames
- 📊 **Video Metadata**: Retrieve detailed information about video files
- 🔗 **Builder Pattern API**: Chain operations for complex video editing
- 📱 **Platform Support**: Works on both Android and iOS

## Installation

Add this to your package's `pubspec.yaml` file:

```yaml
dependencies:
  easy_video_editor: ^0.0.4
```

Or install via command line:

```bash
flutter pub add easy_video_editor
```

## Usage

### Basic Example

```dart
import 'package:easy_video_editor/easy_video_editor.dart';

// Create a builder instance
final editor = VideoEditorBuilder(videoPath: '/path/to/video.mp4')
  .trim(startTimeMs: 0, endTimeMs: 5000)  // Trim first 5 seconds
  .speed(speed: 1.5)     // Speed up by 1.5x
  .removeAudio(); // Remove audio

// Export the edited video
final outputPath = await editor.export(
  outputPath: '/path/to/output.mp4' // Optional output path
);
```

### Advanced Example

```dart
final editor = VideoEditorBuilder(videoPath: '/path/to/video.mp4')
  // Trim video
  .trim(startTimeMs: 1000, endTimeMs: 6000)

  // Merge with another video
  .merge(otherVideoPaths: ['/path/to/second.mp4'])

  // Adjust speed
  .speed(speed: 1.5)

  // Compress video
  .compress(resolution: VideoResolution.p720)

  // Crop video
  .crop(aspectRatio: VideoAspectRatio.ratio16x9)

  // Rotate video
  .rotate(degree: RotationDegree.d90);

// Export the final video
final outputPath = await editor.export(
  outputPath: '/path/to/output.mp4' // Optional output path
);

// Extract audio
final audioPath = await editor.extractAudio(
  outputPath: '/path/to/output.m4a' // Optional output path, iOS outputs M4A format
);

// Generate thumbnail
final thumbnailPath = await editor.generateThumbnail(
  positionMs: 2000,
  quality: 85,
  width: 1280,    // optional
  height: 720,    // optional
  outputPath: '/path/to/thumbnail.jpg' // Optional output path
);

// Get video metadata
final metadata = await editor.getVideoMetadata();
print('Duration: ${metadata.duration} ms');
print('Dimensions: ${metadata.width}x${metadata.height}');
print('Orientation: ${metadata.rotation}°');
print('File size: ${metadata.fileSize} bytes');
```

### Cancel Operation

```dart
final editor = VideoEditorBuilder(videoPath: '/path/to/video.mp4');

// Start an operation
final outputPath = await editor.trim(startTimeMs: 0, endTimeMs: 5000);

// Cancel the operation
await editor.cancel();
```

## API Reference

### VideoEditorBuilder

The main class for chaining video operations.

#### Constructor

- `VideoEditorBuilder({required String videoPath})`: Creates a new builder instance

#### Methods

- `trim({required int startTimeMs, required int endTimeMs})`: Trim video to specified duration (outputs MP4)
- `merge({required List<String> otherVideoPaths})`: Merge with other videos (outputs MP4)
- `speed({required double speed})`: Adjust playback speed (e.g., 0.5 for half speed, 2.0 for double speed) (outputs MP4)
- `removeAudio()`: Remove audio track (outputs MP4)
- `rotate({required RotationDegree degree})`: Rotate video by 90, 180, or 270 degrees (outputs MP4)
- `crop({required VideoAspectRatio aspectRatio})`: Crop video to predefined aspect ratio (outputs MP4)
- `compress({VideoResolution resolution = VideoResolution.p720})`: Compress video to standard resolution (outputs MP4)
  - Available resolutions: 360p, 480p, 720p, 1080p, 1440p, 2160p (4K)
  - Maintains original aspect ratio
- `export({String? outputPath})`: Process all operations and return output path (outputs MP4)
- `extractAudio({String? outputPath})`: Extract audio to separate file (outputs M4A on iOS, AAC on Android)
- `generateThumbnail({required int positionMs, required int quality, int? width, int? height, String? outputPath})`: Generate thumbnail (outputs JPEG)
- `getVideoMetadata()`: Retrieves detailed metadata about the current video file
- `get currentPath`: Gets the current video path

## Platform Specific Setup

### Android

Add the following permissions to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```

### iOS

Requires iOS 13.0 or later.

Add the following keys to your `Info.plist`:

```xml
<key>NSPhotoLibraryUsageDescription</key>
<string>This app requires access to the photo library for video editing.</string>
<key>NSPhotoLibraryAddUsageDescription</key>
<string>This app requires access to the photo library to save edited videos.</string>
```

## 🤝 Contributing

Contributions are always welcome! Here's how you can help:

1. 🐛 Report bugs by opening an issue
2. 💡 Suggest new features or improvements
3. 📝 Improve documentation
4. 🔧 Submit pull requests

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📧 Author

[iawtk2302](https://github.com/iawtk2302)

## ⭐ Show Your Support

If you find this plugin helpful, please give it a star on [GitHub](https://github.com/iawtk2302/easy_video_editor)! It helps others discover the plugin and motivates me to keep improving it.
