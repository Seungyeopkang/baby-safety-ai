package com.project.kidsvaguard.domain.alarm.service;

import com.project.kidsvaguard.domain.alarm.dto.AlarmDetailResponse;
import com.project.kidsvaguard.domain.alarm.entity.Alarm;
import com.project.kidsvaguard.domain.alarm.entity.Alarm.AlarmType;
import com.project.kidsvaguard.domain.alarm.repository.AlarmRepository;
import com.project.kidsvaguard.domain.fell_detection.entity.FellDetection;
import com.project.kidsvaguard.domain.fell_detection.repository.FellDetectionRepository;
import com.project.kidsvaguard.domain.user.entity.User;
import com.project.kidsvaguard.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlarmService {

    private final AlarmRepository alarmRepository;
    private final FellDetectionRepository fellDetectionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<AlarmDetailResponse> getFellAlarmsByUser(String userId, Pageable pageable) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return alarmRepository.findByUserAndAlarmType(user, AlarmType.FELL_DETECTION, pageable)
                .map(alarm -> {
                    FellDetection fellDetection = fellDetectionRepository.findByAlarm_AlarmId(alarm.getAlarmId())
                            .orElse(null);
                    // AlarmDetailResponse.from(alarm, fellDetection)이 이제 Alarm의 videoPath도 고려합니다.
                    return AlarmDetailResponse.from(alarm, fellDetection);
                });
    }

    @Transactional(readOnly = true)
    public Page<AlarmDetailResponse> getOvercrowdingAlarmsByUser(String userId, Pageable pageable) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return alarmRepository.findByUserAndAlarmType(user, AlarmType.OVERCROWDING, pageable)
                .map(AlarmDetailResponse::from); // AlarmDetailResponse.from(Alarm alarm)이 Alarm의 videoPath를 사용합니다.
    }

    @Transactional
    public void deleteAlarm(Long alarmId, String userId) {
        Alarm alarm = alarmRepository.findById(alarmId)
                .orElseThrow(() -> new IllegalArgumentException("해당 알림을 찾을 수 없습니다."));

        // 알림의 소유자 확인 (중요 보안 로직)
        if (!alarm.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("이 알림을 삭제할 권한이 없습니다.");
        }

        // 관련 FellDetection 데이터도 삭제할지 여부 결정 (비즈니스 로직에 따라)
        // 예를 들어, Alarm이 삭제되면 해당 FellDetection도 삭제해야 한다면:
        // fellDetectionRepository.findByAlarm_AlarmId(alarmId).ifPresent(fellDetectionRepository::delete);

        alarmRepository.delete(alarm);
    }
}