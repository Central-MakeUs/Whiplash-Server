package akuma.whiplash.domains.place.domain.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleReverseGeocodeResponse(String status, List<Result> results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(
        @JsonProperty("formatted_address") String formattedAddress,
        @JsonProperty("address_components") List<AddressComponent> addressComponents
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AddressComponent(
        @JsonProperty("long_name") String longName,
        @JsonProperty("short_name") String shortName,
        List<String> types
    ) {}
}
