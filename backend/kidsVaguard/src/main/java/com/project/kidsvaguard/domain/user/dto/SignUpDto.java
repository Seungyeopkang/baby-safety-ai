package com.project.kidsvaguard.domain.user.dto;

import com.project.kidsvaguard.domain.user.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j
public class SignUpDto {



    @Pattern(regexp = "^[a-zA-Z0-9]{5,20}$", message = "아이디는 영문자와 숫자 조합으로 5~20자여야 합니다.")
    @NotBlank(message = "사용자 아이디는 필수입니다.")
    private String userId;

    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,20}$",
            message = "비밀번호는 영문자, 숫자, 특수문자를 포함한 8~20자여야 합니다.")
    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;

    @NotBlank(message = "유저이름은 필수입니다.")
    private String username;

    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "전화번호는 010-xxxx-xxxx 형식이어야 합니다.")
    private String phone;

    private String role; // "ADMIN", "USER", "STAFF" 등

    public User toEntity(String encodedPassword) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("비밀번호는 필수입니다.");
        }

        // roles 리스트가 없거나 비어있으면 기본 USER 역할 부여
        User.Role resolvedRole = (role != null && !role.isEmpty())
                ? User.Role.from(role)
                : User.Role.USER; // 기본값 USER
        log.info("Creating user entity with role: {}", resolvedRole);

        return User.builder()
                .userId(userId)
                .password(encodedPassword)
                .username(username)
                .email(email)
                .phone(phone)
                .role(resolvedRole)
                .build();
    }
}
