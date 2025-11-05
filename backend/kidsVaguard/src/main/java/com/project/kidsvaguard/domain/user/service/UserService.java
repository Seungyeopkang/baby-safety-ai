package com.project.kidsvaguard.domain.user.service;

import com.project.kidsvaguard.domain.user.dto.SignInDto;
import com.project.kidsvaguard.domain.user.dto.SignUpDto;
import com.project.kidsvaguard.domain.user.dto.UserDto;
import com.project.kidsvaguard.domain.user.entity.User;
import com.project.kidsvaguard.domain.user.repository.UserRepository;
import com.project.kidsvaguard.global.jwtToken.JwtToken;
import com.project.kidsvaguard.global.jwtToken.JwtTokenProvider;
import com.project.kidsvaguard.global.jwtToken.RefreshToken;
import com.project.kidsvaguard.global.jwtToken.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;


    @Transactional
    public JwtToken signIn(SignInDto signInDto) {
        String userId = signInDto.getUserId();
        String password = signInDto.getPassword();
        try {
            // 사용자 ID와 비밀번호로 Authentication Token 생성
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userId, password);

            // 실제 인증 수행 (비밀번호 검증 등)
            Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

            // 인증 정보를 기반으로 JWT 토큰 생성
            JwtToken jwtToken = jwtTokenProvider.generateToken(authentication);
            User user = (User) authentication.getPrincipal();

            // 1. 기존 Refresh Token 조회
            Optional<RefreshToken> existingTokenOpt = refreshTokenRepository.findByUser(user);

            // 2. 만약 존재하면 삭제
            existingTokenOpt.ifPresent(existingToken -> {
                log.debug("기존 Refresh Token 삭제 (User PK: {}): {}", user.getUserPk(), existingToken.getId());
                refreshTokenRepository.delete(existingToken);
            });

            // 3. 새 Refresh Token 저장
            log.debug("새 Refresh Token 저장 (User PK: {})", user.getUserPk());
            refreshTokenRepository.save(
                    RefreshToken.builder()
                            .refreshToken(jwtToken.getRefreshToken())
                            .user(user)
                            .build()
            );

            // 최종 생성된 JWT 토큰 반환
            return jwtToken;

        } catch (BadCredentialsException e) { // 인증 실패(비밀번호 틀림 등) 처리
            log.error("로그인 실패: userId: {}", userId);
            // e.printStackTrace(); // 디버깅 시 스택 트레이스 출력
            throw new BadCredentialsException("아이디 또는 비밀번호가 잘못되었습니다.", e); // 구체적인 예외 메시지 전달 권장
        } catch (Exception e) { // 기타 예외 처리
            log.error("로그인 중 예상치 못한 오류 발생: userId: {}", userId, e);
            throw new RuntimeException("로그인 처리 중 오류가 발생했습니다.", e);
        }
    }
    @Transactional
    public UserDto signUp(SignUpDto signUpDto) {
        if (userRepository.existsByUserId(signUpDto.getUserId())) {
            throw new IllegalArgumentException("이미 사용 중인 사용자 아이디입니다.");
        }
        if (userRepository.existsByEmail(signUpDto.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        // Password 암호화
        String encodedPassword = passwordEncoder.encode(signUpDto.getPassword());

        // 필요에 따라 추가적으로 다른 역할을 추가할 수 있음
        // 역할을 받아서 설정 (없으면 SignUpDto에서 기본값 처리)
        return UserDto.toDto(
                userRepository.save(
                        signUpDto.toEntity(encodedPassword)
                )
        );
    }

    @Transactional
    public void logout(String userId) {
        User user = userRepository.findByUserId(userId).orElseThrow(() ->
                new IllegalArgumentException("유저를 찾을 수 없습니다.")
        );
        refreshTokenRepository.deleteByUser(user); // DB에서 Refresh Token 삭제
    }

    /**
     * 사용자의 FCM 토큰을 업데이트합니다.
     * @param userId 업데이트할 사용자의 ID (UserDetails의 username과 매핑)
     * @param newFcmToken 새로운 FCM 등록 토큰
     */
    @Transactional // DB 업데이트이므로 트랜잭션 처리
    public void updateFcmToken(String userId, String newFcmToken) {
        // userId(UserDetails의 username)를 사용하여 사용자 조회
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.error("🚨 User not found with userId: {} for FCM token update.", userId);
                    // 보통 인증된 사용자이므로 이 예외가 발생하면 안됨. 발생 시 인증 시스템 문제 가능성.
                    return new RuntimeException("User not found during FCM token update.");
                });

        log.info("🔄 Updating FCM token for user: {}", userId);
        // User 엔티티의 fcmToken 필드 업데이트 (Setter 사용)
        user.setFcmToken(newFcmToken);

        // @Transactional 환경에서는 변경 감지(dirty checking)에 의해
        // 메소드 종료 시 자동으로 UPDATE 쿼리가 실행됩니다.
        // 명시적으로 save를 호출해도 문제는 없습니다. (호출 시 즉시 UPDATE 쿼리 실행)
        // userRepository.save(user);

        log.info("✅ FCM token updated successfully for user: {}", userId);
    }

    //UserId로 사용자 찾기. (place에서 사용)
    public Optional<User> findByUserId(String userId) {
        return userRepository.findByUserId(userId);
    }

    @Transactional //회원삭제
    public void signOutUser(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        // 해당 유저의 Refresh Token 모두 삭제 (로그아웃 처리)
        refreshTokenRepository.deleteByUser(user);

        // 유저 삭제
        userRepository.delete(user);

        log.info("회원탈퇴 처리 완료 - userId: {}", userId);
    }
}
