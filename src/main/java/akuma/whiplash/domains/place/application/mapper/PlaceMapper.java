package akuma.whiplash.domains.place.application.mapper;

import akuma.whiplash.domains.place.application.dto.response.PlaceDetailResponse;
import akuma.whiplash.domains.place.application.dto.response.PlaceAutocompleteResponse;
import akuma.whiplash.domains.place.application.dto.response.PlaceAutocompleteSuggestionResponse;
import akuma.whiplash.domains.place.domain.model.PlaceDetail;
import akuma.whiplash.domains.place.domain.model.PlaceAutocompleteSuggestion;
import akuma.whiplash.domains.place.application.dto.response.PlaceInfoResponse;
import akuma.whiplash.domains.place.domain.model.PlaceSearchResult;
import akuma.whiplash.domains.place.domain.model.SelectedPlaceDetail;
import java.util.List;

public class PlaceMapper {

    private PlaceMapper() {
        throw new IllegalArgumentException();
    }

    public static PlaceAutocompleteResponse mapToPlaceAutocompleteResponse(
        List<PlaceAutocompleteSuggestion> suggestions
    ) {
        return new PlaceAutocompleteResponse(suggestions.stream()
            .map(suggestion -> new PlaceAutocompleteSuggestionResponse(
                suggestion.mainText(),
                suggestion.secondaryText(),
                suggestion.providerPlaceId()
            ))
            .toList());
    }

    public static PlaceDetailResponse mapToPlaceDetailResponse(PlaceDetail placeDetail) {
        return PlaceDetailResponse.builder()
            .address(placeDetail.address())
            .placeName(placeDetail.placeName())
            .roadAddress(placeDetail.roadAddress())
            .latitude(placeDetail.latitude())
            .longitude(placeDetail.longitude())
            .countryCode(placeDetail.countryCode())
            .build();
    }

    public static PlaceDetailResponse mapToPlaceDetailResponse(SelectedPlaceDetail placeDetail) {
        return PlaceDetailResponse.builder()
            .address(placeDetail.address())
            .placeName(null)
            .roadAddress(placeDetail.address())
            .latitude(placeDetail.latitude())
            .longitude(placeDetail.longitude())
            .countryCode(placeDetail.countryCode())
            .build();
    }

    public static List<PlaceInfoResponse> mapToPlaceInfoResponses(List<PlaceSearchResult> places) {
        return places.stream()
            .map(place -> PlaceInfoResponse.builder()
                .name(place.name())
                .address(place.address())
                .latitude(place.latitude())
                .longitude(place.longitude())
                .distanceMeters(place.distanceMeters())
                .provider(place.provider().name())
                .providerPlaceId(place.providerPlaceId())
                .countryCode(place.countryCode())
                .build())
            .toList();
    }
}
