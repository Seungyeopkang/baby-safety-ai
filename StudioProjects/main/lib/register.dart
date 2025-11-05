import 'package:flutter/cupertino.dart';
import 'login.dart';
import 'apiservice.dart'; // 실제 프로젝트 경로에 맞게 수정하세요.

class RegisterPage extends StatefulWidget{
  const RegisterPage({Key? key}) : super(key: key);

  @override // @override 어노테이션 추가 (선택 사항이지만 좋은 습관입니다)
  CupertinoRegisterPageState createState() => CupertinoRegisterPageState();
}

class CupertinoRegisterPageState extends State<RegisterPage> {
  final TextEditingController idController = TextEditingController();
  final TextEditingController pwController = TextEditingController();
  final TextEditingController EmailController = TextEditingController();
  final TextEditingController NameController = TextEditingController();
  final TextEditingController PhoneController = TextEditingController();
  bool _isSwitched = false;

  // ApiService 인스턴스 선언
  late final ApiService _apiService;

  @override
  void initState() {
    super.initState();
    // ApiService 인스턴스 초기화. 'YOUR_BASE_URL_HERE'를 실제 API 서버 주소로 변경하세요.
    // 예시: ''http://10.0.2.2:8080'' 또는 'https://api.yourdomain.com'
    _apiService = ApiService(baseUrl: 'http://10.0.2.2:8080');
  }

