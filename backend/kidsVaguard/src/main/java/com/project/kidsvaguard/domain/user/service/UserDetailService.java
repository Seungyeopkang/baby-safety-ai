package com.project.kidsvaguard.domain.user.service;

import com.project.kidsvaguard.domain.user.entity.User;
import com.project.kidsvaguard.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailService implements UserDetailsService {

    private final UserRepository userRepository;


    @Override
    public UserDetails loadUserByUsername(String userid) throws UsernameNotFoundException {
        log.info("🔍 요청된 사용자 ID: {}", userid); // 여기에 로그 추가
        return userRepository.findByUserId(userid)
                .map(this::createUserDetails)
                .orElseThrow(() -> new UsernameNotFoundException("해당하는 사용자 ID를 찾을 수 없습니다."));
    }

    // User 엔티티를 UserDetails 객체로 변환
    private UserDetails createUserDetails(User user) {
        return user;
    }
}
