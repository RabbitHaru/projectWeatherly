package me.shinsunyoung.projectweatherly.board.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.board.dto.BoardRequest;
import me.shinsunyoung.projectweatherly.board.dto.BoardResponse;
import me.shinsunyoung.projectweatherly.board.dto.BoardUpdateRequest;
import me.shinsunyoung.projectweatherly.board.service.BoardService;
import me.shinsunyoung.projectweatherly.member.dto.UserSecurityDTO;
import me.shinsunyoung.projectweatherly.util.FileNameUtil;
import me.shinsunyoung.projectweatherly.util.FileUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/community")
@RequiredArgsConstructor
public class CommunityController {

    private final BoardService boardService;
    private final FileUtil fileUtil;

    @GetMapping({"", "/", "/boards"})
    public String community(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sort", defaultValue = "latest") String sort,
            Model model,
            HttpServletRequest request,
            @AuthenticationPrincipal UserSecurityDTO user) {

        try {
            model.addAttribute("requestURI", request.getRequestURI());

            if (user != null && user.getUser() != null) {
                if (user.getUser().getNickname() != null) model.addAttribute("nickname", user.getUser().getNickname());
                if (user.getUser().getId() != null) model.addAttribute("memberId", user.getUser().getId());
            }

            Page<BoardResponse> boardPage;

            if (keyword != null && !keyword.trim().isEmpty()) {
                boardPage = boardService.searchBoards(keyword, pageable);
            } else {
                switch (sort) {
                    case "popular": boardPage = boardService.getPopularBoards(pageable); break;
                    case "recent": boardPage = boardService.getRecentBoards(pageable); break;
                    case "latest": default: boardPage = boardService.getAllBoards(pageable); break;
                }
            }

            List<BoardResponse> popularBoards = new ArrayList<>();
            try {
                Page<BoardResponse> popularPage = boardService.getPopularBoards(
                        org.springframework.data.domain.PageRequest.of(0, 5, Sort.by("viewCount").descending())
                );
                popularBoards = popularPage.getContent();
            } catch (Exception e) {
                log.warn("인기 게시글 조회 실패: {}", e.getMessage());
            }

            model.addAttribute("boards", boardPage);
            model.addAttribute("popularBoards", popularBoards);
            model.addAttribute("category", category);
            model.addAttribute("keyword", keyword);
            model.addAttribute("sort", sort);
            model.addAttribute("currentPage", pageable.getPageNumber());
            model.addAttribute("totalPosts", boardPage.getTotalElements());

            return "community";

        } catch (Exception e) {
            log.error("커뮤니티 페이지 로딩 중 오류 발생: ", e);
            model.addAttribute("errorMessage", "페이지를 불러오는데 실패했습니다.");
            return "redirect:/community";
        }
    }

    @GetMapping("/write")
    public String redirectToWriteForm(@RequestParam(value = "category", defaultValue = "general") String category) {
        return "redirect:/community/boards/write?category=" + category;
    }

    @GetMapping("/boards/write")
    public String writeForm(
            @RequestParam(value = "category", defaultValue = "general") String category,
            Model model,
            HttpServletRequest request,
            @AuthenticationPrincipal UserSecurityDTO user) {

        if (user == null || user.getUser() == null) {
            return "redirect:/login?redirect=/community/boards/write?category=" + category;
        }

        model.addAttribute("requestURI", request.getRequestURI());
        model.addAttribute("category", category);
        model.addAttribute("nickname", user.getUser().getNickname());
        model.addAttribute("memberId", user.getUser().getId());
        model.addAttribute("boardRequest", new BoardRequest());

        return "write";
    }

    @PostMapping("/boards/write")
    public String createBoard(
            @AuthenticationPrincipal UserSecurityDTO user,
            @ModelAttribute BoardRequest boardRequest,
            RedirectAttributes redirectAttributes) throws IOException {

        if (user == null || user.getUser() == null) return "redirect:/login";

        try {
            List<FileNameUtil> uploadedFiles = fileUtil.uploadFile(boardRequest.getImageFiles());

            if (uploadedFiles != null && !uploadedFiles.isEmpty()) {
                List<String> fileNames = uploadedFiles.stream()
                        .map(FileNameUtil::getNewFileName)
                        .collect(Collectors.toList());
                boardRequest.setImageUrls(fileNames);
            }

            BoardResponse response = boardService.createBoard(user.getUser().getId(), boardRequest);
            redirectAttributes.addFlashAttribute("message", "게시글이 작성되었습니다.");
            return "redirect:/community/boards/" + response.getId();
        } catch (Exception e) {
            log.error("게시글 작성 중 오류 발생: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "게시글 작성에 실패했습니다.");
            return "redirect:/community/boards/write";
        }
    }

    /**
     * 게시글 상세 보기 (수정됨: 작성자 본인 제외 로직 + 중복 방지)
     */
    @GetMapping("/boards/{id}")
    public String getBoard(@PathVariable Long id, Model model,
                           @AuthenticationPrincipal UserSecurityDTO user,
                           HttpServletRequest request,
                           HttpServletResponse response) { // response는 안쓰지만 유지
        try {
            if (id == null || id <= 0) return "redirect:/community?error=invalid_id";

            // 1. 게시글 데이터 가져오기 (조회수 증가 X, 순수 데이터)
            BoardResponse board = boardService.getBoard(id);

            // 2. 로그인 유저 정보 및 작성자 여부 확인 (화면 표시용)
            boolean isAuthor = false;
            if (user != null && user.getUser() != null) {
                model.addAttribute("nickname", user.getUser().getNickname());
                model.addAttribute("memberId", user.getUser().getId());

                isAuthor = board.getMemberId().equals(user.getUser().getId());
                board.setIsAuthor(isAuthor);

                try {
                    board.setLiked(boardService.isLiked(id, user.getUser().getId()));
                } catch (Exception e) {
                    // 좋아요 확인 실패 시 무시
                }
            } else {
                board.setIsAuthor(false);
            }

            // 3. 이미지 URL 처리
            if (board.getImageUrls() != null && board.getImages() == null) {
                board.setImages(board.getImageUrls());
            }

            // ============================================================
            // 4. [수정됨] 조회수 무조건 증가 로직 (쿠키 X, 작성자 체크 X)
            // ============================================================
            boardService.increaseViewCount(id); // DB 조회수 +1
            board.setViewCount(board.getViewCount() + 1); // 화면 표시용 조회수 +1
            // ============================================================

            model.addAttribute("board", board);

            return "view";
        } catch (Exception e) {
            log.error("게시글 상세 조회 중 오류: ", e);
            return "redirect:/community?error=not_found";
        }
    }

    @GetMapping("/boards/{boardId}/edit")
    public String editForm(@PathVariable Long boardId, @AuthenticationPrincipal UserSecurityDTO user, Model model, HttpServletRequest request) {
        if (user == null || user.getUser() == null) return "redirect:/login";

        try {
            BoardResponse board = boardService.getBoard(boardId);
            if (!board.getMemberId().equals(user.getUser().getId())) return "redirect:/community/boards/" + boardId;

            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null) model.addAttribute("_csrf", csrfToken);

            model.addAttribute("nickname", user.getUser().getNickname());
            model.addAttribute("memberId", user.getUser().getId());

            model.addAttribute("requestURI", request.getRequestURI());
            model.addAttribute("board", board);
            model.addAttribute("boardUpdateRequest", new BoardUpdateRequest());
            return "edit";
        } catch (Exception e) {
            return "redirect:/community/boards/" + boardId;
        }
    }

