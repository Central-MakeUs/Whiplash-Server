package akuma.whiplash.domains.place.domain.service;

import akuma.whiplash.domains.place.domain.client.NaverClient;
import akuma.whiplash.domains.place.domain.client.dto.NaverLocalSearchResponse;
import akuma.whiplash.domains.place.domain.client.dto.NaverLocalSearchResponse.Item;
import akuma.whiplash.domains.place.domain.model.PlaceDetail;
import akuma.whiplash.domains.place.domain.model.PlaceSearchCriteria;
import akuma.whiplash.domains.place.domain.model.PlaceSearchResult;
import akuma.whiplash.global.util.GeoUtils;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlaceQueryServiceImpl implements PlaceQueryService {

    private final NaverClient naverClient;
    private final PlaceProviderRouter placeProviderRouter;

    @Override
    public List<PlaceSearchResult> searchPlaces(PlaceSearchCriteria criteria) {
        return placeProviderRouter.searchPlaces(criteria).stream()
            .map(place -> withDistance(place, criteria.latitude(), criteria.longitude()))
            .toList();
    }

    @Override
    public PlaceDetail getPlaceDetailByCoord(double latitude, double longitude, String languageCode) {
        return placeProviderRouter.getPlaceDetail(latitude, longitude, languageCode);
    }

    @Override
    public List<String> searchPlaceKeywords(String query) {
        NaverLocalSearchResponse response = naverClient.searchLocal(query);

        if (response == null || response.items() == null) return List.of();

        Pattern keywordPattern = Pattern.compile(".*?(동|로|길)");

        Set<String> keywordSuggestions = new LinkedHashSet<>();

        for (Item item : response.items()) {
            extractKeyword(item.address(), keywordPattern).ifPresent(keywordSuggestions::add);
            extractKeyword(item.roadAddress(), keywordPattern).ifPresent(keywordSuggestions::add);
        }

        return new ArrayList<>(keywordSuggestions);
    }

    private Optional<String> extractKeyword(String address, Pattern pattern) {
        if (address == null) return Optional.empty();
        Matcher matcher = pattern.matcher(address);
        return matcher.find() ? Optional.of(matcher.group()) : Optional.empty();
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
