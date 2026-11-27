package hei.school.kofiatrans.controller.dto;

import java.time.Instant;

public record LoginResponseDto(
        String accessToken,
        String tokenType,
        long expiresIn,
        Instant expiresAt,
        UserDto user
) {
}
