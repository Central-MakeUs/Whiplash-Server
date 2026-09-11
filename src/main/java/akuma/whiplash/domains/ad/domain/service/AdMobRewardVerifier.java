package akuma.whiplash.domains.ad.domain.service;

import akuma.whiplash.domains.ad.application.dto.etc.AdMobRewardCallback;
import akuma.whiplash.domains.ad.application.dto.request.AdMobRewardCallbackRequest;

public interface AdMobRewardVerifier {

    AdMobRewardCallback verify(AdMobRewardCallbackRequest request);
}
