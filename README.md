# Easy Video Editor

A powerful Flutter plugin for video editing operations with a simple, chainable API - without FFmpeg dependency. Perform common video editing tasks like trimming, merging, speed adjustment, and more with ease by leveraging native platform capabilities.

[![pub package](https://img.shields.io/pub/v/easy_video_editor.svg)](https://pub.dev/packages/easy_video_editor)
[![likes](https://img.shields.io/pub/likes/easy_video_editor)](https://pub.dev/packages/easy_video_editor/score)
[![popularity](https://img.shields.io/pub/popularity/easy_video_editor)](https://pub.dev/packages/easy_video_editor/score)
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
- 🖼️ **Thumbnail Generation**: Create thumbnails from video frames
- 🔗 **Builder Pattern API**: Chain operations for complex video editing
- 📱 **Platform Support**: Works on both Android and iOS

## Installation

Add this to your package's `pubspec.yaml` file:

```yaml
dependencies:
  easy_video_editor: ^0.0.1
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
final editor = VideoEditorBuilder('/path/to/video.mp4')
  .trim(0, 5000)  // Trim first 5 seconds
  .speed(1.5)     // Speed up by 1.5x
  .removeAudio(); // Remove audio

// Export the edited video
final outputPath = await editor.export();
```

### Advanced Example

```dart
final editor = VideoEditorBuilder('/path/to/video.mp4')
  // Trim video
  .trim(startTimeMs: 1000, endTimeMs: 6000)

  // Merge with another video
  .merge(['/path/to/second.mp4'])

  // Adjust speed
  .speed(2.0)

  // Scale dimensions
  .scale(width: 1280, height: 720)

  // Rotate video
  .rotate(90);

// Export the final video
final outputPath = await editor.export();

// Generate thumbnail
final thumbnailPath = await editor.generateThumbnail(
  timeMs: 2000,
  quality: 85
);
```

## API Reference

### VideoEditorBuilder

The main class for chaining video operations.

#### Methods

- `trim(int startTimeMs, int endTimeMs)`: Trim video to specified duration
- `merge(List<String> otherVideoPaths)`: Merge with other videos
- `speed(double speed)`: Adjust playback speed
- `removeAudio()`: Remove audio track
- `scale(int width, int height)`: Scale video dimensions
- `rotate(int degrees)`: Rotate video
- `export()`: Process all operations and return output path
- `extractAudio()`: Extract audio to separate file
- `generateThumbnail(int timeMs, int quality)`: Generate thumbnail

## Platform Specific Setup

### Android

Add the following permissions to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```

### iOS

Add the following keys to your `Info.plist`:

```xml
<key>NSPhotoLibraryUsageDescription</key>
<string>This app requires access to the photo library for video editing.</string>
<key>NSPhotoLibraryAddUsageDescription</key>
<string>This app requires access to the photo library to save edited videos.</string>
```

## Contributing

Contributions are welcome! Please read our [Contributing Guide](CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
