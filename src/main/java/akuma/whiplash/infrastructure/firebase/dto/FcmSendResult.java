package akuma.whiplash.infrastructure.firebase.dto;

import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FcmSendResult {
    private final Set<Long> successOccurrenceIds;          // 적어도 1개 토큰 전송 성공한 occurrence
    private final List<String> invalidTokens;              // 등록 말소 대상 토큰
    private final Map<Long, List<String>> memberToTokens;  // (선택) 멤버별 성공 토큰 집계
    private final int successCount;
    private final int failedCount;
}
