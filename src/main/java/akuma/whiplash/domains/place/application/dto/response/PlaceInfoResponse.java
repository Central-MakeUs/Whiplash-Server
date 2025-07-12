package akuma.whiplash.domains.place.application.dto.response;

import lombok.Builder;

@Builder
public record PlaceInfoResponse(
    String name,
    String address,
    double latitude,
    double longitude
) {}