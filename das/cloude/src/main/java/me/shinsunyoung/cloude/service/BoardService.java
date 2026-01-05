package me.shinsunyoung.cloude.service;

import me.shinsunyoung.cloude.dto.BoardPostDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BoardService {

    public List<BoardPostDto> getRecentPosts() {
        return List.of(
                new BoardPostDto(1L, "서울 오늘 눈 왔어요", "user1"),
                new BoardPostDto(2L, "부산은 비네요", "user2")
        );
    }
}
