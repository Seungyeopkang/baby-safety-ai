package com.project.kidsvaguard.domain.fell_detection.service;

import com.project.kidsvaguard.domain.alarm.entity.Alarm;
import com.project.kidsvaguard.domain.alarm.repository.AlarmRepository;
import com.project.kidsvaguard.domain.fell_detection.dto.FallCryRequestDto; // JSON용 DTO
import com.project.kidsvaguard.domain.fell_detection.dto.FallCryFileRequestDto; // 파일용 DTO
import com.project.kidsvaguard.domain.fell_detection.entity.FellDetection;
import com.project.kidsvaguard.domain.fell_detection.repository.FellDetectionRepository;
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
public class FallAlarmHandlerService {

    private final UserRepository userRepository;
    private final AlarmRepository alarmRepository;
    private final FellDetectionRepository fellDetectionRepository;
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
     * JSON Body를 통해 넘어짐 감지 알림을 처리합니다. (/api/notify/fall_cry 엔드포인트용)
     */
    public void handleFallNotification(FallCryRequestDto dto) {
        log.info("Handling fall notification with JSON data: {}", dto.toString());
        // 공통 로직을 호출합니다. videoUrl은 FallCryRequestDto에 포함되어 있습니다.
        processFallDetection(dto.getUserId(), dto.getIsFell(), dto.getTitle(),
                dto.getContent(), dto.getTimeStr(), dto.getVideoUrl());

        log.info("🏁 Fall notification processing finished successfully for user: {}", dto.getUserId());
    }

    /**
     * MultipartFile을 통해 넘어짐 감지 알림을 처리합니다. (/api/notify/fall_cry_file 엔드포인트용)
     */
    public String handleFallNotificationWithFile(MultipartFile videoFile, FallCryFileRequestDto dto) throws IOException {
        log.info("Handling fall notification with file upload for DTO: {}", dto.toString());

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
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension; // UUID로 고유한 파일명 생성
        Path filePath = uploadPath.resolve(uniqueFileName);
        Files.copy(videoFile.getInputStream(), filePath); // NIO.2 사용

        String relativeFilePath = "/uploads/" + uniqueFileName; // 클라이언트에 반환할 상대 경로

        // 3. 공통 로직을 호출합니다. videoUrl 대신 저장된 파일 경로를 전달합니다.
        processFallDetection(dto.getUserId(), dto.getIsFell(), dto.getTitle(),
                dto.getContent(), dto.getTimeStr(), relativeFilePath);

        log.info("🏁 Fall notification processing finished successfully for user: {}", dto.getUserId());

        return relativeFilePath; // 저장된 파일 경로 반환
    }

    /**
     * 넘어짐 감지 알림 처리의 핵심 로직을 담당하는 공통 메서드.
     * videoPath는 URL이거나 로컬 파일 경로가 될 수 있습니다.
     */
    private void processFallDetection(String userId, Boolean isFell, String title,
                                      String content, String timeStr, String videoPath) {
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
                .alarmType(Alarm.AlarmType.FELL_DETECTION)
                .build();
        alarmRepository.save(alarm);
        log.info("✅ 알람 정보 저장 성공: {}", alarm.getAlarmId());

        // 4. 낙상 감지 정보 저장
        FellDetection fellDetection = FellDetection.builder()
                .isFell(isFell)
                .filePath(videoPath)  // DB에는 URL이거나 파일 경로가 저장
                .createdAt(timestamp)
                .alarm(alarm)
                .build();
        fellDetectionRepository.save(fellDetection);
        log.info("✅ 낙상 감지 정보 저장 성공 (비디오 경로 포함): {}", fellDetection.getFilePath());

        // 5. FCM 푸시 알림 발송
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
            log.info("   - Preparing to send FCM notification for fall detection to User ID: {}", user.getUserId());
            try {
                String notificationContent = content + " (User ID: " + user.getUserId() + ")";
                fcmService.sendMessageTo(fcmToken, title, notificationContent);
            } catch (Exception e) {
                log.error("🔥 Failed to send FCM notification for fall detection (User: {}), but DB operations were successful.", user.getUserId(), e);
            }
        } else {
            log.warn("⚠️ User ID {} does not have an FCM token. Skipping FCM notification for fall detection.", user.getUserId());
        }
    }

    // MultipartFile의 ContentType을 가져오는 유틸리티 메서드
    private String getVideoContentType(MultipartFile videoFile) {
        String contentType = videoFile.getContentType();
        return contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}