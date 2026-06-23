package akuma.whiplash.domains.place.application.dto.response;

import java.util.List;

public record PlaceAutocompleteResponse(
    List<PlaceAutocompleteSuggestionResponse> suggestions
) {}
