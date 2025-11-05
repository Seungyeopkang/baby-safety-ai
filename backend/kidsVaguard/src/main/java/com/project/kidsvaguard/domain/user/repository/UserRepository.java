package com.project.kidsvaguard.domain.user.repository;

import com.project.kidsvaguard.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserId(String userId);

    boolean existsByUserId(String userId);

    //이메일 중복체크
    boolean existsByEmail(String email);


    /*
    관리자 페이지 관련
     */
    long countByRole(User.Role role);
    List<User> findByUserIdContainingIgnoreCase(String keyword);

}

