package akuma.whiplash.domains.place.application.dto.response;

import lombok.Builder;

@Builder
public record PlaceDetailResponse(
    String address,
    String name
) {}