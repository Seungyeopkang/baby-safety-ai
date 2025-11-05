import 'dart:convert';
import 'dart:io';

/// 로컬 HTTP 서버 시작 함수
Future<void> startHttpServer(Function(String title, String msg) onNotify) async {
  // 서버 바인딩 (0.0.0.0 = 모든 IPv4 인터페이스)
  var server = await HttpServer.bind(InternetAddress.anyIPv4, 8080);

  Future<String> getLocalIp() async {
    for (var interface in await NetworkInterface.list()) {
      for (var addr in interface.addresses) {
        if (addr.type == InternetAddressType.IPv4 && !addr.isLoopback) {
          return addr.address;
        }
      }
    }
    return 'localhost';
  }

  String localIp = await getLocalIp();
  print('Server running on http://$localIp:${server.port}');

  await for (HttpRequest request in server) {
    if (request.method == 'POST' && request.uri.path == '/notify') {
      try {
        // 요청 바디 읽기
        final content = await utf8.decoder.bind(request).join();
        final data = jsonDecode(content);

        final title = data['title'] ?? '제목 없음';
        final msg = data['msg'] ?? '메시지 없음';

        // 알림 콜백 호출
        onNotify(title, msg);

        // 응답
        request.response
          ..statusCode = HttpStatus.ok
          ..write('Notification sent')
          ..close();
      } catch (e) {
        request.response
          ..statusCode = HttpStatus.badRequest
          ..write('Invalid request: $e')
          ..close();
      }
    } else {
      request.response
        ..statusCode = HttpStatus.notFound
        ..write('Not found')
        ..close();
    }
  }
}
