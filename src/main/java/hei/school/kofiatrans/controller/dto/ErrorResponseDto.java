package hei.school.kofiatrans.controller.dto;

import java.time.Instant;

public record ErrorResponseDto(
        Instant timestamp,
        int status,
        String error,
        String message
) {
    public static ErrorResponseDto of(int status, String error, String message) {
        return new ErrorResponseDto(Instant.now(), status, error, message);
    }
}
