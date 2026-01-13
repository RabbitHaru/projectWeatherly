package me.shinsunyoung.projectweatherly.member.domain.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "board_posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member author;

    private Integer viewCount = 0;
    private Integer likeCount = 0;
    private Integer commentCount = 0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}