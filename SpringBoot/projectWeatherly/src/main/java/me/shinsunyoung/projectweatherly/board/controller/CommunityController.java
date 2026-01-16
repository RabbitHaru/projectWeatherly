package me.shinsunyoung.projectweatherly.board.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.shinsunyoung.projectweatherly.board.dto.BoardRequest;
import me.shinsunyoung.projectweatherly.board.dto.BoardResponse;
import me.shinsunyoung.projectweatherly.board.dto.BoardUpdateRequest;
import me.shinsunyoung.projectweatherly.board.service.BoardService;
import me.shinsunyoung.projectweatherly.member.dto.UserSecurityDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/community")
@RequiredArgsConstructor
public class CommunityController {

    private final BoardService boardService;

    /**
     * 커뮤니티 메인 페이지 (게시판 목록)
     */
    @GetMapping({"", "/"})
    public String community(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sort", defaultValue = "latest") String sort,
            Model model,
            HttpServletRequest request,
            @AuthenticationPrincipal UserSecurityDTO user) {

        try {
            log.info("커뮤니티 페이지 접속 - category: {}, keyword: {}, sort: {}",
                    category, keyword, sort);

            // 기본값 설정
            model.addAttribute("requestURI", request.getRequestURI());

            // 사용자 정보 추가
            if (user != null && user.getUser() != null) {
                if (user.getUser().getNickname() != null) {
                    model.addAttribute("nickname", user.getUser().getNickname());
                }
                if (user.getUser().getId() != null) {
                    model.addAttribute("memberId", user.getUser().getId());
                }
            }

            // 게시글 목록 조회
            Page<BoardResponse> boardPage;

            // 검색어가 있으면 검색, 없으면 정렬 방식에 따라 조회
            if (keyword != null && !keyword.trim().isEmpty()) {
                boardPage = boardService.searchBoards(keyword, pageable);
            } else {
                switch (sort) {
                    case "popular":
                        boardPage = boardService.getPopularBoards(pageable);
                        break;
                    case "recent":
                        boardPage = boardService.getRecentBoards(pageable);
                        break;
                    case "latest":
                    default:
                        boardPage = boardService.getAllBoards(pageable);
                        break;
                }
            }

            // 인기 게시글 (상위 5개)
            List<BoardResponse> popularBoards = new ArrayList<>();
            try {
                Page<BoardResponse> popularPage = boardService.getPopularBoards(
                        org.springframework.data.domain.PageRequest.of(0, 5, Sort.by("viewCount").descending())
                );
                popularBoards = popularPage.getContent();
            } catch (Exception e) {
                log.warn("인기 게시글 조회 실패: {}", e.getMessage());
            }

            // 모델에 데이터 추가
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
            return "error";
        }
    }

    /**
     * 게시글 상세 보기
     */
    @GetMapping("/boards/{id}")
    public String getBoard(@PathVariable Long id, Model model,
                           @AuthenticationPrincipal UserSecurityDTO user) {
        try {
            BoardResponse board = boardService.getBoard(id);

            // ✅ images 필드를 명시적으로 설정
            if (board.getImageUrls() != null && board.getImages() == null) {
                board.setImages(board.getImageUrls());
            }

            // ✅ 현재 로그인한 사용자가 작성자인지 확인
            if (user != null && user.getUser() != null) {
                board.setIsAuthor(board.getMemberId().equals(user.getUser().getId()));
            } else {
                board.setIsAuthor(false);
            }

            model.addAttribute("board", board);
            return "view";
        } catch (Exception e) {
            log.error("게시글 상세 조회 중 오류 발생: ", e);
            model.addAttribute("errorMessage", "게시글을 불러오는데 실패했습니다.");
            return "error";
        }
    }

    /**
     * 게시글 작성 폼
     */
    @GetMapping("/boards/write")
    public String writeForm(
            @RequestParam(value = "category", defaultValue = "general") String category,
            Model model,
            @AuthenticationPrincipal UserSecurityDTO user) {

        // 로그인 체크
        if (user == null || user.getUser() == null) {
            return "redirect:/login?redirect=/community/boards/write";
        }

        log.info("게시글 작성 폼 - userId: {}, category: {}", user.getUser().getId(), category);

        model.addAttribute("category", category);
        model.addAttribute("nickname", user.getUser().getNickname());
        model.addAttribute("memberId", user.getUser().getId());

        // 빈 DTO 객체 생성
        BoardRequest boardRequest = new BoardRequest();
        model.addAttribute("boardRequest", boardRequest);

        return "write";
    }

