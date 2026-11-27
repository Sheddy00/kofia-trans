package hei.school.kofiatrans.model;

import hei.school.kofiatrans.model.enums.TripStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Trip {
    private String id;
    private Driver driver;
    private Vehicle vehicle;
    private LocalDate tripDate;
    private String departureCity;
    private String arrivalCity;
    private int distanceKm;
    private long billedAmount;
    private TripStatus status;

    public boolean isBillable() {
        return status == TripStatus.COMPLETED;
    }

    public double pricePerKm() {
        if (distanceKm == 0) {
            return 0.0;
        }
        return (double) billedAmount / distanceKm;
    }
}
