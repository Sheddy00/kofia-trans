package hei.school.kofiatrans.controller;

import hei.school.kofiatrans.controller.dto.ErrorResponseDto;
import hei.school.kofiatrans.controller.dto.TopEarningDriverResponseDto;
import hei.school.kofiatrans.exception.ClientException;
import hei.school.kofiatrans.model.Driver;
import hei.school.kofiatrans.service.RevenueService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@RestController
public class StatisticsController {

    private final RevenueService revenueService;

    public StatisticsController(RevenueService revenueService) {
        this.revenueService = revenueService;
    }

    @GetMapping("/statistics/top-earning-driver")
    public ResponseEntity<?> getTopEarningDriver(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        if (from == null || to == null) {
            throw new ClientException("Les paramètres 'from' et 'to' sont obligatoires");
        }
        LocalDate fromDate = parseDate("from", from);
        LocalDate toDate = parseDate("to", to);

        Driver topDriver = revenueService.findTopEarningDriver(fromDate, toDate);
        if (topDriver == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ErrorResponseDto.of(404, "Not Found", "Aucun chauffeur trouvé")
            );
        }

        long revenue = revenueService.computeDriverRevenue(topDriver.getId(), fromDate, toDate);
        return ResponseEntity.ok(new TopEarningDriverResponseDto(
                topDriver.getId(), topDriver.getName(), revenue
        ));
    }

    private LocalDate parseDate(String paramName, String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new ClientException("Paramètre '" + paramName + "' invalide, format attendu AAAA-MM-JJ");
        }
    }
}
