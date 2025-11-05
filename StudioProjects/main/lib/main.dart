import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart'; // NotificationLandingPage의 Scaffold/AppBar를 위해 (혹은 Cupertino 위젯으로 통일)
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'buffer.dart';
import 'login.dart'; // 로그인 화면 임포트
import 'kidscafe.dart'; // 메인 화면 임포트 (mainPage 위젯 포함)
import 'firebase.dart'; // firebase.dart에서 정의된 initFCM 함수
import 'apiservice.dart'; // ApiService 임포트


Future<void> _firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  // 백그라운드 핸들러는 자체적인 초기화가 필요할 수 있습니다.
  await Firebase.initializeApp();

  _saveNotificationToBuffer(message);
}


void _saveNotificationToBuffer(RemoteMessage message) {
  final String? title = message.notification?.title;
  final String? body = message.notification?.body;

  // 제목과 본문이 모두 있을 때만 저장 (또는 둘 중 하나만 있어도 저장하도록 정책 변경 가능)
  if (title != null && body != null) {
    NotificationPut(title, body);
  } else if (title != null) { // 제목만 있는 경우
    NotificationPut(title, message.data['custom_body'] ?? '내용 없음'); // data 페이로드에서 body를 찾거나 기본값 사용
  } else if (message.data.isNotEmpty) { // notification 페이로드 없이 data 페이로드만 있는 경우
    // data 페이로드에서 title, body 에 해당하는 값을 찾아 저장
    String dataTitle = message.data['title'] ?? message.data['alert_title'] ?? '제목 없음';
    String dataBody = message.data['body'] ?? message.data['alert_body'] ?? '내용 없음';
    NotificationPut(dataTitle, dataBody);
  }
  else {
  }
}

final GlobalKey<NavigatorState> navigatorKey = GlobalKey<NavigatorState>();
final CupertinoTabController tabController = CupertinoTabController();

class NotificationLandingPage extends StatelessWidget {
  static const String routeName = '/notification_landing_page'; // 명명된 라우트 사용 시

  const NotificationLandingPage({super.key});


  @override
  Widget build(BuildContext context) {
    return CupertinoPageScaffold( // Cupertino 스타일로 통일
      navigationBar: CupertinoNavigationBar(
        middle: const Text('알림 도착!'),
        // leading을 사용하여 뒤로가기 버튼을 명시적으로 추가할 수 있습니다.
        // CupertinoPageRoute는 기본적으로 스와이프로 뒤로가기를 지원합니다.
        leading: CupertinoButton(
          padding: EdgeInsets.zero,
          child: const Icon(CupertinoIcons.back),
          onPressed: () {
            if (Navigator.canPop(context)) {
              Navigator.pop(context);
            }
            // 혹은 특정 페이지로 돌아가고 싶다면 Navigator.pushReplacementNamed 등 사용
          },
        ),
      ),
      child: const Center(

        child: Text('알림을 통해 이 페이지로 이동했습니다!'),
      ),
    );
  }
}