    @PostMapping("/boards/{boardId}/edit")
    public String updateBoard(@AuthenticationPrincipal UserSecurityDTO user, @PathVariable Long boardId, @ModelAttribute("boardUpdateRequest") BoardUpdateRequest updateRequest, RedirectAttributes redirectAttributes) {
        if (user == null || user.getUser() == null) return "redirect:/login";
        try {
            BoardResponse response = boardService.updateBoard(boardId, user.getUser().getId(), updateRequest);
            redirectAttributes.addFlashAttribute("message", "게시글이 수정되었습니다.");
            return "redirect:/community/boards/" + response.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "게시글 수정에 실패했습니다.");
            return "redirect:/community/boards/" + boardId + "/edit";
        }
    }

    @PostMapping("/boards/{boardId}/delete")
    public String deleteBoard(@AuthenticationPrincipal UserSecurityDTO user, @PathVariable Long boardId, RedirectAttributes redirectAttributes) {
        if (user == null || user.getUser() == null) return "redirect:/login";
        try {
            boardService.deleteBoard(boardId, user.getUser().getId());
            redirectAttributes.addFlashAttribute("message", "게시글이 삭제되었습니다.");
            return "redirect:/community";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "게시글 삭제에 실패했습니다.");
            return "redirect:/community/boards/" + boardId;
        }
    }

    @GetMapping({"/search", "/boards/search"})
    public String searchBoards(@RequestParam String keyword, @PageableDefault(size = 15) Pageable pageable, Model model, HttpServletRequest request) {
        try {
            Page<BoardResponse> boardPage = boardService.searchBoards(keyword, pageable);
            model.addAttribute("boards", boardPage);
            model.addAttribute("keyword", keyword);
            model.addAttribute("requestURI", request.getRequestURI());
            return "community";
        } catch (Exception e) {
            return "redirect:/community?error=search_error";
        }
    }

    @PostMapping("/boards/{boardId}/like")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> likeBoard(@PathVariable Long boardId, @AuthenticationPrincipal UserSecurityDTO user) {
        if (user == null || user.getUser() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "로그인 필요"));
        try {
            boolean liked = boardService.toggleLike(boardId, user.getUser().getId());
            int likeCount = boardService.getLikeCount(boardId);
            return ResponseEntity.ok(Map.of("success", true, "liked", liked, "likeCount", likeCount));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false));
        }
    }

    @PostMapping("/boards/{boardId}/view")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> increaseViewCount(@PathVariable Long boardId) {
        try {
            boardService.increaseViewCount(boardId);
            BoardResponse board = boardService.getBoard(boardId);
            return ResponseEntity.ok(Map.of("success", true, "viewCount", board.getViewCount()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false));
        }
    }

    @GetMapping("/my-posts")
    public String getMyPosts(@PageableDefault(size = 15) Pageable pageable, Model model, HttpServletRequest request, @AuthenticationPrincipal UserSecurityDTO user) {
        if (user == null) return "redirect:/login";
        model.addAttribute("boards", boardService.getMyBoards(user.getUser().getId(), pageable));
        model.addAttribute("nickname", user.getUser().getNickname());
        model.addAttribute("requestURI", request.getRequestURI());
        return "my-posts";
    }

    @GetMapping("/category/{category}")
    public String getBoardsByCategory(@PathVariable String category, @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable, Model model, HttpServletRequest request, @AuthenticationPrincipal UserSecurityDTO user) {

        if (user != null && user.getUser() != null) {
            model.addAttribute("nickname", user.getUser().getNickname());
        }

        model.addAttribute("boards", boardService.getBoardsByCategory(category, pageable));
        model.addAttribute("category", category);
        model.addAttribute("requestURI", request.getRequestURI());
        return "community";
    }
}