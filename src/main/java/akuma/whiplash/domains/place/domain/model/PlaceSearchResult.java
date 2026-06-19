package akuma.whiplash.domains.place.domain.model;

import akuma.whiplash.domains.place.domain.constant.PlaceProvider;

public record PlaceSearchResult(
    String name,
    String address,
    double latitude,
    double longitude,
    PlaceProvider provider,
    String providerPlaceId,
    String countryCode,
    Integer distanceMeters
) {}
