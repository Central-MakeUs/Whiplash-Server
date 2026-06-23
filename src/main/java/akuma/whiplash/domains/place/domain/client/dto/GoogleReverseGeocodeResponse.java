package akuma.whiplash.domains.place.domain.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleReverseGeocodeResponse(List<Result> results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(
        String formattedAddress,
        List<AddressComponent> addressComponents,
        List<String> types
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AddressComponent(
        String longText,
        String shortText,
        List<String> types
    ) {}
}
