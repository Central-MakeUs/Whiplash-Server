package akuma.whiplash.domains.place.domain.client.impl;

import akuma.whiplash.domains.place.domain.client.NaverClient;
import akuma.whiplash.domains.place.domain.client.dto.NaverLocalSearchResponse;
import akuma.whiplash.domains.place.domain.client.dto.NaverLocalSearchResponse.Item;
import akuma.whiplash.domains.place.domain.constant.PlaceProvider;
import akuma.whiplash.domains.place.domain.model.PlaceDetail;
import akuma.whiplash.domains.place.domain.model.PlaceSearchResult;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "test"})
public class MockNaverClient implements NaverClient {

    private static final List<String> ADDRESS_SUFFIXES = List.of("동", "로", "길");

    @Override
    public PlaceDetail reverseGeocode(double latitude, double longitude) {
        return new PlaceDetail(
            "Seoul, Gangnam-gu, Mock-ro 123",
            "Mock Detail Place",
            "Seoul, Gangnam-gu, Mock-ro 123",
            latitude,
            longitude,
            "KR",
            PlaceProvider.NAVER
        );
    }

    @Override
    public NaverLocalSearchResponse searchLocal(String query) {
        List<Item> items = IntStream.rangeClosed(1, 5)
            .mapToObj(index -> new Item(
                "Mock Place " + index + " for " + query,
                query + ADDRESS_SUFFIXES.get((index - 1) % ADDRESS_SUFFIXES.size()),
                "Seoul, Gangnam-gu, Teheran-ro " + index,
                String.valueOf((127.0276 + (index * 0.001)) * 1e7),
                String.valueOf((37.4979 + (index * 0.001)) * 1e7)
            ))
            .toList();
        return new NaverLocalSearchResponse(items);
    }

    @Override
    public List<PlaceSearchResult> searchPlaces(String query, int size) {
        return List.of();
    }
}
