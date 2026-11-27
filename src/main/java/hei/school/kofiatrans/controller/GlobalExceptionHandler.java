package hei.school.kofiatrans.controller;

import hei.school.kofiatrans.controller.dto.ErrorResponseDto;
import hei.school.kofiatrans.exception.ClientException;
import hei.school.kofiatrans.exception.ServerException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ClientException.class)
    public ResponseEntity<ErrorResponseDto> handleClientException(ClientException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDto.of(400, "Bad Request", e.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleUnreadableBody(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDto.of(400, "Bad Request", "Corps de requête invalide ou illisible"));
    }

    @ExceptionHandler(ServerException.class)
    public ResponseEntity<ErrorResponseDto> handleServerException(ServerException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponseDto.of(500, "Internal Server Error", "Une erreur interne est survenue"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleUnexpected(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponseDto.of(500, "Internal Server Error", "Une erreur interne est survenue"));
    }
}
