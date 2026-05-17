package akuma.whiplash.domains.place.domain.service;

import akuma.whiplash.domains.place.application.dto.response.PlaceDetailResponse;
import akuma.whiplash.domains.place.application.dto.response.PlaceInfoResponse;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile({"local", "test"})
@Primary
public class MockPlaceQueryService implements PlaceQueryService {

    @Override
    public List<PlaceInfoResponse> searchPlaces(String query, Double latitude, Double longitude) {
        return IntStream.range(1, 6)
            .mapToObj(i -> PlaceInfoResponse.builder()
                .name("Mock Place " + i + " for " + query)
                .address("Seoul, Gangnam-gu, Teheran-ro " + i)
                .latitude(37.4979 + (i * 0.001))
                .longitude(127.0276 + (i * 0.001))
                .distanceMeters(calculateMockDistance(latitude, longitude, i))
                .build())
            .toList();
    }

    @Override
    public PlaceDetailResponse getPlaceDetailByCoord(double latitude, double longitude) {
        return PlaceDetailResponse.builder()
            .placeName("Mock Detail Place")
            .address("Seoul, Gangnam-gu, Mock-ro 123")
            .roadAddress("Seoul, Gangnam-gu, Mock-ro 123")
            .build();
    }

    @Override
    public List<String> searchPlaceKeywords(String query) {
        return List.of(query + " station", query + " park", query + " school");
    }

    private Integer calculateMockDistance(Double latitude, Double longitude, int index) {
        if (latitude == null || longitude == null) {
            return null;
        }
        return index * 100;
    }
}
