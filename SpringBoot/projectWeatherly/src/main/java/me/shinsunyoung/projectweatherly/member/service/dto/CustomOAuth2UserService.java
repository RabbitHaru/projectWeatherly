package me.shinsunyoung.projectweatherly.member.service.dto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
import me.shinsunyoung.projectweatherly.member.domain.enums.AuthProvider;
import me.shinsunyoung.projectweatherly.member.domain.enums.MemberRole;
import me.shinsunyoung.projectweatherly.member.repository.MemberRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        log.info("OAuth2 로그인 요청: {}", registrationId);

        Map<String, Object> attributes = oAuth2User.getAttributes();

        // 네이버인 경우
        if ("naver".equals(registrationId)) {
            return processOAuthUser(attributes, AuthProvider.naver);
        }

        // 카카오인 경우
        else if ("kakao".equals(registrationId)) {
            return processOAuthUser(attributes, AuthProvider.kakao);
        }

        throw new OAuth2AuthenticationException("지원하지 않는 OAuth2 제공자입니다: " + registrationId);
    }

    private OAuth2User processOAuthUser(Map<String, Object> attributes, AuthProvider provider) {
        String providerId;
        String email = null;
        String nickname = null;
        String profileImage = null;
        String userNameAttributeName = "id"; // 기본값

        try {
            switch (provider) {
                case naver:
                    Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                    if (response == null) {
                        throw new OAuth2AuthenticationException("네이버 응답 데이터가 없습니다.");
                    }
                    providerId = (String) response.get("id");
                    email = (String) response.get("email");
                    nickname = (String) response.get("nickname");
                    profileImage = (String) response.get("profile_image");
                    userNameAttributeName = "response";
                    log.info("네이버 사용자 정보: id={}, email={}, nickname={}", providerId, email, nickname);
                    break;

                case kakao:
                    providerId = String.valueOf(attributes.get("id"));

                    // 카카오 properties에서 닉네임과 프로필 이미지 추출
                    Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
                    if (properties != null) {
                        nickname = (String) properties.get("nickname");
                        profileImage = (String) properties.get("profile_image");
                    }

                    // 카카오 계정 정보에서 이메일 추출
                    Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
                    if (kakaoAccount != null) {
                        email = (String) kakaoAccount.get("email");

                        // 카카오 계정 내 프로필 정보가 별도로 있는 경우
                        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                        if (profile != null) {
                            if (nickname == null) {
                                nickname = (String) profile.get("nickname");
                            }
                            if (profileImage == null) {
                                profileImage = (String) profile.get("profile_image_url");
                            }
                        }
                    }

                    // 이메일이 없는 경우 처리 (카카오는 이메일 동의 선택사항)
                    if (email == null) {
                        email = providerId + "@kakao.com";
                    }

                    log.info("카카오 사용자 정보: id={}, email={}, nickname={}", providerId, email, nickname);
                    break;

                default:
                    throw new OAuth2AuthenticationException("지원하지 않는 OAuth2 제공자입니다.");
            }
        } catch (ClassCastException | NullPointerException e) {
            log.error("OAuth2 응답 파싱 오류: {}", e.getMessage());
            throw new OAuth2AuthenticationException("OAuth2 응답 데이터 형식이 올바르지 않습니다.");
        }

        // 회원 처리 로직
        Member member = processMember(providerId, email, nickname, profileImage, provider);

        // JWT 토큰 관련 코드 제거됨

        // 프론트엔드로 전달할 추가 정보 저장 (JWT 제외)
        attributes.put("member_id", member.getId());
        attributes.put("email", member.getEmail());
        attributes.put("nickname", member.getNickname());
        attributes.put("profile_image", member.getProfileImage());
        attributes.put("auth_provider", provider.name());

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + member.getRole().name())),
                attributes,
                userNameAttributeName
        );
    }

    private Member processMember(String providerId, String email, String nickname,
                                 String profileImage, AuthProvider authProvider) {

        // 1. providerId와 authProvider로 기존 회원 확인
        Optional<Member> existingMember = memberRepository.findByProviderIdAndAuthProvider(
                providerId, authProvider);

        if (existingMember.isPresent()) {
            // 기존 회원: 정보 업데이트
            Member member = existingMember.get();
            updateMemberInfo(member, email, nickname, profileImage);
            return member;
        }

        // 2. 이메일로도 확인 (이미 일반 회원가입한 경우)
        if (email != null) {
            // 이메일과 동일한 인증 제공자로 찾기
            Optional<Member> emailMember = memberRepository.findByEmailAndAuthProvider(email, authProvider);
            if (emailMember.isPresent()) {
                Member member = emailMember.get();
                // providerId 업데이트
                memberRepository.updateAuthProvider(member.getId(), authProvider, providerId);
                return member;
            }

            // 이메일로만 찾기 (다른 인증 제공자와 연결되지 않은 경우)
            Optional<Member> emailOnlyMember = memberRepository.findByEmail(email);
            if (emailOnlyMember.isPresent()) {
                Member member = emailOnlyMember.get();
                // 소셜 로그인 정보 연결
                memberRepository.updateAuthProvider(member.getId(), authProvider, providerId);
                return member;
            }
        }

        // 3. 새 회원 생성
        return createNewOAuthMember(providerId, email, nickname, profileImage, authProvider);
    }

    private Member createNewOAuthMember(String providerId, String email, String nickname,
                                        String profileImage, AuthProvider authProvider) {

        // 이메일이 없는 경우 (카카오는 이메일 동의 안할 수 있음)
        String userEmail = email;
        if (userEmail == null || userEmail.isEmpty()) {
            userEmail = providerId + "@" + authProvider.name().toLowerCase() + ".com";
        }

        // 닉네임이 없는 경우
        String userNickname = nickname;
        if (userNickname == null || userNickname.isEmpty()) {
            userNickname = authProvider.name().toLowerCase() + "_user_" +
                    providerId.substring(0, Math.min(providerId.length(), 8));
        }

        Member member = Member.builder()
                .email(userEmail)
                .nickname(userNickname)
                .profileImage(profileImage)
                .authProvider(authProvider)
                .providerId(providerId)
                .role(MemberRole.USER)
                .isActive(true)
                .build();

        return memberRepository.save(member);
    }

    private void updateMemberInfo(Member member, String email, String nickname, String profileImage) {
        boolean updated = false;

        if (email != null && !email.isEmpty() && !email.equals(member.getEmail())) {
            member.setEmail(email);
            updated = true;
        }
        if (nickname != null && !nickname.isEmpty() && !nickname.equals(member.getNickname())) {
            member.setNickname(nickname);
            updated = true;
        }
        if (profileImage != null && !profileImage.isEmpty() && !profileImage.equals(member.getProfileImage())) {
            member.setProfileImage(profileImage);
            updated = true;
        }

        if (updated) {
            memberRepository.save(member);
        }
    }
}