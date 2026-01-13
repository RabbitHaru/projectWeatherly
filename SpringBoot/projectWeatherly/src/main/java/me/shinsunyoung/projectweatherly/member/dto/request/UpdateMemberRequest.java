package me.shinsunyoung.projectweatherly.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "회원 정보 수정 요청 DTO")
public class UpdateMemberRequest {

    @Schema(
            description = "닉네임",
            example = "날씨탐험가",
            minLength = 2,
            maxLength = 50,
            nullable = true
    )
    @Size(min = 2, max = 50, message = "닉네임은 2~50자 이내로 입력해주세요.")
    @Pattern(
            regexp = "^[a-zA-Z0-9가-힣\\s]*$",
            message = "닉네임은 한글, 영문, 숫자, 공백만 사용 가능합니다."
    )
    private String nickname;

    @Schema(
            description = "프로필 이미지 URL",
            example = "https://example.com/profile.jpg",
            nullable = true
    )
    @Pattern(
            regexp = "^(https?://.*\\.(jpg|jpeg|png|gif|webp|bmp|svg))?$",
            message = "프로필 이미지는 JPG, PNG, GIF, WebP, BMP, SVG 형식의 URL이어야 합니다."
    )
    private String profileImage;

    @Schema(
            description = "자기소개",
            example = "날씨와 함께하는 즐거운 일상을 기록합니다.",
            maxLength = 500,
            nullable = true
    )
    @Size(max = 500, message = "자기소개는 500자 이내로 입력해주세요.")
    private String bio;

    @Schema(
            description = "위치 정보 (도시명)",
            example = "서울특별시",
            maxLength = 100,
            nullable = true
    )
    @Size(max = 100, message = "위치 정보는 100자 이내로 입력해주세요.")
    private String location;

    @Schema(
            description = "웹사이트 URL",
            example = "https://blog.example.com",
            nullable = true
    )
    @Pattern(
            regexp = "^(https?://[\\w.-]+(\\.[\\w.-]+)+[/\\w ./-]*)?$",
            message = "올바른 웹사이트 URL 형식이 아닙니다."
    )
    private String website;

    @Schema(
            description = "연락처 (전화번호)",
            example = "010-1234-5678",
            nullable = true
    )
    @Pattern(
            regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$",
            message = "올바른 전화번호 형식이 아닙니다. (예: 010-1234-5678)"
    )
    private String phoneNumber;

    @Schema(
            description = "생년월일 (YYYY-MM-DD 형식)",
            example = "1990-01-01",
            nullable = true
    )
    @Pattern(
            regexp = "^\\d{4}-\\d{2}-\\d{2}$",
            message = "생년월일은 YYYY-MM-DD 형식이어야 합니다."
    )
    private String birthDate;

    @Schema(
            description = "성별",
            example = "M",
            allowableValues = {"M", "F", "O"},
            nullable = true
    )
    @Pattern(
            regexp = "^[MFO]$",
            message = "성별은 M(남성), F(여성), O(기타) 중 하나여야 합니다."
    )
    private String gender;

    @Schema(
            description = "공개 여부",
            example = "true",
            defaultValue = "true",
            nullable = true
    )
    private Boolean isPublic;

    @Schema(
            description = "이메일 수신 동의 여부",
            example = "true",
            defaultValue = "true",
            nullable = true
    )
    private Boolean emailNotificationAgree;

    @Schema(
            description = "SNS 수신 동의 여부",
            example = "true",
            defaultValue = "true",
            nullable = true
    )
    private Boolean smsNotificationAgree;

    // ==================== 유효성 검사 메서드 ====================

    /**
     * 모든 필드가 null인지 확인 (업데이트할 내용이 없는 경우)
     */
    public boolean isEmpty() {
        return nickname == null &&
                profileImage == null &&
                bio == null &&
                location == null &&
                website == null &&
                phoneNumber == null &&
                birthDate == null &&
                gender == null &&
                isPublic == null &&
                emailNotificationAgree == null &&
                smsNotificationAgree == null;
    }

    /**
     * 닉네임 변경 요청인지 확인
     */
    public boolean hasNicknameUpdate() {
        return nickname != null && !nickname.trim().isEmpty();
    }

    /**
     * 프로필 이미지 변경 요청인지 확인
     */
    public boolean hasProfileImageUpdate() {
        return profileImage != null && !profileImage.trim().isEmpty();
    }

    /**
     * 개인정보 관련 필드 업데이트가 있는지 확인
     */
    public boolean hasPersonalInfoUpdate() {
        return phoneNumber != null || birthDate != null || gender != null;
    }

    /**
     * 위치 정보 업데이트가 있는지 확인
     */
    public boolean hasLocationUpdate() {
        return location != null && !location.trim().isEmpty();
    }

    /**
     * 웹사이트 업데이트가 있는지 확인
     */
    public boolean hasWebsiteUpdate() {
        return website != null && !website.trim().isEmpty();
    }

    /**
     * 자기소개 업데이트가 있는지 확인
     */
    public boolean hasBioUpdate() {
        return bio != null && !bio.trim().isEmpty();
    }

