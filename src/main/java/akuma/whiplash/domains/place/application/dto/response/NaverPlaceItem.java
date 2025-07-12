package akuma.whiplash.domains.place.application.dto.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class NaverPlaceItem {
    private String title;
    private String address;
    private String mapx;
    private String mapy;
}
