package akuma.whiplash.loadtest.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "테스트 FCM 푸시 전송 요청 DTO")
public record TestPushRequest(
    @Schema(description = "전송할 FCM 토큰", example = "fcm-token")
    @NotBlank(message = "FCM 토큰을 입력해주세요.")
    String fcmToken,

    @Schema(description = "알림 제목", example = "테스트 알림")
    @NotBlank(message = "푸시 제목을 입력해주세요.")
    String title,

    @Schema(description = "알림 본문", example = "푸시가 정상 도착했습니다.")
    @NotBlank(message = "푸시 본문을 입력해주세요.")
    String body,

    @Schema(description = "앱 딥링크", example = "nuntteo://main")
    @NotBlank(message = "딥링크를 입력해주세요.")
    String deeplink
) {}
