package hei.school.kofiatrans.controller.dto;

import java.time.LocalDate;

public record DriverUpsertRequestDto(String name, String licenseCategory, LocalDate affiliationDate) {
}
