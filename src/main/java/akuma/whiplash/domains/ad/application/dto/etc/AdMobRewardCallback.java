package akuma.whiplash.domains.ad.application.dto.etc;

public record AdMobRewardCallback(
    String customData,
    String transactionId,
    String adUnitId,
    Integer rewardAmount,
    String rewardItem
) {
}
