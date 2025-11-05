package com.project.kidsvaguard.domain.user.controller;

import com.project.kidsvaguard.domain.user.service.UserService;
import com.project.kidsvaguard.global.jwtToken.JwtToken;
import com.project.kidsvaguard.global.jwtToken.JwtTokenProvider;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders; // HttpHeaders 임포트
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j // 로깅 사용
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    @PostMapping("/token/refresh")
    public ResponseEntity<?> refreshAccessToken(@RequestHeader("Refresh-Token") String refreshToken) {
        log.info("Refresh Token으로 Access Token 재발급 시도: {}", refreshToken); // 로깅 추가
        if (refreshToken == null || refreshToken.isEmpty()) {
            log.warn("Refresh Token 헤더가 비어있습니다.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Refresh Token 헤더가 없거나 비어있습니다.");
        }
        try {
            // 1. DB 검증 및 새 Access Token 발급 (Provider의 핵심 로직)
            JwtToken newJwtToken = jwtTokenProvider.refreshAccessToken(refreshToken);
            log.info("토큰 재발급 성공 (Refresh Token 연관 사용자)");

            // 2. 성공 시 새 토큰 정보 반환 (Access Token + 기존 Refresh Token)
            return ResponseEntity.ok(newJwtToken);

        } catch (JwtException e) {
            // validateToken 실패 또는 refreshAccessToken 내부 오류 (DB 조회 실패, 사용자 없음 등)
            log.error("토큰 재발급 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh Token이 만료되었거나 유효하지 않습니다: " + e.getMessage());
        } catch (Exception e) {
            // 예상치 못한 다른 오류 처리
            log.error("토큰 재발급 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("토큰 재발급 중 오류가 발생했습니다.");
        }
    }
    @GetMapping("/validate")
    public ResponseEntity<?> validateAccessToken(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Authorization 헤더가 없거나 형식이 잘못되었습니다.");
        }

        String token = authHeader.substring(7); // "Bearer " 제거

        try {
            jwtTokenProvider.validateToken(token); // 유효성만 확인, 실패하면 예외
            return ResponseEntity.ok().body("토큰이 유효합니다.");
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("토큰이 만료되었거나 유효하지 않습니다: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) { // HttpHeaders 상수 사용
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.warn("로그아웃 요청 헤더 오류: Authorization 헤더가 없거나 'Bearer ' 타입이 아님");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Authorization 헤더가 없거나 형식이 잘못되었습니다.");
        }
        try {
            String accessToken = authorizationHeader.substring(7); // "Bearer " 접두사 제거
            String userId = jwtTokenProvider.getUserIdFromToken(accessToken); // Access Token에서 userId 추출

            userService.logout(userId); // UserService의 logout 호출 (DB에서 해당 유저의 모든 Refresh Token 삭제)
            log.info("사용자 {} 로그아웃 성공.", userId);

            return ResponseEntity.ok("로그아웃 되었습니다.");

        } catch (JwtException e) {
            // Access Token 파싱/검증 실패
            log.error("로그아웃 실패: 제공된 Access Token이 유효하지 않음. {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("제공된 Access Token이 유효하지 않습니다: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            // userService.logout 내부에서 발생 가능 (e.g., 사용자를 찾을 수 없음)
            log.error("로그아웃 처리 중 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("로그아웃 처리 중 문제가 발생했습니다: " + e.getMessage());
        } catch (Exception e) {
            // 기타 예상치 못한 오류
            log.error("로그아웃 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("로그아웃 중 오류가 발생했습니다.");
        }
    }
    @DeleteMapping("/sign-out")
    public ResponseEntity<?> withdraw(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증된 사용자만 접근 가능합니다.");
        }
        String userId = userDetails.getUsername();
        try {
            userService.signOutUser(userId);
            return ResponseEntity.ok("회원탈퇴가 정상 처리되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("회원탈퇴 중 오류 발생 - userId: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("회원탈퇴 처리 중 오류가 발생했습니다.");
        }
    }
}