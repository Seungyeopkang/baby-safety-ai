package com.project.kidsvaguard.domain.place.controller;

import com.project.kidsvaguard.domain.place.dto.PlaceCreateRequest;
import com.project.kidsvaguard.domain.place.dto.ToggleAnalysisRequest;
import com.project.kidsvaguard.domain.place.entity.Place;
import com.project.kidsvaguard.domain.place.service.PlaceService;
import com.project.kidsvaguard.domain.user.entity.User;
import com.project.kidsvaguard.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Logger 대신 @Slf4j 사용
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/place")
@RequiredArgsConstructor
@Slf4j // 로깅을 위한 어노테이션 (org.slf4j.Logger 대신 사용)
public class PlaceController {

    private final PlaceService placeService;
    private final RestTemplate restTemplate;
    private final UserService userService;

    /**
     * 장소 생성 API: 로그인된 사용자의 CCTV 주소와 장소 이름을 등록합니다.
     * @param request 장소 생성 요청 DTO (placeName, cctvAddress, action 포함)
     * @param currentUser 현재 로그인된 사용자 정보 (Spring Security가 주입)
     * @return 성공 메시지 또는 에러 메시지를 담은 ResponseEntity
     */
    @PostMapping
    public ResponseEntity<?> createPlace(@RequestBody PlaceCreateRequest request,
                                         @AuthenticationPrincipal User currentUser) {
        // @AuthenticationPrincipal을 통해 현재 로그인된 사용자의 ID를 안전하게 가져옵니다.
        String userId = currentUser.getUserId();
        String cctvAddress = request.getCctvAddress();
        String placeName = request.getPlaceName();
        Place.Action action = request.getAction(); // Place.Action enum 타입으로 바로 받음

        log.info("장소 생성 요청 수신: userId={}, placeName={}, cctvAddress={}, action={}",
                userId, placeName, cctvAddress, action);

        // 현재 사용자 정보를 DB에서 다시 로드 (영속성 컨텍스트 관리 등을 위해)
        // @AuthenticationPrincipal로 받은 User 객체가 영속 상태가 아닐 수 있으므로 다시 조회하는 것이 안전합니다.
        User user = userService.findByUserId(userId)
                .orElseThrow(() -> {
                    log.error("장소 생성 실패: 해당 userId를 가진 사용자가 없습니다. userId={}", userId);
                    return new IllegalArgumentException("해당 userId를 가진 사용자가 없습니다.");
                });

        // Place 엔티티 빌드
        Place place = Place.builder()
                .user(user) // 현재 인증된 User 객체를 사용
                .cctvAddress(cctvAddress)
                .placeName(placeName)
                .action(action != null ? action : Place.Action.STOP) // action이 null이면 기본값 STOP
                .build();

        // 서비스 계층을 통해 장소 저장 (중복 검사 및 업데이트 로직 포함)
        Place savedPlace = placeService.savePlace(place);

        log.info("장소 생성 완료: 장소 ID={}", savedPlace.getPlaceId());
        return ResponseEntity.ok("장소가 성공적으로 등록되었습니다.");
    }

