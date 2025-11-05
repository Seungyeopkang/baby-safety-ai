package com.project.kidsvaguard.global.jwtToken;

import com.project.kidsvaguard.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;

@Entity
@Table(name = "refresh_token", indexes = { // 인덱스 추가
        @Index(name = "idx_refresh_token", columnList = "refreshToken")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // unique = true 제거, 길이 명시 (JWT 길이는 가변적이므로 넉넉하게)
    @Column(nullable = false, length = 512)
    private String refreshToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_pk", nullable = false)
    private User user;

    @Column(nullable = false, updatable = false) // 생성 시에만 설정되도록 updatable=false 추가
    private Timestamp createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }
}