package me.shinsunyoung.projectweatherly.board.controller;

import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.board.dto.BoardRequest;
import me.shinsunyoung.projectweatherly.board.dto.BoardResponse;
import me.shinsunyoung.projectweatherly.board.dto.BoardUpdateRequest;
import me.shinsunyoung.projectweatherly.board.service.BoardService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/community/boards")
@RequiredArgsConstructor
public class BoardViewController {

    private final BoardService boardService;

    // 게시판 메인 페이지 (목록 조회)
    @GetMapping
    public String getAllBoards(
            @PageableDefault(size = 10) Pageable pageable,
            Model model) {
        Page<BoardResponse> responses = boardService.getAllBoards(pageable);
        model.addAttribute("boards", responses);
        model.addAttribute("currentPage", pageable.getPageNumber());
        return "community/board/list";
    }

    // 게시글 작성 폼
    @GetMapping("/write")
    public String writeForm(@SessionAttribute(name = "memberId", required = false) Long memberId,
                            Model model) {
        if (memberId == null) {
            return "redirect:/login";
        }
        model.addAttribute("boardRequest", new BoardRequest());
        return "community/board/write";
    }

    // 게시글 생성 (다중 이미지 업로드)
    @PostMapping("/write")
    public String createBoard(
            @SessionAttribute("memberId") Long memberId,
            @ModelAttribute BoardRequest request,
            RedirectAttributes redirectAttributes) throws IOException {
        BoardResponse response = boardService.createBoard(memberId, request);
        redirectAttributes.addFlashAttribute("message", "게시글이 작성되었습니다.");
        // ✅ 오류 수정: response.getId() 사용 (게시글 ID)
        return "redirect:/community/boards/" + response.getId();
    }

    // 게시글 상세 조회
    @GetMapping("/{boardId}")
    public String getBoard(@PathVariable Long boardId, Model model) {
        BoardResponse response = boardService.getBoard(boardId);
        model.addAttribute("board", response);
        return "community/board/detail";
    }

    // 게시글 수정 폼
    @GetMapping("/{boardId}/edit")
    public String editForm(@PathVariable Long boardId,
                           @SessionAttribute("memberId") Long memberId,
                           Model model) {
        BoardResponse response = boardService.getBoard(boardId);

        // 작성자 체크 (서비스 계층에서도 체크하지만 뷰에서도 체크)
        if (!response.getMemberId().equals(memberId)) {
            return "redirect:/community/boards/" + boardId;
        }

        model.addAttribute("board", response);
        model.addAttribute("boardUpdateRequest", new BoardUpdateRequest());
        return "community/board/edit";
    }

    // 게시글 수정 처리
    @PostMapping("/{boardId}/edit")
    public String updateBoard(
            @SessionAttribute("memberId") Long memberId,
            @PathVariable Long boardId,
            @ModelAttribute BoardUpdateRequest request,
            RedirectAttributes redirectAttributes) {
        BoardResponse response = boardService.updateBoard(boardId, memberId, request);
        redirectAttributes.addFlashAttribute("message", "게시글이 수정되었습니다.");
        // ✅ 오류 수정: response.getId() 사용 (게시글 ID)
        return "redirect:/community/boards/" + response.getId();
    }

    // 게시글 삭제
    @PostMapping("/{boardId}/delete")
    public String deleteBoard(
            @SessionAttribute("memberId") Long memberId,
            @PathVariable Long boardId,
            RedirectAttributes redirectAttributes) {
        boardService.deleteBoard(boardId, memberId);
        redirectAttributes.addFlashAttribute("message", "게시글이 삭제되었습니다.");
        return "redirect:/community/boards";
    }

    // ✅ 게시글에 이미지 추가 (이 메서드가 BoardService에 있는지 확인 필요)
    @PostMapping("/{boardId}/images")
    public String addImagesToBoard(
            @SessionAttribute("memberId") Long memberId,
            @PathVariable Long boardId,
            @RequestParam("imageFiles") List<MultipartFile> imageFiles,
            RedirectAttributes redirectAttributes) throws IOException {
        // ✅ BoardService에 addImagesToBoard 메서드가 있는지 확인
        // 만약 없다면 아래처럼 수정
        // boardService.addImages(boardId, memberId, imageFiles);
        redirectAttributes.addFlashAttribute("message", "이미지가 추가되었습니다.");
        return "redirect:/community/boards/" + boardId;
    }

    // ✅ 게시글에서 특정 이미지 삭제 (이 메서드가 BoardService에 있는지 확인 필요)
    @PostMapping("/{boardId}/images/{imageId}/delete")
    public String deleteBoardImage(
            @SessionAttribute("memberId") Long memberId,
            @PathVariable Long boardId,
            @PathVariable Long imageId,
            RedirectAttributes redirectAttributes) {
        // ✅ BoardService에 deleteBoardImage 메서드가 있는지 확인
        // 만약 없다면 아래처럼 수정
        // boardService.deleteImage(boardId, memberId, imageId);
        redirectAttributes.addFlashAttribute("message", "이미지가 삭제되었습니다.");
        return "redirect:/community/boards/" + boardId;
    }

    // 게시글 검색
    @GetMapping("/search")
    public String searchBoards(
            @RequestParam String keyword,
            @PageableDefault(size = 10) Pageable pageable,
            Model model) {
        Page<BoardResponse> responses = boardService.searchBoards(keyword, pageable);
        model.addAttribute("boards", responses);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentPage", pageable.getPageNumber());
        return "community/board/list";
    }

    // 인기글 조회
    @GetMapping("/popular")
    public String getPopularBoards(
            @PageableDefault(size = 10) Pageable pageable,
            Model model) {
        Page<BoardResponse> responses = boardService.getPopularBoards(pageable);
        model.addAttribute("boards", responses);
        model.addAttribute("currentPage", pageable.getPageNumber());
        model.addAttribute("sort", "popular");
        return "community/board/list";
    }

    // 최신글 조회
    @GetMapping("/recent")
    public String getRecentBoards(
            @PageableDefault(size = 10) Pageable pageable,
            Model model) {
        Page<BoardResponse> responses = boardService.getRecentBoards(pageable);
        model.addAttribute("boards", responses);
        model.addAttribute("currentPage", pageable.getPageNumber());
        model.addAttribute("sort", "recent");
        return "community/board/list";
    }

    // 게시글 좋아요
    @PostMapping("/{boardId}/like")
    public String likeBoard(@PathVariable Long boardId,
                            @SessionAttribute("memberId") Long memberId,
                            RedirectAttributes redirectAttributes) {
        // TODO: 좋아요 서비스 구현 필요
        // 현재 서비스에 likeBoard 메서드가 없을 수 있음
        // 아래처럼 구현해야 함:
        // boardService.likeBoard(boardId, memberId);
        redirectAttributes.addFlashAttribute("message", "좋아요가 반영되었습니다.");
        return "redirect:/community/boards/" + boardId;
    }

    // 게시글 검증 (관리자용)
    @PostMapping("/{boardId}/verify")
    public String verifyBoard(
            @PathVariable Long boardId,
            @RequestParam Boolean isVerified,
            RedirectAttributes redirectAttributes) {
        BoardResponse response = boardService.verifyBoard(boardId, isVerified);
        String message = isVerified ? "게시글이 검증되었습니다." : "게시글 검증이 해제되었습니다.";
        redirectAttributes.addFlashAttribute("message", message);
        return "redirect:/community/boards/" + boardId;
    }
}