package hei.school.kofiatrans.controller;

import hei.school.kofiatrans.controller.dto.DriverDto;
import hei.school.kofiatrans.controller.dto.DriverStatisticsResponseDto;
import hei.school.kofiatrans.controller.dto.DriverUpsertRequestDto;
import hei.school.kofiatrans.controller.dto.ErrorResponseDto;
import hei.school.kofiatrans.controller.dto.RevenueResponseDto;
import hei.school.kofiatrans.db.DataRetriever;
import hei.school.kofiatrans.exception.ClientException;
import hei.school.kofiatrans.model.Driver;
import hei.school.kofiatrans.model.enums.LicenseCategory;
import hei.school.kofiatrans.service.RevenueService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/drivers")
public class DriverController {

    private final DataRetriever dataRetriever;
    private final RevenueService revenueService;

    public DriverController(DataRetriever dataRetriever, RevenueService revenueService) {
        this.dataRetriever = dataRetriever;
        this.revenueService = revenueService;
    }

    @GetMapping
    public List<DriverDto> findDrivers() {
        return dataRetriever.findAllDrivers().stream()
                .sorted(Comparator.comparing(Driver::getName, String.CASE_INSENSITIVE_ORDER))
                .map(DriverController::toDto)
                .toList();
    }

    @GetMapping("/{driverId}")
    public ResponseEntity<?> findDriver(@PathVariable String driverId) {
        Driver driver = dataRetriever.findDriverById(driverId);
        if (driver == null) {
            return notFound("Chauffeur introuvable : " + driverId);
        }
        return ResponseEntity.ok(toDto(driver));
    }

    @PutMapping("/{driverId}")
    public ResponseEntity<DriverDto> upsertDriver(@PathVariable String driverId,
                                                  @RequestBody DriverUpsertRequestDto request) {
        Validation.requireId(driverId, "chauffeur");
        if (request == null) {
            throw new ClientException("Corps de requête manquant");
        }
        String name = Validation.requireText(request.name(), "name", 100);
        LicenseCategory category = Validation.requireEnum(request.licenseCategory(), LicenseCategory.class, "licenseCategory");
        if (request.affiliationDate() == null) {
            throw new ClientException("Le champ 'affiliationDate' est obligatoire");
        }
        boolean existed = dataRetriever.findDriverById(driverId) != null;
        Driver saved = dataRetriever.saveDriver(new Driver(driverId, name, category, request.affiliationDate()));
        return ResponseEntity.status(existed ? HttpStatus.OK : HttpStatus.CREATED).body(toDto(saved));
    }

    @DeleteMapping("/{driverId}")
    public ResponseEntity<?> deleteDriver(@PathVariable String driverId) {
        if (!dataRetriever.deleteDriver(driverId)) {
            return notFound("Chauffeur introuvable : " + driverId);
        }
        return ResponseEntity.noContent().build();
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

    private static DriverDto toDto(Driver driver) {
        return new DriverDto(driver.getId(), driver.getName(), driver.getLicenseCategory().name(), driver.getAffiliationDate());
    }
}
