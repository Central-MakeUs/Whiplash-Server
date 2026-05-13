package akuma.whiplash.domains.place.application.usecase;

import akuma.whiplash.domains.place.application.dto.response.PlaceInfoResponse;
import akuma.whiplash.domains.place.application.dto.response.PlaceDetailResponse;
import akuma.whiplash.domains.place.domain.service.PlaceQueryService;
import akuma.whiplash.global.annotation.architecture.UseCase;
import akuma.whiplash.global.exception.ApplicationException;
import akuma.whiplash.global.response.code.CommonErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

@UseCase
@RequiredArgsConstructor
public class PlaceUseCase {

    private final PlaceQueryService placeQueryService;

    public List<PlaceInfoResponse> searchPlaces(String query, Double latitude, Double longitude) {
        validateSearchRequest(query, latitude, longitude);
        return placeQueryService.searchPlaces(query, latitude, longitude);
    }

    public PlaceDetailResponse getPlaceDetail(double latitude, double longitude) {
        return placeQueryService.getPlaceDetailByCoord(latitude, longitude);
    }

    public List<String> searchPlaceKeywords(String query) {
        return placeQueryService.searchPlaceKeywords(query);
    }

    private void validateSearchRequest(String query, Double latitude, Double longitude) {
        if (!StringUtils.hasText(query)) {
            throw ApplicationException.from(CommonErrorCode.BAD_REQUEST);
        }

        if ((latitude == null) != (longitude == null)) {
            throw ApplicationException.from(CommonErrorCode.BAD_REQUEST);
        }
    }
}
