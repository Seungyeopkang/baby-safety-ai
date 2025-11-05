// lib/apiservice.dart
import 'dart:developer';

import 'package:http/http.dart' as http;
import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:http_parser/http_parser.dart';
import 'package:flutter/cupertino.dart'; // BuildContext 사용을 위해 남겨둠 (하지만 직접 다이얼로그 띄우지는 않음)


class ApiService {
  final String baseUrl;

  ApiService({required this.baseUrl});

  // JWT 토큰 관련 헬퍼 함수
  Future<String?> getJwtToken() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString('jwt_token');
  }

  Future<void> saveJwtToken(String token) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('jwt_token', token);
  }

  Future<void> deleteJwtToken() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('jwt_token');
  }

  Future<String?> getRefreshToken() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString('refresh_token');
  }

  Future<void> saveRefreshToken(String token) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('refresh_token', token);
  }

  Future<void> deleteRefreshToken() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('refresh_token');
  }

  Future<bool> isTokenValid(String token) async {
    // 💡 변경: baseUrl을 활용하도록 수정합니다.
    final response = await http.get(
      Uri.parse('$baseUrl/auth/validate'), // <--- 이렇게 변경하는 것이 가장 권장됩니다.
      headers: {
        'Authorization': 'Bearer $token',
      },
    );


    if (response.statusCode == 200) {
      return true;
    } else {
      // 토큰이 유효하지 않거나 만료되었을 때 (예: 401 Unauthorized 등)
      // 서버 응답 본문을 로깅하여 디버깅에 도움을 줄 수 있습니다.
      log('Token validation failed: ${response.statusCode}, ${response.body}');
      return false;
    }
  }

  /// POST /auth/token/refresh: Refresh Token으로 Access Token 재발급
  Future<String?> refreshToken() async {
    final String? refreshToken = await getRefreshToken();
    if (refreshToken == null) {
      log('Refresh Token이 없습니다. 재로그인이 필요합니다.');
      return null;
    }

    final url = Uri.parse('$baseUrl/auth/token/refresh');
    try {
      final response = await http.post(
        url,
        headers: {
          'Refresh-Token': 'Bearer $refreshToken',
        },
      );

      if (response.statusCode == 200) {
        final body = jsonDecode(response.body);
        String newAccessToken = body['accessToken'];
        String newRefreshToken = body['refreshToken'] ?? refreshToken;

        await saveJwtToken(newAccessToken);
        await saveRefreshToken(newRefreshToken);
        log('Access Token 재발급 성공: $newAccessToken');
        return newAccessToken;
      } else {
        log('Access Token 재발급 실패: ${response.statusCode}, ${response.body}');
        await deleteJwtToken();
        await deleteRefreshToken();
        return null;
      }
    } catch (e) {
      log('Access Token 재발급 중 오류 발생: $e');
      return null;
    }
  }

  /// JWT 토큰을 사용하여 API 요청을 보내고, 토큰 만료 시 자동으로 갱신 후 재시도합니다.
  /// 오류 발생 시 null을 반환하며, 401 Unauthorized일 경우 토큰 갱신을 시도합니다.
  Future<http.Response?> requestWithTokenRefresh(
      Future<http.Response> Function(String? token) requestBuilder,
      ) async {
    String? accessToken = await getJwtToken();

    http.Response response = await requestBuilder(accessToken);

    if (response.statusCode == 401 && accessToken != null) {
      log('Access Token 만료 감지, Refresh Token으로 갱신 시도...');
      String? newAccessToken = await refreshToken();

      if (newAccessToken != null) {
        log('Access Token 갱신 성공, 요청 재시도...');
        response = await requestBuilder(newAccessToken);
      } else {
        log('Access Token 갱신 실패. 사용자에게 재로그인 요청.');
        return null; // 토큰 갱신 실패 시 요청 실패로 간주하고 null 반환
      }
    }

    return response;
  }

  // --- 사용자 인증 및 관리 (UserController & AuthController) ---

  /// POST /users/sign-in: 로그인 및 JWT 토큰 발급
  /// 로그인 성공 시 JWT 토큰(accessToken, refreshToken)을 저장하고 accessToken을 반환합니다.
  Future<String?> signIn(String userId, String password) async {
    final url = Uri.parse('$baseUrl/users/sign-in');
    try {
      final response = await http.post(
        url,
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          "userId": userId,
          "password": password,
        }),
      );

      if (response.statusCode == 200) {
        final body = jsonDecode(response.body);
        String accessToken = body['accessToken'];
        String refreshToken = body['refreshToken'];

        await saveJwtToken(accessToken);
        await saveRefreshToken(refreshToken);

        log("로그인 성공, accessToken: $accessToken, refreshToken: $refreshToken");
        return accessToken;
      } else {
        log('로그인 실패: ${response.statusCode}, ${response.body}');
        return null;
      }
    } catch (e) {
      log('로그인 중 오류 발생: $e');
      return null;
    }
  }

  /// POST /users/sign-up: 회원가입
  Future<bool> signUp(String userId, String password, String username, String phone, String email, String role) async {
    final url = Uri.parse('$baseUrl/users/sign-up');
    try {
      final response = await http.post(
        url,
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'userId': userId,
          'password': password,
          'username': username,
          'phone': phone,
          'email': email,
          'role': role,
        }),
      );

      if (response.statusCode == 200 || response.statusCode == 201) {
        log('회원가입 성공');
        return true;
      } else {
        log('회원가입 실패: ${response.statusCode}, ${response.body}');
        return false;
      }
    } catch (e) {
      log('회원가입 중 오류 발생: $e');
      return false;
    }
  }

  /// PUT /users/fcm-token: 인증된 사용자의 FCM 토큰 갱신
  Future<bool> sendFCMToken(String fcmToken) async { // context 인자 제거
    final url = Uri.parse('$baseUrl/users/fcm-token');

    final response = await requestWithTokenRefresh(
          (token) => http.put(
        url,
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
        body: jsonEncode({
          "fcmToken": fcmToken,
        }),
      ),
    );

    if (response == null) return false;

    if (response.statusCode == 200) {
      log('FCM 토큰 전송 성공');
      return true;
    } else {
      log('FCM 토큰 전송 실패: ${response.statusCode}, ${response.body}');
      return false;
    }
  }

  /// POST /auth/logout: 로그아웃 (Refresh Token 무효화)
  Future<bool> logout() async { // context 인자 제거
    final url = Uri.parse('$baseUrl/auth/logout');

    final response = await requestWithTokenRefresh(
          (token) => http.post(
        url,
        headers: {
          'Authorization': 'Bearer $token',
        },
      ),
    );

    if (response == null) {
      return false;
    }

    if (response.statusCode == 200) {
      await deleteJwtToken();
      await deleteRefreshToken();
      return true;
    } else {
      return false;
    }
  }

  // --- 장소 관리 및 AI 분석 제어 (PlaceController) ---

  /// POST /api/place: 장소 등록 (CCTV 등 메타데이터 포함)
  Future<bool> createPlace({
    required String placeName,
    required String cctvAddress,
    String? userId,
    String? action,
  }) async { // context 인자 제거
    final url = Uri.parse('$baseUrl/api/place');

    final response = await requestWithTokenRefresh(
          (token) => http.post(
        url,
        headers: <String, String>{
          'Content-Type': 'application/json; charset=UTF-8',
          'Authorization': 'Bearer $token',
        },
        body: jsonEncode(<String, String>{
          'placeName': placeName,
          'cctvAddress': cctvAddress,
          if (userId != null) 'userId': userId,
          if (action != null) 'action': action,
        }),
      ),
    );

    if (response == null) return false;

    if (response.statusCode == 201) {
      log('장소 등록 성공: ${response.body}');
      return true;
    } else {
      log('장소 등록 실패: ${response.statusCode}, ${response.body}');
      return false;
    }
  }

  /// GET /api/place/{placeName}: 장소 이름으로 조회
  Future<Map<String, dynamic>?> getPlaceByName(String placeName) async { // context 인자 제거
    final url = Uri.parse('$baseUrl/api/place/$placeName');

    final response = await requestWithTokenRefresh(
          (token) => http.get(
        url,
        headers: {
          'Authorization': 'Bearer $token',
        },
      ),
    );

    if (response == null) return null;

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else if (response.statusCode == 404) {
      log('장소를 찾을 수 없습니다: $placeName');
      return null;
    } else {
      log('장소 조회 실패: ${response.statusCode}, ${response.body}');
      return null;
    }
  }

  /// POST /api/place/toggle-analysis: FastAPI로 실시간 AI 분석 ON/OFF 전송
  Future<bool> toggleAnalysis({
    required String action,
    String? userId,
    String? cctvAddress,
  }) async { // context 인자 제거
    final url = Uri.parse('$baseUrl/api/place/toggle-analysis');

    final response = await requestWithTokenRefresh(
          (token) => http.post(
        url,
        headers: <String, String>{
          'Content-Type': 'application/json; charset=UTF-8',
          'Authorization': 'Bearer $token',
        },
        body: jsonEncode(<String, String>{
          'action': action,
        }),
      ),
    );

    if (response == null) return false;

    if (response.statusCode == 200) {
      log('AI 분석 전송 성공: ${response.body}');
      return true;
    } else {
      log('AI 분석 전송 실패: ${response.statusCode}, ${response.body}');
      return false;
    }
  }

  /// DELETE /api/place/{placeId}: 특정 장소 삭제
  Future<bool> deletePlace(int placeId) async { // context 인자 제거
    final url = Uri.parse('$baseUrl/api/place/$placeId');

    final response = await requestWithTokenRefresh(
          (token) => http.delete(
        url,
        headers: {
          'Authorization': 'Bearer $token',
        },
      ),
    );

    if (response == null) return false;

    if (response.statusCode == 200) {
      log('장소가 삭제되었습니다.');
      return true;
    } else {
      log('장소 삭제 실패: ${response.statusCode}, ${response.body}');
      return false;
    }
  }

  // --- 알림 관련 (FallNotifyController, OverNotifyController, AlarmController) ---

  /// POST /api/notify/fall_cry: 낙상 및 울음 알림 수신 (URL 기반)
  // 이 엔드포인트는 토큰이 필요 없다고 가정하고 기존 방식 유지
  Future<bool> sendFallCryNotificationUrl({
    required String videoUrl,
    required bool isFell,
    required String title,
    required String content,
    required String userId,
    required String timeStr,
  }) async {
    final url = Uri.parse('$baseUrl/api/notify/fall_cry');
    try {
      final response = await http.post(
        url,
        headers: {'Content-Type': 'application/json; charset=UTF-8'},
        body: jsonEncode({
          "videoUrl": videoUrl,
          "isFell": isFell,
          "title": title,
          "content": content,
          "userId": userId,
          "timeStr": timeStr,
        }),
      );

      if (response.statusCode == 200) {
        log('낙상/울음 알림 전송 성공: ${response.body}');
        return true;
      } else {
        log('낙상/울음 알림 전송 실패: ${response.statusCode}, ${response.body}');
        return false;
      }
    } catch (e) {
      log('낙상/울음 알림 전송 중 오류 발생: $e');
      return false;
    }
  }

  /// POST /api/notify/fall_cry_file: 낙상 알림 (mp4 형식으로 영상 전송 및 저장)
  // 이 엔드포인트는 토큰이 필요 없다고 가정하고 기존 방식 유지 (Multipart 요청은 _requestWithTokenRefresh에 바로 적용하기 어려움)
  Future<bool> uploadFallCryFile({
    required String videoPath,
    required bool isFell,
    required String title,
    required String content,
    required String userId,
    required String timeStr,
    required BuildContext context, // 이 부분은 MultipartRequest 자체에 context가 직접 필요 없으므로, UI 알림을 위한다면 ApiService 외부에서 처리하도록 하는 것이 더 낫습니다.
  }) async {
    final url = Uri.parse('$baseUrl/api/notify/fall_cry_file');
    try {
      var request = http.MultipartRequest('POST', url);

      request.files.add(await http.MultipartFile.fromPath(
        'video',
        videoPath,
        contentType: MediaType('video', 'mp4'),
      ));

      request.fields['dto'] = jsonEncode({
        "isFell": isFell,
        "title": title,
        "content": content,
        "userId": userId,
        "timeStr": timeStr,
      });

      var response = await request.send();
      var responseBody = await response.stream.bytesToString();

      if (response.statusCode == 200) {
        log('낙상 알림 영상 업로드 성공: $responseBody');
        return true;
      } else {
        log('낙상 알림 영상 업로드 실패: ${response.statusCode}, $responseBody');
        return false;
      }
    } catch (e) {
      log('낙상 알림 영상 업로드 중 오류 발생: $e');
      return false;
    }
  }

  /// GET /api/fell-detection/user/{userId}: 특정 사용자의 낙상 기록 목록 조회
  Future<List<dynamic>?> getUserFellDetections(String userId) async { // context 인자 제거
    final url = Uri.parse('$baseUrl/api/fell-detection/user/$userId');

    final response = await requestWithTokenRefresh(
          (token) => http.get(
        url,
        headers: {
          'Authorization': 'Bearer $token',
        },
      ),
    );

    if (response == null) return null;

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      log('낙상 기록 조회 실패: ${response.statusCode}, ${response.body}');
      return null;
    }
  }

  /// GET /api/fell-detection/{fellId}: 낙상 기록 ID로 단건 조회
  Future<Map<String, dynamic>?> getFellDetectionById(int fellId) async { // context 인자 제거
    final url = Uri.parse('$baseUrl/api/fell-detection/$fellId');

    final response = await requestWithTokenRefresh(
          (token) => http.get(
        url,
        headers: {
          'Authorization': 'Bearer $token',
        },
      ),
    );

    if (response == null) return null;

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else if (response.statusCode == 404) {
      log('낙상 기록을 찾을 수 없습니다: $fellId');
      return null;
    } else {
      log('낙상 단일 기록 조회 실패: ${response.statusCode}, ${response.body}');
      return null;
    }
  }

  /// POST /api/alarms/overcrowd: 인원 초과 감지 알림 전송 (JSON 기반)
  // 이 엔드포인트는 토큰이 필요 없다고 가정하고 기존 방식 유지
  Future<bool> sendOvercrowdNotification({
    required String title,
    required String content,
    required String userId,
    required String timeStr,
  }) async {
    final url = Uri.parse('$baseUrl/api/alarms/overcrowd');
    try {
      final response = await http.post(
        url,
        headers: {'Content-Type': 'application/json; charset=UTF-8'},
        body: jsonEncode({
          "title": title,
          "content": content,
          "userId": userId,
          "timeStr": timeStr,
        }),
      );

      if (response.statusCode == 200) {
        log('인원 초과 알림 전송 성공: ${response.body}');
        return true;
      } else {
        log('인원 초과 알림 전송 실패: ${response.statusCode}, ${response.body}');
        return false;
      }
    } catch (e) {
      log('인원 초과 알림 전송 중 오류 발생: $e');
      return false;
    }
  }

  /// POST /api/alarms/overcrowd_file: 인원 초과 알림 (mp4 형식으로 영상 전송 및 저장)
  // 이 엔드포인트는 토큰이 필요 없다고 가정하고 기존 방식 유지 (Multipart 요청은 _requestWithTokenRefresh에 바로 적용하기 어려움)
  Future<bool> uploadOvercrowdFile({
    required String videoPath,
    required String title,
    required String content,
    required String userId,
    required String timeStr,
    required BuildContext context, // 이 부분은 MultipartRequest 자체에 context가 직접 필요 없으므로, UI 알림을 위한다면 ApiService 외부에서 처리하도록 하는 것이 더 낫습니다.
  }) async {
    final url = Uri.parse('$baseUrl/api/alarms/overcrowd_file');
    try {
      var request = http.MultipartRequest('POST', url);

      request.files.add(await http.MultipartFile.fromPath(
        'video',
        videoPath,
        contentType: MediaType('video', 'mp4'),
      ));

      request.fields['dto'] = jsonEncode({
        "title": title,
        "content": content,
        "userId": userId,
        "timeStr": timeStr,
      });

      var response = await request.send();
      var responseBody = await response.stream.bytesToString();

      if (response.statusCode == 200) {
        log('인원 초과 알림 영상 업로드 성공: $responseBody');
        return true;
      } else {
        log('인원 초과 알림 영상 업로드 실패: ${response.statusCode}, $responseBody');
        return false;
      }
    } catch (e) {
      log('인원 초과 알림 영상 업로드 중 오류 발생: $e');
      return false;
    }
  }

  /// GET /api/alarms/fell: 로그인 사용자의 낙상 알람 목록 (페이징)
  Future<Map<String, dynamic>?> getFellAlarms({
    int page = 0,
    int size = 20,
    String sort = 'createdAt,desc',
  }) async { // context 인자 제거
    final url = Uri.parse('$baseUrl/api/alarms/fell?page=$page&size=$size&sort=$sort');

    final response = await requestWithTokenRefresh(
          (token) => http.get(
        url,
        headers: {
          'Authorization': 'Bearer $token',
        },
      ),
    );

    if (response == null) return null;

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      log('낙상 알람 조회 실패: ${response.statusCode}, ${response.body}');
      return null;
    }
  }

  /// GET /api/alarms/overcrowding: 로그인 사용자의 인원 초과 알림 목록 (페이징)
  Future<Map<String, dynamic>?> getOvercrowdingAlarms({
    int page = 0,
    int size = 20,
    String sort = 'createdAt,desc',
  }) async { // context 인자 제거
    final url = Uri.parse('$baseUrl/api/alarms/overcrowding?page=$page&size=$size&sort=$sort');

    final response = await requestWithTokenRefresh(
          (token) => http.get(
        url,
        headers: {
          'Authorization': 'Bearer $token',
        },
      ),
    );

    if (response == null) return null;

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      log('인원 초과 알람 조회 실패: ${response.statusCode}, ${response.body}');
      return null;
    }
  }
}