package akuma.whiplash.infrastructure.firebase.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FcmMetricResult {
    private final int successCount;
    private final int failedCount;
}
