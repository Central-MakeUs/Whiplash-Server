package akuma.whiplash.domains.place.domain.service;

import akuma.whiplash.domains.place.domain.client.GoogleClient;
import akuma.whiplash.domains.place.domain.client.NaverClient;
import akuma.whiplash.domains.place.domain.model.PlaceDetail;
import akuma.whiplash.domains.place.domain.model.PlaceSearchCriteria;
import akuma.whiplash.domains.place.domain.model.PlaceSearchResult;
import akuma.whiplash.domains.place.exception.PlaceErrorCode;
import akuma.whiplash.global.exception.ApplicationException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlaceProviderRouter {

    private static final double KOREA_MIN_LATITUDE = 33.0;
    private static final double KOREA_MAX_LATITUDE = 38.7;
    private static final double KOREA_MIN_LONGITUDE = 124.5;
    private static final double KOREA_MAX_LONGITUDE = 132.0;

    private final NaverClient naverClient;
    private final GoogleClient googleClient;

    public List<PlaceSearchResult> searchPlaces(PlaceSearchCriteria criteria) {
        List<PlaceSearchResult> results;
        if (shouldUseNaver(criteria)) {
            results = naverClient.searchPlaces(criteria.query(), criteria.size());
        } else {
            results = googleClient.searchPlaces(
                criteria.query(), criteria.size(), criteria.languageCode(), criteria.regionCode()
            );
        }

        if (results == null || results.isEmpty()) {
            throw ApplicationException.from(PlaceErrorCode.PLACE_NOT_FOUND);
        }
        return results;
    }

    public PlaceDetail getPlaceDetail(double latitude, double longitude, String languageCode) {
        // 조회하려고 하는 장소가 한국이면 Naver API 이용
        if (isKoreaCoordinate(latitude, longitude)) {
            return naverClient.reverseGeocode(latitude, longitude);
        }
        return googleClient.reverseGeocode(latitude, longitude, languageCode);
    }

    private boolean shouldUseNaver(PlaceSearchCriteria criteria) {
        if (criteria.regionCode() != null) {
            return "KR".equals(criteria.regionCode());
        }
        if (criteria.latitude() == null) {
            return true;
        }
        return isKoreaCoordinate(criteria.latitude(), criteria.longitude());
    }

    private boolean isKoreaCoordinate(double latitude, double longitude) {
        return latitude >= KOREA_MIN_LATITUDE && latitude <= KOREA_MAX_LATITUDE
            && longitude >= KOREA_MIN_LONGITUDE && longitude <= KOREA_MAX_LONGITUDE;
    }
}
