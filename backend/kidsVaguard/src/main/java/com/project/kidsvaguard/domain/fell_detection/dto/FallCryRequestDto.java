package com.project.kidsvaguard.domain.fell_detection.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// JSON 본문 요청 (videoUrl 포함)
@Getter
@Setter
@NoArgsConstructor
@ToString
public class FallCryRequestDto {
    private String videoUrl; // URL 기반 알림에서 사용
    private Boolean isFell;
    private String title;
    private String content;
    private String userId;
    private String timeStr;
}