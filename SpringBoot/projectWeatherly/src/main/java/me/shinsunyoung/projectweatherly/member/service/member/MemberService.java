package me.shinsunyoung.projectweatherly.member.service.member;





import me.shinsunyoung.projectweatherly.member.RequestDto.MemberJoinRequest;
import me.shinsunyoung.projectweatherly.member.UserResponseDto.MemberResponseDto;
import me.shinsunyoung.projectweatherly.member.domain.member.Member;
import me.shinsunyoung.projectweatherly.member.dto.user.MemberUpdateRequest;

import java.util.List;

public interface MemberService {

    // 회원 가입
    MemberResponseDto register(MemberJoinRequest request);

    // 회원 조회
    MemberResponseDto getMemberById(Long memberId);
    MemberResponseDto getMemberByEmail(String email);

    // 회원 정보 수정
    MemberResponseDto updateMember(Long memberId, MemberUpdateRequest request);

    // 비밀번호 변경 (현재 비밀번호 확인 방식)
    void changePassword(Long memberId, String currentPassword, String newPassword);

    // 프로필 이미지 업데이트
    MemberResponseDto updateProfileImage(Long memberId, String imageUrl);

    // 회원 탈퇴 (비활성화)
    void deactivateMember(Long memberId);

    // 회원 활성화
    void activateMember(Long memberId);

    // 모든 회원 조회 (관리자용)
    List<MemberResponseDto> getAllMembers();

    // 역할별 회원 조회 (관리자용)
    List<MemberResponseDto> getMembersByRole(String role);

    // 엔티티 조a회 (내부용)
    Member getMemberEntityById(Long memberId);
    Member getMemberEntityByEmail(String email);

    // 이메일 중복 체크
    boolean isEmailDuplicate(String email);

    // 회원 검색 (관리자용)
    List<MemberResponseDto> searchMembers(String keyword);
}