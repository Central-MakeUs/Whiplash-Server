package akuma.whiplash.domains.place.domain.model;

public record SelectedPlaceDetail(
    String address,
    double latitude,
    double longitude,
    String countryCode,
    String providerPlaceId
) {}
