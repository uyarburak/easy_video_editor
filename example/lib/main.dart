import 'dart:async';

import 'package:easy_video_editor/easy_video_editor.dart';
import 'package:flutter/material.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return const MaterialApp(
      home: HomePage(),
    );
  }
}

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  String _status = '';

  Future<void> _trimAndSpeed() async {
    try {
      final editor = VideoEditorBuilder(videoPath: '/path/to/video.mp4')
          .trim(startTimeMs: 0, endTimeMs: 5000) // Cắt 5 giây đầu
          .speed(speed: 1.5); // Tăng tốc độ 1.5x

      final result = await editor.export();
      setState(() {
        _status = 'Video processed: $result';
      });
    } catch (e) {
      setState(() {
        _status = 'Error: $e';
      });
    }
  }

  Future<void> _removeAudio() async {
    try {
      final editor =
          VideoEditorBuilder(videoPath: '/path/to/video.mp4').removeAudio();

      final result = await editor.export();
      setState(() {
        _status = 'Audio removed: $result';
      });
    } catch (e) {
      setState(() {
        _status = 'Error: $e';
      });
    }
  }

  Future<void> _cropAndRotate() async {
    try {
      final editor = VideoEditorBuilder(videoPath: '/path/to/video.mp4')
          .crop(aspectRatio: VideoAspectRatio.ratio16x9) // Crop to widescreen format
          .rotate(degree: RotationDegree.degree90);

      final result = await editor.export();
      setState(() {
        _status = 'Video transformed: $result';
      });
    } catch (e) {
      setState(() {
        _status = 'Error: $e';
      });
    }
  }

  Future<void> _generateThumbnail() async {
    try {
      final editor = VideoEditorBuilder(videoPath: '/path/to/video.mp4');
      final result =
          await editor.generateThumbnail(positionMs: 1000, quality: 85);
      setState(() {
        _status = 'Thumbnail generated: $result';
      });
    } catch (e) {
      setState(() {
        _status = 'Error: $e';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Video Editor Builder Demo'),
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(_status, style: const TextStyle(fontSize: 16)),
            const SizedBox(height: 20),
            ElevatedButton(
              onPressed: _trimAndSpeed,
              child: const Text('Trim & Speed Up'),
            ),
            const SizedBox(height: 10),
            ElevatedButton(
              onPressed: _removeAudio,
              child: const Text('Remove Audio'),
            ),
            const SizedBox(height: 10),
            ElevatedButton(
              onPressed: _cropAndRotate,
              child: const Text('Crop & Rotate'),
            ),
            const SizedBox(height: 10),
            ElevatedButton(
              onPressed: _generateThumbnail,
              child: const Text('Generate Thumbnail'),
            ),
          ],
        ),
      ),
    );
  }
}
