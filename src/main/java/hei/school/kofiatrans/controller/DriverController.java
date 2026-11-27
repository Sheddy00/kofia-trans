package hei.school.kofiatrans.controller;

import hei.school.kofiatrans.controller.dto.DriverStatisticsResponseDto;
import hei.school.kofiatrans.controller.dto.ErrorResponseDto;
import hei.school.kofiatrans.controller.dto.RevenueResponseDto;
import hei.school.kofiatrans.db.DataRetriever;
import hei.school.kofiatrans.exception.ClientException;
import hei.school.kofiatrans.model.Driver;
import hei.school.kofiatrans.service.RevenueService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/drivers")
public class DriverController {

    private final DataRetriever dataRetriever;
    private final RevenueService revenueService;

    public DriverController(DataRetriever dataRetriever, RevenueService revenueService) {
        this.dataRetriever = dataRetriever;
        this.revenueService = revenueService;
    }

    @GetMapping("/{driverId}/revenue")
    public ResponseEntity<?> getRevenue(
            @PathVariable String driverId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        if (from == null || to == null) {
            throw new ClientException("Les paramètres 'from' et 'to' sont obligatoires");
        }
        LocalDate fromDate = parseDate("from", from);
        LocalDate toDate = parseDate("to", to);

        Driver driver = dataRetriever.findDriverById(driverId);
        if (driver == null) {
            return notFound("Chauffeur introuvable : " + driverId);
        }

        long revenue = revenueService.computeDriverRevenue(driverId, fromDate, toDate);
        long fee = revenueService.computeCooperativeFee(driverId, fromDate, toDate);

        return ResponseEntity.ok(new RevenueResponseDto(
                driver.getId(), driver.getName(), fromDate, toDate, revenue, fee
        ));
    }

    @GetMapping("/{driverId}/statistics")
    public ResponseEntity<?> getStatistics(@PathVariable String driverId) {
        Driver driver = dataRetriever.findDriverById(driverId);
        if (driver == null) {
            return notFound("Chauffeur introuvable : " + driverId);
        }

        int completedTrips = revenueService.computeCompletedTripsCount(driverId);
        long totalDistanceKm = revenueService.computeTotalDistanceKm(driverId);
        double averagePricePerKm = revenueService.computeAveragePricePerKm(driverId);

        return ResponseEntity.ok(new DriverStatisticsResponseDto(
                driver.getId(), driver.getName(), completedTrips, totalDistanceKm, averagePricePerKm
        ));
    }

    private LocalDate parseDate(String paramName, String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new ClientException("Paramètre '" + paramName + "' invalide, format attendu AAAA-MM-JJ");
        }
    }

    private ResponseEntity<ErrorResponseDto> notFound(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponseDto.of(404, "Not Found", message));
    }
}
