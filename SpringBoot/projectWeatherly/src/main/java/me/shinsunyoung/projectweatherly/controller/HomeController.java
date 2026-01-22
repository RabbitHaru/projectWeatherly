package me.shinsunyoung.projectweatherly.controller;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.board.dto.BoardResponse;
import me.shinsunyoung.projectweatherly.board.service.BoardService;
import me.shinsunyoung.projectweatherly.member.dto.UserSecurityDTO;
import org.springframework.beans.factory.annotation.Value; // [필수] import 추가

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;


@Controller
@RequiredArgsConstructor // final 필드 자동 주입
public class HomeController {

    private final BoardService boardService;
    // [추가] application.properties에서 키 가져오기
    @Value("${weatherly.kakao.map.key}")
    private String kakaoMapKey;

    @GetMapping({"/", "/index.html"})
    public String home(Model model, HttpServletRequest request, @AuthenticationPrincipal UserSecurityDTO user) {
        model.addAttribute("requestURI", request.getRequestURI());

        // [추가] HTML로 키 전달
        model.addAttribute("kakaoMapKey", kakaoMapKey);

        if(user != null && user.getUser().getNickname() != null) {
            model.addAttribute("nickname", user.getUser().getNickname());
        }
// ==================================================================
        // ★ [수정됨] 일주일 내 작성된 글 중 조회수 상위 2개 가져오기
        // ==================================================================
        List<BoardResponse> popularPosts = boardService.getWeeklyPopularBoards(2);

        model.addAttribute("popularPosts", popularPosts);


        return "index"; // templates/index.html을 반환
    }

    // 미세먼지 페이지 매핑
    @GetMapping("/fine-dust")
    public String fineDust(Model model, HttpServletRequest request, @AuthenticationPrincipal UserSecurityDTO user) {
        model.addAttribute("requestURI", request.getRequestURI());

        // [추가] 여기도 지도 쓸 수 있으니 키 전달
        model.addAttribute("kakaoMapKey", kakaoMapKey);

        if(user != null && user.getUser().getNickname() != null) {
            model.addAttribute("nickname", user.getUser().getNickname());
        }
        return "fine-dust";
    }
}