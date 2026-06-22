package akuma.whiplash.domains.place.domain.client;

import akuma.whiplash.domains.place.domain.model.PlaceDetail;
import akuma.whiplash.domains.place.domain.model.PlaceAutocompleteCriteria;
import akuma.whiplash.domains.place.domain.model.PlaceAutocompleteSuggestion;
import akuma.whiplash.domains.place.domain.model.PlaceSearchCriteria;
import akuma.whiplash.domains.place.domain.model.PlaceSearchResult;
import java.util.List;

public interface GoogleClient {

    PlaceDetail reverseGeocode(double latitude, double longitude, String languageCode);
    List<PlaceAutocompleteSuggestion> autocomplete(PlaceAutocompleteCriteria criteria);
    List<PlaceSearchResult> searchPlaces(PlaceSearchCriteria criteria);
}
