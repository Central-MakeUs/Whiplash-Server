package akuma.whiplash.domains.alarm.application.dto.response;

public enum LocationPreparationState {
    READY,
    PREPARING,
    DELAYED,
    RETRYING,
    EXHAUSTED,
    UNAVAILABLE,
    BUSY
}
