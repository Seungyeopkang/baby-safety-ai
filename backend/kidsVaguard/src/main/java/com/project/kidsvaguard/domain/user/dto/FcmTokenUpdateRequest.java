package com.project.kidsvaguard.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter // Controller에서 값을 받을 때 필요할 수 있음
@NoArgsConstructor
@AllArgsConstructor
public class FcmTokenUpdateRequest {
    private String fcmToken; // 클라이언트로부터 받을 새로운 FCM 토큰
}