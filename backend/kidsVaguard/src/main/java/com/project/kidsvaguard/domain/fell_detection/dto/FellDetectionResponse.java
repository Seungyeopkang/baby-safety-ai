package com.project.kidsvaguard.domain.fell_detection.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FellDetectionResponse {
    private Long fellId;
    private Boolean isFell;
    private String filePath;
    private String data;
    private String createdAt;
    private Long alarmId;
    private String alarmTitle;
    private String alarmContent;
}
