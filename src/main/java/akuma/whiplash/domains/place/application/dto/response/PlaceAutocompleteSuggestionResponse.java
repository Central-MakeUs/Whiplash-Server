package akuma.whiplash.domains.place.application.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

public record PlaceAutocompleteSuggestionResponse(
    String mainText,
    @JsonInclude(JsonInclude.Include.ALWAYS)
    String secondaryText,
    String providerPlaceId
) {}
