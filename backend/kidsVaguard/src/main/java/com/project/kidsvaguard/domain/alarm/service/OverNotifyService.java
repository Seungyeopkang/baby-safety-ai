package com.project.kidsvaguard.domain.alarm.service;

import com.project.kidsvaguard.domain.alarm.dto.OvercrowdNotificationRequestDto;
import com.project.kidsvaguard.domain.alarm.entity.Alarm;
import com.project.kidsvaguard.domain.alarm.repository.AlarmRepository;
import com.project.kidsvaguard.domain.user.entity.User;
import com.project.kidsvaguard.domain.user.repository.UserRepository;
import com.project.kidsvaguard.global.fcm.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OverNotifyService {

    private final UserRepository userRepository;
    private final AlarmRepository alarmRepository;
    private final FcmService fcmService;

    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads/";

    private static final List<String> ALLOWED_VIDEO_TYPES = Arrays.asList(
            MediaType.APPLICATION_OCTET_STREAM_VALUE,
            "video/mp4",
            "video/quicktime",
            "video/x-msvideo",
            "video/x-matroska"
    );

    /**
     * JSON Body를 통해 과밀 감지 알림을 처리합니다. (/api/alarms/overcrowd 엔드포인트용)
     */
    public void handleOvercrowdingNotification(OvercrowdNotificationRequestDto dto) {
        log.info("Handling overcrowding notification with JSON data: {}", dto.toString());
        // JSON 요청에서는 비디오 경로가 없으므로 null 전달
        processOvercrowding(dto.getUserId(), dto.getTitle(), dto.getContent(), dto.getTimeStr(), null);
        log.info("🏁 Overcrowding notification processing finished successfully for user: {}", dto.getUserId());
    }

    /**
     * MultipartFile을 통해 비디오 파일과 함께 과밀 감지 알림을 처리합니다. (/api/alarms/overcrowd_file 엔드포인트용)
     */
    public String handleOvercrowdingNotificationWithFile(MultipartFile videoFile, OvercrowdNotificationRequestDto dto) throws IOException {
        log.info("Handling overcrowding notification with file upload for DTO: {}", dto.toString());

        // 1. 파일 유효성 검사 (MIME 타입 기반)
        if (videoFile.isEmpty() || !ALLOWED_VIDEO_TYPES.contains(getVideoContentType(videoFile))) {
            throw new IllegalArgumentException("유효하지 않은 비디오 파일입니다. (비어있거나 지원하지 않는 형식)");
        }

        // 2. 파일 저장
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = videoFile.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
        Path filePath = uploadPath.resolve(uniqueFileName);
        Files.copy(videoFile.getInputStream(), filePath);

        String relativeFilePath = "/uploads/" + uniqueFileName;

        // 3. 공통 로직을 호출합니다. 저장된 파일 경로를 videoPath로 전달합니다.
        processOvercrowding(dto.getUserId(), dto.getTitle(), dto.getContent(), dto.getTimeStr(), relativeFilePath);

        log.info("🏁 Overcrowding notification (file) processing finished successfully for user: {}", dto.getUserId());

        return relativeFilePath; // 저장된 파일 경로 반환
    }

    /**
     * 과밀 감지 알림 처리의 핵심 로직을 담당하는 공통 메서드.
     * videoPath는 파일 업로드 시에만 사용되며, 그렇지 않은 경우 null입니다.
     */
    private void processOvercrowding(String userId, String title, String content, String timeStr, String videoPath) {
        // 1. 사용자 조회
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        // 2. 타임스탬프 처리
        Timestamp timestamp = convertToTimestamp(timeStr);

        // 3. 알람 저장
        Alarm alarm = Alarm.builder()
                .title(title)
                .content(content)
                .isRead(false)
                .createdAt(timestamp)
                .user(user)
                .alarmType(Alarm.AlarmType.OVERCROWDING)
                .videoPath(videoPath) // ✅ 추가: Alarm 엔티티에 videoPath 저장
                .build();
        alarmRepository.save(alarm);
        log.info("✅ 알람 정보 저장 성공: {}", alarm.getAlarmId());

        // 4. FCM 푸시 알림 발송
        sendFcmNotification(user, title, content);
    }

    // 시간 문자열을 Timestamp로 변환하는 유틸리티 메서드
    private Timestamp convertToTimestamp(String timeStr) {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        if (timeStr != null && !timeStr.isEmpty()) {
            try {
                timestamp = Timestamp.valueOf(timeStr.replace("T", " "));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid timestamp format received: {}. Using current time instead.", timeStr);
            }
        }
        return timestamp;
    }

    // FCM 알림을 발송하는 유틸리티 메서드
    private void sendFcmNotification(User user, String title, String content) {
        String fcmToken = user.getFcmToken();
        if (fcmToken != null && !fcmToken.isBlank()) {
            log.info("   - Preparing to send FCM notification for overcrowding to User ID: {}", user.getUserId());
            try {
                fcmService.sendMessageTo(fcmToken, title, content);
            } catch (Exception e) {
                log.error("🔥 Failed to send FCM notification for overcrowding (User: {}), but DB operations were successful.", user.getUserId(), e);
            }
        } else {
            log.warn("⚠️ User ID {} does not have an FCM token. Skipping FCM notification for overcrowding.", user.getUserId());
        }
    }

    // MultipartFile의 ContentType을 가져오는 유틸리티 메서드
    private String getVideoContentType(MultipartFile videoFile) {
        String contentType = videoFile.getContentType();
        return contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}