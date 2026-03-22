package akuma.whiplash.loadtest.presentation;

import akuma.whiplash.global.response.ApplicationResponse;
import akuma.whiplash.loadtest.application.usecase.AlarmPipelineLoadTestUseCase;
import akuma.whiplash.loadtest.application.usecase.FcmBulkSendLoadTestUseCase;
import akuma.whiplash.loadtest.application.usecase.FcmTokenLoadTestUseCase;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 부하 테스트 전용 컨트롤러
 *
 * @Profile("!prod") — prod 환경에서는 Bean 자체가 생성되지 않아 엔드포인트가 노출되지 않는다.
 *
 * 3가지 개선 사례에 대한 AS-IS / TO-BE 비교 엔드포인트를 하나의 컨트롤러에 통합한다:
 *   - /api/load-test/fcm-bulk/**      사례 1. FCM 대량 발송 최적화
 *   - /api/load-test/alarm-pipeline/** 사례 2. 알람 파이프라인 멱등성 및 재시도
 *   - /api/load-test/fcm-token/**     사례 3. Redis 다중 디바이스 FCM 토큰 관리
 */
@Profile("!prod")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/load-test")
public class LoadTestController {

    private final FcmBulkSendLoadTestUseCase fcmBulkUseCase;
    private final AlarmPipelineLoadTestUseCase alarmPipelineUseCase;
    private final FcmTokenLoadTestUseCase fcmTokenUseCase;

    // =========================================================================
    // 사례 1. FCM 대량 발송 최적화  (/api/load-test/fcm-bulk)
    // =========================================================================

    /** 테스트 회원 생성 + 각 회원에 FCM 토큰 등록 */
    @PostMapping("/fcm-bulk/setup")
    public ApplicationResponse<FcmBulkSendLoadTestUseCase.SetupResponse> fcmBulkSetup(
        @RequestParam(defaultValue = "10") int memberCount,
        @RequestParam(defaultValue = "3") int tokensPerMember
    ) {
        return ApplicationResponse.onSuccess(fcmBulkUseCase.setup(memberCount, tokensPerMember));
    }

    /**
     * AS-IS: 토큰 1건씩 순차 전송 시뮬레이션
     * totalMs ≈ tokenCount × fcmLatencyMs
     */
    @PostMapping("/fcm-bulk/send-sequential")
    public ApplicationResponse<FcmBulkSendLoadTestUseCase.SendResponse> fcmBulkSendSequential(
        @RequestParam List<Long> memberIds,
        @RequestParam(defaultValue = "1") int iterations,
        @RequestParam(defaultValue = "100") long fcmLatencyMs
    ) {
        return ApplicationResponse.onSuccess(fcmBulkUseCase.sendSequential(memberIds, iterations, fcmLatencyMs));
    }

    /**
     * TO-BE: 500건 배치 + 병렬 전송 시뮬레이션
     * totalMs ≈ ceil(tokenCount / 500) × fcmLatencyMs
     */
    @PostMapping("/fcm-bulk/send-batch")
    public ApplicationResponse<FcmBulkSendLoadTestUseCase.SendResponse> fcmBulkSendBatch(
        @RequestParam List<Long> memberIds,
        @RequestParam(defaultValue = "1") int iterations,
        @RequestParam(defaultValue = "100") long fcmLatencyMs
    ) {
        return ApplicationResponse.onSuccess(fcmBulkUseCase.sendBatch(memberIds, iterations, fcmLatencyMs));
    }

    /** 테스트 회원 + Redis 토큰 정리 */
    @DeleteMapping("/fcm-bulk/cleanup")
    public ApplicationResponse<String> fcmBulkCleanup(
        @RequestParam List<Long> memberIds,
        @RequestParam(defaultValue = "3") int tokensPerMember
    ) {
        fcmBulkUseCase.cleanup(memberIds, tokensPerMember);
        return ApplicationResponse.onSuccess("cleanup completed");
    }

    // =========================================================================
    // 사례 2. 알람 파이프라인 — 배치 멱등성 및 재시도  (/api/load-test/alarm-pipeline)
    // =========================================================================

    /** 테스트 회원 생성 + 각 회원에 알람 1개 생성 */
    @PostMapping("/alarm-pipeline/setup")
    public ApplicationResponse<AlarmPipelineLoadTestUseCase.SetupResponse> alarmPipelineSetup(
        @RequestParam(defaultValue = "5") int memberCount
    ) {
        return ApplicationResponse.onSuccess(alarmPipelineUseCase.setup(memberCount));
    }

