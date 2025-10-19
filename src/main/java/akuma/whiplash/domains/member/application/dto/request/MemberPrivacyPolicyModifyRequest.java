package akuma.whiplash.domains.member.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record MemberPrivacyPolicyModifyRequest(
    @Schema(description = "개인정보 수집 동의 여부", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "개인정보 수집 동의 여부를 선택해주세요.")
    Boolean privacyPolicy
) {
}