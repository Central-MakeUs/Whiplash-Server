package akuma.whiplash.domains.place.domain.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GooglePlaceSearchRequest(
    String textQuery,
    int pageSize,
    String languageCode,
    String regionCode,
    LocationBias locationBias
) {

    public record LocationBias(Circle circle) {}

    public record Circle(Center center, double radius) {}

    public record Center(double latitude, double longitude) {}
}
