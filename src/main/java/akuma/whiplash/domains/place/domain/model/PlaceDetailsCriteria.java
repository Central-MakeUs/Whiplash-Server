package akuma.whiplash.domains.place.domain.model;

public record PlaceDetailsCriteria(
    String providerPlaceId,
    String sessionToken,
    String languageCode,
    String regionCode
) {}
