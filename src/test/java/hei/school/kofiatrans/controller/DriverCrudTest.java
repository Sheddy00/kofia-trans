package hei.school.kofiatrans.controller;

import hei.school.kofiatrans.controller.dto.DriverDto;
import hei.school.kofiatrans.controller.dto.DriverUpsertRequestDto;
import hei.school.kofiatrans.db.DataRetriever;
import hei.school.kofiatrans.exception.ClientException;
import hei.school.kofiatrans.exception.ConflictException;
import hei.school.kofiatrans.model.Driver;
import hei.school.kofiatrans.model.enums.LicenseCategory;
import hei.school.kofiatrans.service.RevenueService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverCrudTest {

    private static final LocalDate DATE = LocalDate.of(2024, 1, 10);

    @Mock
    private DataRetriever dataRetriever;

    @Mock
    private RevenueService revenueService;

    @InjectMocks
    private DriverController controller;

    @Test
    void findDrivers_shouldReturnAllDriversSortedByName() {
        when(dataRetriever.findAllDrivers()).thenReturn(List.of(
                new Driver("d-2", "Zo", LicenseCategory.C, DATE),
                new Driver("d-1", "alice", LicenseCategory.B, DATE)));

        assertThat(controller.findDrivers()).extracting(DriverDto::name).containsExactly("alice", "Zo");
    }

    @Test
    void findDriver_shouldReturn404_whenUnknown() {
        assertThat(controller.findDriver("d-9").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void upsertDriver_shouldCreate_thenUpdate() {
        Driver saved = new Driver("d-6", "Lova Ranaivo", LicenseCategory.D, DATE);
        when(dataRetriever.saveDriver(any())).thenReturn(saved);

        when(dataRetriever.findDriverById("d-6")).thenReturn(null);
        ResponseEntity<DriverDto> created = controller.upsertDriver("d-6", new DriverUpsertRequestDto("  Lova Ranaivo ", "D", DATE));
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().licenseCategory()).isEqualTo("D");

        ArgumentCaptor<Driver> captor = ArgumentCaptor.forClass(Driver.class);
        verify(dataRetriever).saveDriver(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Lova Ranaivo");

        when(dataRetriever.findDriverById("d-6")).thenReturn(saved);
        assertThat(controller.upsertDriver("d-6", new DriverUpsertRequestDto("Lova", "D", DATE)).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void upsertDriver_shouldRejectInvalidInput() {
        assertThatThrownBy(() -> controller.upsertDriver("d-6", new DriverUpsertRequestDto(" ", "B", DATE)))
                .isInstanceOf(ClientException.class).hasMessageContaining("name");
        assertThatThrownBy(() -> controller.upsertDriver("d-6", new DriverUpsertRequestDto("Lova", "Z", DATE)))
                .isInstanceOf(ClientException.class).hasMessageContaining("licenseCategory");
        assertThatThrownBy(() -> controller.upsertDriver("d-6", new DriverUpsertRequestDto("Lova", "B", null)))
                .isInstanceOf(ClientException.class).hasMessageContaining("affiliationDate");
        assertThatThrownBy(() -> controller.upsertDriver("d-123456789012345678901", new DriverUpsertRequestDto("Lova", "B", DATE)))
                .isInstanceOf(ClientException.class).hasMessageContaining("Identifiant");
        verify(dataRetriever, never()).saveDriver(any());
    }

    @Test
    void deleteDriver_shouldReturn204_or404() {
        when(dataRetriever.deleteDriver("d-1")).thenReturn(true);
        when(dataRetriever.deleteDriver("d-9")).thenReturn(false);

        assertThat(controller.deleteDriver("d-1").getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.deleteDriver("d-9").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteDriver_shouldPropagateConflict_whenTripsReferenceIt() {
        when(dataRetriever.deleteDriver("d-1")).thenThrow(new ConflictException("4 course(s)"));

        assertThatThrownBy(() -> controller.deleteDriver("d-1")).isInstanceOf(ConflictException.class);
    }
}