// 3. 네비게이션 함수 (firebase.dart 에서도 호출될 수 있도록 main.dart 최상단 또는 접근 가능한 곳에 위치)
void navigateToFixedPageFromNotification() {
  // navigatorKey.currentState가 null이 아닐 때만 push 시도
  if (navigatorKey.currentState != null) {
    print("Navigating to NotificationLandingPage from notification click.");
    tabController.index = 1;
    // 명명된 라우트 사용 시 (CupertinoApp의 routes에 정의 필요)
    // navigatorKey.currentState!.pushNamed(NotificationLandingPage.routeName);
  } else {
    print('🔴 네비게이터 상태가 null입니다. 페이지 이동 실패 (navigateToFixedPageFromNotification).');
    // 앱 초기화 과정에서 너무 빨리 호출될 경우를 대비해 약간의 지연 후 재시도 고려 가능
    // Future.delayed(const Duration(milliseconds: 500), () {
    //   if (navigatorKey.currentState != null) {
    //     navigatorKey.currentState!.push(
    //       CupertinoPageRoute(builder: (_) => const NotificationLandingPage())
    //     );
    //   }
    // });
  }
}

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  buffer();
  // Firebase.initializeApp()은 initFCM 내부에서 호출되거나,
  // 여기서 명시적으로 호출하고 initFCM에서는 중복 호출을 피하도록 할 수 있습니다.
  // 현재 firebase.dart 코드 기준으로 initFCM이 Firebase.initializeApp()을 호출합니다.
  await initFCM(); // FCM 리스너 설정 (onMessageOpenedApp 포함)

  // 앱이 종료된 상태에서 알림 클릭으로 실행된 경우 초기 메시지 처리
  RemoteMessage? initialMessage = await FirebaseMessaging.instance.getInitialMessage();
  bool openedFromNotification = initialMessage != null;

  FirebaseMessaging.onBackgroundMessage(_firebaseMessagingBackgroundHandler);

  if (openedFromNotification) {
    print("App launched from terminated state via notification.");
    // 네비게이션은 MyApp 위젯이 빌드되고 초기화된 후 수행하도록 플래그만 전달
  }

  runApp(MyApp(openedFromNotification: openedFromNotification));
}

class MyApp extends StatefulWidget {
  final bool openedFromNotification;

  const MyApp({super.key, this.openedFromNotification = false});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final ApiService _apiService = ApiService(baseUrl: 'http://10.0.2.2:8080');

  // 초기 위젯은 로딩 인디케이터로 설정
  Widget _initialWidget = const CupertinoPageScaffold(
    child: Center(child: CupertinoActivityIndicator()),
  );
  bool _isInitializationComplete = false;

  @override
  void initState() {
    super.initState();
    _initializeApp();
  }

  Future<void> _initializeApp() async {
    await _checkLoginStatus(); // 로그인 상태 확인 및 _initialWidget 설정

    // 로그인 상태 확인 및 초기 화면 설정이 완료된 후,
    // 그리고 위젯이 화면에 그려진 후에 알림 네비게이션 수행
    if (widget.openedFromNotification) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted) { // 위젯이 여전히 마운트된 상태인지 확인
          navigateToFixedPageFromNotification();
        }
      });
    }
    if(mounted){
      setState(() {
        _isInitializationComplete = true;
      });
    }
  }

  Future<void> _checkLoginStatus() async {
    String? token = await _apiService.getJwtToken();
    Widget determinedWidget;

    if (token == null) {
      determinedWidget = const LoginPage();
      print("No JWT token found. Setting LoginPage.");
    } else {
      bool isValid = await _apiService.isTokenValid(token);
      if (isValid) {
        determinedWidget = const mainPage(); // kidscafe.dart에 정의된 mainPage 위젯
        print("Valid JWT token found. Setting mainPage.");
      } else {
        await _apiService.deleteJwtToken(); // 토큰 삭제 기다림
        determinedWidget = const LoginPage();
        print("Invalid or expired JWT token. Setting LoginPage.");
      }
    }

    // setState는 mounted된 상태에서만 호출해야 함
    if (mounted) {
      setState(() {
        _initialWidget = determinedWidget;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return CupertinoApp(
      navigatorKey: navigatorKey, // 전역 네비게이터 키 할당
      title: 'AI Kids Cafe',
      theme: const CupertinoThemeData(
        primaryColor: Color.fromRGBO(76, 88, 229, 1.0),
        brightness: Brightness.light,
      ),
      // _isInitializationComplete 플래그를 사용하여 _initialWidget이 확정되기 전까지 로딩 표시
      home: _isInitializationComplete ? _initialWidget : const CupertinoPageScaffold(
        child: Center(child: CupertinoActivityIndicator()),
      ),
      // 명명된 라우트 사용 시 여기에 등록
      routes: {
        NotificationLandingPage.routeName: (context) => const NotificationLandingPage(),
      },
    );
  }
}