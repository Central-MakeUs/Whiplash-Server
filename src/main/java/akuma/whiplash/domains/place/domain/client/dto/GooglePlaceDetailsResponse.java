package akuma.whiplash.domains.place.domain.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GooglePlaceDetailsResponse(
    String id,
    String formattedAddress,
    Location location,
    List<AddressComponent> addressComponents
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Location(double latitude, double longitude) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AddressComponent(String shortText, List<String> types) {}
}
