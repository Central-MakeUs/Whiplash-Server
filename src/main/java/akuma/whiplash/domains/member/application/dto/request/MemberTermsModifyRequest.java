package akuma.whiplash.domains.member.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record MemberTermsModifyRequest(
    @Schema(description = "개인정보 수집 동의 여부")
    Boolean privacyPolicy,

    @Schema(description = "푸시 알림 수신 동의 여부")
    Boolean pushNotificationPolicy
) {

}
