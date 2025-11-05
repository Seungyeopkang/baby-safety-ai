package com.project.kidsvaguard.domain.fell_detection.repository;

import com.project.kidsvaguard.domain.fell_detection.entity.FellDetection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FellDetectionRepository extends JpaRepository<FellDetection, Long> {
    // ✅ Alarm의 ID로 FellDetection 찾기
    Optional<FellDetection> findByAlarm_AlarmId(Long alarmId);

    // ✅ 수정: User 엔티티의 userId 경로로 접근
    List<FellDetection> findByAlarm_User_UserId(String userId);
}
