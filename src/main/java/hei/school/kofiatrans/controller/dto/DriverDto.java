package hei.school.kofiatrans.controller.dto;

import java.time.LocalDate;

public record DriverDto(String id, String name, String licenseCategory, LocalDate affiliationDate) {
}
