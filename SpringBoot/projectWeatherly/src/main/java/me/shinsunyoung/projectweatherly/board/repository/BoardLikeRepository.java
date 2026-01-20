package me.shinsunyoung.projectweatherly.board.repository;


import me.shinsunyoung.projectweatherly.board.domain.entity.Board;
import me.shinsunyoung.projectweatherly.board.domain.entity.BoardLike;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BoardLikeRepository extends JpaRepository<BoardLike, Long> {
    Optional<BoardLike> findByBoardAndMember(Board board, Member member);
    boolean existsByBoardAndMember(Board board, Member member);
    int countByBoard(Board board);
}