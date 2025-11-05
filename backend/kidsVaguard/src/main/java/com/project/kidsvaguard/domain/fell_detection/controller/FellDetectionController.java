package com.project.kidsvaguard.domain.fell_detection.controller;

import com.project.kidsvaguard.domain.fell_detection.dto.FellDetectionResponse;
import com.project.kidsvaguard.domain.fell_detection.entity.FellDetection;
import com.project.kidsvaguard.domain.fell_detection.service.FellDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
@RestController
@RequestMapping("/api/fell-detection")
@RequiredArgsConstructor
public class FellDetectionController {

    private final FellDetectionService fellDetectionService;

    // ✅ 사용자별 낙상 감지 기록 조회
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<FellDetectionResponse>> getFallDetectionsByUser(@PathVariable String userId) {
        List<FellDetection> detections = fellDetectionService.findByAlarmUserUserId(userId);

        List<FellDetectionResponse> response = detections.stream()
                .map(f -> new FellDetectionResponse(
                        f.getFellId(),
                        f.getIsFell(),
                        f.getFilePath(),
                        f.getData(),
                        f.getCreatedAt().toString(),
                        f.getAlarm() != null ? f.getAlarm().getAlarmId() : null,
                        f.getAlarm() != null ? f.getAlarm().getTitle() : null,
                        f.getAlarm() != null ? f.getAlarm().getContent() : null
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // ✅ 특정 낙상 ID 조회
    @GetMapping("/{fellId}")
    public ResponseEntity<FellDetectionResponse> getFallDetectionById(@PathVariable Long fellId) {
        FellDetection detection = fellDetectionService.findById(fellId)
                .orElseThrow(() -> new IllegalArgumentException("낙상 기록을 찾을 수 없습니다. ID: " + fellId));

        FellDetectionResponse response = new FellDetectionResponse(
                detection.getFellId(),
                detection.getIsFell(),
                detection.getFilePath(),
                detection.getData(),
                detection.getCreatedAt().toString(),
                detection.getAlarm() != null ? detection.getAlarm().getAlarmId() : null,
                detection.getAlarm() != null ? detection.getAlarm().getTitle() : null,
                detection.getAlarm() != null ? detection.getAlarm().getContent() : null
        );

        return ResponseEntity.ok(response);
    }

    // ✅ 개별 낙상 감지 기록 삭제 API 추가
    @DeleteMapping("/{fellId}")
    public ResponseEntity<String> deleteFellDetection(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long fellId) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body("인증이 필요합니다.");
        }

        String currentUserId = userDetails.getUsername();

        try {
            fellDetectionService.deleteFellDetection(fellId, currentUserId);
            return ResponseEntity.ok("낙상 감지 기록이 성공적으로 삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage()); // 기록을 찾을 수 없을 때
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).body(e.getMessage()); // 권한이 없을 때
        } catch (Exception e) {
            // 필요에 따라 로깅 추가
            return ResponseEntity.status(500).body("낙상 감지 기록 삭제 중 오류가 발생했습니다.");
        }
    }
}
