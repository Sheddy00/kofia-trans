package hei.school.kofiatrans.controller;

import hei.school.kofiatrans.controller.dto.VehicleDto;
import hei.school.kofiatrans.controller.dto.VehicleUpsertRequestDto;
import hei.school.kofiatrans.db.DataRetriever;
import hei.school.kofiatrans.exception.ClientException;
import hei.school.kofiatrans.model.Vehicle;
import hei.school.kofiatrans.model.enums.VehicleType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleControllerTest {

    @Mock
    private DataRetriever dataRetriever;

    @InjectMocks
    private VehicleController controller;

    @Test
    void findVehicles_shouldMapAllVehicles() {
        when(dataRetriever.findAllVehicles()).thenReturn(List.of(new Vehicle("v-1", "TAA-1001", VehicleType.CAMION, 12.5)));

        assertThat(controller.findVehicles()).containsExactly(new VehicleDto("v-1", "TAA-1001", "CAMION", 12.5));
    }

    @Test
    void findVehicle_shouldReturn404_whenUnknown() {
        assertThat(controller.findVehicle("v-9").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void upsertVehicle_shouldCreateWithUppercasedPlate() {
        when(dataRetriever.saveVehicle(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = controller.upsertVehicle("v-5", new VehicleUpsertRequestDto(" tdd-4001 ", "FOURGON", 3.0));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ArgumentCaptor<Vehicle> captor = ArgumentCaptor.forClass(Vehicle.class);
        verify(dataRetriever).saveVehicle(captor.capture());
        assertThat(captor.getValue().getPlateNumber()).isEqualTo("TDD-4001");
    }

    @Test
    void upsertVehicle_shouldRejectInvalidInput() {
        assertThatThrownBy(() -> controller.upsertVehicle("v-5", new VehicleUpsertRequestDto("", "FOURGON", 3.0)))
                .isInstanceOf(ClientException.class).hasMessageContaining("plateNumber");
        assertThatThrownBy(() -> controller.upsertVehicle("v-5", new VehicleUpsertRequestDto("TDD-4001", "BUS", 3.0)))
                .isInstanceOf(ClientException.class).hasMessageContaining("type");
        assertThatThrownBy(() -> controller.upsertVehicle("v-5", new VehicleUpsertRequestDto("TDD-4001", "FOURGON", 0.0)))
                .isInstanceOf(ClientException.class).hasMessageContaining("capacityTons");
        verify(dataRetriever, never()).saveVehicle(any());
    }

    @Test
    void deleteVehicle_shouldReturn204_or404() {
        when(dataRetriever.deleteVehicle("v-1")).thenReturn(true);
        when(dataRetriever.deleteVehicle("v-9")).thenReturn(false);

        assertThat(controller.deleteVehicle("v-1").getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.deleteVehicle("v-9").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
