import 'package:flutter/cupertino.dart';


class FindidPage extends StatefulWidget{
  const FindidPage({Key? key}) : super(key: key);

  CupertinoFindidPageState createState() => CupertinoFindidPageState();
}

class CupertinoFindidPageState extends State<FindidPage> {
  final TextEditingController EmailController = TextEditingController();

  void register() async {}

  Widget build(BuildContext context) {
    return CupertinoPageScaffold(
      navigationBar: const CupertinoNavigationBar(
        middle: Text(''),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            CupertinoTextField(
              controller: EmailController,
              placeholder: 'E-Mail',
              prefix: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 15),
                child: const Icon(CupertinoIcons.envelope),
              ),
              padding: EdgeInsets.symmetric(horizontal: 5, vertical: 12),
              decoration: BoxDecoration(
                color: CupertinoColors.white,
                border: Border.all(color: CupertinoColors.systemGrey4),
                borderRadius: BorderRadius.circular(20),
                boxShadow: [
                  BoxShadow(
                    color: CupertinoColors.systemGrey.withOpacity(0.4),
                    offset: Offset(0, 0),     // 수직 그림자 위치
                    blurRadius: 8,            // 그림자 번짐 정도
                    spreadRadius: 2,          // 그림자 퍼짐 정도
                  ),
                ],
              ),
            ),
            const SizedBox(height: 40),
            CupertinoButton.filled( //돌출된 느낌의 버튼 기능
              onPressed: register,
              padding: EdgeInsets.symmetric(horizontal: 100, vertical: 15),
              borderRadius: BorderRadius.circular(20),
              child: const Text('Enter'),
            ),
          ],
        ),
      ),

    );
  }
}