    /**
     * 장소 검색 API (장소 이름으로 검색)
     * TODO: 이 API는 특정 사용자에 종속되어야 합니다. 현재는 모든 사용자의 장소를 검색할 수 있습니다.
     * @param placeName 검색할 장소 이름
     * @return 검색된 Place 엔티티 또는 404 Not Found
     */
    @GetMapping("/{placeName}")
    public ResponseEntity<?> getPlaceByName(@PathVariable String placeName,
                                            @AuthenticationPrincipal User currentUser) {
        log.info("장소 검색 요청 수신: placeName={}, userId={}", placeName, currentUser.getUserId());
        String userId = currentUser.getUserId();
        log.info("PlaceController: toggleAnalysis 호출. Extracted userId from principal = '{}'", userId); // <<-- 추가

        Place place = placeService.getPlaceByName(placeName)
                .orElseThrow(() -> new IllegalArgumentException("장소를 찾을 수 없습니다: " + placeName));

        if (!place.getUser().getUserId().equals(currentUser.getUserId())) {
            log.warn("권한 없는 사용자 장소 검색 시도: placeName={}, userId={}", placeName, currentUser.getUserId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("해당 장소에 접근할 권한이 없습니다.");
        }

        return ResponseEntity.ok(place);
    }

    /**
     * AI 분석 버튼(시작, 정지) 토글 API
     * 로그인된 사용자의 모든 등록된 CCTV에 대해 AI 분석 시작/정지 명령을 FastAPI 서버로 전송합니다.
     * @param request 토글 요청 DTO (action: "START" 또는 "STOP")
     * @param currentUser 현재 로그인된 사용자 정보 (Spring Security가 주입)
     * @return 성공 메시지 또는 에러 메시지를 담은 ResponseEntity
     */
    @PostMapping("/toggle-analysis")
    public ResponseEntity<?> toggleAnalysis(@RequestBody ToggleAnalysisRequest request,
                                            @AuthenticationPrincipal User currentUser) {

        if (currentUser == null) {
            throw new IllegalArgumentException("User not authenticated");
        }

        String userId = currentUser.getUserId();
        String action = request.getAction(); // "START" 또는 "STOP" 문자열

        log.info("AI 분석 토글 요청 수신: userId={}, action={}", userId, action);

        List<Place> places = placeService.getPlacesByUserId(userId);

        if (places.isEmpty()) {
            log.warn("사용자 ID {}에 연결된 CCTV 장소가 없어 AI 분석을 시작/정지할 수 없습니다.", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("이 사용자에게 연결된 CCTV 장소가 없습니다. 먼저 장소를 등록해주세요.");
        }

        for (Place place : places) {
            String cctvAddress = place.getCctvAddress();
            // TODO: 실제 FastAPI 서버의 정확한 IP 주소와 포트로 변경해야 합니다.
            // 예: "http://192.168.0.XXX:8000/control_camera" (XXX는 FastAPI 서버의 실제 IP)
            String fastapiUrl = "http://127.0.0.1:8000/control_camera"; // 기본값

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> fastapiRequest = new HashMap<>();
            fastapiRequest.put("userId", userId);
            fastapiRequest.put("cctvAddress", cctvAddress);
            fastapiRequest.put("action", action); // "START" 또는 "STOP"

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(fastapiRequest, headers);

            try {
                log.info("FastAPI로 AI 분석 명령 전송: userId={}, cctvAddress={}, action={}", userId, cctvAddress, action);
                restTemplate.postForEntity(fastapiUrl, entity, String.class);
                log.info("FastAPI 전송 성공: userId={}, cctvAddress={}", userId, cctvAddress);
            } catch (Exception e) {
                System.out.println("AI 서버 전송 실패 발생! userId=" + userId + ", cctvAddress=" + cctvAddress);
                log.error("AI 서버(FastAPI) 전송 실패: userId={}, cctvAddress={}, 에러: {}", userId, cctvAddress, e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("AI 서버 전송 실패: " + e.getMessage());
            }
        }

        log.info("AI 분석 토글 요청 전체 처리 완료: userId={}", userId);
        return ResponseEntity.ok("AI 분석 전송 완료");
    }

    /**
     * 장소 삭제 API
     * TODO: 이 API 또한 현재 로그인된 사용자만 자신의 장소를 삭제할 수 있도록 보안 로직을 추가해야 합니다.
     * @param placeId 삭제할 장소의 ID
     * @return 성공 메시지 또는 404 Not Found 에러 메시지
     */
    @DeleteMapping("/{placeId}")
    public ResponseEntity<?> deletePlace(@PathVariable Long placeId,
                                         @AuthenticationPrincipal User currentUser) {
        log.info("장소 삭제 요청 수신: placeId={}, userId={}", placeId, currentUser.getUserId());

        Place place = placeService.getPlaceById(placeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 장소를 찾을 수 없습니다."));

        if (!place.getUser().getUserId().equals(currentUser.getUserId())) {
            log.warn("권한 없는 사용자 접근 시도: placeId={}, userId={}", placeId, currentUser.getUserId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("해당 장소를 삭제할 권한이 없습니다.");
        }

        placeService.deletePlace(placeId);
        log.info("장소 삭제 완료: placeId={}", placeId);
        return ResponseEntity.ok("장소가 삭제되었습니다.");
    }

}