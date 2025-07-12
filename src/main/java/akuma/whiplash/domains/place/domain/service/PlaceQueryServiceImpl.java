package akuma.whiplash.domains.place.domain.service;

import akuma.whiplash.domains.place.application.dto.response.PlaceSearchResponse;
import akuma.whiplash.domains.place.application.dto.response.PlaceInfoResponse;
import akuma.whiplash.domains.place.application.dto.response.ReverseGeocodeApiResponse;
import akuma.whiplash.domains.place.application.dto.response.PlaceDetailResponse;
import akuma.whiplash.global.exception.ApplicationException;
import akuma.whiplash.global.response.code.CommonErrorCode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
@Slf4j
public class PlaceQueryServiceImpl implements PlaceQueryService {

    private final WebClient webClient;

    @Value("${naver.search.client-id}")
    private String naverClientId;

    @Value("${naver.search.client-secret}")
    private String naverClientSecret;

    @Value("${naver.map.client-id}")
    private String ncpClientId;

    @Value("${naver.map.client-secret}")
    private String ncpClientSecret;

    @Override
    public List<PlaceInfoResponse> searchPlaces(String query) {
        String uri = UriComponentsBuilder
            .fromUriString("https://openapi.naver.com/v1/search/local.json")
            .queryParam("query", query)
            .queryParam("display", "5")
            .build()
            .toUriString();

        PlaceSearchResponse response = webClient.get()
            .uri(uri)
            .header("X-Naver-Client-Id", naverClientId)
            .header("X-Naver-Client-Secret", naverClientSecret)
            .retrieve()
            .bodyToMono(PlaceSearchResponse.class)
            .block();

        if (response == null || response.getItems() == null) return List.of();

        return response.getItems().stream()
            .map(item -> PlaceInfoResponse.builder()
                .name(sanitize(item.getTitle()))
                .address(item.getAddress())
                .latitude(parseDouble(item.getMapy()) / 1e7)
                .longitude(parseDouble(item.getMapx()) / 1e7)
                .build()
            )
            .toList();
    }

    @Override
    public PlaceDetailResponse getPlaceDetailByCoord(double latitude, double longitude) {
        String uri = UriComponentsBuilder
            .fromUriString("https://maps.apigw.ntruss.com/map-reversegeocode/v2/gc")
            .queryParam("coords", longitude + "," + latitude)
            .queryParam("output", "json")
            .queryParam("orders", "roadaddr,addr")
            .build()
            .toUriString();

        ReverseGeocodeApiResponse apiResponse = webClient.get()
            .uri(uri)
            .header("x-ncp-apigw-api-key-id", ncpClientId)
            .header("x-ncp-apigw-api-key", ncpClientSecret)
            .retrieve()
            .bodyToMono(ReverseGeocodeApiResponse.class)
            .block();

        if (apiResponse == null || apiResponse.getResults().isEmpty()) {
            log.warn("reverse geocode api result is empty, latitude: {}, longitude: {}", latitude, longitude);
            throw ApplicationException.from(CommonErrorCode.BAD_REQUEST);
        }

        // roadaddr 우선 사용, 없으면 addr fallback
        var result = apiResponse.getResults().stream()
            .filter(r -> r.getName().equals("roadaddr"))
            .findFirst()
            .orElse(apiResponse.getResults().get(0));

        var region = result.getRegion();
        var land = result.getLand();

        String fullAddress = Stream.of(
                region.getArea1().getName(),
                region.getArea2().getName(),
                region.getArea3().getName(),
                region.getArea4() != null ? region.getArea4().getName() : null,
                land.getName(),
                land.getNumber1(),
                land.getNumber2() != null && !land.getNumber2().isBlank() ? "-" + land.getNumber2() : ""
            ).filter(Objects::nonNull)
            .filter(s -> !s.isBlank())
            .collect(Collectors.joining(" "));

        String buildingName = Optional.ofNullable(land.getAddition0())
            .map(ReverseGeocodeApiResponse.Result.Land.Addition::getValue)
            .filter(v -> !v.isBlank())
            .orElse(null);

        String placeName = buildingName != null ? buildingName : land.getName();

        return PlaceDetailResponse.builder()
            .address(fullAddress)
            .name(placeName != null ? placeName : "장소 없음")
            .build();
    }

    private String sanitize(String html) {
        return html == null ? "" : html.replaceAll("<.*?>", "");
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0.0;
        }
    }
}
