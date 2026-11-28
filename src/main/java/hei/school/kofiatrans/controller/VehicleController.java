package hei.school.kofiatrans.controller;

import hei.school.kofiatrans.controller.dto.ErrorResponseDto;
import hei.school.kofiatrans.controller.dto.VehicleDto;
import hei.school.kofiatrans.controller.dto.VehicleUpsertRequestDto;
import hei.school.kofiatrans.db.DataRetriever;
import hei.school.kofiatrans.exception.ClientException;
import hei.school.kofiatrans.model.Vehicle;
import hei.school.kofiatrans.model.enums.VehicleType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/vehicles")
public class VehicleController {

    private final DataRetriever dataRetriever;

    public VehicleController(DataRetriever dataRetriever) {
        this.dataRetriever = dataRetriever;
    }

    @GetMapping
    public List<VehicleDto> findVehicles() {
        return dataRetriever.findAllVehicles().stream().map(VehicleController::toDto).toList();
    }

    @GetMapping("/{vehicleId}")
    public ResponseEntity<?> findVehicle(@PathVariable String vehicleId) {
        Vehicle vehicle = dataRetriever.findVehicleById(vehicleId);
        if (vehicle == null) {
            return notFound(vehicleId);
        }
        return ResponseEntity.ok(toDto(vehicle));
    }

    @PutMapping("/{vehicleId}")
    public ResponseEntity<VehicleDto> upsertVehicle(@PathVariable String vehicleId,
                                                    @RequestBody VehicleUpsertRequestDto request) {
        Validation.requireId(vehicleId, "véhicule");
        if (request == null) {
            throw new ClientException("Corps de requête manquant");
        }
        String plate = Validation.requireText(request.plateNumber(), "plateNumber", 20).toUpperCase();
        VehicleType type = Validation.requireEnum(request.type(), VehicleType.class, "type");
        if (request.capacityTons() == null || request.capacityTons() <= 0) {
            throw new ClientException("capacityTons doit être strictement positif");
        }
        boolean existed = dataRetriever.findVehicleById(vehicleId) != null;
        Vehicle saved = dataRetriever.saveVehicle(new Vehicle(vehicleId, plate, type, request.capacityTons()));
        return ResponseEntity.status(existed ? HttpStatus.OK : HttpStatus.CREATED).body(toDto(saved));
    }

    @DeleteMapping("/{vehicleId}")
    public ResponseEntity<?> deleteVehicle(@PathVariable String vehicleId) {
        if (!dataRetriever.deleteVehicle(vehicleId)) {
            return notFound(vehicleId);
        }
        return ResponseEntity.noContent().build();
    }

    private static VehicleDto toDto(Vehicle vehicle) {
        return new VehicleDto(vehicle.getId(), vehicle.getPlateNumber(), vehicle.getType().name(), vehicle.getCapacityTons());
    }

    private static ResponseEntity<ErrorResponseDto> notFound(String vehicleId) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponseDto.of(404, "Not Found", "Véhicule introuvable : " + vehicleId));
    }
}
