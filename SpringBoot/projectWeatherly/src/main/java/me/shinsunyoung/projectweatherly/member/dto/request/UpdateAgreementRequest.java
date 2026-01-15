package me.shinsunyoung.projectweatherly.member.dto.request;



import lombok.Data;

@Data
public class UpdateAgreementRequest {

    private Boolean termsOfServiceAgree;
    private Boolean privacyPolicyAgree;
    private Boolean marketingAgree;
}