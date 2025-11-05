package com.project.kidsvaguard.domain.alarm.controller;

import com.project.kidsvaguard.domain.alarm.dto.AlarmDetailResponse;
import com.project.kidsvaguard.domain.alarm.service.AlarmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alarms") // 알람 관련 기본 경로 예시
@RequiredArgsConstructor
@Slf4j
public class AlarmController {

    private final AlarmService alarmService;

    @GetMapping("/fell")
    public ResponseEntity<?> getFellDetectionAlarms(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body("Authentication required.");
        }

        String currentUserId = userDetails.getUsername();
        Page<AlarmDetailResponse> fellAlarms = alarmService.getFellAlarmsByUser(currentUserId, pageable);
        return ResponseEntity.ok(fellAlarms);
    }

    @GetMapping("/overcrowding")
    public ResponseEntity<?> getOvercrowdingAlarms(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body("Authentication required.");
        }

        String currentUserId = userDetails.getUsername();
        Page<AlarmDetailResponse> overAlarms = alarmService.getOvercrowdingAlarmsByUser(currentUserId, pageable);
        return ResponseEntity.ok(overAlarms);
    }
    @DeleteMapping("/{alarmId}")
    public ResponseEntity<String> deleteAlarm(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long alarmId) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body("인증이 필요합니다.");
        }

        String currentUserId = userDetails.getUsername();

        try {
            alarmService.deleteAlarm(alarmId, currentUserId);
            return ResponseEntity.ok("알림이 성공적으로 삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage()); // 알림을 찾을 수 없을 때
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).body(e.getMessage()); // 권한이 없을 때
        } catch (Exception e) {
            log.error("알림 삭제 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("알림 삭제 중 오류가 발생했습니다.");
        }
    }
}