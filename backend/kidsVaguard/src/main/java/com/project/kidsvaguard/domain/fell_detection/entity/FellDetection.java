package com.project.kidsvaguard.domain.fell_detection.entity;

import com.project.kidsvaguard.domain.alarm.entity.Alarm;
import jakarta.persistence.*;
import lombok.*;
import java.sql.Timestamp;


@Entity
@Table(name = "fell_detection")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FellDetection {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fellId;

    private Boolean isFell;

    private String filePath;  // ✅ Base64 대신 파일 경로 저장

    @Lob
    private String data;  // JSON 형태로 저장

    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp createdAt;

    @ManyToOne
    @JoinColumn(name = "alarm_id")
    private Alarm alarm;
}
