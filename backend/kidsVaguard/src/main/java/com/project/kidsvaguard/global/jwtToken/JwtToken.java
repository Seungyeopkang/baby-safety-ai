package com.project.kidsvaguard.global.jwtToken;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Builder
@Data
@AllArgsConstructor
public class JwtToken {
    private String grantType;
    private String accessToken;
    private String refreshToken;

    // ⚠️ Refresh Token 만료일시 필드 추가
    private Instant refreshTokenExpiryInstant; // Instant 타입 사용
}