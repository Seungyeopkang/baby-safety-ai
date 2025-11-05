package com.project.kidsvaguard.global.fcm;

/**
 * FCM 메시지 발송 기능을 정의하는 인터페이스
 */
public interface FcmService {

    /**
     * 특정 FCM 등록 토큰을 가진 사용자(기기)에게 알림 메시지를 보냅니다.
     *
     * @param targetToken 메시지를 수신할 기기의 FCM 등록 토큰
     * @param title       알림의 제목
     * @param body        알림의 내용 (본문)
     */
    void sendMessageTo(String targetToken, String title, String body);

    // 필요에 따라 다른 FCM 관련 기능 메소드를 추가할 수 있습니다.
    // 예: 특정 주제(topic) 구독자 전체에게 메시지 보내기 등
    // void sendMessageToTopic(String topic, String title, String body);
}