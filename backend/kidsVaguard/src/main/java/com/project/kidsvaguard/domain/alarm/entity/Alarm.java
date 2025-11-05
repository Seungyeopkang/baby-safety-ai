package com.project.kidsvaguard.domain.alarm.entity;

import com.project.kidsvaguard.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.sql.Timestamp;

@Entity
@Table(name = "alarm")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alarm {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long alarmId;

    @Column(nullable = false, length = 100)
    private String title;

    private String content;

    private Boolean isRead;

    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp createdAt;

    // ✅ 추가: 비디오 파일 경로 (모든 알람 타입에 대해)
    // 낙상 감지, 과밀 감지 등 비디오와 관련된 알람이라면 여기에 경로를 저장합니다.
    // 비디오가 없는 알람의 경우 이 필드는 null이 됩니다.
    private String videoPath;

    public enum AlarmType {
        FELL_DETECTION,
        OVERCROWDING
    }
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlarmType alarmType;

    @ManyToOne
    @JoinColumn(name = "user_pk")
    private User user;
}