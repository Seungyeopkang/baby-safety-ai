package com.project.kidsvaguard.domain.user.controller;

import com.project.kidsvaguard.domain.user.dto.FcmTokenUpdateRequest;
import com.project.kidsvaguard.domain.user.dto.SignInDto;
import com.project.kidsvaguard.domain.user.dto.SignUpDto;
import com.project.kidsvaguard.domain.user.dto.UserDto;
import com.project.kidsvaguard.domain.user.service.UserService;
import com.project.kidsvaguard.global.jwtToken.JwtToken;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @PostMapping("/sign-in")
    public JwtToken signIn(@RequestBody SignInDto signInDto) {
        JwtToken jwtToken = userService.signIn(signInDto); // SignInDto를 직접 전달
        log.info("request username = {}", signInDto.getUserId());
        log.info("jwtToken accessToken = {}, refreshToken = {}", jwtToken.getAccessToken(),jwtToken.getRefreshToken());
        return jwtToken;
    }

    @PostMapping("/sign-up")
    public ResponseEntity<?> signUp(@RequestBody @Valid SignUpDto signUpDto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().get(0).getDefaultMessage();
            return ResponseEntity.badRequest().body(errorMessage);
        }

        UserDto userDto = userService.signUp(signUpDto);
        return ResponseEntity.ok(userDto);
    }

    /**
     * 현재 인증된 사용자의 FCM 등록 토큰을 업데이트합니다.
     *
     * @param userDetails 현재 인증된 사용자 정보 (Spring Security가 주입)
     * @param request     새로운 FCM 토큰을 포함하는 요청 DTO
     * @return 성공 시 200 OK, 실패 시 에러 응답
     */
    @PutMapping("/fcm-token") // 엔드포인트 경로: /users/fcm-token
    @PreAuthorize("isAuthenticated()")
    // @PatchMapping("/me/fcm-token") // PATCH 사용도 가능
    public ResponseEntity<String> updateUserFcmToken(
            @AuthenticationPrincipal UserDetails userDetails, // 현재 사용자 정보 주입
            @RequestBody FcmTokenUpdateRequest request) {

        // 1. 인증된 사용자인지 확인 (userDetails가 null이면 Spring Security 설정 문제)
        if (userDetails == null) {
            log.warn("🚨 Unauthorized attempt to update FCM token.");
            // Spring Security에서 @Secured 나 pre/post 어노테이션으로 처리하는 것이 더 좋음
            return ResponseEntity.status(401).body("Authentication required.");
        }

        // 2. 요청 본문 및 토큰 값 유효성 검사
        if (request == null || request.getFcmToken() == null || request.getFcmToken().isBlank()) {
            log.warn("🚨 FCM token update request is invalid (token missing or empty) for user: {}", userDetails.getUsername());
            return ResponseEntity.badRequest().body("FCM token is required.");
        }

        try {
            // UserDetails 에서 사용자 식별자(여기서는 userId) 가져오기
            String userId = userDetails.getUsername();
            log.info("➡️ Received FCM token update request for user: {}", userId);

            // UserService 호출하여 토큰 업데이트
            userService.updateFcmToken(userId, request.getFcmToken());

            log.info("✅ FCM token updated successfully for user: {}", userId);
            return ResponseEntity.ok("FCM token updated successfully.");

        } catch (RuntimeException e) {
            // UserService에서 사용자를 못 찾는 경우 등 (정상적으론 발생하기 어려움)
            log.error("🔥 Error updating FCM token for user: {}", userDetails.getUsername(), e);
            log.warn("🚨 Unauthorized attempt to update FCM token.");
            return ResponseEntity.internalServerError().body("Error updating FCM token: " + e.getMessage());
        } catch (Exception e) {
            // 기타 예상치 못한 오류
            log.error("🔥 Unexpected error during FCM token update for user: {}", userDetails.getUsername(), e);
            log.warn("🚨  FCM token이 예상치 못하게 없습니다.");
            return ResponseEntity.internalServerError().body("An unexpected error occurred.");
        }

    }



}