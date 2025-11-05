package com.project.kidsvaguard.domain.user.service;

import com.project.kidsvaguard.domain.user.dto.profileDto.ChangeNameRequestDto;
import com.project.kidsvaguard.domain.user.dto.profileDto.ChangePasswordRequestDto;
import com.project.kidsvaguard.domain.user.entity.User;
import com.project.kidsvaguard.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void changePassword(String userId, ChangePasswordRequestDto requestDto) {
        User existingUser = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당하는 회원을 찾을 수 없습니다."));

        if (!passwordEncoder.matches(requestDto.getCurrentPassword(), existingUser.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        // ✅ 기존 객체의 필드만 변경 (변경 감지 사용)
        existingUser.changePassword(passwordEncoder, requestDto.getNewPassword());

        log.info("사용자 {} 비밀번호 변경 완료.", userId);
    }

    @Transactional
    public void changeUsername(String userId, ChangeNameRequestDto requestDto) {
        User existingUser = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당하는 회원을 찾을 수 없습니다."));

        // ✅ 기존 객체의 필드만 변경 (변경 감지 사용)
        existingUser.changeUsername(requestDto.getNewUsername());

        log.info("사용자 {} 이름 변경 완료.", userId);
    }

}