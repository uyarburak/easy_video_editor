import 'package:flutter_test/flutter_test.dart';
import 'package:easy_video_editor/easy_video_editor.dart';

void main() {
  group('VideoEditorBuilder', () {
    late VideoEditorBuilder builder;
    const testVideoPath = 'test_video.mp4';

    setUp(() {
      builder = VideoEditorBuilder(testVideoPath);
    });

    test('initializes with correct video path', () {
      expect(builder.currentPath, equals(testVideoPath));
    });

    test('builds chain of operations', () {
      final result = builder
          .trim(0, 1000)
          .removeAudio()
          .rotate(90)
          .scale(1280, 720);

      expect(result, isA<VideoEditorBuilder>());
      expect(result.currentPath, equals(testVideoPath));
    });
  });
}
