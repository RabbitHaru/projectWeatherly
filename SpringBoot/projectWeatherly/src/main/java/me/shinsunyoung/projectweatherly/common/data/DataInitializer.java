package me.shinsunyoung.projectweatherly.common.data;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.board.domain.entity.Board;
import me.shinsunyoung.projectweatherly.board.domain.enums.BoardStatus;
import me.shinsunyoung.projectweatherly.board.repository.BoardRepository;
import me.shinsunyoung.projectweatherly.member.domain.entity.Agreement;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
import me.shinsunyoung.projectweatherly.member.domain.enums.AuthProvider;
import me.shinsunyoung.projectweatherly.member.domain.enums.MemberRole;
import me.shinsunyoung.projectweatherly.member.repository.MemberRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final BoardRepository boardRepository;
    private final PasswordEncoder passwordEncoder; // WebSecurityConfig에 등록된 BCryptPasswordEncoder 주입

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 데이터 중복 생성 방지: 멤버가 1명이라도 있으면 실행하지 않음
        if (memberRepository.count() > 0) {
            log.info("이미 데이터가 존재하여 더미 데이터 생성을 건너뜁니다.");
            return;
        }

        log.info("더미 데이터 생성을 시작합니다...");

        List<Member> allMembers = new ArrayList<>();

        // 1. 관리자 계정 생성 (2명)
        for (int i = 1; i <= 2; i++) {
            Member admin = createMember(
                    "admin" + i + "@weatherly.com", // 이메일 (아이디)
                    "1234",                        // 비밀번호
                    "관리자" + i,                    // 닉네임
                    MemberRole.ADMIN               // 권한
            );
            allMembers.add(memberRepository.save(admin));
        }

        // 2. 일반 유저 계정 생성 (10명)
        for (int i = 1; i <= 10; i++) {
            Member user = createMember(
                    "user" + i + "@weatherly.com",
                    "1234",
                    "웨더러" + i,
                    MemberRole.USER
            );
            allMembers.add(memberRepository.save(user));
        }

        // 3. 게시글 데이터 생성 (20개)
        createDummyBoards(allMembers);

        log.info("더미 데이터 생성 완료: 관리자 2명, 유저 10명, 게시글 20개");
    }

    private Member createMember(String email, String rawPassword, String nickname, MemberRole role) {
        // 회원 객체 생성
        Member member = Member.builder()
                .email(email)
                .password(passwordEncoder.encode(rawPassword)) // 비밀번호 암호화 필수
                .nickname(nickname)
                .role(role)
                .authProvider(AuthProvider.local)
                .isActive(true)
                .build();

        // 약관 동의 객체 생성
        Agreement agreement = Agreement.builder()
                .member(member)
                .termsOfServiceAgree(true)
                .privacyPolicyAgree(true)
                .boardNotificationAgree(true)
                .weatherAlertAgree(true)
                .build();

        // 연관관계 설정 (Cascade.ALL 설정으로 Member 저장 시 Agreement도 함께 저장됨)
        member.setAgreement(agreement);

        return member;
    }

    private void createDummyBoards(List<Member> members) {
        String[] categories = {"weather", "dust", "outfit", "general"};
        String[] categoryNames = {"날씨", "미세먼지", "옷차림", "자유주제"};
        Random random = new Random();

        for (int i = 1; i <= 20; i++) {
            // 랜덤 작성자 및 카테고리 선정
            Member writer = members.get(random.nextInt(members.size()));
            int catIdx = random.nextInt(categories.length);

            Board board = Board.builder()
                    .title(categoryNames[catIdx] + " 관련 이야기 " + i)
                    .content("안녕하세요. " + writer.getNickname() + "입니다.\n" +
                            "이 글은 테스트용 더미 데이터입니다.\n" +
                            "카테고리: " + categoryNames[catIdx] + "\n" +
                            "번호: " + i + "\n\n" +
                            "오늘 날씨가 참 좋네요. 모두 행복한 하루 되세요!")
                    .category(categories[catIdx])
                    .member(writer)
                    .viewCount(random.nextInt(200)) // 조회수 0~199 랜덤
                    .likeCount(random.nextInt(50))  // 좋아요 0~49 랜덤
                    .isVerified(false)
                    .boardStatus(BoardStatus.ACTIVE)
                    .build();

            boardRepository.save(board);
        }
    }
}