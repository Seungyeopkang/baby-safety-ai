import 'package:flutter/material.dart';
import 'package:video_player/video_player.dart';

class GoogleDriveVideoPlayer extends StatefulWidget {
  final String videoId;

  /// [videoId]는 Google Drive 파일의 고유 ID입니다.
  /// 예: 링크 https://drive.google.com/file/d/1ISjXZpys2lZbpXz0t6CGkwaEw7g0gMbP/view
  /// 에서 videoId는 '1ISjXZpys2lZbpXz0t6CGkwaEw7g0gMbP'
  const GoogleDriveVideoPlayer({super.key, required this.videoId});

  @override
  State<GoogleDriveVideoPlayer> createState() => _GoogleDriveVideoPlayerState();
}

class _GoogleDriveVideoPlayerState extends State<GoogleDriveVideoPlayer> {
  late VideoPlayerController _controller;
  bool _isInitialized = false;

  @override
  void initState() {
    super.initState();
    // Google Drive 직접 링크는 불안정할 수 있으므로 주의.
    // 실제 서비스에서는 적절한 비디오 호스팅 또는 API를 사용하는 것이 좋습니다.
    final videoUrl =
        'https://drive.google.com/uc?export=download&confirm=t&id=${widget.videoId}';
    // 'confirm=t' 또는 다른 확인 파라미터가 필요할 수 있습니다.
    // 또는 'https://docs.google.com/uc?export=download&id=${widget.videoId}'
    // 이 URL은 Google Drive 정책 변경에 따라 작동하지 않을 수 있습니다.

    _controller = VideoPlayerController.networkUrl(Uri.parse(videoUrl)) // .network 대신 .networkUrl 사용 권장
      ..initialize().then((_) {
        if (!mounted) return; // 위젯이 dispose된 후 setState 호출 방지
        setState(() {
          _isInitialized = true;
        });
        _controller.play(); // 영상 재생 시작
        _controller.setLooping(true); // <<< 영상 반복 재생 설정 추가
      }).catchError((e) {
        if (!mounted) return;
        print('Video init error: $e');
        // 사용자에게 오류를 알리는 UI 처리도 고려해볼 수 있습니다.
        setState(() {
          _isInitialized = false; // 초기화 실패 시 로딩 상태 유지 또는 오류 메시지 표시
        });
      });
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (!_isInitialized && _controller.value.hasError) {
      // 초기화 중 오류 발생 시 오류 메시지 표시 (선택 사항)
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.error_outline, color: Colors.red, size: 50),
            const SizedBox(height: 10),
            const Text('영상을 불러올 수 없습니다.'),
            Text(
              '오류: ${_controller.value.errorDescription}',
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 12, color: Colors.grey),
            ),
          ],
        ),
      );
    }

    return _isInitialized
        ? AspectRatio(
      aspectRatio: _controller.value.aspectRatio,
      child: VideoPlayer(_controller),
    )
        : const Center(child: CircularProgressIndicator());
  }
}