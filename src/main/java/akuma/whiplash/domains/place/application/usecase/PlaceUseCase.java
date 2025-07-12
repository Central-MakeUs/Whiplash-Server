package akuma.whiplash.domains.place.application.usecase;

import akuma.whiplash.domains.place.application.dto.response.PlaceInfoResponse;
import akuma.whiplash.domains.place.application.dto.response.PlaceDetailResponse;
import akuma.whiplash.domains.place.domain.service.PlaceQueryService;
import akuma.whiplash.global.annotation.architecture.UseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class PlaceUseCase {

    private final PlaceQueryService placeQueryService;

    public List<PlaceInfoResponse> searchPlaces(String query) {
        return placeQueryService.searchPlaces(query);
    }

    public PlaceDetailResponse getPlaceDetail(double latitude, double longitude) {
        return placeQueryService.getPlaceDetailByCoord(latitude, longitude);
    }
}
