package me.shinsunyoung.projectweatherly.board.service;

import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.board.service.BoardService;
import me.shinsunyoung.projectweatherly.member.dto.response.CommunityPostResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityService {  // ✅ 이름이 CommunityService로 확인

    private final BoardService boardService;

    // 내가 작성한 게시물 조회
    public List<CommunityPostResponse> getMyPosts(Long memberId) {
        // BoardService의 새 메서드 사용
        var boardResponses = boardService.getMyBoardsSimple(memberId);

        return boardResponses.stream()
                .map(board -> new CommunityPostResponse(
                        board.getId(),
                        board.getTitle(),
                        board.getContent(),
                        board.getViewCount(),
                        board.getLikeCount(),
                        board.getCreatedAt(),
                        board.getUpdatedAt()
                ))
                .collect(Collectors.toList());
    }

    // 게시물 삭제 (내 게시물만)
    @Transactional
    public void deleteMyPost(Long memberId, Long boardId) {
        boardService.deleteBoard(boardId, memberId);
    }

    // 내 게시물 수 가져오기
    public int getPostCountByMemberId(Long memberId) {
        return (int) boardService.countByMemberId(memberId);
    }

    // 게시물 존재 여부 확인
    public boolean existsPost(Long boardId, Long memberId) {
        // 간단한 구현: 내 게시물 목록에서 찾기
        // 실제로는 BoardRepository를 직접 호출하는 것이 효율적
        var myPosts = getMyPosts(memberId);
        return myPosts.stream().anyMatch(post -> post.getPostId().equals(boardId));
    }
}