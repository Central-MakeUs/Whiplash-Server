package akuma.whiplash.domains.place.domain.client.impl;

import akuma.whiplash.domains.place.domain.client.GoogleClient;
import akuma.whiplash.domains.place.domain.client.dto.GoogleReverseGeocodeResponse;
import akuma.whiplash.domains.place.domain.client.dto.GooglePlaceAutocompleteRequest;
import akuma.whiplash.domains.place.domain.client.dto.GooglePlaceAutocompleteRequest.AutocompleteCenter;
import akuma.whiplash.domains.place.domain.client.dto.GooglePlaceAutocompleteRequest.AutocompleteCircle;
import akuma.whiplash.domains.place.domain.client.dto.GooglePlaceAutocompleteRequest.AutocompleteLocationBias;
import akuma.whiplash.domains.place.domain.client.dto.GooglePlaceAutocompleteResponse;
import akuma.whiplash.domains.place.domain.client.dto.GooglePlaceDetailsResponse;
import akuma.whiplash.domains.place.domain.client.dto.GooglePlaceSearchRequest;
import akuma.whiplash.domains.place.domain.client.dto.GooglePlaceSearchRequest.Center;
import akuma.whiplash.domains.place.domain.client.dto.GooglePlaceSearchRequest.Circle;
import akuma.whiplash.domains.place.domain.client.dto.GooglePlaceSearchRequest.LocationBias;
import akuma.whiplash.domains.place.domain.client.dto.GooglePlaceSearchResponse;
import akuma.whiplash.domains.place.domain.client.dto.GooglePlaceSearchResponse.Place;
import akuma.whiplash.domains.place.domain.client.dto.GoogleReverseGeocodeResponse.AddressComponent;
import akuma.whiplash.domains.place.domain.client.dto.GoogleReverseGeocodeResponse.Result;
import akuma.whiplash.domains.place.domain.constant.PlaceProvider;
import akuma.whiplash.domains.place.domain.model.PlaceDetail;
import akuma.whiplash.domains.place.domain.model.PlaceAutocompleteCriteria;
import akuma.whiplash.domains.place.domain.model.PlaceAutocompleteSuggestion;
import akuma.whiplash.domains.place.domain.model.PlaceDetailsCriteria;
import akuma.whiplash.domains.place.domain.model.PlaceSearchCriteria;
import akuma.whiplash.domains.place.domain.model.PlaceSearchResult;
import akuma.whiplash.domains.place.domain.model.SelectedPlaceDetail;
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

    private static final String REVERSE_GEOCODE_FIELD_MASK =
        "results.formattedAddress,results.addressComponents,results.types";
    private static final String PLACE_SEARCH_FIELD_MASK =
        "places.id,places.displayName,places.formattedAddress,places.location,places.addressComponents";
    private static final String PLACE_AUTOCOMPLETE_FIELD_MASK =
        "suggestions.placePrediction.placeId,"
            + "suggestions.placePrediction.structuredFormat.mainText.text,"
            + "suggestions.placePrediction.structuredFormat.secondaryText.text";
    private static final String PLACE_DETAILS_FIELD_MASK =
        "id,formattedAddress,location,addressComponents";
    private static final double PLACE_SEARCH_BIAS_RADIUS_METERS = 5_000.0;

    private static final List<String> PLACE_NAME_TYPE_PRIORITY = List.of(
        "premise",
        "subpremise",
        "sublocality_level_5",
        "sublocality_level_4",
        "sublocality_level_3",
        "sublocality_level_2",
        "sublocality_level_1",
        "neighborhood",
        "administrative_area_level_3",
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
    private final String placeAutocompleteUrl;
    private final String placeDetailsUrl;

    public GoogleClientImpl(
        WebClient webClient,
        @Value("${google.maps.api-key}") String apiKey,
        @Value("${google.maps.geocoding-base-url:https://geocode.googleapis.com/v4/geocode/location}")
        String geocodingUrl,
        @Value("${google.maps.places-base-url:https://places.googleapis.com}") String placesBaseUrl
    ) {
        this.webClient = webClient;
        this.apiKey = apiKey;
        this.geocodingUrl = geocodingUrl;
        this.placeSearchUrl = placesBaseUrl.replaceAll("/+$", "") + "/v1/places:searchText";
        this.placeAutocompleteUrl = placesBaseUrl.replaceAll("/+$", "") + "/v1/places:autocomplete";
        this.placeDetailsUrl = placesBaseUrl.replaceAll("/+$", "") + "/v1/places";
    }

    @Override
    public List<PlaceAutocompleteSuggestion> autocomplete(PlaceAutocompleteCriteria criteria) {
        GooglePlaceAutocompleteResponse response = requestAutocomplete(criteria);
        if (response == null || response.suggestions() == null || response.suggestions().isEmpty()) {
            throw ApplicationException.from(PlaceErrorCode.AUTOCOMPLETE_NOT_FOUND);
        }
        return response.suggestions().stream()
            .map(this::mapToPlaceAutocompleteSuggestion)
            .toList();
    }

    private GooglePlaceAutocompleteResponse requestAutocomplete(PlaceAutocompleteCriteria criteria) {
        try {
            return webClient.post()
                .uri(placeAutocompleteUrl)
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", PLACE_AUTOCOMPLETE_FIELD_MASK)
                .bodyValue(new GooglePlaceAutocompleteRequest(
                    criteria.query(),
                    criteria.languageCode(),
                    criteria.regionCode(),
                    criteria.sessionToken(),
                    createAutocompleteLocationBias(criteria)
                ))
                .retrieve()
                .bodyToMono(GooglePlaceAutocompleteResponse.class)
                .block();
        } catch (WebClientResponseException exception) {
            throw PlaceProviderErrorMapper.map(exception);
        } catch (WebClientRequestException exception) {
            throw PlaceProviderErrorMapper.map(exception);
        } catch (RuntimeException exception) {
            throw ApplicationException.from(PlaceErrorCode.PROVIDER_ERROR);
        }
    }

    private AutocompleteLocationBias createAutocompleteLocationBias(PlaceAutocompleteCriteria criteria) {
        if (criteria.latitude() == null || criteria.longitude() == null) {
            return null;
        }
        return new AutocompleteLocationBias(new AutocompleteCircle(
            new AutocompleteCenter(criteria.latitude(), criteria.longitude()),
            PLACE_SEARCH_BIAS_RADIUS_METERS
        ));
    }

    private PlaceAutocompleteSuggestion mapToPlaceAutocompleteSuggestion(
        GooglePlaceAutocompleteResponse.Suggestion suggestion
    ) {
        if (suggestion.placePrediction() == null
            || suggestion.placePrediction().placeId() == null
            || suggestion.placePrediction().structuredFormat() == null
            || suggestion.placePrediction().structuredFormat().mainText() == null
            || suggestion.placePrediction().structuredFormat().mainText().text() == null) {
            throw ApplicationException.from(PlaceErrorCode.PROVIDER_ERROR);
        }
        GooglePlaceAutocompleteResponse.StructuredFormat format =
            suggestion.placePrediction().structuredFormat();
        return new PlaceAutocompleteSuggestion(
            format.mainText().text(),
            format.secondaryText() == null ? null : format.secondaryText().text(),
            suggestion.placePrediction().placeId()
        );
    }

    @Override
    public SelectedPlaceDetail getPlaceDetails(PlaceDetailsCriteria criteria) {
        GooglePlaceDetailsResponse response = requestPlaceDetails(criteria);
        if (response == null || response.id() == null || response.id().isBlank()
            || response.formattedAddress() == null || response.formattedAddress().isBlank()
            || response.location() == null) {
            throw ApplicationException.from(PlaceErrorCode.PROVIDER_ERROR);
        }
        return new SelectedPlaceDetail(
            response.formattedAddress(),
            response.location().latitude(),
            response.location().longitude(),
            findCountryCode(response),
            response.id()
        );
    }

    private GooglePlaceDetailsResponse requestPlaceDetails(PlaceDetailsCriteria criteria) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(placeDetailsUrl)
            .pathSegment(criteria.providerPlaceId())
            .queryParam("sessionToken", criteria.sessionToken());
        if (criteria.languageCode() != null) {
            uriBuilder.queryParam("languageCode", criteria.languageCode());
        }
        if (criteria.regionCode() != null) {
            uriBuilder.queryParam("regionCode", criteria.regionCode());
        }

        try {
            return webClient.get()
                .uri(uriBuilder.build().encode().toUri())
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", PLACE_DETAILS_FIELD_MASK)
                .retrieve()
                .bodyToMono(GooglePlaceDetailsResponse.class)
                .block();
        } catch (WebClientResponseException exception) {
            throw PlaceProviderErrorMapper.map(exception);
        } catch (WebClientRequestException exception) {
            throw PlaceProviderErrorMapper.map(exception);
        } catch (RuntimeException exception) {
            throw ApplicationException.from(PlaceErrorCode.PROVIDER_ERROR);
        }
    }

    private String findCountryCode(GooglePlaceDetailsResponse response) {
        if (response.addressComponents() == null) {
            return null;
        }
        return response.addressComponents().stream()
            .filter(component -> component.types() != null && component.types().contains("country"))
            .map(GooglePlaceDetailsResponse.AddressComponent::shortText)
            .findFirst()
            .orElse(null);
    }

    @Override
    public PlaceDetail reverseGeocode(double latitude, double longitude, String languageCode) {
        GoogleReverseGeocodeResponse response = request(latitude, longitude, languageCode);
        validateResponse(response);

        Result result = response.results().get(0);
        String address = result.formattedAddress();
        if (address == null || address.isBlank()) {
            throw ApplicationException.from(PlaceErrorCode.PLACE_NOT_FOUND);
        }

        return new PlaceDetail(
            address,
            findAddressLabel(result).orElse(address),
            address,
            latitude,
            longitude,
            findComponent(result, "country").map(AddressComponent::shortText).orElse(null)
        );
    }

    @Override
    public List<PlaceSearchResult> searchPlaces(PlaceSearchCriteria criteria) {
        GooglePlaceSearchResponse response = requestPlaces(criteria);
        if (response == null || response.places() == null || response.places().isEmpty()) {
            throw ApplicationException.from(PlaceErrorCode.PLACE_NOT_FOUND);
        }
        return response.places().stream().map(this::mapToPlaceSearchResult).toList();
    }

    private GooglePlaceSearchResponse requestPlaces(PlaceSearchCriteria criteria) {
        try {
            return webClient.post()
                .uri(placeSearchUrl)
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", PLACE_SEARCH_FIELD_MASK)
                .bodyValue(new GooglePlaceSearchRequest(
                    criteria.query(),
                    criteria.size(),
                    criteria.languageCode(),
                    criteria.regionCode(),
                    createLocationBias(criteria)
                ))
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

    private LocationBias createLocationBias(PlaceSearchCriteria criteria) {
        if (criteria.latitude() == null || criteria.longitude() == null) {
            return null;
        }
        return new LocationBias(new Circle(
            new Center(criteria.latitude(), criteria.longitude()),
            PLACE_SEARCH_BIAS_RADIUS_METERS
        ));
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
            .queryParam("location.latitude", latitude)
            .queryParam("location.longitude", longitude);
        if (languageCode != null) {
            uriBuilder.queryParam("languageCode", languageCode);
        }

        try {
            return webClient.get()
                .uri(uriBuilder.build().toUriString())
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", REVERSE_GEOCODE_FIELD_MASK)
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

    private void validateResponse(GoogleReverseGeocodeResponse response) {
        if (response == null || response.results() == null || response.results().isEmpty()) {
            throw ApplicationException.from(PlaceErrorCode.PLACE_NOT_FOUND);
        }
    }

    private Optional<String> findAddressLabel(Result result) {
        return PLACE_NAME_TYPE_PRIORITY.stream()
            .map(type -> findComponent(result, type))
            .flatMap(Optional::stream)
            .map(AddressComponent::longText)
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
