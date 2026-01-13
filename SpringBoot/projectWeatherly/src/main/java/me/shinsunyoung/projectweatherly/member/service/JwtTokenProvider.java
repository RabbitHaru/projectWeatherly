package me.shinsunyoung.projectweatherly.member.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
import me.shinsunyoung.projectweatherly.member.domain.enums.AuthProvider;
import me.shinsunyoung.projectweatherly.member.domain.enums.MemberRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String secretKey;

    @Value("${app.jwt.access-token-expiration}")
    private long accessTokenValidityInMilliseconds;

    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshTokenValidityInMilliseconds;

    private Key key;

    @PostConstruct
    protected void init() {
        byte[] keyBytes = Base64.getEncoder().encode(secretKey.getBytes());
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    // 1. Access Token 생성 (일반 로그인용)
    public String createToken(String email, MemberRole role) {
        return createToken(email, role, null, null);
    }

    // 2. Access Token 생성 (OAuth2 로그인용 - 추가 클레임 포함)
    public String createToken(String email, MemberRole role, AuthProvider authProvider, String providerId) {
        Claims claims = Jwts.claims().setSubject(email);
        claims.put("role", role != null ? role.name() : MemberRole.USER.name());
        claims.put("authProvider", authProvider != null ? authProvider.name() : AuthProvider.LOCAL.name());
        claims.put("providerId", providerId != null ? providerId : "");
        claims.put("type", "ACCESS");

        Date now = new Date();
        Date validity = new Date(now.getTime() + accessTokenValidityInMilliseconds);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 3. Member 객체로 Access Token 생성 (새로운 Member 엔티티에 맞게 수정)
    public String createTokenFromMember(Member member) {
        String email = member.getEmail() != null ? member.getEmail() : "";
        Long memberId = member.getId() != null ? member.getId() : 0L;
        String nickname = member.getNickname() != null ? member.getNickname() : "";
        String role = member.getRole() != null ? member.getRole().name() : MemberRole.USER.name();
        String authProvider = member.getAuthProvider() != null ? member.getAuthProvider().name() : AuthProvider.LOCAL.name();
        String providerId = member.getProviderId() != null ? member.getProviderId() : "";
        String profileImage = member.getProfileImage() != null ? member.getProfileImage() : "";

        Claims claims = Jwts.claims().setSubject(email);
        claims.put("memberId", memberId);
        claims.put("email", email);
        claims.put("nickname", nickname);
        claims.put("role", role);
        claims.put("authProvider", authProvider);
        claims.put("providerId", providerId);
        claims.put("profileImage", profileImage);
        claims.put("type", "ACCESS");

        Date now = new Date();
        Date validity = new Date(now.getTime() + accessTokenValidityInMilliseconds);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 4. Refresh Token 생성
    public String createRefreshToken(String email) {
        Claims claims = Jwts.claims().setSubject(email);
        claims.put("type", "REFRESH");

        Date now = new Date();
        Date validity = new Date(now.getTime() + refreshTokenValidityInMilliseconds);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 5. 멤버 객체로 Refresh Token 생성 (새로운 Member 엔티티에 맞게 수정)
    public String createRefreshTokenFromMember(Member member) {
        String email = member.getEmail() != null ? member.getEmail() : "";
        Long memberId = member.getId() != null ? member.getId() : 0L;
        String role = member.getRole() != null ? member.getRole().name() : MemberRole.USER.name();
        String authProvider = member.getAuthProvider() != null ? member.getAuthProvider().name() : AuthProvider.LOCAL.name();
        String providerId = member.getProviderId() != null ? member.getProviderId() : "";

        Claims claims = Jwts.claims().setSubject(email);
        claims.put("memberId", memberId);
        claims.put("email", email);
        claims.put("role", role);
        claims.put("authProvider", authProvider);
        claims.put("providerId", providerId);
        claims.put("type", "REFRESH");

        Date now = new Date();
        Date validity = new Date(now.getTime() + refreshTokenValidityInMilliseconds);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 6. Refresh Token에서 Access Token 재생성
    public String recreateAccessTokenFromRefreshToken(String refreshToken) {
        if (!validateToken(refreshToken)) {
            throw new JwtException("유효하지 않은 Refresh Token입니다.");
        }

        Claims claims = getClaims(refreshToken);
        if (!"REFRESH".equals(claims.get("type", String.class))) {
            throw new JwtException("Refresh Token이 아닙니다.");
        }

        String email = claims.getSubject();
        String role = claims.get("role", String.class);
        String authProvider = claims.get("authProvider", String.class);
        String providerId = claims.get("providerId", String.class);

        // Enum으로 변환
        MemberRole memberRole = MemberRole.valueOf(role);
        AuthProvider provider = AuthProvider.valueOf(authProvider);

        return createToken(email, memberRole, provider, providerId);
    }

    // 7. Authentication 객체 생성
    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);

        Collection<? extends GrantedAuthority> authorities;
        if (claims.get("role") != null) {
            String role = claims.get("role", String.class);
            authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
        } else {
            authorities = Collections.emptyList();
        }

        UserDetails userDetails = User.builder()
                .username(claims.getSubject())
                .password("")
                .authorities(authorities)
                .build();

        return new UsernamePasswordAuthenticationToken(userDetails, "", authorities);
    }

    // 8. 토큰에서 사용자명(이메일) 추출
    public String getUsername(String token) {
        return getClaims(token).getSubject();
    }

    // 9. 토큰에서 사용자 ID 추출 (memberId로 변경)
    public Long getMemberId(String token) {
        return getClaims(token).get("memberId", Long.class);
    }

    // 10. 토큰에서 역할 추출
    public String getRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    // 11. 토큰에서 인증 제공자 추출
    public String getAuthProvider(String token) {
        return getClaims(token).get("authProvider", String.class);
    }

    // 12. 토큰에서 모든 클레임 추출
    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // 13. 토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (SecurityException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    // 14. Access Token인지 확인
    public boolean isAccessToken(String token) {
        try {
            Claims claims = getClaims(token);
            return "ACCESS".equals(claims.get("type", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    // 15. Refresh Token인지 확인
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = getClaims(token);
            return "REFRESH".equals(claims.get("type", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    // 16. 토큰 만료 시간 계산
    public Date getExpirationDate(String token) {
        return getClaims(token).getExpiration();
    }

    // 17. 토큰 남은 유효 시간 계산 (밀리초)
    public long getRemainingValidity(String token) {
        Date expiration = getExpirationDate(token);
        Date now = new Date();
        return expiration.getTime() - now.getTime();
    }

    // 18. Refresh Token 만료 시간 계산 (LocalDateTime)
    public LocalDateTime calculateRefreshTokenExpiry() {
        return LocalDateTime.now()
                .plusSeconds(refreshTokenValidityInMilliseconds / 1000);
    }

    // 19. 토큰에서 모든 정보 추출 (디버깅용)
    public Map<String, Object> getAllClaims(String token) {
        Claims claims = getClaims(token);
        Map<String, Object> result = new HashMap<>();

        claims.forEach((key, value) -> {
            result.put(key, value);
        });

        return result;
    }

    // 20. 특정 클레임 존재 여부 확인
    public boolean hasClaim(String token, String claimName) {
        try {
            Claims claims = getClaims(token);
            return claims.get(claimName) != null;
        } catch (Exception e) {
            return false;
        }
    }

    // 21. 토큰 생성 시간 가져오기
    public Date getIssuedAt(String token) {
        return getClaims(token).getIssuedAt();
    }

    // 22. 프로필 이미지 URL 가져오기
    public String getProfileImage(String token) {
        return getClaims(token).get("profileImage", String.class);
    }

    // 23. 닉네임 가져오기
    public String getNickname(String token) {
        return getClaims(token).get("nickname", String.class);
    }

    // 24. 토큰 페이로드 디코딩 (디버깅용)
    public String decodeTokenPayload(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return "Invalid token format";
            }

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            return payload;
        } catch (Exception e) {
            return "Error decoding token: " + e.getMessage();
        }
    }
}