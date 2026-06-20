package akuma.whiplash.domains.place.domain.client.impl;

import akuma.whiplash.domains.place.domain.client.GoogleClient;
import akuma.whiplash.domains.place.domain.client.dto.GoogleReverseGeocodeResponse;
import akuma.whiplash.domains.place.domain.client.dto.GooglePlaceSearchRequest;
import akuma.whiplash.domains.place.domain.client.dto.GooglePlaceSearchResponse;
import akuma.whiplash.domains.place.domain.client.dto.GooglePlaceSearchResponse.Place;
import akuma.whiplash.domains.place.domain.client.dto.GoogleReverseGeocodeResponse.AddressComponent;
import akuma.whiplash.domains.place.domain.client.dto.GoogleReverseGeocodeResponse.Result;
import akuma.whiplash.domains.place.domain.constant.PlaceProvider;
import akuma.whiplash.domains.place.domain.model.PlaceDetail;
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
public class GoogleClientImpl implements GoogleClient {

    private static final String PLACE_SEARCH_FIELD_MASK =
        "places.id,places.displayName,places.formattedAddress,places.location,places.addressComponents";

    private static final List<String> PLACE_NAME_TYPE_PRIORITY = List.of(
        "point_of_interest",
        "establishment",
        "premise",
        "locality",
        "sublocality",
        "administrative_area_level_2",
        "administrative_area_level_1",
        "country"
    );

    private final WebClient webClient;
    private final String apiKey;
    private final String geocodingUrl;
    private final String placeSearchUrl;

    public GoogleClientImpl(
        WebClient webClient,
        @Value("${google.maps.api-key}") String apiKey,
        @Value("${google.maps.geocoding-base-url:https://maps.googleapis.com/maps/api/geocode/json}")
        String geocodingUrl,
        @Value("${google.maps.places-base-url:https://places.googleapis.com}") String placesBaseUrl
    ) {
        this.webClient = webClient;
        this.apiKey = apiKey;
        this.geocodingUrl = geocodingUrl;
        this.placeSearchUrl = placesBaseUrl.replaceAll("/+$", "") + "/v1/places:searchText";
    }

    @Override
    public PlaceDetail reverseGeocode(double latitude, double longitude, String languageCode) {
        GoogleReverseGeocodeResponse response = request(latitude, longitude, languageCode);
        validateStatus(response);

        Result result = response.results().get(0);
        String address = result.formattedAddress();
        if (address == null || address.isBlank()) {
            throw ApplicationException.from(PlaceErrorCode.PLACE_NOT_FOUND);
        }

        return new PlaceDetail(
            address,
            resolvePlaceName(result).orElse(address),
            address,
            latitude,
            longitude,
            findComponent(result, "country").map(AddressComponent::shortName).orElse(null),
            PlaceProvider.GOOGLE
        );
    }

    @Override
    public List<PlaceSearchResult> searchPlaces(String query, int size, String languageCode, String regionCode) {
        GooglePlaceSearchResponse response = requestPlaces(query, size, languageCode, regionCode);
        if (response == null || response.places() == null || response.places().isEmpty()) {
            throw ApplicationException.from(PlaceErrorCode.PLACE_NOT_FOUND);
        }
        return response.places().stream().map(this::mapToPlaceSearchResult).toList();
    }

    private GooglePlaceSearchResponse requestPlaces(
        String query,
        int size,
        String languageCode,
        String regionCode
    ) {
        try {
            return webClient.post()
                .uri(placeSearchUrl)
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", PLACE_SEARCH_FIELD_MASK)
                .bodyValue(new GooglePlaceSearchRequest(query, size, languageCode, regionCode))
                .retrieve()
                .bodyToMono(GooglePlaceSearchResponse.class)
                .block();
        } catch (WebClientResponseException exception) {
            throw PlaceProviderErrorMapper.map(exception);
        } catch (WebClientRequestException exception) {
            throw PlaceProviderErrorMapper.map(exception);
        } catch (RuntimeException exception) {
            throw ApplicationException.from(PlaceErrorCode.PROVIDER_ERROR);
        }
    }

    private PlaceSearchResult mapToPlaceSearchResult(Place place) {
        if (place.displayName() == null || place.location() == null) {
            throw ApplicationException.from(PlaceErrorCode.PROVIDER_ERROR);
        }
        return new PlaceSearchResult(
            place.displayName().text(),
            place.formattedAddress(),
            place.location().latitude(),
            place.location().longitude(),
            PlaceProvider.GOOGLE,
            place.id(),
            findCountryCode(place),
            null
        );
    }

    private String findCountryCode(Place place) {
        if (place.addressComponents() == null) {
            return null;
        }
        return place.addressComponents().stream()
            .filter(component -> component.types() != null && component.types().contains("country"))
            .map(GooglePlaceSearchResponse.AddressComponent::shortText)
            .findFirst()
            .orElse(null);
    }

    private GoogleReverseGeocodeResponse request(double latitude, double longitude, String languageCode) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(geocodingUrl)
            .queryParam("latlng", latitude + "," + longitude)
            .queryParam("key", apiKey);
        if (languageCode != null) {
            uriBuilder.queryParam("language", languageCode);
        }

        try {
            return webClient.get()
                .uri(uriBuilder.build().toUriString())
                .retrieve()
                .bodyToMono(GoogleReverseGeocodeResponse.class)
                .block();
        } catch (WebClientResponseException exception) {
            throw PlaceProviderErrorMapper.map(exception);
        } catch (WebClientRequestException exception) {
            throw PlaceProviderErrorMapper.map(exception);
        } catch (RuntimeException exception) {
            throw ApplicationException.from(PlaceErrorCode.PROVIDER_ERROR);
        }
    }

    private void validateStatus(GoogleReverseGeocodeResponse response) {
        if (response == null || response.status() == null) {
            throw ApplicationException.from(PlaceErrorCode.PROVIDER_ERROR);
        }

        switch (response.status()) {
            case "OK" -> {
                if (response.results() == null || response.results().isEmpty()) {
                    throw ApplicationException.from(PlaceErrorCode.PLACE_NOT_FOUND);
                }
            }
            case "ZERO_RESULTS" -> throw ApplicationException.from(PlaceErrorCode.PLACE_NOT_FOUND);
            case "REQUEST_DENIED" -> throw ApplicationException.from(PlaceErrorCode.PROVIDER_PERMISSION_DENIED);
            case "OVER_QUERY_LIMIT", "OVER_DAILY_LIMIT" ->
                throw ApplicationException.from(PlaceErrorCode.PROVIDER_QUOTA_EXCEEDED);
            default -> throw ApplicationException.from(PlaceErrorCode.PROVIDER_ERROR);
        }
    }

    private Optional<String> resolvePlaceName(Result result) {
        return PLACE_NAME_TYPE_PRIORITY.stream()
            .map(type -> findComponent(result, type))
            .flatMap(Optional::stream)
            .map(AddressComponent::longName)
            .filter(value -> value != null && !value.isBlank())
            .findFirst();
    }

    private Optional<AddressComponent> findComponent(Result result, String type) {
        if (result.addressComponents() == null) {
            return Optional.empty();
        }
        return result.addressComponents().stream()
            .filter(component -> component.types() != null && component.types().contains(type))
            .findFirst();
    }
}
