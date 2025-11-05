package com.project.kidsvaguard.global.fcm;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor // 생성자 주입 (필요시)
public class FcmServiceImpl implements FcmService {

    // FirebaseMessaging 인스턴스는 FirebaseApp 초기화 후 getInstance()로 얻어옴
    // 별도 주입 없이 getInstance() 사용 가능

    @Override
    public void sendMessageTo(String targetToken, String title, String body) {
        if (targetToken == null || targetToken.isBlank()) {
            log.warn("⚠️ FCM Target token is missing or empty. Cannot send notification.");
            return;
        }

        // 알림(Notification) 객체 생성
        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        // 메시지(Message) 객체 생성
        Message message = Message.builder()
                .setNotification(notification) // 사용자에게 보여질 알림 설정
                .setToken(targetToken)        // 특정 기기로 보낼 때 사용
                // .putData("alarmId", "123") // 앱이 백그라운드/포그라운드에서 추가 데이터 처리 필요 시 사용
                // .putData("type", "CRYING_ALERT")
                .build();

        try {
            // FirebaseMessaging 인스턴스를 통해 메시지 발송 요청
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("✅ Successfully sent FCM message to token [{}]: {}", maskToken(targetToken), response);
        } catch (FirebaseMessagingException e) {
            log.error("🔥 Failed to send FCM message to token [{}]: {}", maskToken(targetToken), e.getMessage());
            // 에러 코드에 따른 처리 가능 (예: UNREGISTERED 이면 DB에서 해당 토큰 삭제)
            if ("UNREGISTERED".equals(e.getErrorCode()) || "INVALID_ARGUMENT".equals(e.getErrorCode())) {
                log.warn("   -> FCM token seems invalid or unregistered. Consider removing it for the user.");
                // TODO: DB에서 해당 사용자의 fcmToken을 삭제하거나 비활성화하는 로직 추가 필요
            }
            // 다른 종류의 오류(네트워크 문제 등)는 재시도 로직을 고려해 볼 수 있음
        } catch (Exception e) {
            // Firebase 외 다른 예외 발생 가능성
             log.error("🔥 Unexpected error during FCM message sending to token [{}]", maskToken(targetToken), e);
        }
    }

    // 토큰 마스킹 함수 (이전과 동일)
    private String maskToken(String token) {
        if (token == null || token.length() < 10) {
            return token;
        }
        return token.substring(0, 5) + "..." + token.substring(token.length() - 5);
    }
}