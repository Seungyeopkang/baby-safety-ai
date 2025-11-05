package com.project.kidsvaguard.domain.place.service;

import com.project.kidsvaguard.domain.place.entity.Place;
import com.project.kidsvaguard.domain.place.repository.PlaceRepository;
import com.project.kidsvaguard.domain.user.entity.User;
import com.project.kidsvaguard.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Logger 대신 @Slf4j 사용
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 트랜잭션 어노테이션 추가

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j // 로깅을 위한 어노테이션
@Transactional(readOnly = true) // 읽기 전용 트랜잭션 기본 설정
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;

    /**
     * 장소 저장 또는 업데이트: 동일한 사용자가 동일한 장소 이름으로 등록하려 하면 기존 장소를 업데이트합니다.
     * @param place 저장할 Place 엔티티
     * @return 저장되거나 업데이트된 Place 엔티티
     */
    @Transactional
    public Place savePlace(Place place) {
        log.info("장소 저장/업데이트 시도: userId={}, placeName={}, cctvAddress={}",
                place.getUser().getUserId(), place.getPlaceName(), place.getCctvAddress());

        // 사용자별로 저장된 장소가 있나 체크 (placename은 무시하고 user만)
        List<Place> userPlaces = placeRepository.findByUser(place.getUser());

        if (!userPlaces.isEmpty()) {
            // 사용자당 장소 1개만 저장된다는 가정이니까 첫번째 꺼 업데이트
            Place existingPlace = userPlaces.get(0);
            existingPlace.setPlaceName(place.getPlaceName());
            existingPlace.setCctvAddress(place.getCctvAddress());
            existingPlace.setAction(place.getAction());
            log.info("기존 장소 업데이트 완료: placeId={}", existingPlace.getPlaceId());
            return placeRepository.save(existingPlace);
        } else {
            // 없으면 새로 저장
            log.info("새로운 장소 저장 완료: placeName={}", place.getPlaceName());
            return placeRepository.save(place);
        }
    }


    /**
     * 장소 삭제
     * TODO: 이 메서드에 사용자의 소유권 확인 로직을 추가하는 것이 좋습니다.
     * 예: public void deletePlace(Long placeId, String userId)
     * @param placeId 삭제할 장소의 ID
     */
    @Transactional // 쓰기 작업이므로 트랜잭션 활성화
    public void deletePlace(Long placeId) {
        log.info("장소 삭제 시도: placeId={}", placeId);
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> {
                    log.error("장소 삭제 실패: 해당 장소가 존재하지 않습니다. ID: {}", placeId);
                    return new IllegalArgumentException("해당 장소가 존재하지 않습니다.");
                });
        placeRepository.delete(place); // 장소 삭제
        log.info("장소 삭제 완료: placeId={}", placeId);
    }

    /**
     * 장소 이름으로 장소 조회
     * TODO: 이 메서드도 사용자 ID를 받아 특정 사용자의 장소만 조회하도록 변경하는 것이 좋습니다.
     * 현재는 모든 장소를 조회하여 필터링하는 방식이므로, 장소 개수가 많아지면 성능 문제가 발생할 수 있습니다.
     * @param placeName 검색할 장소 이름
     * @return Optional<Place> (장소가 있을 수도 있고 없을 수도 있음)
     */
    public Optional<Place> getPlaceByName(String placeName) {
        log.info("장소 이름으로 조회 시도: placeName={}", placeName);
        // TODO: 실제로는 사용자의 ID와 함께 장소 이름을 검색하는 것이 더 일반적이고 안전합니다.
        // 예: placeRepository.findByUserAndPlaceName(user, placeName)
        Optional<Place> place = placeRepository.findAll().stream()
                .filter(p -> p.getPlaceName().equals(placeName))
                .findFirst();
        if (place.isPresent()) {
            log.info("장소 발견: placeId={}", place.get().getPlaceId());
        } else {
            log.info("장소 찾을 수 없음: placeName={}", placeName);
        }
        return place;
    }

    /**
     * 사용자 ID로 해당 사용자가 등록한 모든 장소(CCTV) 조회
     * @param userId 조회할 사용자의 ID (문자열)
     * @return 해당 사용자와 연결된 Place 리스트
     */
    public List<Place> getPlacesByUserId(String userId) {
        log.info("사용자 ID로 장소 목록 조회 시도: userId={}", userId);
        log.info("PlaceService: getPlacesByUserId 호출. userId = '{}'", userId); // <<-- 추가
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.error("장소 목록 조회 실패: User not found with ID: {}", userId);
                    return new IllegalArgumentException("User not found");
                });
        List<Place> places = placeRepository.findByUser(user);
        log.info("사용자 ID {}에 대한 장소 {}개 발견", userId, places.size());
        return places;
    }
    public Optional<Place> getPlaceById(Long placeId) {
        log.info("장소 ID로 조회 시도: placeId={}", placeId);
        return placeRepository.findById(placeId);
    }
}