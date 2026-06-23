package akuma.whiplash.domains.place.application.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
public record PlaceDetailResponse(
    String address,
    String placeName,
    String roadAddress,
    double latitude,
    double longitude,
    String countryCode,
    @JsonInclude(JsonInclude.Include.ALWAYS)
    String providerPlaceId
) {}
