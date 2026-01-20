package me.shinsunyoung.projectweatherly.board.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import me.shinsunyoung.projectweatherly.board.domain.entity.Board;
import me.shinsunyoung.projectweatherly.member.domain.entity.Member;


@Entity
@Table(name = "board_likes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"board_id", "member_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BoardLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, cascade =  CascadeType.ALL)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
}