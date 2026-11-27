package hei.school.kofiatrans.model;

import hei.school.kofiatrans.model.enums.LicenseCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Driver {
    private String id;
    private String name;
    private LicenseCategory licenseCategory;
    private LocalDate affiliationDate;
}
