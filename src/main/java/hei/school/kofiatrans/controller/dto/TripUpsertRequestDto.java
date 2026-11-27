package hei.school.kofiatrans.controller.dto;

import java.time.LocalDate;

public record TripUpsertRequestDto(
        String driverId,
        String vehicleId,
        LocalDate tripDate,
        String departureCity,
        String arrivalCity,
        Integer distanceKm,
        Long billedAmount
) {
}
