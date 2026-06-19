package akuma.whiplash.domains.place.domain.service;

import akuma.whiplash.domains.place.domain.model.PlaceDetail;
import akuma.whiplash.domains.place.domain.model.PlaceSearchCriteria;
import akuma.whiplash.domains.place.domain.model.PlaceSearchResult;
import java.util.List;

public interface PlaceQueryService {

    List<PlaceSearchResult> searchPlaces(PlaceSearchCriteria criteria);
    PlaceDetail getPlaceDetailByCoord(double latitude, double longitude, String languageCode);
    List<String> searchPlaceKeywords(String query);
}
