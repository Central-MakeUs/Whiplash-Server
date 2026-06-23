package akuma.whiplash.domains.place.domain.client;

import akuma.whiplash.domains.place.domain.client.dto.NaverLocalSearchResponse;

public interface NaverClient {

    NaverLocalSearchResponse searchLocal(String query);
}
