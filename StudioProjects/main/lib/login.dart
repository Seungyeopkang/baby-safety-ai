import 'package:flutter/cupertino.dart';
import 'apiservice.dart';
import 'kidscafe.dart'; // 메인 페이지로 가정합니다.
import 'register.dart';
import 'FindID.dart';
import 'firebase.dart'; // getFcmToken() 함수가 여기에 정의되어 있다고 가정합니다.

class LoginPage extends StatefulWidget {
  const LoginPage({Key? key}) : super(key: key);

  @override
  CupertinoLoginPageState createState() => CupertinoLoginPageState();
}

class CupertinoLoginPageState extends State<LoginPage> {
  final TextEditingController idController = TextEditingController();
  final TextEditingController pwController = TextEditingController();

  late final ApiService _apiService;

  @override
  void initState() {
    super.initState();
    _apiService = ApiService(baseUrl: 'http://10.0.2.2:8080');
  }

  void login() async {
    String id = idController.text.trim();
    String pw = pwController.text.trim();

    String? jwtToken = await _apiService.signIn(id, pw);

    if (jwtToken != null) {
      String? fcmToken = getFcmToken();

      if (fcmToken != null) {
        // sendFCMToken에 context를 전달하도록 수정
        // `context: context` 부분을 제거합니다.
        bool fcmResult = await _apiService.sendFCMToken(fcmToken);
        if (!fcmResult) {
          print("⚠️ FCM 토큰 전송 실패");
        }
      } else {
        print("⚠️ FCM 토큰이 없습니다.");
      }

      Navigator.pushReplacement(
          context, CupertinoPageRoute(builder: (context) => const mainPage()));
    } else {
      showCupertinoDialog(
        context: context,
        builder: (context) => CupertinoAlertDialog(
          title: const Text('로그인 실패'),
          content: const Text('아이디 또는 비밀번호를 확인하세요.'),
          actions: [
            CupertinoDialogAction(
              child: const Text('확인'),
              onPressed: () {
                Navigator.of(context).pop();
              },
            )
          ],
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    const backgroundColor = Color.fromRGBO(76, 88, 229, 1.0);
    const buttonColor = Color.fromRGBO(134, 145, 255, 1.0); // 부드러운 연보라색
    const buttonTextColor = CupertinoColors.white;

    return CupertinoPageScaffold(
      backgroundColor: backgroundColor,
      navigationBar: const CupertinoNavigationBar(middle: Text('')),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Image(
              image: AssetImage('assets/Logo.jpeg'),
              width: 200,
              height: 200,
            ),
            CupertinoTextField(
              controller: idController,
              placeholder: 'ID',
              prefix: const Padding(
                padding: EdgeInsets.symmetric(horizontal: 15),
                child: Icon(CupertinoIcons.person),
              ),
              padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 12),
              decoration: BoxDecoration(
                color: CupertinoColors.white,
                border: Border.all(color: CupertinoColors.systemGrey4),
                borderRadius: BorderRadius.circular(20),
                boxShadow: [
                  BoxShadow(
                    color: CupertinoColors.systemGrey.withOpacity(0.4),
                    offset: Offset(0, 0),
                    blurRadius: 8,
                    spreadRadius: 2,
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
            CupertinoTextField(
              controller: pwController,
              placeholder: 'PW',
              obscureText: true,
              prefix: const Padding(
                padding: EdgeInsets.symmetric(horizontal: 15),
                child: Icon(CupertinoIcons.lock),
              ),
              padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 12),
              decoration: BoxDecoration(
                color: CupertinoColors.white,
                border: Border.all(color: CupertinoColors.systemGrey4),
                borderRadius: BorderRadius.circular(20),
                boxShadow: [
                  BoxShadow(
                    color: CupertinoColors.systemGrey.withOpacity(0.4),
                    offset: Offset(0, 0),
                    blurRadius: 8,
                    spreadRadius: 2,
                  ),
                ],
              ),
            ),
            const SizedBox(height: 40),
            CupertinoButton(
              onPressed: login,
              color: buttonColor,
              padding: const EdgeInsets.symmetric(horizontal: 100, vertical: 15),
              borderRadius: BorderRadius.circular(20),
              child: const Icon(CupertinoIcons.rocket, size: 25.0, color: CupertinoColors.white),
            ),
            const SizedBox(height: 100),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: [
                CupertinoButton(
                  color: buttonColor,
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 12),
                  borderRadius: BorderRadius.circular(20),
                  onPressed: () {
                    Navigator.push(
                      context,
                      CupertinoPageRoute(builder: (context) => const RegisterPage()),
                    );
                  },
                  child: Row(
                    mainAxisSize: MainAxisSize.min, // 이 부분은 원래도 옳았습니다.
                    children: const [
                      Icon(CupertinoIcons.person_add, size: 30.0, color: buttonTextColor),
                      SizedBox(width: 6),
                      Text('회원가입', style: TextStyle(color: buttonTextColor)),
                    ],
                  ),
                ),
                CupertinoButton(
                  color: buttonColor,
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 12),
                  borderRadius: BorderRadius.circular(15),
                  onPressed: () {
                    Navigator.push(
                      context,
                      CupertinoPageRoute(builder: (context) => const FindidPage()),
                    );
                  },
                  child: Row(
                    mainAxisSize: MainAxisSize.min, // <-- 이 부분을 MainAxisSize.min 으로 수정했습니다.
                    children: const [
                      Icon(CupertinoIcons.search, size: 30.0, color: buttonTextColor),
                      SizedBox(width: 6),
                      Text('ID 찾기', style: TextStyle(color: buttonTextColor)),
                    ],
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}