package akuma.whiplash.domains.place.presentation;

import static akuma.whiplash.global.response.code.CommonErrorCode.BAD_REQUEST;
import static akuma.whiplash.domains.place.exception.PlaceErrorCode.AUTOCOMPLETE_NOT_FOUND;
import static akuma.whiplash.domains.place.exception.PlaceErrorCode.INVALID_COORDINATE;
import static akuma.whiplash.domains.place.exception.PlaceErrorCode.PLACE_NOT_FOUND;
import static akuma.whiplash.domains.place.exception.PlaceErrorCode.PROVIDER_AUTHENTICATION_FAILED;
import static akuma.whiplash.domains.place.exception.PlaceErrorCode.PROVIDER_ERROR;
import static akuma.whiplash.domains.place.exception.PlaceErrorCode.PROVIDER_PERMISSION_DENIED;
import static akuma.whiplash.domains.place.exception.PlaceErrorCode.PROVIDER_QUOTA_EXCEEDED;
import static akuma.whiplash.domains.place.exception.PlaceErrorCode.PROVIDER_TIMEOUT;
import static akuma.whiplash.domains.place.exception.PlaceErrorCode.UNSUPPORTED_LANGUAGE;
import static akuma.whiplash.domains.place.exception.PlaceErrorCode.UNSUPPORTED_REGION;

import akuma.whiplash.domains.place.application.dto.response.PlaceDetailResponse;
import akuma.whiplash.domains.place.application.dto.response.PlaceAutocompleteResponse;
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
@RequestMapping("/api/v1/places")
public class PlaceController {

    private final PlaceUseCase placeUseCase;

    @CustomErrorCodes(
        commonErrorCodes = {BAD_REQUEST},
        placeErrorCodes = {
            INVALID_COORDINATE,
            UNSUPPORTED_LANGUAGE,
            UNSUPPORTED_REGION,
            PROVIDER_AUTHENTICATION_FAILED,
            PROVIDER_PERMISSION_DENIED,
            AUTOCOMPLETE_NOT_FOUND,
            PROVIDER_QUOTA_EXCEEDED,
            PROVIDER_TIMEOUT,
            PROVIDER_ERROR
        }
    )
    @Operation(summary = "장소 자동완성", description = "검색어 기반 장소 추천을 제공합니다.")
    @GetMapping("/autocomplete")
    public ApplicationResponse<PlaceAutocompleteResponse> getPlaceAutocompleteSuggestions(
        @RequestParam String query,
        @RequestParam(required = false) Double latitude,
        @RequestParam(required = false) Double longitude,
        @RequestParam(required = false) String languageCode,
        @RequestParam(required = false) String regionCode,
        @RequestParam String sessionToken
    ) {
        return ApplicationResponse.onSuccess(placeUseCase.getPlaceAutocompleteSuggestions(
            query, latitude, longitude, languageCode, regionCode, sessionToken
        ));
    }

    @CustomErrorCodes(
        commonErrorCodes = {BAD_REQUEST},
        placeErrorCodes = {
            INVALID_COORDINATE,
            UNSUPPORTED_LANGUAGE,
            UNSUPPORTED_REGION,
            PROVIDER_AUTHENTICATION_FAILED,
            PROVIDER_PERMISSION_DENIED,
            PLACE_NOT_FOUND,
            PROVIDER_QUOTA_EXCEEDED,
            PROVIDER_TIMEOUT,
            PROVIDER_ERROR
        }
    )
    @Operation(summary = "장소 목록 검색", description = "키워드 기반 장소 검색을 제공합니다.")
    @GetMapping("/search")
    public ApplicationResponse<List<PlaceInfoResponse>> searchPlaces(
        @RequestParam String query,
        @RequestParam(required = false) Double latitude,
        @RequestParam(required = false) Double longitude,
        @RequestParam(defaultValue = "5") int size,
        @RequestParam(required = false) String languageCode,
        @RequestParam(required = false) String regionCode
    ) {
        List<PlaceInfoResponse> placeInfoResponses = placeUseCase.searchPlaces(
            query, latitude, longitude, size, languageCode, regionCode
        );
        return ApplicationResponse.onSuccess(placeInfoResponses);
    }

    @CustomErrorCodes(
        commonErrorCodes = {BAD_REQUEST},
        placeErrorCodes = {
            INVALID_COORDINATE,
            UNSUPPORTED_LANGUAGE,
            PROVIDER_AUTHENTICATION_FAILED,
            PROVIDER_PERMISSION_DENIED,
            PLACE_NOT_FOUND,
            PROVIDER_QUOTA_EXCEEDED,
            PROVIDER_TIMEOUT,
            PROVIDER_ERROR
        }
    )
    @Operation(summary = "장소 역지오코딩", description = "좌표를 사람이 읽을 수 있는 주소로 변환합니다.")
    @GetMapping("/reverse-geocode")
    public ApplicationResponse<PlaceDetailResponse> getPlaceReverseGeocode(
        @RequestParam Double latitude,
        @RequestParam Double longitude,
        @RequestParam(required = false) String languageCode
    ) {
        PlaceDetailResponse placeDetail = placeUseCase.getPlaceReverseGeocode(
            latitude, longitude, languageCode
        );
        return ApplicationResponse.onSuccess(placeDetail);
    }

    @CustomErrorCodes(
        commonErrorCodes = {BAD_REQUEST},
        placeErrorCodes = {
            UNSUPPORTED_LANGUAGE,
            UNSUPPORTED_REGION,
            PROVIDER_AUTHENTICATION_FAILED,
            PROVIDER_PERMISSION_DENIED,
            PLACE_NOT_FOUND,
            PROVIDER_QUOTA_EXCEEDED,
            PROVIDER_TIMEOUT,
            PROVIDER_ERROR
        }
    )
    @Operation(summary = "선택 장소 상세 조회", description = "선택한 Google place ID의 상세 정보를 조회합니다.")
    @GetMapping("/details")
    public ApplicationResponse<PlaceDetailResponse> getPlaceDetails(
        @RequestParam String providerPlaceId,
        @RequestParam String sessionToken,
        @RequestParam(required = false) String languageCode,
        @RequestParam(required = false) String regionCode
    ) {
        PlaceDetailResponse placeDetail = placeUseCase.getPlaceDetails(
            providerPlaceId, sessionToken, languageCode, regionCode
        );
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
