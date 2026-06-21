package akuma.whiplash.domains.place.domain.model;

import akuma.whiplash.domains.place.domain.constant.PlaceProvider;

public record PlaceDetail(
    String address,
    String placeName,
    String roadAddress,
    double latitude,
    double longitude,
    String countryCode
) {}
