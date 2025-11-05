// lib/file_video_player.dart

import 'dart:io';
import 'package:flutter/material.dart';
import 'package:video_player/video_player.dart';
import 'package:chewie/chewie.dart';

/// 외부 로컬 파일(.mp4 등)을 재생할 수 있는 비디오 플레이어 위젯
class FileVideoPlayerWidget extends StatefulWidget {
  final String videoPath; // 로컬 경로: 예) /storage/emulated/0/Download/video.mp4

  const FileVideoPlayerWidget({
    Key? key,
    required this.videoPath,
  }) : super(key: key);

  @override
  State<FileVideoPlayerWidget> createState() => _FileVideoPlayerWidgetState();
}

class _FileVideoPlayerWidgetState extends State<FileVideoPlayerWidget> {
  late VideoPlayerController _videoController;
  ChewieController? _chewieController;
  bool _isLoading = true;
  bool _hasError = false;

  @override
  void initState() {
    super.initState();
    _initializePlayer();
  }

  Future<void> _initializePlayer() async {
    try {
      final file = File(widget.videoPath);
      if (!file.existsSync()) {
        throw Exception('파일이 존재하지 않습니다: ${widget.videoPath}');
      }

      _videoController = VideoPlayerController.file(file);
      await _videoController.initialize();

      _chewieController = ChewieController(
        videoPlayerController: _videoController,
        autoPlay: false,
        looping: false,
        aspectRatio: _videoController.value.aspectRatio,
        errorBuilder: (context, errorMessage) {
          return Center(child: Text("재생 오류: $errorMessage"));
        },
      );

      setState(() {
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _hasError = true;
      });
      debugPrint('비디오 초기화 오류: $e');
    }
  }

  @override
  void dispose() {
    _videoController.dispose();
    _chewieController?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (_hasError) {
      return const Center(child: Text("영상을 불러오는 데 실패했습니다."));
    }

    if (_isLoading || _chewieController == null) {
      return const Center(child: CircularProgressIndicator());
    }

    return Chewie(controller: _chewieController!);
  }
}
