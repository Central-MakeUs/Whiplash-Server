package akuma.whiplash.domains.place.domain.service;

import akuma.whiplash.domains.place.application.dto.response.PlaceInfoResponse;
import akuma.whiplash.domains.place.application.dto.response.PlaceDetailResponse;
import java.util.List;

public interface PlaceQueryService {

    List<PlaceInfoResponse> searchPlaces(String query);
    PlaceDetailResponse getPlaceDetailByCoord(double latitude, double longitude);
}
