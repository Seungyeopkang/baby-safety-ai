import 'package:flutter/cupertino.dart';
import 'kidscafe.dart';

class settingPage extends StatefulWidget{
  const settingPage({Key?key}) : super(key: key);

  State<settingPage> createState() => settingPageState();
}

class settingPageState extends State<settingPage> {
  Widget build(BuildContext context) {
    return CupertinoPageScaffold(
      navigationBar: CupertinoNavigationBar(
        middle: Text('설정'),
        leading: CupertinoButton(
            child: Icon(CupertinoIcons.back),
            onPressed: () {
              Navigator.push(
                context,
                CupertinoPageRoute(
                builder: (context) => const mainPage(),
                )
              );
            },
        ),
      ),
      child: Text("설정"),
    );
  }
}