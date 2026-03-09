package me.shinsunyoung.projectweatherly.common.data;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.board.domain.entity.Board;
import me.shinsunyoung.projectweatherly.board.domain.entity.Comment;
import me.shinsunyoung.projectweatherly.board.domain.entity.Report;
import me.shinsunyoung.projectweatherly.board.domain.enums.BoardStatus;
import me.shinsunyoung.projectweatherly.board.domain.enums.ReportStatus;
import me.shinsunyoung.projectweatherly.board.repository.BoardRepository;
import me.shinsunyoung.projectweatherly.board.repository.CommentRepository;
import me.shinsunyoung.projectweatherly.board.repository.ReportRepository;
import me.shinsunyoung.projectweatherly.member.domain.entity.Agreement;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
import me.shinsunyoung.projectweatherly.member.domain.enums.AuthProvider;
import me.shinsunyoung.projectweatherly.member.domain.enums.MemberRole;
import me.shinsunyoung.projectweatherly.member.repository.MemberRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final BoardRepository boardRepository;
    private final ReportRepository reportRepository;
    private final CommentRepository commentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("🏁 DataInitializer 시작: 데이터 초기화 확인 중...");

        // [수정] 게시글이 하나라도 있으면 더미 데이터 생성을 중단합니다. (중복 생성 방지)
        if (boardRepository.count() > 0) {
            log.info("✅ 이미 게시글 데이터가 존재합니다. 더미 데이터 생성을 건너뜁니다.");
            return;
        }

        // --- 아래부터는 데이터가 없을 때만 실행됩니다 ---

        // 1. 멤버 확보 (없으면 생성, 있으면 DB에서 가져오기)
        List<Member> basicMembers = getOrCreateBasicMembers();
        Member zenomia = getOrCreateZenomiaMember();

        // 2. 게시글 및 신고 데이터 생성
        createDummyBoards(basicMembers);       // 기본 게시글 20개 추가
        createZenomiaContent(zenomia);         // 제노미아 글 50개 + 신고 50개 추가
        createNoticeData(zenomia);             // 공지사항 50개 추가 (좋아요 0 고정)

        // 3. 더미 댓글 및 좋아요 추가
        createCommentData();                   // (공지사항은 댓글 제외됨)

        log.info("✨ 초기 더미 데이터 생성이 완료되었습니다.");
    }

    // ----------------------------------------------------------------
    // 1. 멤버 확보 로직 (Get or Create)
    // ----------------------------------------------------------------

    private List<Member> getOrCreateBasicMembers() {
        List<Member> members = new ArrayList<>();

        // 관리자 2명
        for (int i = 1; i <= 2; i++) {
            members.add(getOrCreateMember("admin" + i + "@weatherly.com", "12341234", "관리자" + i, MemberRole.ADMIN));
        }
        // 일반 유저 10명
        for (int i = 1; i <= 10; i++) {
            members.add(getOrCreateMember("user" + i + "@weatherly.com", "12341234", "웨더러" + i, MemberRole.USER));
        }
        return members;
    }

    private Member getOrCreateZenomiaMember() {
        // 제노미아 (ADMIN 권한)
        return getOrCreateMember("zeta1234@naver.com", "12341234", "제노미아", MemberRole.ADMIN);
    }

    private Member getOrCreateMember(String email, String rawPwd, String nickname, MemberRole role) {
        return memberRepository.findByEmail(email)
                .orElseGet(() -> {
                    log.info("✨ 새 멤버 생성: {}", email);
                    Member member = Member.builder()
                            .email(email)
                            .password(passwordEncoder.encode(rawPwd))
                            .nickname(nickname)
                            .role(role)
                            .authProvider(AuthProvider.local)
                            .isActive(true)
                            .build();

                    Agreement agreement = Agreement.builder()
                            .member(member)
                            .termsOfServiceAgree(true)
                            .privacyPolicyAgree(true)
                            .boardNotificationAgree(true)
                            .weatherAlertAgree(true)
                            .build();
                    member.setAgreement(agreement);

                    return memberRepository.save(member);
                });
    }

    // ----------------------------------------------------------------
    // 2. 컨텐츠 생성 로직 (Always Create)
    // ----------------------------------------------------------------

    private void createDummyBoards(List<Member> members) {
        String[] categories = {"weather", "dust", "outfit", "general"};
        String[] categoryNames = {"날씨", "미세먼지", "옷차림", "자유주제"};
        Random random = new Random();

        List<Board> boards = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            Member writer = members.get(random.nextInt(members.size()));
            int catIdx = random.nextInt(categories.length);

            Board board = Board.builder()
                    .title(categoryNames[catIdx] + " 관련 이야기 " + i + " (자동생성)")
                    .content("안녕하세요. " + writer.getNickname() + "입니다.\n초기 생성된 더미 데이터입니다.\n번호: " + i)
                    .category(categories[catIdx])
                    .member(writer)
                    .viewCount(random.nextInt(200))
                    .likeCount(random.nextInt(50))
                    .isVerified(false)
                    .boardStatus(BoardStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .build();
            boards.add(board);
        }
        boardRepository.saveAll(boards);
        log.info("   + 기본 게시글 20개 추가됨");
    }

    private void createZenomiaContent(Member zenomia) {
        Random random = new Random();

        // 1) 제노미아 게시글 50개
        List<Board> boards = new ArrayList<>();
        String[] categories = {"general", "question", "info", "outfit"};

        for (int i = 1; i <= 50; i++) {
            Board board = Board.builder()
                    .title("제노미아의 게시글 - " + i)
                    .content("안녕하세요. 제노미아입니다.\n이 글은 초기 생성 더미 데이터입니다.\n번호: " + i)
                    .category(categories[random.nextInt(categories.length)])
                    .member(zenomia)
                    .viewCount(random.nextInt(100))
                    .likeCount(0)
                    .isVerified(false)
                    .boardStatus(BoardStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .build();
            boards.add(board);
        }
        boardRepository.saveAll(boards);

        // 2) 제노미아 신고 내역 50개
        List<Report> reports = new ArrayList<>();
        String[] reasonValues = {"spam", "abuse", "illegal", "other"};
        String[] reasonTexts = {"스팸/광고", "욕설/혐오 발언", "불법 정보", "기타"};
        ReportStatus[] statuses = ReportStatus.values();

        for (int i = 1; i <= 50; i++) {
            Long targetId = (long) (random.nextInt(1000) + 1);
            int idx = random.nextInt(reasonValues.length);
            ReportStatus randomStatus = statuses[random.nextInt(statuses.length)];

            Report report = Report.builder()
                    .reporter(zenomia)
                    .targetId(targetId)
                    .type("BOARD")
                    .reason(reasonValues[idx])
                    .details("자동 생성된 신고 [" + reasonTexts[idx] + "] #" + i)
                    .status(randomStatus)
                    .createdAt(LocalDateTime.now())
                    .build();
            reports.add(report);
        }
        reportRepository.saveAll(reports);
        log.info("   + 제노미아 게시글 50개, 신고 50개 추가됨");
    }

    private void createNoticeData(Member zenomia) {
        // 작성자 후보군 (admin1, admin2, zenomia)
        List<Member> admins = new ArrayList<>();
        memberRepository.findByEmail("admin1@weatherly.com").ifPresent(admins::add);
        memberRepository.findByEmail("admin2@weatherly.com").ifPresent(admins::add);
        admins.add(zenomia); // 제노미아도 포함

        Random random = new Random();
        List<Board> notices = new ArrayList<>();

        for (int i = 1; i <= 50; i++) {
            Member writer = admins.get(random.nextInt(admins.size()));

            Board notice = Board.builder()
                    .title("[공지] 웨더리 서비스 업데이트 안내 - " + i)
                    .content("안녕하세요, " + writer.getNickname() + "입니다.\n\n" +
                            "시스템 업데이트 안내입니다.\n" +
                            "공지 번호: #" + i)
                    .category("notice")
                    .member(writer)
                    .viewCount(random.nextInt(500) + 100)
                    .likeCount(0) // 좋아요 0 고정
                    .isVerified(true)
                    .boardStatus(BoardStatus.ACTIVE)
                    .createdAt(LocalDateTime.now().minusHours(i))
                    .build();
            notices.add(notice);
        }
        boardRepository.saveAll(notices);
        log.info("   + 공지사항 50개 추가됨 (좋아요/댓글 0개)");
    }

    // ----------------------------------------------------------------
    // 3. 댓글 생성 로직
    // ----------------------------------------------------------------

    private void createCommentData() {
        List<Member> allMembers = memberRepository.findAll();
        List<Board> allBoards = boardRepository.findAll();

        if (allMembers.isEmpty() || allBoards.isEmpty()) {
            return;
        }

        Random random = new Random();
        List<Comment> comments = new ArrayList<>();

        String[] commentTexts = {
                "좋은 정보 감사합니다!", "오늘 날씨 정말 춥네요 ㅠㅠ", "옷차림 정보 유용해요~",
                "저도 공감합니다.", "항상 잘 보고 있습니다.", "다음 업데이트도 기대되네요.",
                "미세먼지 조심하세요!", "이런 기능도 있었군요.", "작성자님 화이팅!", "잘 읽었습니다."
        };

        for (Board board : allBoards) {
            // 공지사항인 경우 댓글 생성 건너뛰기
            if ("notice".equalsIgnoreCase(board.getCategory())) {
                continue;
            }

            int commentCount = random.nextInt(6);

            for (int i = 0; i < commentCount; i++) {
                Member writer = allMembers.get(random.nextInt(allMembers.size()));

                Comment comment = Comment.builder()
                        .board(board)
                        .member(writer)
                        .content(commentTexts[random.nextInt(commentTexts.length)])
                        .likeCount(random.nextInt(31))
                        .createdAt(LocalDateTime.now().minusMinutes(random.nextInt(1000)))
                        .build();
                comments.add(comment);
            }
        }

        commentRepository.saveAll(comments);
        log.info("   + 전체 게시글(공지 제외)에 대한 더미 댓글 생성 완료 (총 {}개)", comments.size());
    }
}