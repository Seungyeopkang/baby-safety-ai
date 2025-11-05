import 'package:hive/hive.dart';
import 'package:hive_flutter/hive_flutter.dart';

void buffer() async{
  await Hive.initFlutter();
  await Hive.openBox('NotificationBuffer');
}

void NotificationPut(String title, String msg) {
   final box = Hive.box('NotificationBuffer');
   box.add({'title' : title, 'msg' : msg});
}

List<dynamic> NotificationGet() {
  final box = Hive.box('NotificationBuffer');
  List<dynamic> data = [];
  for (var item in box.values) {
    data.add(item);
  }
  return data;
}

void NotificationDelete(int index) {
  final box = Hive.box('NotificationBuffer');
  box.deleteAt(index);
}

void initializeBuffer(){
  final box = Hive.box('NotificationBuffer');
  box.clear();
}