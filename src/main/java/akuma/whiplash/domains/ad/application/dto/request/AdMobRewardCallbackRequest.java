package akuma.whiplash.domains.ad.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AdMob 보상형 광고 SSV 콜백 요청 DTO")
public record AdMobRewardCallbackRequest(
    String customData,
    String transactionId,
    String adUnit,
    Integer rewardAmount,
    String rewardItem
) {
}
