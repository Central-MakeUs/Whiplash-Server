package akuma.whiplash.domains.place.domain.service;

import akuma.whiplash.domains.place.domain.client.GoogleClient;
import akuma.whiplash.domains.place.domain.model.PlaceDetail;
import akuma.whiplash.domains.place.domain.model.PlaceAutocompleteCriteria;
import akuma.whiplash.domains.place.domain.model.PlaceAutocompleteSuggestion;
import akuma.whiplash.domains.place.domain.model.PlaceDetailsCriteria;
import akuma.whiplash.domains.place.domain.model.PlaceSearchCriteria;
import akuma.whiplash.domains.place.domain.model.PlaceSearchResult;
import akuma.whiplash.domains.place.domain.model.SelectedPlaceDetail;
import akuma.whiplash.global.util.GeoUtils;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlaceQueryServiceImpl implements PlaceQueryService {

    private final GoogleClient googleClient;

    @Override
    public List<PlaceAutocompleteSuggestion> getPlaceAutocompleteSuggestions(
        PlaceAutocompleteCriteria criteria
    ) {
        return googleClient.autocomplete(criteria);
    }

    @Override
    public SelectedPlaceDetail getPlaceDetails(PlaceDetailsCriteria criteria) {
        return googleClient.getPlaceDetails(criteria);
    }

    @Override
    public List<PlaceSearchResult> searchPlaces(PlaceSearchCriteria criteria) {
        return googleClient.searchPlaces(criteria).stream()
            .map(place -> withDistance(place, criteria.latitude(), criteria.longitude()))
            .toList();
    }

    @Override
    public PlaceDetail getPlaceDetailByCoord(double latitude, double longitude, String languageCode) {
        return googleClient.reverseGeocode(latitude, longitude, languageCode);
    }

    private PlaceSearchResult withDistance(
        PlaceSearchResult place,
        Double requestLatitude,
        Double requestLongitude
    ) {
        Integer distanceMeters = null;
        if (requestLatitude == null || requestLongitude == null) {
            return place;
        }
        distanceMeters = GeoUtils.calculateDistanceMeters(
            requestLatitude, requestLongitude, place.latitude(), place.longitude()
        );
        return new PlaceSearchResult(
            place.name(), place.address(), place.latitude(), place.longitude(), place.provider(),
            place.providerPlaceId(), place.countryCode(), distanceMeters
        );
    }
}
