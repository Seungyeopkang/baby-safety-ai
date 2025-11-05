package com.project.kidsvaguard.domain.user.dto;

import com.project.kidsvaguard.domain.user.entity.User;
import lombok.*;

@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDto {
    private String userId;
    private String username;
    private String phone;
    private String email;

    static public UserDto toDto(User user) {
        return UserDto.builder()
                .userId(user.getUserId())
                .username(user.getUserrealname())
                .phone(user.getPhone())
                .email(user.getEmail())
                .build();
    }
}
