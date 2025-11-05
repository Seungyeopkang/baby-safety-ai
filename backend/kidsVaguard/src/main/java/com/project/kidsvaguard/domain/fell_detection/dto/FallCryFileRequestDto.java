package com.project.kidsvaguard.domain.fell_detection.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// 파일 업로드 요청 (videoUrl 없음)
@Getter
@Setter
@NoArgsConstructor
@ToString
public class FallCryFileRequestDto {
    private Boolean isFell;
    private String title;
    private String content;
    private String userId;
    private String timeStr;
}