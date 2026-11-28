package hei.school.kofiatrans.controller;

import hei.school.kofiatrans.db.DataRetriever;
import hei.school.kofiatrans.exception.ClientException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripPaginationTest {

    @Mock
    private DataRetriever dataRetriever;

    @InjectMocks
    private TripController controller;

    @Test
    void listTrips_withPage_shouldQueryOnePageAndSendTotal() {
        LocalDate from = LocalDate.of(2026, 4, 1);
        LocalDate to = LocalDate.of(2026, 4, 30);
        when(dataRetriever.countTrips("d-1", from, to)).thenReturn(42L);
        when(dataRetriever.findTripsPage("d-1", from, to, 10, 20)).thenReturn(List.of());

        var response = controller.listTrips("d-1", "2026-04-01", "2026-04-30", 2, 10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(TripController.TOTAL_COUNT_HEADER)).isEqualTo("42");
        verify(dataRetriever).findTripsPage("d-1", from, to, 10, 20);
    }

    @Test
    void listTrips_withOnlySize_shouldStartAtFirstPage() {
        when(dataRetriever.countTrips(null, null, null)).thenReturn(3L);
        when(dataRetriever.findTripsPage(null, null, null, 5, 0)).thenReturn(List.of());

        controller.listTrips(null, null, null, null, 5);

        verify(dataRetriever).findTripsPage(null, null, null, 5, 0);
    }

    @Test
    void listTrips_withoutPaging_shouldKeepFullListWithoutTotalHeader() {
        when(dataRetriever.findAllTrips()).thenReturn(List.of());

        var response = controller.listTrips(null, null, null, null, null);

        assertThat(response.getHeaders().containsHeader(TripController.TOTAL_COUNT_HEADER)).isFalse();
        verify(dataRetriever).findAllTrips();
    }

    @Test
    void listTrips_shouldRejectInvalidPaging() {
        assertThatThrownBy(() -> controller.listTrips(null, null, null, -1, 10)).isInstanceOf(ClientException.class);
        assertThatThrownBy(() -> controller.listTrips(null, null, null, 0, 0)).isInstanceOf(ClientException.class);
        assertThatThrownBy(() -> controller.listTrips(null, null, null, 0, 101)).isInstanceOf(ClientException.class);
    }

    @Test
    void deleteTrip_shouldReturn204_or404() {
        when(dataRetriever.deleteTrip("t-1")).thenReturn(true);
        when(dataRetriever.deleteTrip("t-9")).thenReturn(false);

        assertThat(controller.deleteTrip("t-1").getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.deleteTrip("t-9").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
