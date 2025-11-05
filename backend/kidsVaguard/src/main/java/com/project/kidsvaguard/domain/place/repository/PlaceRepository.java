package com.project.kidsvaguard.domain.place.repository;

import com.project.kidsvaguard.domain.place.entity.Place;
import com.project.kidsvaguard.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlaceRepository extends JpaRepository<Place, Long> {
    List<Place> findByUser(User user);
    Optional<Place> findByUserAndPlaceName(User user, String placeName);

}
