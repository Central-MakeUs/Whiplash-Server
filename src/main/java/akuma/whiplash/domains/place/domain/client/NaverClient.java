package akuma.whiplash.domains.place.domain.client;

import akuma.whiplash.domains.place.domain.client.dto.NaverLocalSearchResponse;
import akuma.whiplash.domains.place.domain.model.PlaceDetail;
import akuma.whiplash.domains.place.domain.model.PlaceSearchResult;
import java.util.List;

public interface NaverClient {

    PlaceDetail reverseGeocode(double latitude, double longitude);
    NaverLocalSearchResponse searchLocal(String query);
    List<PlaceSearchResult> searchPlaces(String query, int size);
}