  void register() async {
    String id = idController.text.trim(); // trim()으로 공백 제거
    String pw = pwController.text.trim();
    String email = EmailController.text.trim();
    String name = NameController.text.trim();
    String phone = PhoneController.text.trim(); // 중요: PhoneController.text로 수정

    String role = _isSwitched ? 'ADMIN' : 'USER';

    // _apiService 인스턴스를 통해 signUp 메서드를 호출합니다.
    bool isRegistered = await _apiService.signUp(id, pw, name, phone, email, role);

    if (isRegistered) {
      showCupertinoDialog(
        context: context,
        builder: (context) => CupertinoAlertDialog(
          title: const Text('회원가입 성공'),
          content: const Text('성공적으로 회원가입되었습니다.'), // 사용자에게 더 명확한 메시지
          actions: <Widget>[
            CupertinoDialogAction(
              child: const Text('확인'),
              onPressed: () {
                Navigator.of(context).pop();
                Navigator.pushReplacement(
                    context,
                    CupertinoPageRoute(builder: (context) => const LoginPage())
                );
              },
            ),
          ],
        ),
      );
    } else {
      // 회원가입 실패 시 알림
      showCupertinoDialog(
        context: context,
        builder: (context) => CupertinoAlertDialog(
          title: const Text('회원가입 실패'),
          content: const Text('회원가입에 실패했습니다. 아이디 중복 또는 정보를 확인해주세요.'), // 실패 메시지
          actions: <Widget>[
            CupertinoDialogAction(
              child: const Text('확인'),
              onPressed: () {
                Navigator.of(context).pop();
              },
            ),
          ],
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return CupertinoPageScaffold(
      navigationBar: const CupertinoNavigationBar(
        middle: Text('회원가입'), // 내비게이션 바 제목 추가
      ),
      child: SafeArea( // 화면 상단 바(노치 등)와의 겹침 방지
        child: SingleChildScrollView( // 키보드가 올라올 때 스크롤 가능하도록 추가
          padding: const EdgeInsets.all(16.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              const SizedBox(height: 50), // 상단 여백 추가
              CupertinoTextField(
                controller: idController,
                placeholder: '아이디', // 플레이스홀더 한글화
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
                      offset: const Offset(0, 0),
                      blurRadius: 8,
                      spreadRadius: 2,
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),
              CupertinoTextField(
                controller: pwController,
                placeholder: '비밀번호', // 플레이스홀더 한글화
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
                      offset: const Offset(0, 0),
                      blurRadius: 8,
                      spreadRadius: 2,
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),
              CupertinoTextField(
                controller: NameController,
                placeholder: '사용자 이름', // 플레이스홀더 한글화
                prefix: const Padding(
                  padding: EdgeInsets.symmetric(horizontal: 15),
                  child: Icon(CupertinoIcons.person_alt), // 더 적절한 아이콘
                ),
                padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 12),
                decoration: BoxDecoration(
                  color: CupertinoColors.white,
                  border: Border.all(color: CupertinoColors.systemGrey4),
                  borderRadius: BorderRadius.circular(20),
                  boxShadow: [
                    BoxShadow(
                      color: CupertinoColors.systemGrey.withOpacity(0.4),
                      offset: const Offset(0, 0),
                      blurRadius: 8,
                      spreadRadius: 2,
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),
              CupertinoTextField(
                controller: EmailController,
                placeholder: '이메일', // 플레이스홀더 한글화
                keyboardType: TextInputType.emailAddress, // 이메일 키보드 타입 설정
                prefix: const Padding(
                  padding: EdgeInsets.symmetric(horizontal: 15),
                  child: Icon(CupertinoIcons.envelope),
                ),
                padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 12),
                decoration: BoxDecoration(
                  color: CupertinoColors.white,
                  border: Border.all(color: CupertinoColors.systemGrey4),
                  borderRadius: BorderRadius.circular(20),
                  boxShadow: [
                    BoxShadow(
                      color: CupertinoColors.systemGrey.withOpacity(0.4),
                      offset: const Offset(0, 0),
                      blurRadius: 8,
                      spreadRadius: 2,
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),
              CupertinoTextField(
                controller: PhoneController,
                placeholder: '전화번호 (숫자만 입력)', // 플레이스홀더 한글화 및 가이드 추가
                keyboardType: TextInputType.phone, // 전화번호 키보드 타입 설정
                prefix: const Padding(
                  padding: EdgeInsets.symmetric(horizontal: 15),
                  child: Icon(CupertinoIcons.phone), // 전화 아이콘으로 변경
                ),
                padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 12),
                decoration: BoxDecoration(
                  color: CupertinoColors.white,
                  border: Border.all(color: CupertinoColors.systemGrey4),
                  borderRadius: BorderRadius.circular(20),
                  boxShadow: [
                    BoxShadow(
                      color: CupertinoColors.systemGrey.withOpacity(0.4),
                      offset: const Offset(0, 0),
                      blurRadius: 8,
                      spreadRadius: 2,
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),
              Row(
                  mainAxisAlignment: MainAxisAlignment.spaceAround,
                  children: <Widget>[
                    Expanded( // 버튼이 화면 너비에 맞춰 유연하게 확장되도록 Expanded 추가
                      child: CupertinoButton.filled(
                        onPressed: () {
                          setState(() {
                            _isSwitched = true; // '관리자' 버튼 클릭 시 ADMIN으로 설정
                          });
                        },
                        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
                        borderRadius: BorderRadius.circular(20),
                        // 선택된 상태에 따라 색상 변경
                        child: Text(
                          '관리자',
                          style: TextStyle(color: _isSwitched ? CupertinoColors.white : CupertinoColors.black),
                        ),
                        // 선택된 상태에 따라 배경색 변경
                        //color: _isSwitched ? CupertinoColors.systemBlue : CupertinoColors.systemGrey4,
                      ),
                    ),
                    const SizedBox(width: 10), // 버튼 간 간격
                    Expanded( // 버튼이 화면 너비에 맞춰 유연하게 확장되도록 Expanded 추가
                      child: CupertinoButton.filled(
                        onPressed: () {
                          setState(() {
                            _isSwitched = false; // '유저' 버튼 클릭 시 USER로 설정
                          });
                        },
                        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
                        borderRadius: BorderRadius.circular(20),
                        // 선택된 상태에 따라 색상 변경
                        child: Text(
                          '유저',
                          style: TextStyle(color: !_isSwitched ? CupertinoColors.white : CupertinoColors.black),
                        ),
                        // 선택된 상태에 따라 배경색 변경
                        //color: !_isSwitched ? CupertinoColors.systemBlue : CupertinoColors.systemGrey4,
                      ),
                    ),
                  ]
              ),
              const SizedBox(height: 40),
              CupertinoButton.filled(
                onPressed: register,
                padding: const EdgeInsets.symmetric(horizontal: 100, vertical: 15),
                borderRadius: BorderRadius.circular(20),
                child: const Text('회원가입'), // 버튼 텍스트 변경
              ),
            ],
          ),
        ),
      ),
    );
  }
}