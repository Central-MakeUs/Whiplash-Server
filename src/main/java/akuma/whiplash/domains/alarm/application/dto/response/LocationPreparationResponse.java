package akuma.whiplash.domains.alarm.application.dto.response;

public record LocationPreparationResponse(
    LocationPreparationState state,
    Long nextPollAfterMillis,
    boolean canCheckin,
    TargetLocation targetLocation
) {

    public static LocationPreparationResponse ready(Double latitude, Double longitude, String address) {
        return new LocationPreparationResponse(
            LocationPreparationState.READY,
            null,
            true,
            new TargetLocation(latitude, longitude, address)
        );
    }

    public static LocationPreparationResponse waiting(LocationPreparationState state) {
        return new LocationPreparationResponse(state, 1_000L, false, null);
    }

    public static LocationPreparationResponse terminal(LocationPreparationState state) {
        return new LocationPreparationResponse(state, null, false, null);
    }

    public record TargetLocation(Double latitude, Double longitude, String address) {
    }
}
