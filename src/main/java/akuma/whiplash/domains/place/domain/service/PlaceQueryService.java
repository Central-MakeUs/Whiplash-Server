package akuma.whiplash.domains.place.domain.service;

import akuma.whiplash.domains.place.domain.model.PlaceDetail;
import akuma.whiplash.domains.place.domain.model.PlaceAutocompleteCriteria;
import akuma.whiplash.domains.place.domain.model.PlaceAutocompleteSuggestion;
import akuma.whiplash.domains.place.domain.model.PlaceDetailsCriteria;
import akuma.whiplash.domains.place.domain.model.PlaceSearchCriteria;
import akuma.whiplash.domains.place.domain.model.PlaceSearchResult;
import akuma.whiplash.domains.place.domain.model.SelectedPlaceDetail;
import java.util.List;

public interface PlaceQueryService {

    List<PlaceAutocompleteSuggestion> getPlaceAutocompleteSuggestions(PlaceAutocompleteCriteria criteria);
    SelectedPlaceDetail getPlaceDetails(PlaceDetailsCriteria criteria);
    List<PlaceSearchResult> searchPlaces(PlaceSearchCriteria criteria);
    PlaceDetail getPlaceDetailByCoord(double latitude, double longitude, String languageCode);
    List<String> searchPlaceKeywords(String query);
}
