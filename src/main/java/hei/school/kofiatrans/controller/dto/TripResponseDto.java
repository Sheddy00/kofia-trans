package hei.school.kofiatrans.controller.dto;

import hei.school.kofiatrans.model.enums.TripStatus;

import java.time.LocalDate;

public record TripResponseDto(
        String id,
        DriverRefDto driver,
        VehicleRefDto vehicle,
        LocalDate tripDate,
        String departureCity,
        String arrivalCity,
        int distanceKm,
        long billedAmount,
        TripStatus status
) {
}
