import 'package:flutter_local_notifications/flutter_local_notifications.dart';

final FlutterLocalNotificationsPlugin flutterLocalNotificationsPlugin =
FlutterLocalNotificationsPlugin();

Future<void> initializeNotification() async {
  // iOS 권한 요청
  final bool? result = await flutterLocalNotificationsPlugin
      .resolvePlatformSpecificImplementation<IOSFlutterLocalNotificationsPlugin>()
      ?.requestPermissions(alert: true, badge: true, sound: true);
  print('iOS 알림 권한: $result');

  // Android 설정 추가
  const AndroidInitializationSettings initializationSettingsAndroid =
  AndroidInitializationSettings('@mipmap/ic_launcher'); // 앱 아이콘 경로

  const DarwinInitializationSettings initializationSettingsIOS =
  DarwinInitializationSettings();

  const InitializationSettings initializationSettings = InitializationSettings(
    android: initializationSettingsAndroid,
    iOS: initializationSettingsIOS,
  );

  await flutterLocalNotificationsPlugin.initialize(initializationSettings);
}
Future<void> showNotification(String title, String msg) async {
  const AndroidNotificationDetails androidDetails = AndroidNotificationDetails(
    'your_channel_id', // 반드시 고유한 채널 ID
    'your_channel_name', // 사용자에게 표시될 이름
    channelDescription: 'your_channel_description',
    importance: Importance.max,
    priority: Priority.high,
  );

  const DarwinNotificationDetails iosDetails = DarwinNotificationDetails();

  const NotificationDetails platformDetails = NotificationDetails(
    android: androidDetails,
    iOS: iosDetails,
  );

  await flutterLocalNotificationsPlugin.show(
    1,
    title,
    msg,
    platformDetails,
    // payload: 'videoId_123', // 필요한 경우 사용
  );
}