package akuma.whiplash.domains.place.domain.client.impl;

import akuma.whiplash.domains.place.domain.client.NaverClient;
import akuma.whiplash.domains.place.domain.client.dto.NaverLocalSearchResponse;
import akuma.whiplash.domains.place.domain.client.dto.NaverLocalSearchResponse.Item;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "test"})
public class MockNaverClient implements NaverClient {

    private static final List<String> ADDRESS_SUFFIXES = List.of("동", "로", "길");

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

}
