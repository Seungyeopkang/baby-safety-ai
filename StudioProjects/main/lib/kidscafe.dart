import 'package:flutter/cupertino.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'localVideo.dart';
import 'pushmsg.dart';
import 'buffer.dart';
import 'playVideo.dart';
import 'login.dart';
import 'apiservice.dart';
import 'main.dart';

class mainPage extends StatefulWidget {
  const mainPage({Key? key}) : super(key: key);

  @override
  State<mainPage> createState() => mainState();
}

class mainState extends State<mainPage> {
  bool isSwitched = false;
  bool isSound = true;

  final String _baseUrl = 'http://10.0.2.2:8080';

  late ApiService _apiService;

  final TextEditingController placeNameController = TextEditingController();
  final TextEditingController cctvAddressController = TextEditingController();

  @override
  void initState() {
    super.initState();
    _apiService = ApiService(baseUrl: _baseUrl);
    tabController.index = 0;

    initializeNotification();
    buffer();

    _loadSwitchState();
  }

  @override
  void dispose() {
    placeNameController.dispose();
    cctvAddressController.dispose();
    super.dispose();
  }

  // CupertinoAlertDialog로 메시지 띄우기 (mainState 내에서만 사용)
  void _showCupertinoDialog(String title, String message) { // title 인자 추가
    if (!mounted) return;
    showCupertinoDialog(
      context: context,
      builder: (context) => CupertinoAlertDialog(
        title: Text(title), // title 사용
        content: Text(message),
        actions: [
          CupertinoDialogAction(
            child: const Text('확인'),
            onPressed: () => Navigator.of(context).pop(),
          ),
        ],
      ),
    );
  }

