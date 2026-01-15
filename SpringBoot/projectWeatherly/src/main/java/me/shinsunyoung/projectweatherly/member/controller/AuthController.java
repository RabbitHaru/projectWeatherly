package me.shinsunyoung.projectweatherly.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.shinsunyoung.projectweatherly.member.dto.request.LoginRequest;
import me.shinsunyoung.projectweatherly.member.dto.request.SignupRequest;
import me.shinsunyoung.projectweatherly.member.dto.response.ApiResponse2;
import me.shinsunyoung.projectweatherly.member.dto.response.LoginResponse;
import me.shinsunyoung.projectweatherly.member.service.MemberService;
import me.shinsunyoung.projectweatherly.util.FileNameUtil;
import me.shinsunyoung.projectweatherly.util.FileUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "인증 API", description = "회원가입, 로그인, 로그아웃 관련 API")
public class AuthController {

    private final MemberService memberService;
    private final FileUtil fileUtil;
    @PostMapping(value = "/signup",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "회원가입", description = "새로운 회원을 등록합니다.")
    public ResponseEntity<ApiResponse2<Long>> signup(
            @ModelAttribute SignupRequest request) {
        List<FileNameUtil> fileNames = fileUtil.uploadFile(request.getProfileImage());
        String profileImg = fileNames != null && !fileNames.isEmpty() ? fileNames.get(0).getNewFileName() : "default.png";
        Long memberId = memberService.signup(request,profileImg);


        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse2.success("회원가입이 완료되었습니다.", memberId));
    }

    @PostMapping("/login")
    @Operation(summary = "로그인",
            description = "이메일과 비밀번호로 로그인합니다. 로그인 성공 시 세션을 생성합니다.")
    public ResponseEntity<ApiResponse2<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        // Spring Security가 로그인을 처리하므로, 여기서는 추가적인 로그인 응답 정보만 반환
        LoginResponse response = memberService.login(request);

        return ResponseEntity.ok(ApiResponse2.success("로그인되었습니다.", response));
    }


}