package com.project.kidsvaguard.global.config;

import com.project.kidsvaguard.global.jwtToken.JwtAuthenticationFilter;
import com.project.kidsvaguard.global.jwtToken.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtTokenProvider jwtTokenProvider;
    private final IpOrAdminAuthorizationManager ipOrAdminAuthorizationManager;

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .httpBasic(httpBasic -> httpBasic.disable())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/users/sign-in").permitAll()
                        .requestMatchers("/users/sign-up").permitAll()
                        .requestMatchers("/users/profile/**").permitAll()
                        .requestMatchers("/auth/token/refresh").permitAll()  // refresh 토큰 재발급만 허용
                        .requestMatchers("/auth/logout").authenticated()    // 로그아웃은 인증 필요
                        .requestMatchers("/auth/sign-out").authenticated()    // 회원탈퇴는 인증 필요
                        .requestMatchers("/api/notify/**").permitAll()  // 인증 없이 해당 엔드포인트 접근 허용
                        .requestMatchers("/api/alarms/**").permitAll()
                        .requestMatchers("/api/place/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/users/fcm-token").authenticated() // ✅ 해당 엔드포인트는 인증 필요
                        .requestMatchers("/users/test").hasRole("USER")
                        // ✅ 관리자 접근 제한 추가
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/admin/**").access(ipOrAdminAuthorizationManager)
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
