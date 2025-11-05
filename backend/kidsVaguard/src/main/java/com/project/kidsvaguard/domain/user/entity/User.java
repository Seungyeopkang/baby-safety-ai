package com.project.kidsvaguard.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User implements UserDetails {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userPk;

    @Column(name = "user_id", nullable = false, length = 20)
    private String userId;

    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(name = "username", nullable = false, length = 15)
    private String username;

    @Column(nullable = false,length = 13)
    private String phone;

    @Column(name = "email", nullable = false,length = 30, unique = true)
    private String email;

    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp createdAt;

    private Boolean alarmSetting;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Override
    public String getUsername() {
        return this.userId; // ✅ Spring.sercurity 인증용 : UserDetail.getUsername
    }
    public String getUserrealname() {
        return username; // 실제 닉네임 등 표시용 이름
    }

    public enum Role {
        ADMIN, STAFF, USER;

        public static Role from(String value) {
            try {
                return Role.valueOf(value.toUpperCase());
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid role: " + value);
            }
        }
    }

    // ===== FCM 토큰 필드 추가 =====
    @Setter
    @Column(length = 255) // FCM 토큰은 길 수 있으므로 충분한 길이 확보
    private String fcmToken;
    // ===========================


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        // User.Role Enum 값을 사용하여 권한 부여
        if (role != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        }

        return authorities;
    }


    @Override
    public boolean isAccountNonExpired(){
        // 만료되었는지 확인하는 로직
        return true; // true -> 만료되지 않음
    }

    // 계정 잠금 여부 반환
    @Override
    public boolean isAccountNonLocked(){
        return true; // true -> 잠금되지 않음
    }

    // 패스워드 만료 여부 반환
    @Override
    public boolean isCredentialsNonExpired(){
        return true; // true -> 만료되지 않음
    }

    // 계정 사용 가능 여부 변환
    @Override
    public boolean isEnabled(){
        return true; // true -> 사용 가능
    }

    // 비밀번호 변경 메서드 (새로운 User 객체 반환)
    public void changePassword(PasswordEncoder passwordEncoder, String newPassword) {
        this.password = passwordEncoder.encode(newPassword);
    }

    // 이름 변경 메서드 (새로운 User 객체 반환)
    public void changeUsername(String newUsername) {
        this.username = newUsername;
    }

}
