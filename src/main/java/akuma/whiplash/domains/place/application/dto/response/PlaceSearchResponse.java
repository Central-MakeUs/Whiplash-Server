package akuma.whiplash.domains.place.application.dto.response;

import java.util.List;
import lombok.Getter;

@Getter
public class PlaceSearchResponse {
    private List<NaverPlaceItem> items;
}