  Future<void> _loadSwitchState() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      isSwitched = prefs.getBool('isSwitched') ?? false;
    });
  }

  Future<void> _saveSwitchState(bool value) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('isSwitched', value);
  }

  void _addPlace() async {
    String placeName = placeNameController.text.trim();
    String cctvAddress = cctvAddressController.text.trim();

    if (placeName.isEmpty || cctvAddress.isEmpty) {
      _showCupertinoDialog('입력 오류', '장소 이름과 CCTV 주소를 입력해주세요.');
      return;
    }

    final success = await _apiService.createPlace(
      placeName: placeName,
      cctvAddress: cctvAddress,
      // context 인자 삭제
    );

    if (success) {
      placeNameController.clear();
      cctvAddressController.clear();
      _showCupertinoDialog('성공', '장소 등록 성공!');
      // 추가적인 로직 (예: 장소 목록 갱신)
    } else {
      _showCupertinoDialog('등록 실패', '장소 등록에 실패했습니다.');
    }
  }

  @override
  Widget build(BuildContext context) {
    return CupertinoTabScaffold(

      controller: tabController,
      tabBar: CupertinoTabBar(
        items: const <BottomNavigationBarItem>[
          BottomNavigationBarItem(
            icon: Icon(CupertinoIcons.home),
          ),
          BottomNavigationBarItem(
            icon: Icon(CupertinoIcons.bell_fill),
          ),
          BottomNavigationBarItem(
            icon: Icon(CupertinoIcons.add_circled_solid),
          ),
          BottomNavigationBarItem(
            icon: Icon(CupertinoIcons.videocam_circle_fill),
          ),
        ],
      ),
      tabBuilder: (BuildContext context, int index) {
        switch (index) {
          case 0: // 홈 탭
            return CupertinoTabView(
              builder: (context) {
                return CupertinoPageScaffold(
                  backgroundColor: const Color.fromRGBO(76, 88, 229, 1.0),
                  navigationBar: CupertinoNavigationBar(
                    middle: Text(
                      isSwitched
                          ? '현재 AI키즈카페는 활성화 상태입니다'
                          : '현재 AI키즈카페는 비활성화 상태입니다',
                      style: const TextStyle(
                          color: CupertinoColors.white, fontSize: 20),
                    ),
                  ),
                  child: Padding(
                    padding: const EdgeInsets.all(16.0),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: <Widget>[
                        Image(
                          image: isSwitched
                              ? const AssetImage('assets/Logo.jpeg')
                              : const AssetImage('assets/Logo_off.png'),
                        ),
                        Center(
                          child: Transform.scale(
                            scale: 1.5,
                            child: CupertinoSwitch(
                              value: isSwitched,
                              onChanged: (value) async {
                                setState(() {
                                  isSwitched = value;
                                });

                                final success = await _apiService.toggleAnalysis(
                                  action: value ? 'START' : 'STOP',
                                );

                                if (success) {
                                  await _saveSwitchState(value);
                                  _showCupertinoDialog('성공', 'AI 분석 ${value ? '활성화' : '비활성화'} 성공!');
                                } else {
                                  setState(() {
                                    isSwitched = !value; // API 호출 실패 시 스위치 상태를 되돌림
                                  });
                                  _showCupertinoDialog('전송 실패', 'AI 분석 ${value ? '활성화' : '비활성화'} 실패.');
                                }
                              },
                            ),
                          ),
                        ),
                        const SizedBox(height: 60),
                        CupertinoButton(
                          child: const Text("로그아웃",
                              style: TextStyle(color: CupertinoColors.white)),
                          onPressed: () async {
                            final bool loggedOut = await _apiService.logout();
                            if (loggedOut) {
                              // Navigator.pushReplacement 전에 다이얼로그를 띄우지 않습니다.
                              // 로그인 페이지로 이동 후 로그인 페이지에서 필요하면 알림을 띄우는 것이 좋습니다.
                              Navigator.pushReplacement(
                                  context,
                                  CupertinoPageRoute(
                                      builder: (context) => const LoginPage()));
                              // 로그아웃 성공 알림은 로그인 페이지에서 로그인 성공/실패와 구분하여 띄울 수 있습니다.
                            } else {
                              _showCupertinoDialog('로그아웃 실패', '로그아웃 요청에 실패했습니다. 다시 시도해주세요.');
                            }
                          },
                        ),
                      ],
                    ),
                  ),
                );
              },
            );
        // 아래 나머지 탭들은 기존 코드와 동일하게 유지하세요
        // 알림 탭, 장소추가 탭, 영상 탭...
        // 필요하면 _showCupertinoDialog로 알림 띄우기 적용해주세요.
          case 1:
            return CupertinoTabView(
              builder: (context) {
                return CupertinoPageScaffold(
                  backgroundColor: const Color.fromRGBO(76, 88, 229, 1.0),
                  navigationBar: CupertinoNavigationBar(
                    trailing: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: <Widget>[
                        isSound
                            ? const Icon(CupertinoIcons.bell,
                            color: CupertinoColors.white)
                            : const Icon(CupertinoIcons.bell_slash,
                            color: CupertinoColors.white),
                        CupertinoSwitch(
                          value: isSound,
                          onChanged: (value) {
                            setState(() {
                              isSound = value;
                            });
                          },
                        ),
                      ],
                    ),
                    leading: CupertinoButton.tinted(
                      color: CupertinoColors.white,
                      onPressed: () {
                        initializeBuffer(); // 모든 알림 삭제 함수
                        setState(() {});
                      },
                      child:
                      const Icon(CupertinoIcons.delete, color: CupertinoColors.white),
                    ),
                  ),
                  child: Padding(
                    padding: const EdgeInsets.all(5.0),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: <Widget>[
                        const SizedBox(
                          height: 20,
                        ),
                        Expanded(
                          child: ListView.builder(
                            itemCount: NotificationGet().length,
                            itemBuilder: (context, index) {
                              return CupertinoListTile(
                                title: Text(NotificationGet()[index]['title'],
                                    style: const TextStyle(color: CupertinoColors.white)),
                                subtitle: Text(NotificationGet()[index]['msg'],
                                    style: const TextStyle(color: CupertinoColors.white)),
                                trailing: CupertinoButton(
                                  child: const Icon(CupertinoIcons.xmark,
                                      color: CupertinoColors.white),
                                  onPressed: () {
                                    NotificationDelete(index);
                                    setState(() {});
                                  },
                                ),
                              );
                            },
                          ),
                        )
                      ],
                    ),
                  ),
                );
              },
            );
          case 2:
            return CupertinoTabView(
              builder: (context) {
                return CupertinoPageScaffold(
                    navigationBar: const CupertinoNavigationBar(
                      middle: Text('장소추가'),
                    ),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: <Widget>[
                        const SizedBox(height: 16),
                        CupertinoTextField(
                          controller: placeNameController,
                          placeholder: '장소 이름',
                          prefix: const Padding(
                            padding: EdgeInsets.symmetric(horizontal: 15),
                            child: Icon(CupertinoIcons.pencil),
                          ),
                          padding: const EdgeInsets.symmetric(
                              horizontal: 5, vertical: 12),
                          decoration: BoxDecoration(
                            color: CupertinoColors.white,
                            border: Border.all(color: CupertinoColors.systemGrey4),
                            borderRadius: BorderRadius.circular(20),
                            boxShadow: [
                              BoxShadow(
                                color:
                                CupertinoColors.systemGrey.withOpacity(0.4),
                                offset: const Offset(0, 0),
                                blurRadius: 8,
                                spreadRadius: 2,
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: 16),
                        CupertinoTextField(
                          controller: cctvAddressController,
                          placeholder: 'CCTV 주소 (URL)',
                          prefix: const Padding(
                            padding: EdgeInsets.symmetric(horizontal: 15),
                            child: Icon(CupertinoIcons.link),
                          ),
                          padding: const EdgeInsets.symmetric(
                              horizontal: 5, vertical: 12),
                          decoration: BoxDecoration(
                            color: CupertinoColors.white,
                            border: Border.all(color: CupertinoColors.systemGrey4),
                            borderRadius: BorderRadius.circular(20),
                            boxShadow: [
                              BoxShadow(
                                color:
                                CupertinoColors.systemGrey.withOpacity(0.4),
                                offset: const Offset(0, 0),
                                blurRadius: 8,
                                spreadRadius: 2,
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: 40),
                        CupertinoButton.filled(
                          onPressed: _addPlace,
                          padding: const EdgeInsets.symmetric(
                              horizontal: 100, vertical: 15),
                          borderRadius: BorderRadius.circular(20),
                          child: const Icon(CupertinoIcons.rocket, size: 25.0),
                        ),
                      ],
                    ));
              },
            );
          case 3:
            return CupertinoTabView(
              builder: (context) {
                return CupertinoPageScaffold(
                    navigationBar: const CupertinoNavigationBar(
                      middle: Text('영상 보기'),
                    ),
                    child: SafeArea(
                        child: Center(
                          child: GoogleDriveVideoPlayer(
                              videoId: '117ZCYOVnIyHfbZd6jQACu0S8aIDdoTYa'
                          ),

                        )));
              },
            );
          default:
            return const Center(child: Text('탭이 없습니다.'));
        }
      },
    );
  }
}