    /**
     * 알림 설정 업데이트가 있는지 확인
     */
    public boolean hasNotificationUpdate() {
        return emailNotificationAgree != null || smsNotificationAgree != null;
    }

    /**
     * 공개 설정 업데이트가 있는지 확인
     */
    public boolean hasPrivacyUpdate() {
        return isPublic != null;
    }

    // ==================== 빌더 팩토리 메서드 ====================

    /**
     * 닉네임만 업데이트하는 요청 생성
     */
    public static UpdateMemberRequest forNickname(String nickname) {
        return UpdateMemberRequest.builder()
                .nickname(nickname)
                .build();
    }

    /**
     * 프로필 이미지만 업데이트하는 요청 생성
     */
    public static UpdateMemberRequest forProfileImage(String profileImage) {
        return UpdateMemberRequest.builder()
                .profileImage(profileImage)
                .build();
    }

    /**
     * 자기소개만 업데이트하는 요청 생성
     */
    public static UpdateMemberRequest forBio(String bio) {
        return UpdateMemberRequest.builder()
                .bio(bio)
                .build();
    }

    /**
     * 위치 정보만 업데이트하는 요청 생성
     */
    public static UpdateMemberRequest forLocation(String location) {
        return UpdateMemberRequest.builder()
                .location(location)
                .build();
    }

    /**
     * 전체 프로필 업데이트 요청 생성
     */
    public static UpdateMemberRequest forFullProfile(
            String nickname,
            String profileImage,
            String bio,
            String location,
            String website) {
        return UpdateMemberRequest.builder()
                .nickname(nickname)
                .profileImage(profileImage)
                .bio(bio)
                .location(location)
                .website(website)
                .build();
    }

    /**
     * 알림 설정 업데이트 요청 생성
     */
    public static UpdateMemberRequest forNotificationSettings(
            Boolean emailNotificationAgree,
            Boolean smsNotificationAgree) {
        return UpdateMemberRequest.builder()
                .emailNotificationAgree(emailNotificationAgree)
                .smsNotificationAgree(smsNotificationAgree)
                .build();
    }

    /**
     * 개인정보 업데이트 요청 생성
     */
    public static UpdateMemberRequest forPersonalInfo(
            String phoneNumber,
            String birthDate,
            String gender) {
        return UpdateMemberRequest.builder()
                .phoneNumber(phoneNumber)
                .birthDate(birthDate)
                .gender(gender)
                .build();
    }

    // ==================== 데이터 정제 메서드 ====================

    /**
     * 요청 데이터 정제 (공백 제거 등)
     */
    public void sanitize() {
        if (nickname != null) {
            nickname = nickname.trim();
        }
        if (bio != null) {
            bio = bio.trim();
        }
        if (location != null) {
            location = location.trim();
        }
        if (website != null) {
            website = website.trim();
        }
        if (phoneNumber != null) {
            phoneNumber = phoneNumber.trim();
        }
        if (birthDate != null) {
            birthDate = birthDate.trim();
        }
        if (gender != null) {
            gender = gender.trim().toUpperCase();
        }
        if (profileImage != null && profileImage.trim().isEmpty()) {
            profileImage = null; // 빈 문자열을 null로 변환
        }
    }

    /**
     * 필드값 검증 (커스텀 검증 로직)
     */
    public void validate() {
        // 닉네임에 금지어 체크 (예시)
        if (nickname != null) {
            String[] forbiddenWords = {"관리자", "운영자", "admin", "administrator"};
            for (String word : forbiddenWords) {
                if (nickname.toLowerCase().contains(word.toLowerCase())) {
                    throw new IllegalArgumentException(
                            "닉네임에 '" + word + "'는 사용할 수 없습니다.");
                }
            }
        }

        // 생년월일 유효성 검사
        if (birthDate != null) {
            try {
                java.time.LocalDate date = java.time.LocalDate.parse(birthDate);
                java.time.LocalDate now = java.time.LocalDate.now();
                if (date.isAfter(now)) {
                    throw new IllegalArgumentException("생년월일은 현재 날짜보다 이전이어야 합니다.");
                }
                // 100살 이상 체크
                if (date.isBefore(now.minusYears(100))) {
                    throw new IllegalArgumentException("생년월일이 너무 오래되었습니다.");
                }
            } catch (java.time.format.DateTimeParseException e) {
                throw new IllegalArgumentException("생년월일 형식이 올바르지 않습니다. (YYYY-MM-DD)");
            }
        }

        // 프로필 이미지 URL 검증
        if (profileImage != null && !profileImage.isEmpty()) {
            // URL 형식 검증 (간단한 체크)
            if (!profileImage.startsWith("http://") && !profileImage.startsWith("https://")) {
                throw new IllegalArgumentException("프로필 이미지 URL은 http:// 또는 https://로 시작해야 합니다.");
            }
            // 파일 크기 제한 (URL 기반 이미지의 경우 추정)
            if (profileImage.length() > 1000) {
                throw new IllegalArgumentException("프로필 이미지 URL이 너무 깁니다.");
            }
        }
    }
}