package akuma.whiplash.domains.place.application.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
public record PlaceInfoResponse(
    String name,
    String address,
    double latitude,
    double longitude,
    @JsonInclude(JsonInclude.Include.ALWAYS)
    Integer distanceMeters,
    String provider,
    @JsonInclude(JsonInclude.Include.ALWAYS)
    String providerPlaceId,
    @JsonInclude(JsonInclude.Include.ALWAYS)
    String countryCode
) {}
