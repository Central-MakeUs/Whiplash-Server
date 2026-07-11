package akuma.whiplash.domains.ad.domain.service;

import akuma.whiplash.domains.ad.application.dto.etc.AdMobRewardCallback;
import jakarta.servlet.http.HttpServletRequest;

public interface AdMobRewardVerifier {

    AdMobRewardCallback verify(HttpServletRequest request);
}
