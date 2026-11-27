package hei.school.kofiatrans.model;

import hei.school.kofiatrans.model.enums.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {
    private String id;
    private String plateNumber;
    private VehicleType type;
    private double capacityTons;
}
