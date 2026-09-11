package akuma.whiplash.loadtest.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "테스트 FCM 푸시 전송 결과 DTO")
public record TestPushResponse(
    @Schema(description = "FCM 전송 성공 건수", example = "1")
    int successCount,

    @Schema(description = "FCM 전송 실패 건수", example = "0")
    int failedCount
) {}
