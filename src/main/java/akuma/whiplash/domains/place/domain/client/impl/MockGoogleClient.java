package akuma.whiplash.domains.place.domain.client.impl;

import akuma.whiplash.domains.place.domain.client.GoogleClient;
import akuma.whiplash.domains.place.domain.constant.PlaceProvider;
import akuma.whiplash.domains.place.domain.model.PlaceDetail;
import akuma.whiplash.domains.place.domain.model.PlaceSearchCriteria;
import akuma.whiplash.domains.place.domain.model.PlaceSearchResult;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "test"})
public class MockGoogleClient implements GoogleClient {

    private static final double KOREA_MIN_LATITUDE = 33.0;
    private static final double KOREA_MAX_LATITUDE = 38.7;
    private static final double KOREA_MIN_LONGITUDE = 124.5;
    private static final double KOREA_MAX_LONGITUDE = 132.0;

    @Override
    public PlaceDetail reverseGeocode(double latitude, double longitude, String languageCode) {
        return new PlaceDetail(
            "Mock Google Address",
            "Mock Google Place",
            "Mock Google Address",
            latitude,
            longitude,
            isKoreaCoordinate(latitude, longitude) ? "KR" : "US"
        );
    }

    @Override
    public List<PlaceSearchResult> searchPlaces(PlaceSearchCriteria criteria) {
        return IntStream.rangeClosed(1, criteria.size())
            .mapToObj(index -> new PlaceSearchResult(
                "Mock Google Place " + index + " for " + criteria.query(),
                "Mock Google Address " + index,
                40.7128 + (index * 0.001),
                -74.0060 + (index * 0.001),
                PlaceProvider.GOOGLE,
                "mock-google-" + index,
                criteria.regionCode(),
                null
            ))
            .toList();
    }

    private boolean isKoreaCoordinate(double latitude, double longitude) {
        return latitude >= KOREA_MIN_LATITUDE && latitude <= KOREA_MAX_LATITUDE
            && longitude >= KOREA_MIN_LONGITUDE && longitude <= KOREA_MAX_LONGITUDE;
    }
}
