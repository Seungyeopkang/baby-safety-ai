package com.project.kidsvaguard.global.jwtToken;

import com.project.kidsvaguard.domain.user.entity.User; // User 엔티티 import
import com.project.kidsvaguard.domain.user.service.UserDetailService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException; // 명시적 import
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
// import java.util.Arrays; // 미사용 import 제거
// import java.util.Collection; // 미사용 import 제거
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {

    private final Key key;
    private final UserDetailService userDetailService;
    private final RefreshTokenRepository refreshTokenRepository; // Repository 주입

    // Access Token 및 Refresh Token 만료 시간 (Value로 설정하는 것 권장)
    @Value("${jwt.access-token-validity-in-milliseconds:3600000}") // 1시간 (기본값)
    private long accessTokenValidityInMilliseconds;

    @Value("${jwt.refresh-token-validity-in-milliseconds:604800000}") // 7일 (기본값)
    private long refreshTokenValidityInMilliseconds;


    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey,
                            UserDetailService userDetailService,
                            RefreshTokenRepository refreshTokenRepository) { // 생성자에 Repository 추가
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.userDetailService = userDetailService;
        this.refreshTokenRepository = refreshTokenRepository; // Repository 할당
    }

    // AccessToken + RefreshToken 생성
    public JwtToken generateToken(Authentication authentication) {
        String authorities = authentication.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .collect(Collectors.joining(","));

        long now = System.currentTimeMillis();
        Date accessTokenExpiresIn = new Date(now + accessTokenValidityInMilliseconds);
        Date refreshTokenExpiresIn = new Date(now + refreshTokenValidityInMilliseconds); // Refresh Token 만료 시간

        User principal = (User) authentication.getPrincipal();
        String userId = principal.getUserId(); // userId 가져오기

        String accessToken = Jwts.builder()
                .setSubject(userId)  // userId
                .claim("auth", authorities)
                .setExpiration(accessTokenExpiresIn)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        // Refresh Token은 DB 검증에 의존하므로, Claim 최소화 (만료 시간만 설정)
        String refreshToken = Jwts.builder()
                .setExpiration(refreshTokenExpiresIn)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        return JwtToken.builder()
                .grantType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    // AccessToken -> Authentication (엑세스 토큰으로 인증)
    public Authentication getAuthentication(String accessToken) {
        Claims claims = parseClaims(accessToken);
        log.info("토큰에서 추출된 subject(userId): {}", claims.getSubject());
        if (claims.get("auth") == null) {
            // SecurityContextHolder에 저장된 Authentication 객체가 없거나 권한 정보가 없는 경우 예외 발생
            // 실제로는 UserDetailsService에서 loadUserByUsername 호출 시 UsernameNotFoundException 등이 발생할 수 있음
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }

        String userId = claims.getSubject();
        // UserDetailService를 통해 UserDetails 객체를 가져옴
        UserDetails userDetails = userDetailService.loadUserByUsername(userId);
        log.info("로드된 사용자 정보: {}", userDetails.getUsername());
        // UserDetails 객체와 권한 정보를 기반으로 Authentication 객체 생성
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    // === Refresh Token을 사용하여 새로운 Access Token 발급 (DB 검증 방식) ===
    public JwtToken refreshAccessToken(String refreshToken) {
        // 1. Refresh Token 유효성 검증
        // 만약 여기서도 검증하려면:
        if (!validateToken(refreshToken)) {
            throw new JwtException("유효하지 않은 Refresh Token");
        }

        // 2. DB에서 Refresh Token 조회 및 존재 유무 확인
        RefreshToken foundRefreshToken = refreshTokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new JwtException("Refresh Token을 DB에서 찾을 수 없습니다."));

        // 3. Refresh Token에 연결된 User 정보 가져오기
        User user = foundRefreshToken.getUser();
        if (user == null) {
            log.error("Refresh Token ID {} 에 연결된 사용자가 없습니다.", foundRefreshToken.getId());
            throw new JwtException("Refresh Token에 연결된 사용자 정보가 없습니다.");
        }
        // 4. 기존 Refresh Token 삭제 (Rotation)
        refreshTokenRepository.delete(foundRefreshToken);

        // 5. User 정보를 기반으로 Authentication 객체 생성
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user,
                "",
                user.getAuthorities()
        );

        // 6. 새로운 Access Token 생성
        String authorities = authentication.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .collect(Collectors.joining(","));
        long now = System.currentTimeMillis();
        Date accessTokenExpiresIn = new Date(now + accessTokenValidityInMilliseconds);

        String newAccessToken = Jwts.builder()
                .setSubject(user.getUserId())
                .claim("auth", authorities)
                .setExpiration(accessTokenExpiresIn)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        // 7. 새로운 Refresh Token 생성 및 DB 저장
        Date newRefreshTokenExpiresIn = new Date(now + refreshTokenValidityInMilliseconds);
        String newRefreshTokenString = Jwts.builder()
                .setExpiration(newRefreshTokenExpiresIn)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        RefreshToken newRefreshToken = RefreshToken.builder()
                .refreshToken(newRefreshTokenString)
                .user(user)
                .build();
        refreshTokenRepository.save(newRefreshToken);

        // 8. 결과 반환: 새로운 Access Token과 새로운 Refresh Token
        return JwtToken.builder()
                .grantType("Bearer")
                .accessToken(newAccessToken)
                .refreshToken(newRefreshTokenString) // 새로운 Refresh Token 반환
                .build();
    }
    // 토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("잘못된 JWT 서명입니다.", e);
            // 실제 운영에서는 예외를 그대로 던지기보다, 커스텀 예외나 에러 코드를 사용하는 것이 좋습니다.
            throw new JwtException("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰입니다."); // 만료는 예상 가능한 경우이므로 warn 레벨로 조정 가능
            throw new JwtException("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 JWT 토큰입니다.", e);
            throw new JwtException("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            // token == null || token.isEmpty() 인 경우 발생 가능성 있음
            log.error("JWT 토큰이 잘못되었습니다. (비어 있거나 형식 오류)", e);
            throw new JwtException("JWT 토큰이 잘못되었습니다.");
        }
    }

    // 토큰에서 Claims 추출 (만료 시에도 추출 시도)
    private Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            // 만료된 토큰에서도 Claim 정보가 필요할 수 있으므로 반환
            // (예: 만료되었지만 어떤 사용자의 토큰이었는지 확인)
            return e.getClaims();
        }
    }

    // Access Token에서 userId 추출
    public String getUserIdFromToken(String accessToken) {
        // parseClaims는 만료된 토큰도 처리하므로, Access Token의 subject를 읽는 것은 가능.
        // 단, 이 메소드를 호출하기 전에 validateToken으로 유효성을 먼저 검사하는 것이 일반적.
        return parseClaims(accessToken).getSubject();
    }
}