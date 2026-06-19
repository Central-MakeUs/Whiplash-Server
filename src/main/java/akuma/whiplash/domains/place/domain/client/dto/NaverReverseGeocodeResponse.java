package akuma.whiplash.domains.place.domain.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NaverReverseGeocodeResponse(Status status, List<Result> results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Status(Integer code) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(String name, Region region, Land land) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Region(Area area0, Area area1, Area area2, Area area3, Area area4) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Area(String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Land(String name, String number1, String number2, Addition addition0) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Addition(String value) {}
}
