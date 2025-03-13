import 'package:easy_video_editor/easy_video_editor.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('VideoEditorBuilder', () {
    late VideoEditorBuilder builder;
    const testVideoPath = 'test_video.mp4';

    setUp(() {
      builder = VideoEditorBuilder(videoPath: testVideoPath);
    });

    test('initializes with correct video path', () {
      expect(builder.currentPath, equals(testVideoPath));
    });

    test('builds chain of operations', () {
      final result = builder
          .trim(startTimeMs: 0, endTimeMs: 1000)
          .removeAudio()
          .rotate(degree: RotationDegree.degree90)
          .crop(aspectRatio: VideoAspectRatio.ratio16x9);

      expect(result, isA<VideoEditorBuilder>());
      expect(result.currentPath, equals(testVideoPath));
    });
  });
}
