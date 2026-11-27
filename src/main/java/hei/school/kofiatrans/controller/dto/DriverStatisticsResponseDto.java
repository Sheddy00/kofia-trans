package hei.school.kofiatrans.controller.dto;

public record DriverStatisticsResponseDto(
        String driverId,
        String driverName,
        int completedTrips,
        long totalDistanceKm,
        double averagePricePerKm
) {
}
