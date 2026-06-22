package akuma.whiplash.domains.place.application.usecase;

import akuma.whiplash.domains.place.application.dto.response.PlaceInfoResponse;
import akuma.whiplash.domains.place.application.dto.response.PlaceDetailResponse;
import akuma.whiplash.domains.place.application.dto.response.PlaceAutocompleteResponse;
import akuma.whiplash.domains.place.application.mapper.PlaceMapper;
import akuma.whiplash.domains.place.domain.service.PlaceQueryService;
import akuma.whiplash.domains.place.domain.model.PlaceSearchCriteria;
import akuma.whiplash.domains.place.domain.model.PlaceAutocompleteCriteria;
import akuma.whiplash.domains.place.exception.PlaceErrorCode;
import akuma.whiplash.global.annotation.architecture.UseCase;
import akuma.whiplash.global.exception.ApplicationException;
import akuma.whiplash.global.response.code.CommonErrorCode;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

@UseCase
@RequiredArgsConstructor
public class PlaceUseCase {

    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("ko", "en");
    private static final Set<String> SUPPORTED_REGIONS = Set.of("KR", "US", "CA", "GB", "AU");

    private final PlaceQueryService placeQueryService;

    public PlaceAutocompleteResponse getPlaceAutocompleteSuggestions(
        String query,
        Double latitude,
        Double longitude,
        String languageCode,
        String regionCode,
        String sessionToken
    ) {
        validateAutocompleteRequest(
            query, latitude, longitude, languageCode, regionCode, sessionToken
        );
        return PlaceMapper.mapToPlaceAutocompleteResponse(
            placeQueryService.getPlaceAutocompleteSuggestions(new PlaceAutocompleteCriteria(
                query, latitude, longitude, languageCode, regionCode, sessionToken
            ))
        );
    }

    public List<PlaceInfoResponse> searchPlaces(
        String query,
        Double latitude,
        Double longitude,
        int size,
        String languageCode,
        String regionCode
    ) {
        validateSearchRequest(query, latitude, longitude, size, languageCode, regionCode);
        return PlaceMapper.mapToPlaceInfoResponses(placeQueryService.searchPlaces(
            new PlaceSearchCriteria(query, latitude, longitude, size, languageCode, regionCode)
        ));
    }

    public PlaceDetailResponse getPlaceDetail(double latitude, double longitude, String languageCode) {
        validateDetailRequest(latitude, longitude, languageCode);
        return PlaceMapper.mapToPlaceDetailResponse(
            placeQueryService.getPlaceDetailByCoord(latitude, longitude, languageCode)
        );
    }

    public List<String> searchPlaceKeywords(String query) {
        return placeQueryService.searchPlaceKeywords(query);
    }

    private void validateSearchRequest(
        String query,
        Double latitude,
        Double longitude,
        int size,
        String languageCode,
        String regionCode
    ) {
        if (!StringUtils.hasText(query)) {
            throw ApplicationException.from(CommonErrorCode.BAD_REQUEST);
        }

        if (size < 1 || size > 5 || (latitude == null) != (longitude == null)) {
            throw ApplicationException.from(CommonErrorCode.BAD_REQUEST);
        }

        if (latitude != null) {
            validateCoordinates(latitude, longitude);
        }

        if (languageCode != null && !SUPPORTED_LANGUAGES.contains(languageCode)) {
            throw ApplicationException.from(PlaceErrorCode.UNSUPPORTED_LANGUAGE);
        }

        if (regionCode != null && !SUPPORTED_REGIONS.contains(regionCode)) {
            throw ApplicationException.from(PlaceErrorCode.UNSUPPORTED_REGION);
        }
    }

    private void validateAutocompleteRequest(
        String query,
        Double latitude,
        Double longitude,
        String languageCode,
        String regionCode,
        String sessionToken
    ) {
        if (!StringUtils.hasText(query) || !isUuid(sessionToken)
            || (latitude == null) != (longitude == null)) {
            throw ApplicationException.from(CommonErrorCode.BAD_REQUEST);
        }

        if (latitude != null) {
            validateCoordinates(latitude, longitude);
        }

        if (languageCode != null && !SUPPORTED_LANGUAGES.contains(languageCode)) {
            throw ApplicationException.from(PlaceErrorCode.UNSUPPORTED_LANGUAGE);
        }

        if (regionCode != null && !SUPPORTED_REGIONS.contains(regionCode)) {
            throw ApplicationException.from(PlaceErrorCode.UNSUPPORTED_REGION);
        }
    }

    private boolean isUuid(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        try {
            return UUID.fromString(value).toString().equalsIgnoreCase(value);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private void validateDetailRequest(double latitude, double longitude, String languageCode) {
        validateCoordinates(latitude, longitude);

        if (languageCode != null && !SUPPORTED_LANGUAGES.contains(languageCode)) {
            throw ApplicationException.from(PlaceErrorCode.UNSUPPORTED_LANGUAGE);
        }
    }

    private void validateCoordinates(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
            || latitude < -90 || latitude > 90
            || longitude < -180 || longitude > 180) {
            throw ApplicationException.from(PlaceErrorCode.INVALID_COORDINATE);
        }
    }
}
