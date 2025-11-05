package com.project.kidsvaguard.domain.alarm.controller;

import com.project.kidsvaguard.domain.alarm.dto.OvercrowdNotificationRequestDto; // 새로운 DTO 임포트
import com.project.kidsvaguard.domain.alarm.service.OverNotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Slf4j 추가
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/alarms")
@RequiredArgsConstructor
@Slf4j // Lombok의 @Slf4j 사용
public class OverNotifyController {

    private final OverNotifyService overNotifyService;
    // UPLOAD_DIR은 서비스 계층으로 이동했으므로 컨트롤러에서 제거

    /**
     * JSON Body를 통해 과밀 감지 알림을 수신하는 엔드포인트
     */
    @PostMapping("/overcrowd")
    public ResponseEntity<Map<String, Object>> notifyOvercrowding(@RequestBody OvercrowdNotificationRequestDto payload) {
        log.info("Received /overcrowd request: {}", payload.toString());

        // 서비스 계층으로 모든 데이터 전달
        overNotifyService.handleOvercrowdingNotification(payload);

        Map<String, Object> response = Map.of(
                "status", "success",
                "message", "Overcrowd detection and received."
        );

        return ResponseEntity.ok(response);
    }

    /**
     * MultipartFile을 통해 비디오 파일과 함께 과밀 감지 알림을 수신하는 엔드포인트
     */
    @PostMapping(value = "/overcrowd_file", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, Object>> notifyOvercrowdingWithFile(
            @RequestPart("video") MultipartFile video,
            @RequestPart("dto") OvercrowdNotificationRequestDto dto // DTO로 묶어서 받기
    ) {
        log.info("Received /overcrowd_file request for DTO: {}", dto.toString());

        // 컨트롤러 레벨에서 기본적인 파일 유효성 검사 (선택 사항, 서비스에서 더 상세히 진행)
        if (video.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "fail",
                    "message", "비디오 파일이 없습니다."
            ));
        }

        try {
            // 서비스 계층으로 파일과 DTO 데이터 모두 전달
            String filePath = overNotifyService.handleOvercrowdingNotificationWithFile(video, dto);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Overcrowding 감지 (파일 업로드 방식) 수신 완료.",
                    "videoUrl", filePath // 저장된 파일 경로 반환
            ));
        } catch (IllegalArgumentException e) {
            // 서비스에서 던진 파일 유효성 검사 예외 처리
            log.warn("File validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "fail",
                    "message", e.getMessage()
            ));
        } catch (ResponseStatusException e) {
            // 서비스에서 던진 사용자 조회 실패 예외 처리
            log.error("User not found: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                    "status", "fail",
                    "message", e.getReason()
            ));
        } catch (IOException e) {
            // 파일 저장 중 발생한 I/O 예외 처리
            log.error("Error saving video file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "fail",
                    "message", "비디오 파일 저장 실패",
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            // 그 외 예상치 못한 모든 예외 처리
            log.error("An unexpected error occurred: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "fail",
                    "message", "서버 내부 오류 발생",
                    "error", e.getMessage()
            ));
        }
    }
}