package akuma.whiplash.domains.place.domain.client.impl;

import akuma.whiplash.domains.place.domain.client.NaverClient;
import akuma.whiplash.domains.place.domain.client.dto.NaverLocalSearchResponse;
import akuma.whiplash.domains.place.domain.constant.PlaceProvider;
import akuma.whiplash.domains.place.domain.model.PlaceSearchResult;
import akuma.whiplash.domains.place.exception.PlaceErrorCode;
import akuma.whiplash.global.exception.ApplicationException;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Profile("!local & !test")
public class NaverClientImpl implements NaverClient {

    private final WebClient webClient;
    private final String searchClientId;
    private final String searchClientSecret;
    private final String localSearchUrl;

    public NaverClientImpl(
        WebClient webClient,
        @Value("${naver.search.client-id}") String searchClientId,
        @Value("${naver.search.client-secret}") String searchClientSecret,
        @Value("${naver.search.base-url:https://openapi.naver.com/v1/search/local.json}") String localSearchUrl
    ) {
        this.webClient = webClient;
        this.searchClientId = searchClientId;
        this.searchClientSecret = searchClientSecret;
        this.localSearchUrl = localSearchUrl;
    }

    @Override
    public NaverLocalSearchResponse searchLocal(String query) {
        return searchLocal(query, 5);
    }

    private NaverLocalSearchResponse searchLocal(String query, int size) {
        String uri = UriComponentsBuilder.fromUriString(localSearchUrl)
            .queryParam("query", query)
            .queryParam("display", size)
            .build()
            .toUriString();

        try {
            return webClient.get()
                .uri(uri)
                .header("X-Naver-Client-Id", searchClientId)
                .header("X-Naver-Client-Secret", searchClientSecret)
                .retrieve()
                .bodyToMono(NaverLocalSearchResponse.class)
                .block();
        } catch (WebClientResponseException exception) {
            throw PlaceProviderErrorMapper.map(exception);
        } catch (WebClientRequestException exception) {
            throw PlaceProviderErrorMapper.map(exception);
        } catch (RuntimeException exception) {
            throw ApplicationException.from(PlaceErrorCode.PROVIDER_ERROR);
        }
    }

    @Override
    public List<PlaceSearchResult> searchPlaces(String query, int size) {
        NaverLocalSearchResponse response = searchLocal(query, size);
        if (response == null || response.items() == null) {
            return List.of();
        }
        try {
            return response.items().stream().map(this::mapToPlaceSearchResult).toList();
        } catch (RuntimeException exception) {
            throw ApplicationException.from(PlaceErrorCode.PROVIDER_ERROR);
        }
    }

    private PlaceSearchResult mapToPlaceSearchResult(NaverLocalSearchResponse.Item item) {
        return new PlaceSearchResult(
            item.title() == null ? "" : item.title().replaceAll("<[^>]*>", ""),
            item.roadAddress() != null && !item.roadAddress().isBlank()
                ? item.roadAddress()
                : Optional.ofNullable(item.address()).orElse(""),
            Double.parseDouble(item.mapy()) / 1e7,
            Double.parseDouble(item.mapx()) / 1e7,
            PlaceProvider.NAVER,
            null,
            "KR",
            null
        );
    }

}
