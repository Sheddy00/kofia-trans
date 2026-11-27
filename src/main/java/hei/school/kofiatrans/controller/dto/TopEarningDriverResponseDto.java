package hei.school.kofiatrans.controller.dto;

public record TopEarningDriverResponseDto(
        String driverId,
        String driverName,
        long revenue
) {
}
