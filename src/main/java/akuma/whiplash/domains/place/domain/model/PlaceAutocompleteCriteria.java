package akuma.whiplash.domains.place.domain.model;

public record PlaceAutocompleteCriteria(
    String query,
    Double latitude,
    Double longitude,
    String languageCode,
    String regionCode,
    String sessionToken
) {}
