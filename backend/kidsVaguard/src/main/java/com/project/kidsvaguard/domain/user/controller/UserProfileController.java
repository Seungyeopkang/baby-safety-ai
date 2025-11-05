package com.project.kidsvaguard.domain.user.controller;

import com.project.kidsvaguard.domain.user.dto.profileDto.ChangeNameRequestDto;
import com.project.kidsvaguard.domain.user.dto.profileDto.ChangePasswordRequestDto;
import com.project.kidsvaguard.domain.user.dto.profileDto.FindIdRequestDto;
import com.project.kidsvaguard.domain.user.dto.profileDto.FindPasswordRequestDto;
import com.project.kidsvaguard.domain.user.service.UserProfileService;
import com.project.kidsvaguard.global.jwtToken.JwtTokenProvider;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;

@RestController
@RequestMapping("/users/profile")
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {
    private final UserProfileService userProfileService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/password/change")
    public ResponseEntity<?> changePassword(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
                                            @RequestBody ChangePasswordRequestDto requestDto) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.warn("비밀번호 변경 요청 헤더 오류: Authorization 헤더가 없거나 'Bearer ' 타입이 아님");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Authorization 헤더가 없거나 형식이 잘못되었습니다.");
        }
        try {
            String accessToken = authorizationHeader.substring(7);
            String userId = jwtTokenProvider.getUserIdFromToken(accessToken);

            userProfileService.changePassword(userId, requestDto);
            log.info("사용자 {} 비밀번호 변경 성공.", userId);
            return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");

        } catch (JwtException e) {
            log.error("비밀번호 변경 실패: 제공된 Access Token이 유효하지 않음. {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("제공된 Access Token이 유효하지 않습니다: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("비밀번호 변경 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("비밀번호 변경 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("비밀번호 변경 중 오류가 발생했습니다.");
        }
    }

    @PostMapping("/name/change")
    public ResponseEntity<?> changeName(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
                                        @RequestBody ChangeNameRequestDto requestDto) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.warn("이름 변경 요청 헤더 오류: Authorization 헤더가 없거나 'Bearer ' 타입이 아님");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Authorization 헤더가 없거나 형식이 잘못되었습니다.");
        }
        try {
            String accessToken = authorizationHeader.substring(7);
            String userId = jwtTokenProvider.getUserIdFromToken(accessToken);

            userProfileService.changeUsername(userId, requestDto);
            log.info("사용자 {} 이름 변경 성공.", userId);
            return ResponseEntity.ok("이름이 성공적으로 변경되었습니다.");

        } catch (JwtException e) {
            log.error("이름 변경 실패: 제공된 Access Token이 유효하지 않음. {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("제공된 Access Token이 유효하지 않습니다: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("이름 변경 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("이름 변경 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("이름 변경 중 오류가 발생했습니다.");
        }
    }
}