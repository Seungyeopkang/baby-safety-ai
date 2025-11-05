package com.project.kidsvaguard.domain.fell_detection.controller;

import com.project.kidsvaguard.domain.fell_detection.dto.FallCryRequestDto; // JSON용 DTO
import com.project.kidsvaguard.domain.fell_detection.dto.FallCryFileRequestDto; // 파일용 DTO
import com.project.kidsvaguard.domain.fell_detection.service.FallAlarmHandlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/notify")
@RequiredArgsConstructor
@Slf4j
public class FallNotifyController {

    private final FallAlarmHandlerService fallAlarmHandlerService;

    /**
     * JSON Body를 통해 넘어짐 감지 알림을 수신하는 엔드포인트
     */
    @PostMapping("/fall_cry")
    public ResponseEntity<Map<String, Object>> notifyFall(@RequestBody FallCryRequestDto payload) {
        log.info("Received /fall_cry request: {}", payload.toString());

        // 서비스 계층으로 모든 데이터 전달
        fallAlarmHandlerService.handleFallNotification(payload);

        Map<String, Object> response = Map.of(
                "status", "success",
                "message", "Fall_Crying detection and received.",
                "videoUrl", payload.getVideoUrl()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * MultipartFile을 통해 비디오 파일과 함께 넘어짐 감지 알림을 수신하는 엔드포인트
     */
    @PostMapping(value = "/fall_cry_file", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, Object>> notifyFallWithFile(
            @RequestPart("videoFile") MultipartFile videoFile,
            @RequestPart("dto") FallCryFileRequestDto dto // FallCryFileRequestDto 사용
    ) {
        log.info("Received /fall_cry_file request for DTO: {}", dto.toString());

        // 컨트롤러 레벨에서 기본적인 파일 유효성 검사 (선택 사항, 서비스에서 더 상세히 진행)
        if (videoFile.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "fail",
                    "message", "비디오 파일이 없습니다."
            ));
        }

        try {
            // 서비스 계층으로 파일과 DTO 데이터 모두 전달
            String filePath = fallAlarmHandlerService.handleFallNotificationWithFile(videoFile, dto);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Fall_Crying 감지 (파일 업로드 방식) 수신 완료.",
                    "videoUrl", filePath
            ));
        } catch (IllegalArgumentException e) {
            log.warn("File validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "fail",
                    "message", e.getMessage()
            ));
        } catch (ResponseStatusException e) {
            log.error("User not found or other service error: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                    "status", "fail",
                    "message", e.getReason()
            ));
        } catch (IOException e) {
            log.error("Error saving video file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "fail",
                    "message", "비디오 파일 저장 실패",
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("An unexpected error occurred: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "fail",
                    "message", "서버 내부 오류 발생",
                    "error", e.getMessage()
            ));
        }
    }
}