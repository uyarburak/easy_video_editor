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
  double _exportProgress = 0.0;

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
          .crop(
              aspectRatio:
                  VideoAspectRatio.ratio16x9) // Crop to widescreen format
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

  Future<void> _flipVideo() async {
    try {
      final editor = VideoEditorBuilder(videoPath: '/path/to/video.mp4')
          .flip(flipDirection: FlipDirection.horizontal);

      final result = await editor.export();
      setState(() {
        _status = 'Video flipped: $result';
      });
    } catch (e) {
      setState(() {
        _status = 'Error: $e';
      });
    }
  }

  Future<void> _maxFps() async {
    try {
      final editor = VideoEditorBuilder(videoPath: '/path/to/video.mp4')
          .maxFps(maxFps: 30);

      final result = await editor.export();
      setState(() {
        _status = 'Video max FPS: $result';
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

  Future<void> _exportWithProgress() async {
    try {
      // Reset progress
      setState(() {
        _exportProgress = 0.0;
        _status = 'Starting export with progress tracking...';
      });

      final editor = VideoEditorBuilder(videoPath: '/path/to/video.mp4')
          .trim(startTimeMs: 1000, endTimeMs: 10000)
          .compress(resolution: VideoResolution.p720);

      final result = await editor.export(onProgress: (progress) {
        // Update progress state
        setState(() {
          _exportProgress = progress;
          _status = 'Export progress: ${(progress * 100).toStringAsFixed(1)}%';
        });
      });

      setState(() {
        _status = 'Export completed: $result';
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
            const SizedBox(height: 10),
            ElevatedButton(
              onPressed: _exportWithProgress,
              child: const Text('Export with Progress'),
            ),
            const SizedBox(height: 10),
            ElevatedButton(
              onPressed: _flipVideo,
              child: const Text('Flip Video'),
            ),
            const SizedBox(height: 10),
            ElevatedButton(
              onPressed: _maxFps,
              child: const Text('Max FPS'),
            ),
            const SizedBox(height: 10),
            // Progress indicator
            if (_exportProgress > 0)
              Column(
                children: [
                  LinearProgressIndicator(value: _exportProgress),
                  const SizedBox(height: 5),
                  Text('${(_exportProgress * 100).toStringAsFixed(1)}%',
                      style: const TextStyle(fontWeight: FontWeight.bold)),
                ],
              ),
          ],
        ),
      ),
    );
  }
}
