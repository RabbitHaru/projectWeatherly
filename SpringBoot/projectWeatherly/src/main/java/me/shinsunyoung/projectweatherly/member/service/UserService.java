package me.shinsunyoung.projectweatherly.member.service;

import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.member.domain.entity.Agreement;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
import me.shinsunyoung.projectweatherly.member.domain.enums.MemberRole;
import me.shinsunyoung.projectweatherly.member.dto.request.SignupRequest;
import me.shinsunyoung.projectweatherly.member.repository.MemberRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final MemberRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public Long save(SignupRequest dto){
        Agreement agreement = Agreement.builder()
                .termsOfServiceAgree(dto.getTermsOfServiceAgree())
                .privacyPolicyAgree(dto.getPrivacyPolicyAgree())
                .build();
        // 저장할 계정 데이터 설정
        Member user = Member.builder()
                .email(dto.getEmail()) // email 설정
                // 비밀번호를 BCrypt방식으로 암호화 하여 설정
                .password(bCryptPasswordEncoder.encode(dto.getPassword()))
                .role(MemberRole.USER)
                .nickname(dto.getNickname())
//                .profileImage(dto.getProfileImage())
                .agreement(agreement)
                .build();
        // DB에 계정 저장 후 id값을 반환
        return userRepository.save(user).getId();
    }
}
