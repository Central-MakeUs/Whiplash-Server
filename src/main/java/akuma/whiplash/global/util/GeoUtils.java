package akuma.whiplash.global.util;

public class GeoUtils {

    private static final double EARTH_RADIUS_METERS = 6_371_000;

    private GeoUtils() {
        throw new IllegalArgumentException();
    }

    public static int calculateDistanceMeters(double latitude1, double longitude1, double latitude2, double longitude2) {
        double latitudeDistance = Math.toRadians(latitude2 - latitude1);
        double longitudeDistance = Math.toRadians(longitude2 - longitude1);

        double fromLatitude = Math.toRadians(latitude1);
        double toLatitude = Math.toRadians(latitude2);

        double haversine = Math.pow(Math.sin(latitudeDistance / 2), 2)
            + Math.cos(fromLatitude) * Math.cos(toLatitude) * Math.pow(Math.sin(longitudeDistance / 2), 2);
        double centralAngle = 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));

        return (int) Math.round(EARTH_RADIUS_METERS * centralAngle);
    }
}
