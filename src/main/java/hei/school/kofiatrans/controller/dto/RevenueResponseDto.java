package hei.school.kofiatrans.controller.dto;

import java.time.LocalDate;

public record RevenueResponseDto(
        String driverId,
        String driverName,
        LocalDate from,
        LocalDate to,
        long revenue,
        long cooperativeFee
) {
}
