package akuma.whiplash.domains.place.domain.client.impl;

import akuma.whiplash.domains.place.domain.client.GoogleClient;
import akuma.whiplash.domains.place.domain.constant.PlaceProvider;
import akuma.whiplash.domains.place.domain.model.PlaceDetail;
import akuma.whiplash.domains.place.domain.model.PlaceSearchResult;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "test"})
public class MockGoogleClient implements GoogleClient {

    @Override
    public PlaceDetail reverseGeocode(double latitude, double longitude, String languageCode) {
        return new PlaceDetail(
            "Mock Google Address",
            "Mock Google Place",
            "Mock Google Address",
            latitude,
            longitude,
            "US",
            PlaceProvider.GOOGLE
        );
    }

    @Override
    public List<PlaceSearchResult> searchPlaces(String query, int size, String languageCode, String regionCode) {
        return IntStream.rangeClosed(1, size)
            .mapToObj(index -> new PlaceSearchResult(
                "Mock Google Place " + index + " for " + query,
                "Mock Google Address " + index,
                40.7128 + (index * 0.001),
                -74.0060 + (index * 0.001),
                PlaceProvider.GOOGLE,
                "mock-google-" + index,
                regionCode,
                null
            ))
            .toList();
    }
}
