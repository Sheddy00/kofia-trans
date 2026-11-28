package hei.school.kofiatrans.controller;

import hei.school.kofiatrans.controller.dto.DriverRefDto;
import hei.school.kofiatrans.controller.dto.ErrorResponseDto;
import hei.school.kofiatrans.controller.dto.TripResponseDto;
import hei.school.kofiatrans.controller.dto.TripStatusUpdateRequestDto;
import hei.school.kofiatrans.controller.dto.TripUpsertRequestDto;
import hei.school.kofiatrans.controller.dto.VehicleRefDto;
import hei.school.kofiatrans.db.DataRetriever;
import hei.school.kofiatrans.exception.ClientException;
import hei.school.kofiatrans.model.Driver;
import hei.school.kofiatrans.model.Trip;
import hei.school.kofiatrans.model.Vehicle;
import hei.school.kofiatrans.model.enums.TripStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/trips")
public class TripController {

    private final DataRetriever dataRetriever;

    public TripController(DataRetriever dataRetriever) {
        this.dataRetriever = dataRetriever;
    }

    public static final String TOTAL_COUNT_HEADER = "X-Total-Count";
    static final int MAX_PAGE_SIZE = 100;
    static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * Without {@code page}/{@code size} the whole filtered list is returned (unchanged behaviour).
     * With them, one page is returned and the total number of matching trips is sent in {@code X-Total-Count}.
     */
    @GetMapping
    public ResponseEntity<List<TripResponseDto>> listTrips(
            @RequestParam(required = false) String driverId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        if (page == null && size == null) {
            return ResponseEntity.ok(findTrips(driverId, from, to));
        }
        int pageNumber = page == null ? 0 : page;
        int pageSize = size == null ? DEFAULT_PAGE_SIZE : size;
        if (pageNumber < 0) {
            throw new ClientException("Le paramètre 'page' doit être positif ou nul");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new ClientException("Le paramètre 'size' doit être compris entre 1 et " + MAX_PAGE_SIZE);
        }
        LocalDate fromDate = parseOptionalDate("from", from);
        LocalDate toDate = parseOptionalDate("to", to);
        if ((fromDate == null) != (toDate == null)) {
            throw new ClientException("Les paramètres 'from' et 'to' doivent être fournis ensemble");
        }
        long total = dataRetriever.countTrips(driverId, fromDate, toDate);
        List<TripResponseDto> trips = dataRetriever
                .findTripsPage(driverId, fromDate, toDate, pageSize, Math.multiplyExact(pageNumber, pageSize))
                .stream().map(this::toDto).toList();
        return ResponseEntity.ok().header(TOTAL_COUNT_HEADER, String.valueOf(total)).body(trips);
    }

    public List<TripResponseDto> findTrips(String driverId, String from, String to) {
        LocalDate fromDate = parseOptionalDate("from", from);
        LocalDate toDate = parseOptionalDate("to", to);
        if ((fromDate == null) != (toDate == null)) {
            throw new ClientException("Les paramètres 'from' et 'to' doivent être fournis ensemble");
        }

        List<Trip> trips;
        if (driverId != null) {
            trips = dataRetriever.findTripsByDriver(driverId);
        } else if (fromDate != null) {
            trips = dataRetriever.findTripsByPeriod(fromDate, toDate);
        } else {
            trips = dataRetriever.findAllTrips();
        }

        if (driverId != null && fromDate != null) {
            LocalDate finalFrom = fromDate;
            LocalDate finalTo = toDate;
            trips = trips.stream()
                    .filter(trip -> !trip.getTripDate().isBefore(finalFrom) && !trip.getTripDate().isAfter(finalTo))
                    .toList();
        }

        return trips.stream().map(this::toDto).toList();
    }

    @PutMapping("/{tripId}")
    public ResponseEntity<TripResponseDto> upsertTrip(
            @PathVariable String tripId,
            @RequestBody TripUpsertRequestDto request
    ) {
        if (request == null
                || request.driverId() == null
                || request.vehicleId() == null
                || request.tripDate() == null
                || request.departureCity() == null
                || request.arrivalCity() == null
                || request.distanceKm() == null
                || request.billedAmount() == null) {
            throw new ClientException("Champs manquants dans le corps de la requête");
        }
        if (request.distanceKm() <= 0) {
            throw new ClientException("distanceKm doit être strictement positif");
        }
        if (request.billedAmount() < 0) {
            throw new ClientException("billedAmount ne peut pas être négatif");
        }

        Driver driver = dataRetriever.findDriverById(request.driverId());
        if (driver == null) {
            throw new ClientException("Chauffeur introuvable : " + request.driverId());
        }
        Vehicle vehicle = dataRetriever.findVehicleById(request.vehicleId());
        if (vehicle == null) {
            throw new ClientException("Véhicule introuvable : " + request.vehicleId());
        }

        boolean existed = dataRetriever.findTripById(tripId) != null;

        Trip trip = new Trip(
                tripId,
                driver,
                vehicle,
                request.tripDate(),
                request.departureCity(),
                request.arrivalCity(),
                request.distanceKm(),
                request.billedAmount(),
                TripStatus.COMPLETED
        );

        Trip saved = dataRetriever.saveTrip(trip);
        HttpStatus status = existed ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(toDto(saved));
    }

    @DeleteMapping("/{tripId}")
    public ResponseEntity<?> deleteTrip(@PathVariable String tripId) {
        if (!dataRetriever.deleteTrip(tripId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ErrorResponseDto.of(404, "Not Found", "Course introuvable : " + tripId)
            );
        }
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{tripId}/status")
    public ResponseEntity<?> updateTripStatus(
            @PathVariable String tripId,
            @RequestBody TripStatusUpdateRequestDto request
    ) {
        if (request == null || request.status() == null) {
            throw new ClientException("Le champ 'status' est requis");
        }
        TripStatus status;
        try {
            status = TripStatus.valueOf(request.status());
        } catch (IllegalArgumentException e) {
            throw new ClientException("Statut invalide : " + request.status());
        }

        if (dataRetriever.findTripById(tripId) == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ErrorResponseDto.of(404, "Not Found", "Course introuvable : " + tripId)
            );
        }

        Trip updated = dataRetriever.updateTripStatus(tripId, status);
        return ResponseEntity.ok(toDto(updated));
    }

    private LocalDate parseOptionalDate(String paramName, String value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new ClientException("Paramètre '" + paramName + "' invalide, format attendu AAAA-MM-JJ");
        }
    }

    private TripResponseDto toDto(Trip trip) {
        return new TripResponseDto(
                trip.getId(),
                new DriverRefDto(trip.getDriver().getId(), trip.getDriver().getName()),
                new VehicleRefDto(trip.getVehicle().getId(), trip.getVehicle().getPlateNumber()),
                trip.getTripDate(),
                trip.getDepartureCity(),
                trip.getArrivalCity(),
                trip.getDistanceKm(),
                trip.getBilledAmount(),
                trip.getStatus()
        );
    }
}
