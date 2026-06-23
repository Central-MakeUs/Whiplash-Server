package akuma.whiplash.domains.place.domain.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GooglePlaceAutocompleteResponse(List<Suggestion> suggestions) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Suggestion(PlacePrediction placePrediction) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlacePrediction(
        String placeId,
        StructuredFormat structuredFormat
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StructuredFormat(LocalizedText mainText, LocalizedText secondaryText) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LocalizedText(String text) {}
}
