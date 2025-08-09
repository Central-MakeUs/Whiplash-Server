package akuma.whiplash.domains.alarm.application.dto.etc;

public record PushTargetDto(
    String token,
    String address,
    Long memberId,
    Long occurrenceId
) {

}
