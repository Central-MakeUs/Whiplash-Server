package akuma.whiplash.domains.place.application.dto.response;

import java.util.List;
import lombok.Getter;

@Getter
public class ReverseGeocodeApiResponse {
    private List<Result> results;

    @Getter
    public static class Result {
        private String name; // "roadaddr" 또는 "addr"
        private Region region;
        private Land land;

        @Getter
        public static class Region {
            private Area area1;
            private Area area2;
            private Area area3;
            private Area area4;

            @Getter
            public static class Area {
                private String name;
            }
        }

        @Getter
        public static class Land {
            private String name;        // 도로명 (ex: 의사당대로)
            private String number1;     // 번지 (ex: 96)
            private String number2;     // 번지2 (ex: 2)
            private Addition addition0; // 건물명 등
            private Addition addition1;
            private Addition addition2;
            private Addition addition3;
            private Addition addition4;

            @Getter
            public static class Addition {
                private String type;
                private String value;
            }
        }
    }
}
