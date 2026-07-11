package akuma.whiplash.domains.ad.presentation;

import static akuma.whiplash.domains.ad.exception.AdErrorCode.INVALID_ADMOB_CALLBACK;

import akuma.whiplash.domains.ad.domain.service.AdSessionService;
import akuma.whiplash.global.annotation.swagger.CustomErrorCodes;
import akuma.whiplash.global.response.ApplicationResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdController {

    private final AdSessionService adSessionService;

    @CustomErrorCodes(adErrorCodes = {INVALID_ADMOB_CALLBACK})
    @Operation(summary = "AdMob 보상형 광고 SSV 콜백", description = "AdMob이 호출하는 서버 사이드 검증 콜백입니다.")
    @GetMapping("/api/v1/ads/rewards/callback/admob")
    public ApplicationResponse<Void> verifyRewardCallback(HttpServletRequest request) {
        adSessionService.verifyRewardCallback(request);
        return ApplicationResponse.onSuccess();
    }
}
