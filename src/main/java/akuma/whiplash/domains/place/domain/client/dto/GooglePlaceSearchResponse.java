package akuma.whiplash.domains.place.domain.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GooglePlaceSearchResponse(List<Place> places) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Place(
        String id,
        LocalizedText displayName,
        String formattedAddress,
        Location location,
        List<AddressComponent> addressComponents
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LocalizedText(String text, String languageCode) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Location(double latitude, double longitude) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AddressComponent(String longText, String shortText, List<String> types) {}
}
