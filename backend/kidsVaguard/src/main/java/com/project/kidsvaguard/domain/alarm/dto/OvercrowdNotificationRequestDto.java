package com.project.kidsvaguard.domain.alarm.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// 넘어짐 감지 및 과밀 알림 요청에 사용될 공통 DTO
// title, content, userId, timeStr 필드를 포함합니다.
// videoUrl 필드는 이 DTO에서 제거합니다.
// JSON 요청이든 파일 업로드 요청이든 이 DTO를 사용하여 공통 파라미터를 받습니다.
@Getter
@Setter
@NoArgsConstructor
@ToString
public class OvercrowdNotificationRequestDto {
    private String title;
    private String content;
    private String userId;
    private String timeStr;
}