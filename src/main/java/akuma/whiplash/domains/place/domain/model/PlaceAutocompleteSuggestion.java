package akuma.whiplash.domains.place.domain.model;

public record PlaceAutocompleteSuggestion(
    String mainText,
    String secondaryText,
    String providerPlaceId
) {}
