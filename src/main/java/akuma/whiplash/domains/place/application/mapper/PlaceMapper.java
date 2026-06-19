package akuma.whiplash.domains.place.application.mapper;

import akuma.whiplash.domains.place.application.dto.response.PlaceDetailResponse;
import akuma.whiplash.domains.place.domain.model.PlaceDetail;
import akuma.whiplash.domains.place.application.dto.response.PlaceInfoResponse;
import akuma.whiplash.domains.place.domain.model.PlaceSearchResult;
import java.util.List;

public class PlaceMapper {

    private PlaceMapper() {
        throw new IllegalArgumentException();
    }

    public static PlaceDetailResponse mapToPlaceDetailResponse(PlaceDetail placeDetail) {
        return PlaceDetailResponse.builder()
            .address(placeDetail.address())
            .placeName(placeDetail.placeName())
            .roadAddress(placeDetail.roadAddress())
            .latitude(placeDetail.latitude())
            .longitude(placeDetail.longitude())
            .countryCode(placeDetail.countryCode())
            .provider(placeDetail.provider().name())
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
