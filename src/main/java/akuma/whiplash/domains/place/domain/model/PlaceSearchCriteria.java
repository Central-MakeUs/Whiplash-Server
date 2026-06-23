package akuma.whiplash.domains.place.domain.model;

public record PlaceSearchCriteria(
    String query,
    Double latitude,
    Double longitude,
    int size,
    String languageCode,
    String regionCode
) {}
