package me.shinsunyoung.projectweatherly.member.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
import me.shinsunyoung.projectweatherly.member.domain.enums.MemberRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@ToString
public class UserSecurityDTO implements OAuth2User, UserDetails {
    private final Member user;

    // 카카오에서 받아오는 사용자 데이터를 저장하는 변수
    private Map<String, Object> props;

    // 일반 로그인용 생성자
    public UserSecurityDTO(Member user) {
        this.user = user;
    }
    // 소셜 계정 로그인용 생성자
    public UserSecurityDTO(Member user, Map<String, Object> props) {
        this.user = user;
        this.props = props;
    }

    //Oauth2 로그인 설정 메서드 ------------------------
    @Override
    public Map<String, Object> getAttributes() {
        return props;
    }
    @Override
    public String getName() {
        return user.getEmail();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if(user.getRole().equals(MemberRole.ADMIN)){
            // ROLE_ADMIN : 관리자 권한
            // ROLE_USER : 일반 사용자 권한
            // ROLE_ : SpringSecurity에서 권한 설정시 붙이는 규칙
            return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"),
                    new SimpleGrantedAuthority("ROLE_USER"));
        }
        // 유저가 가진 권환을 반환하는 메서드
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }
    @Override
    public String getUsername() {
        return user.getEmail(); // 실제 로그인에 사용되는 아이디 설정
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public boolean isAccountNonExpired() {
        // 계정 만료 여부 반환
        return true; // 만료되지 않으면 true
    }

    @Override
    public boolean isAccountNonLocked() {
        // 계정 잠금 여부 반환
        return true; // 잠겨있지 않으면 true
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // 패스워드 만료 여부 반환
        return true;
    }

    @Override
    public boolean isEnabled() {
        // 계정 사용 가능 여부 반환
        return true;
    }
}