    /**
     * AS-IS: 중복 체크 없이 INSERT 반복
     * 2번째+ 시도: DataIntegrityViolationException → failed 카운트 증가
     * (date 파라미터: 동일 date 재사용 시 unique constraint 충돌 발생)
     */
    @PostMapping("/alarm-pipeline/create-occurrences-no-guard")
    public ApplicationResponse<AlarmPipelineLoadTestUseCase.BatchResult> alarmPipelineCreateNoGuard(
        @RequestParam List<Long> memberIds,
        @RequestParam LocalDate date,
        @RequestParam(defaultValue = "3") int iterations
    ) {
        return ApplicationResponse.onSuccess(
            alarmPipelineUseCase.createOccurrencesWithoutGuard(memberIds, date, iterations));
    }

    /**
     * TO-BE: 스킵 로직으로 멱등성 보장
     * 2번째+ 시도: existingIds 체크로 skip → skipped 카운트 증가
     * (date 파라미터: AS-IS와 다른 날짜를 사용해야 독립 측정 가능)
     */
    @PostMapping("/alarm-pipeline/create-occurrences")
    public ApplicationResponse<AlarmPipelineLoadTestUseCase.BatchResult> alarmPipelineCreate(
        @RequestParam List<Long> memberIds,
        @RequestParam LocalDate date,
        @RequestParam(defaultValue = "3") int iterations
    ) {
        return ApplicationResponse.onSuccess(
            alarmPipelineUseCase.createOccurrencesWithGuard(memberIds, date, iterations));
    }

    /** 테스트 occurrence + alarm + member 정리 */
    @DeleteMapping("/alarm-pipeline/cleanup")
    public ApplicationResponse<String> alarmPipelineCleanup(
        @RequestParam List<Long> memberIds
    ) {
        alarmPipelineUseCase.cleanup(memberIds);
        return ApplicationResponse.onSuccess("cleanup completed");
    }

    // =========================================================================
    // 사례 3. Redis 다중 디바이스 FCM 토큰 관리  (/api/load-test/fcm-token)
    // =========================================================================

    /** 테스트 회원 생성 */
    @PostMapping("/fcm-token/setup")
    public ApplicationResponse<FcmTokenLoadTestUseCase.SetupResponse> fcmTokenSetup(
        @RequestParam(defaultValue = "2") int memberCount
    ) {
        return ApplicationResponse.onSuccess(fcmTokenUseCase.setup(memberCount));
    }

    /**
     * AS-IS: 비원자적 토큰 등록 (트랜잭션 없이 개별 Redis 명령 실행)
     * 동시 요청 시 stale token 잔류 가능
     */
    @PostMapping("/fcm-token/register-non-atomic")
    public ApplicationResponse<FcmTokenLoadTestUseCase.RegisterResponse> fcmTokenRegisterNonAtomic(
        @RequestBody TokenRegisterRequest request
    ) {
        return ApplicationResponse.onSuccess(
            fcmTokenUseCase.registerNonAtomic(request.memberId(), request.deviceId(), request.fcmToken()));
    }

    /**
     * TO-BE: MULTI/EXEC 원자적 토큰 등록
     * old token 정리 + new token 등록이 원자적으로 처리됨
     */
    @PostMapping("/fcm-token/register-atomic")
    public ApplicationResponse<FcmTokenLoadTestUseCase.RegisterResponse> fcmTokenRegisterAtomic(
        @RequestBody TokenRegisterRequest request
    ) {
        return ApplicationResponse.onSuccess(
            fcmTokenUseCase.registerAtomic(request.memberId(), request.deviceId(), request.fcmToken()));
    }

    /** 특정 회원의 현재 FCM 토큰 수와 목록 조회 (AS-IS/TO-BE 결과 검증용) */
    @GetMapping("/fcm-token/verify/{memberId}")
    public ApplicationResponse<FcmTokenLoadTestUseCase.VerifyResponse> fcmTokenVerify(
        @PathVariable Long memberId
    ) {
        return ApplicationResponse.onSuccess(fcmTokenUseCase.verify(memberId));
    }

    /** 테스트 회원 + Redis 토큰 키 정리 */
    @DeleteMapping("/fcm-token/cleanup")
    public ApplicationResponse<String> fcmTokenCleanup(
        @RequestParam List<Long> memberIds
    ) {
        fcmTokenUseCase.cleanup(memberIds);
        return ApplicationResponse.onSuccess("cleanup completed");
    }

    // ===== Request Records =====

    public record TokenRegisterRequest(Long memberId, String deviceId, String fcmToken) {}
}
