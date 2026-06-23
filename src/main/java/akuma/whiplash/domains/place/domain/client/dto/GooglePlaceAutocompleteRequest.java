package akuma.whiplash.domains.place.domain.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GooglePlaceAutocompleteRequest(
    String input,
    String languageCode,
    String regionCode,
    String sessionToken,
    AutocompleteLocationBias locationBias
) {

    public record AutocompleteLocationBias(AutocompleteCircle circle) {}

    public record AutocompleteCircle(AutocompleteCenter center, double radius) {}

    public record AutocompleteCenter(double latitude, double longitude) {}
}
