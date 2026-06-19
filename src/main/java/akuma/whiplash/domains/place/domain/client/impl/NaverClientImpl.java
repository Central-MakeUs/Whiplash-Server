package akuma.whiplash.domains.place.domain.client.impl;

import akuma.whiplash.domains.place.domain.client.NaverClient;
import akuma.whiplash.domains.place.domain.client.dto.NaverLocalSearchResponse;
import akuma.whiplash.domains.place.domain.client.dto.NaverReverseGeocodeResponse;
import akuma.whiplash.domains.place.domain.client.dto.NaverReverseGeocodeResponse.Area;
import akuma.whiplash.domains.place.domain.client.dto.NaverReverseGeocodeResponse.Land;
import akuma.whiplash.domains.place.domain.client.dto.NaverReverseGeocodeResponse.Result;
import akuma.whiplash.domains.place.domain.constant.PlaceProvider;
import akuma.whiplash.domains.place.domain.model.PlaceDetail;
import akuma.whiplash.domains.place.domain.model.PlaceSearchResult;
import akuma.whiplash.domains.place.exception.PlaceErrorCode;
import akuma.whiplash.global.exception.ApplicationException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
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
    private final String clientId;
    private final String clientSecret;
    private final String reverseGeocodeUrl;
    private final String searchClientId;
    private final String searchClientSecret;
    private final String localSearchUrl;

    public NaverClientImpl(
        WebClient webClient,
        @Value("${naver.map.client-id}") String clientId,
        @Value("${naver.map.client-secret}") String clientSecret,
        @Value("${naver.map.reverse-geocode-url:https://maps.apigw.ntruss.com/map-reversegeocode/v2/gc}")
        String reverseGeocodeUrl,
        @Value("${naver.search.client-id}") String searchClientId,
        @Value("${naver.search.client-secret}") String searchClientSecret,
        @Value("${naver.search.base-url:https://openapi.naver.com/v1/search/local.json}") String localSearchUrl
    ) {
        this.webClient = webClient;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.reverseGeocodeUrl = reverseGeocodeUrl;
        this.searchClientId = searchClientId;
        this.searchClientSecret = searchClientSecret;
        this.localSearchUrl = localSearchUrl;
    }

    @Override
    public PlaceDetail reverseGeocode(double latitude, double longitude) {
        NaverReverseGeocodeResponse response = request(latitude, longitude);
        if (response == null || response.status() == null || response.status().code() == null) {
            throw ApplicationException.from(PlaceErrorCode.PROVIDER_ERROR);
        }
        if (response.status().code() == 3) {
            throw ApplicationException.from(PlaceErrorCode.PLACE_NOT_FOUND);
        }
        if (response.status().code() != 0) {
            throw ApplicationException.from(PlaceErrorCode.PROVIDER_ERROR);
        }
        if (response.results() == null || response.results().isEmpty()) {
            throw ApplicationException.from(PlaceErrorCode.PLACE_NOT_FOUND);
        }

        Result result = response.results().stream()
            .filter(item -> "roadaddr".equals(item.name()))
            .findFirst()
            .orElse(response.results().get(0));
        String address = buildAddress(result);
        String placeName = resolvePlaceName(result, address);

        return new PlaceDetail(
            address,
            placeName,
            address,
            latitude,
            longitude,
            "KR",
            PlaceProvider.NAVER
        );
    }

    @Override
    public NaverLocalSearchResponse searchLocal(String query) {
        String uri = UriComponentsBuilder.fromUriString(localSearchUrl)
            .queryParam("query", query)
            .queryParam("display", "5")
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
        return List.of();
    }

    private NaverReverseGeocodeResponse request(double latitude, double longitude) {
        String uri = UriComponentsBuilder.fromUriString(reverseGeocodeUrl)
            .queryParam("coords", longitude + "," + latitude)
            .queryParam("output", "json")
            .queryParam("orders", "roadaddr,addr")
            .build()
            .toUriString();

        try {
            return webClient.get()
                .uri(uri)
                .header("x-ncp-apigw-api-key-id", clientId)
                .header("x-ncp-apigw-api-key", clientSecret)
                .retrieve()
                .bodyToMono(NaverReverseGeocodeResponse.class)
                .block();
        } catch (WebClientResponseException exception) {
            throw PlaceProviderErrorMapper.map(exception);
        } catch (WebClientRequestException exception) {
            throw PlaceProviderErrorMapper.map(exception);
        } catch (RuntimeException exception) {
            throw ApplicationException.from(PlaceErrorCode.PROVIDER_ERROR);
        }
    }

    private String buildAddress(Result result) {
        List<String> regionNames = result.region() == null
            ? List.of()
            : Stream.of(
                result.region().area1(),
                result.region().area2(),
                result.region().area3(),
                result.region().area4()
            ).filter(Objects::nonNull).map(Area::name).toList();
        Land land = result.land();

        return Stream.concat(
                regionNames.stream(),
                Stream.of(
                    land == null ? null : land.name(),
                    land == null ? null : land.number1(),
                    resolveSubNumber(land)
                )
            )
            .filter(Objects::nonNull)
            .filter(value -> !value.isBlank())
            .reduce((left, right) -> left + " " + right)
            .orElseThrow(() -> ApplicationException.from(PlaceErrorCode.PLACE_NOT_FOUND));
    }

    private String resolvePlaceName(Result result, String address) {
        Land land = result.land();
        if (land != null && land.addition0() != null && hasText(land.addition0().value())) {
            return land.addition0().value();
        }
        if (land != null && hasText(land.name())) {
            return land.name();
        }
        return Optional.ofNullable(result.region())
            .map(region -> Stream.of(region.area4(), region.area3(), region.area2(), region.area1())
                .filter(Objects::nonNull)
                .map(Area::name)
                .filter(this::hasText)
                .findFirst()
                .orElse(address))
            .orElse(address);
    }

    private String resolveSubNumber(Land land) {
        return land != null && hasText(land.number2()) ? "-" + land.number2() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
