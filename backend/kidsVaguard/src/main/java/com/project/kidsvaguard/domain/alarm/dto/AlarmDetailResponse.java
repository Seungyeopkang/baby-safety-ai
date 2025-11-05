package com.project.kidsvaguard.domain.alarm.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.project.kidsvaguard.domain.alarm.entity.Alarm;
import com.project.kidsvaguard.domain.fell_detection.entity.FellDetection;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AlarmDetailResponse {

    private Long alarmId;
    private String title;
    private String content;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "Asia/Seoul")
    private Timestamp createdAt;
    private boolean isRead;
    private String userId;

    // --- 연관된 낙상 감지 정보 (FELL_DETECTION 알람에만 해당) ---
    private Long fellDetectionId;
    private Boolean isFell;
    // ✅ 수정: 이 videoPath는 이제 Alarm 엔티티의 videoPath를 우선적으로 사용하고,
    // FellDetection이 있는 경우 FellDetection의 videoPath를 사용하도록 합니다.
    private String videoPath;

    // 정적 팩토리 메소드 (Service에서 DTO 생성 시 사용)
    public static AlarmDetailResponse from(Alarm alarm, FellDetection fellDetection) {
        String path = null;
        if (alarm.getVideoPath() != null) { // ✅ 추가: Alarm 엔티티의 videoPath 우선 사용
            path = alarm.getVideoPath();
        } else if (fellDetection != null) { // 기존 FellDetection의 videoPath 사용 (낙상 감지일 경우)
            path = fellDetection.getFilePath();
        }

        return new AlarmDetailResponse(
                alarm.getAlarmId(),
                alarm.getTitle(),
                alarm.getContent(),
                alarm.getCreatedAt(),
                alarm.getIsRead(),
                alarm.getUser().getUserId(),
                fellDetection != null ? fellDetection.getFellId() : null,
                fellDetection != null ? fellDetection.getIsFell() : null,
                path // ✅ 수정된 videoPath 적용
        );
    }

    // Alarm만 받는 생성자도 Alarm 엔티티의 videoPath를 활용하도록 수정
    public static AlarmDetailResponse from(Alarm alarm) {
        return new AlarmDetailResponse(
                alarm.getAlarmId(),
                alarm.getTitle(),
                alarm.getContent(),
                alarm.getCreatedAt(),
                alarm.getIsRead(),
                alarm.getUser().getUserId(),
                null,
                null,
                alarm.getVideoPath() // ✅ 추가: Alarm 엔티티의 videoPath를 직접 사용
        );
    }
}