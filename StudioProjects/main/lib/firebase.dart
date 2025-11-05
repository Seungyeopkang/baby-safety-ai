// firebase.dart 파일 내용 (일부)
import 'dart:developer';
import 'dart:convert';
import 'buffer.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'main.dart';

// 이 파일에 전역 _fcmToken 변수를 정의합니다.
String? _fcmToken;
String prettyJson = '';

String getPrettyJson() {
  return prettyJson;
}


Future<void> initFCM() async {
  // Firebase 앱 초기화 (main 함수에서 이미 명시적으로 호출하지 않았다면 여기서 수행)
  // 현재 main.dart에서 initFCM 전에 Firebase.initializeApp()을 호출하지 않으므로 여기서 수행
  await Firebase.initializeApp(
    // options: DefaultFirebaseOptions.currentPlatform, // firebase_options.dart 사용 권장
  );

  NotificationSettings settings = await FirebaseMessaging.instance.requestPermission(
    alert: true,
    announcement: false,
    badge: true,
    carPlay: false,
    criticalAlert: false,
    provisional: false,
    sound: true,
  );
  log('iOS 알림 권한: ${settings.authorizationStatus}');

  _fcmToken = await FirebaseMessaging.instance.getToken();
  log("📲 FCM Token: $_fcmToken");

  FirebaseMessaging.onMessage.listen((RemoteMessage message) {

    log('🔔 Foreground 알림: ${message.notification?.title}');
    try {
      final messageMap = message.toMap();
      const jsonEncoder = JsonEncoder.withIndent('  '); // 보기 좋은 출력을 위해 들여쓰기 사용
      prettyJson = jsonEncoder.convert(messageMap);

      log('   메시지 맵: $prettyJson');
    } catch (e) {
      log('   message.toMap() 또는 JSON 인코딩 중 오류 발생: $e');
      // 오류 발생 시, 개별 필드라도 출력 시도
      log('   대체 로깅:');
      log('     메시지 ID: ${message.messageId}');
      log('     발신자: ${message.from}');
      log('     전송 시간: ${message.sentTime}');
      if (message.notification != null) {
        log('     알림 (Notification Payload):');
        log('       제목: ${message.notification!.title}');
        log('       본문: ${message.notification!.body}');
      }
      if (message.data.isNotEmpty) {
        log('     데이터 페이로드 (Data Payload): ${message.data}');
      }
    }
    log('-------------------------------------');



  });

  FirebaseMessaging.onMessageOpenedApp.listen((RemoteMessage message) {
    log('📨 알림 클릭으로 앱 열림 (onMessageOpenedApp)');

    navigateToFixedPageFromNotification(); // main.dart에 정의된 함수 호출

  });
}

String? getFcmToken() {
  return _fcmToken;
}