    /**
     * 게시글 작성 처리
     */
    @PostMapping("/boards/write")
    public String createBoard(
            @AuthenticationPrincipal UserSecurityDTO user,
            @ModelAttribute BoardRequest request,
            RedirectAttributes redirectAttributes) throws IOException {

        if (user == null || user.getUser() == null) {
            return "redirect:/login";
        }

        try {
            log.info("게시글 작성 - userId: {}, title: {}", user.getUser().getId(), request.getTitle());

            // 게시글 저장
            BoardResponse response = boardService.createBoard(user.getUser().getId(), request);
            redirectAttributes.addFlashAttribute("message", "게시글이 작성되었습니다.");

            return "redirect:/community/boards/" + response.getId();

        } catch (Exception e) {
            log.error("게시글 작성 중 오류 발생: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "게시글 작성에 실패했습니다.");
            return "redirect:/community/boards/write";
        }
    }

    /**
     * 게시글 수정 폼
     */
    @GetMapping("/boards/{boardId}/edit")
    public String editForm(
            @PathVariable Long boardId,
            @AuthenticationPrincipal UserSecurityDTO user,
            Model model) {

        if (user == null || user.getUser() == null) {
            return "redirect:/login";
        }

        try {
            log.info("게시글 수정 폼 - boardId: {}, userId: {}", boardId, user.getUser().getId());

            // 게시글 조회
            BoardResponse board = boardService.getBoard(boardId);

            // 권한 체크 (작성자만 수정 가능)
            if (!board.getMemberId().equals(user.getUser().getId())) {
                return "redirect:/community/boards/" + boardId;
            }

            model.addAttribute("board", board);
            model.addAttribute("boardUpdateRequest", new BoardUpdateRequest());
            model.addAttribute("nickname", user.getUser().getNickname());

            return "edit";

        } catch (Exception e) {
            log.error("게시글 수정 폼 로딩 중 오류 발생: ", e);
            return "redirect:/community/boards/" + boardId;
        }
    }

    /**
     * 게시글 수정 처리
     */
    @PostMapping("/boards/{boardId}/edit")
    public String updateBoard(
            @AuthenticationPrincipal UserSecurityDTO user,
            @PathVariable Long boardId,
            @ModelAttribute BoardUpdateRequest request,
            RedirectAttributes redirectAttributes) {

        if (user == null || user.getUser() == null) {
            return "redirect:/login";
        }

        try {
            log.info("게시글 수정 - boardId: {}, userId: {}", boardId, user.getUser().getId());

            // 게시글 수정
            BoardResponse response = boardService.updateBoard(boardId, user.getUser().getId(), request);
            redirectAttributes.addFlashAttribute("message", "게시글이 수정되었습니다.");

            return "redirect:/community/boards/" + response.getId();

        } catch (Exception e) {
            log.error("게시글 수정 중 오류 발생: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "게시글 수정에 실패했습니다.");
            return "redirect:/community/boards/" + boardId + "/edit";
        }
    }

    /**
     * 게시글 삭제
     */
    @PostMapping("/boards/{boardId}/delete")
    public String deleteBoard(
            @AuthenticationPrincipal UserSecurityDTO user,
            @PathVariable Long boardId,
            RedirectAttributes redirectAttributes) {

        if (user == null || user.getUser() == null) {
            return "redirect:/login";
        }

        try {
            log.info("게시글 삭제 - boardId: {}, userId: {}", boardId, user.getUser().getId());

            // 게시글 삭제
            boardService.deleteBoard(boardId, user.getUser().getId());
            redirectAttributes.addFlashAttribute("message", "게시글이 삭제되었습니다.");

            return "redirect:/community";

        } catch (Exception e) {
            log.error("게시글 삭제 중 오류 발생: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "게시글 삭제에 실패했습니다.");
            return "redirect:/community/boards/" + boardId;
        }
    }

    /**
     * 게시글 검색
     */
    @GetMapping("/search")
    public String searchBoards(
            @RequestParam String keyword,
            @PageableDefault(size = 15) Pageable pageable,
            Model model,
            HttpServletRequest request,
            @AuthenticationPrincipal UserSecurityDTO user) {

        try {
            log.info("게시글 검색 - keyword: {}", keyword);

            // 게시글 검색
            Page<BoardResponse> boardPage = boardService.searchBoards(keyword, pageable);

            model.addAttribute("boards", boardPage);
            model.addAttribute("keyword", keyword);
            model.addAttribute("currentPage", pageable.getPageNumber());
            model.addAttribute("requestURI", request.getRequestURI());

            // 사용자 정보 추가
            if (user != null && user.getUser() != null && user.getUser().getNickname() != null) {
                model.addAttribute("nickname", user.getUser().getNickname());
            }

            return "community";

        } catch (Exception e) {
            log.error("게시글 검색 중 오류 발생: ", e);
            return "redirect:/community?error=검색 중 오류가 발생했습니다";
        }
    }

    /**
     * 게시글 좋아요 처리 (AJAX 용)
     */
    @PostMapping("/boards/{boardId}/like")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> likeBoard(
            @PathVariable Long boardId,
            @AuthenticationPrincipal UserSecurityDTO user) {

        Map<String, Object> response = new HashMap<>();

        if (user == null || user.getUser() == null) {
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            response.put("errorCode", "UNAUTHORIZED");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        try {
            // TODO: 좋아요 서비스 구현 필요
            response.put("success", true);
            response.put("liked", true);
            response.put("likeCount", 1);
            response.put("message", "좋아요가 반영되었습니다.");

            log.info("게시글 좋아요 - boardId: {}, userId: {}", boardId, user.getUser().getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("좋아요 처리 중 오류 발생: ", e);
            response.put("success", false);
            response.put("message", "좋아요 처리에 실패했습니다.");
            response.put("errorCode", "LIKE_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 내가 쓴 글 조회
     */
    @GetMapping("/my-posts")
    public String getMyPosts(
            @PageableDefault(size = 15) Pageable pageable,
            Model model,
            HttpServletRequest request,
            @AuthenticationPrincipal UserSecurityDTO user) {

        if (user == null || user.getUser() == null) {
            return "redirect:/login?redirect=/community/my-posts";
        }

        try {
            log.info("내가 쓴 글 조회 - userId: {}", user.getUser().getId());

            // TODO: BoardService에 사용자별 게시글 조회 메서드 추가 필요
            Page<BoardResponse> boardPage = boardService.getAllBoards(pageable); // 임시

            model.addAttribute("boards", boardPage);
            model.addAttribute("currentPage", pageable.getPageNumber());
            model.addAttribute("requestURI", request.getRequestURI());
            model.addAttribute("nickname", user.getUser().getNickname());
            model.addAttribute("memberId", user.getUser().getId());

            return "my-posts";

        } catch (Exception e) {
            log.error("내가 쓴 글 조회 중 오류 발생: ", e);
            return "redirect:/community";
        }
    }

    /**
     * 카테고리별 게시글 조회
     */
    @GetMapping("/category/{category}")
    public String getBoardsByCategory(
            @PathVariable String category,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Model model,
            HttpServletRequest request,
            @AuthenticationPrincipal UserSecurityDTO user) {

        try {
            log.info("카테고리별 게시글 조회 - category: {}", category);

            // TODO: BoardService에 카테고리별 게시글 조회 메서드 추가 필요
            Page<BoardResponse> boardPage = boardService.getAllBoards(pageable); // 임시

            model.addAttribute("boards", boardPage);
            model.addAttribute("category", category);
            model.addAttribute("currentPage", pageable.getPageNumber());
            model.addAttribute("requestURI", request.getRequestURI());

            // 사용자 정보 추가
            if (user != null && user.getUser() != null) {
                if (user.getUser().getNickname() != null) {
                    model.addAttribute("nickname", user.getUser().getNickname());
                }
                if (user.getUser().getId() != null) {
                    model.addAttribute("memberId", user.getUser().getId());
                }
            }

            return "community";

        } catch (Exception e) {
            log.error("카테고리별 게시글 조회 중 오류 발생: ", e);
            return "redirect:/community";
        }
    }
}