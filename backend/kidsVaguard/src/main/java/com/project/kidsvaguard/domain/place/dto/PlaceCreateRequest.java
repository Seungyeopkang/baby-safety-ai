package com.project.kidsvaguard.domain.place.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.project.kidsvaguard.domain.place.entity.Place;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor; // 추가
import lombok.AllArgsConstructor; // 추가

// DTO 정의
@Getter
@Setter
@NoArgsConstructor // Lombok의 기본 생성자 추가
@AllArgsConstructor // Lombok의 모든 필드를 포함하는 생성자 추가
public class PlaceCreateRequest {
    private String placeName;
    private String cctvAddress;

    // 클라이언트에서 "START" 또는 "STOP" 문자열로 받을 경우 사용합니다.
    // @JsonFormat(shape = JsonFormat.Shape.STRING)은 기본적으로 Enum 필드에 적용되므로 명시적으로 필요하지 않을 수 있습니다.
    // 하지만 명시적으로 두어도 무방합니다.
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Place.Action action;
}