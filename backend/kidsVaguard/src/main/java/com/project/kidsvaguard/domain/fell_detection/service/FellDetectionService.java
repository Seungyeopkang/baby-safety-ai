package com.project.kidsvaguard.domain.fell_detection.service;

import com.project.kidsvaguard.domain.fell_detection.entity.FellDetection;
import com.project.kidsvaguard.domain.fell_detection.repository.FellDetectionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FellDetectionService {

    private final FellDetectionRepository fellDetectionRepository;

    // ✅ 단일 조회
    public Optional<FellDetection> findById(Long fellId) {
        return fellDetectionRepository.findById(fellId);
    }

    // ✅ 사용자별 조회
    public List<FellDetection> findByAlarmUserUserId(String userId) {
        return fellDetectionRepository.findByAlarm_User_UserId(userId);
    }
    // ✅ 개별 낙상 감지 기록 삭제 로직 추가
    @Transactional
    public void deleteFellDetection(Long fellId, String userId) throws AccessDeniedException {
        FellDetection fellDetection = fellDetectionRepository.findById(fellId)
                .orElseThrow(() -> new IllegalArgumentException("해당 낙상 감지 기록을 찾을 수 없습니다."));

        // **중요 보안 로직**: 낙상 기록의 소유자 확인
        // FellDetection 엔티티가 직접 User를 참조하지 않고, Alarm을 통해 User에 연결되어 있습니다.
        if (fellDetection.getAlarm() == null || !fellDetection.getAlarm().getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("이 낙상 감지 기록을 삭제할 권한이 없습니다.");
        }

        // 낙상 감지 기록 삭제
        fellDetectionRepository.delete(fellDetection);
    }
}
