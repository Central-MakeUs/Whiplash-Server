package akuma.whiplash.domains.member.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record MemberPushNotificationPolicyModifyRequest(
    @Schema(description = "푸시 알림 수신 동의 여부")
    @NotNull(message = "푸시 알림 수신 동의 여부를 선택해주세요.")
    Boolean pushNotificationPolicy
) {

}
