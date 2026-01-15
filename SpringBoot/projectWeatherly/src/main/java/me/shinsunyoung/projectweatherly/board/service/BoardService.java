package me.shinsunyoung.projectweatherly.board.service;

import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.board.domain.entity.BoardImage;
import me.shinsunyoung.projectweatherly.board.domain.enums.BoardStatus;
import me.shinsunyoung.projectweatherly.board.domain.entity.Board;
import me.shinsunyoung.projectweatherly.board.dto.BoardImageResponse;
import me.shinsunyoung.projectweatherly.board.dto.BoardRequest;
import me.shinsunyoung.projectweatherly.board.dto.BoardResponse;
import me.shinsunyoung.projectweatherly.board.dto.BoardUpdateRequest;
import me.shinsunyoung.projectweatherly.board.repository.BoardImageRepository;
import me.shinsunyoung.projectweatherly.board.repository.BoardRepository;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
import me.shinsunyoung.projectweatherly.member.repository.MemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardImageRepository boardImageRepository;
    private final MemberRepository memberRepository;
    private final ImageUploadService imageUploadService;

    // 게시글 생성 (다중 이미지 업로드)
    public BoardResponse createBoard(Long memberId, BoardRequest request) throws IOException {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        Board board = Board.builder()
                .member(member)
                .title(request.getTitle())
                .content(request.getContent())
                .build();

        // 다중 이미지 업로드 및 저장
        if (request.getImageFiles() != null && !request.getImageFiles().isEmpty()) {
            int order = 1;
            for (MultipartFile file : request.getImageFiles()) {
                if (!file.isEmpty()) {
                    String imageUrl = imageUploadService.uploadImage(file);

                    BoardImage boardImage = BoardImage.builder()
                            .board(board)
                            .imageUrl(imageUrl)
                            .imageName(file.getOriginalFilename())
                            .imageSize(file.getSize())
                            .displayOrder(order++)
                            .isThumbnail(order == 2) // 첫 번째 이미지를 썸네일로 설정
                            .build();

                    board.addImage(boardImage);
                }
            }
        }

        // 기존 이미지 URL 처리 (URL만 있는 경우)
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            int order = board.getImages().size() + 1;
            for (String imageUrl : request.getImageUrls()) {
                if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                    BoardImage boardImage = BoardImage.builder()
                            .board(board)
                            .imageUrl(imageUrl)
                            .imageName("external_image")
                            .displayOrder(order++)
                            .isThumbnail(board.getImages().isEmpty() && order == 2)
                            .build();

                    board.addImage(boardImage);
                }
            }
        }

        Board savedBoard = boardRepository.save(board);
        return convertToResponse(savedBoard);
    }

    // 게시글 조회 (조회수 증가)
    @Transactional(readOnly = true)
    public BoardResponse getBoard(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        // 조회수 증가
        board.increaseViewCount();
        boardRepository.save(board);

        return convertToResponse(board);
    }

    // 게시글 목록 조회
    @Transactional(readOnly = true)
    public Page<BoardResponse> getAllBoards(Pageable pageable) {
        return boardRepository.findAll(pageable)
                .map(this::convertToResponse);
    }

    // 게시글 수정 (다중 이미지 처리)
    public BoardResponse updateBoard(Long boardId, Long memberId, BoardUpdateRequest request) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        // 작성자 확인
        if (!board.getMember().getId().equals(memberId)) {
            throw new RuntimeException("작성자만 수정할 수 있습니다.");
        }

        board.setTitle(request.getTitle());
        board.setContent(request.getContent());
        board.setWeatherCondition(request.getWeatherCondition());

        // 이미지 삭제 처리
        if (request.getImageIdsToDelete() != null && !request.getImageIdsToDelete().isEmpty()) {
            request.getImageIdsToDelete().forEach(imageId -> {
                boardImageRepository.deleteByBoardIdAndImageId(boardId, imageId);
            });
        }

        if (request.getBoardStatus() != null) {
            board.setBoardStatus(request.getBoardStatus());
        }

        if (request.getIsVerified() != null) {
            board.setIsVerified(request.getIsVerified());
        }

        Board updatedBoard = boardRepository.save(board);
        return convertToResponse(updatedBoard);
    }

    // 게시글에 이미지 추가
    public BoardResponse addImagesToBoard(Long boardId, Long memberId, List<MultipartFile> imageFiles) throws IOException {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        // 작성자 확인
        if (!board.getMember().getId().equals(memberId)) {
            throw new RuntimeException("작성자만 이미지를 추가할 수 있습니다.");
        }

        if (imageFiles != null && !imageFiles.isEmpty()) {
            // 현재 이미지 개수 확인
            int currentImageCount = board.getImages().size();
            int order = currentImageCount + 1;

            for (MultipartFile file : imageFiles) {
                if (!file.isEmpty()) {
                    String imageUrl = imageUploadService.uploadImage(file);

                    BoardImage boardImage = BoardImage.builder()
                            .board(board)
                            .imageUrl(imageUrl)
                            .imageName(file.getOriginalFilename())
                            .imageSize(file.getSize())
                            .displayOrder(order++)
                            .isThumbnail(currentImageCount == 0 && order == 2) // 첫 번째 이미지면 썸네일로 설정
                            .build();

                    board.addImage(boardImage);
                }
            }
        }

        Board updatedBoard = boardRepository.save(board);
        return convertToResponse(updatedBoard);
    }

    // 특정 이미지 삭제
    public void deleteBoardImage(Long boardId, Long memberId, Long imageId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        // 작성자 확인
        if (!board.getMember().getId().equals(memberId)) {
            throw new RuntimeException("작성자만 이미지를 삭제할 수 있습니다.");
        }

        boardImageRepository.deleteByBoardIdAndImageId(boardId, imageId);
    }

    // 게시글 삭제 (상태 변경)
    public void deleteBoard(Long boardId, Long memberId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        // 작성자 확인
        if (!board.getMember().getId().equals(memberId)) {
            throw new RuntimeException("작성자만 삭제할 수 있습니다.");
        }

        board.setBoardStatus(BoardStatus.DELETED);
        boardRepository.save(board);
    }

    // 게시글 검증
    public BoardResponse verifyBoard(Long boardId, Boolean isVerified) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        board.setIsVerified(isVerified);
        Board verifiedBoard = boardRepository.save(board);
        return convertToResponse(verifiedBoard);
    }

    // 좋아요 증가
    public BoardResponse likeBoard(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        board.increaseLikeCount();
        Board likedBoard = boardRepository.save(board);
        return convertToResponse(likedBoard);
    }

    // 검색 기능
    @Transactional(readOnly = true)
    public Page<BoardResponse> searchBoards(String keyword, Pageable pageable) {
        return boardRepository.searchByKeyword(keyword, pageable)
                .map(this::convertToResponse);
    }

    // 날씨 상태별 조회
    @Transactional(readOnly = true)
    public Page<BoardResponse> getBoardsByWeatherCondition(String weatherCondition, Pageable pageable) {
        return boardRepository.findByWeatherCondition(weatherCondition, pageable)
                .map(this::convertToResponse);
    }

    // 인기글 조회
    @Transactional(readOnly = true)
    public Page<BoardResponse> getPopularBoards(Pageable pageable) {
        return boardRepository.findAllByOrderByViewCountDesc(pageable)
                .map(this::convertToResponse);
    }

    // 최신글 조회
    @Transactional(readOnly = true)
    public Page<BoardResponse> getRecentBoards(Pageable pageable) {
        return boardRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::convertToResponse);
    }

    // DTO 변환 메서드
    private BoardResponse convertToResponse(Board board) {
        // 이미지 목록 변환
        List<BoardImageResponse> imageResponses = board.getImages().stream()
                .map(image -> BoardImageResponse.builder()
                        .imageId(image.getId())
                        .imageUrl(image.getImageUrl())
                        .imageName(image.getImageName())
                        .imageSize(image.getImageSize())
                        .displayOrder(image.getDisplayOrder())
                        .isThumbnail(image.getIsThumbnail())
                        .createdAt(image.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return BoardResponse.builder()
                .boardId(board.getId())
                .memberId(board.getMember().getId())
                .memberNickname(board.getMember().getNickname())
                .memberProfileImage(board.getMember().getProfileImage())
                .title(board.getTitle())
                .content(board.getContent())
                .weatherCondition(board.getWeatherCondition())
                .thumbnailUrl(board.getThumbnailUrl())
                .images(imageResponses)
                .viewCount(board.getViewCount())
                .likeCount(board.getLikeCount())
                .isVerified(board.getIsVerified())
                .boardStatus(board.getBoardStatus())
                .boardStatusDescription(board.getBoardStatus() != null ? board.getBoardStatus().getDescription() : null)
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .build();
    }
}