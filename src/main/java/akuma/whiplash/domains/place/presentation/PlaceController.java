package akuma.whiplash.domains.place.presentation;

import static akuma.whiplash.global.response.code.CommonErrorCode.BAD_REQUEST;

import akuma.whiplash.domains.place.application.dto.response.PlaceDetailResponse;
import akuma.whiplash.domains.place.application.dto.response.PlaceInfoResponse;
import akuma.whiplash.domains.place.application.usecase.PlaceUseCase;
import akuma.whiplash.global.annotation.swagger.CustomErrorCodes;
import akuma.whiplash.global.response.ApplicationResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/places")
public class PlaceController {

    private final PlaceUseCase placeUseCase;

    @CustomErrorCodes(commonErrorCodes = {BAD_REQUEST})
    @Operation(summary = "장소 목록 검색", description = "키워드 기반 장소 검색을 제공합니다.")
    @GetMapping("/search")
    public ApplicationResponse<List<PlaceInfoResponse>> searchPlaces(@RequestParam String query) {
        List<PlaceInfoResponse> placeInfoResponses = placeUseCase.searchPlaces(query);
        return ApplicationResponse.onSuccess(placeInfoResponses);
    }

    @CustomErrorCodes(commonErrorCodes = {BAD_REQUEST})
    @Operation(summary = "장소 상세 조회", description = "위/경도를 기반으로 장소 상세 정보를 조회합니다.")
    @GetMapping("/detail")
    public ApplicationResponse<PlaceDetailResponse> getPlaceDetail(@RequestParam double latitude, @RequestParam double longitude) {
        PlaceDetailResponse placeDetail = placeUseCase.getPlaceDetail(latitude, longitude);
        return ApplicationResponse.onSuccess(placeDetail);
    }

    @CustomErrorCodes(commonErrorCodes = {BAD_REQUEST})
    @Operation(summary = "연관 장소 키워드 추천", description = "텍스트 기반 연관 장소 키워드를 도로명 주소 형태로 제공합니다.")
    @GetMapping("/keywords")
    public ApplicationResponse<List<String>> searchPlaceKeywords(@RequestParam String query) {
        List<String> keywords = placeUseCase.searchPlaceKeywords(query);
        return ApplicationResponse.onSuccess(keywords);
    }
}
