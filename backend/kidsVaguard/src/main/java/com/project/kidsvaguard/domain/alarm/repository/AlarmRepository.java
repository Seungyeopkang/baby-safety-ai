package com.project.kidsvaguard.domain.alarm.repository;

import com.project.kidsvaguard.domain.alarm.entity.Alarm;
import com.project.kidsvaguard.domain.user.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;



@Repository
public interface AlarmRepository extends JpaRepository<Alarm, Long> {
    // 사용자별 + 알람 타입별 페이징 조회
    Page<Alarm> findByUserAndAlarmType(User user, Alarm.AlarmType alarmType, Pageable pageable);

    // ✅ 추가: 특정 사용자의 모든 알림 삭제 및 삭제는 트랜잭션이 필요합니다.
    @Transactional
    void deleteByUser(User user);
}