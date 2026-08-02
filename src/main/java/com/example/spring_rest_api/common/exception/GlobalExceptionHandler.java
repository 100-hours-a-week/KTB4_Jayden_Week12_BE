package com.example.spring_rest_api.common.exception;

import com.example.spring_rest_api.common.response.ErrorResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Optional;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private final String maxFileSize;
    private final String maxRequestSize;

    public GlobalExceptionHandler(
            @Value("${spring.servlet.multipart.max-file-size}") String maxFileSize,
            @Value("${spring.servlet.multipart.max-request-size}") String maxRequestSize
    ) {
        this.maxFileSize = maxFileSize;
        this.maxRequestSize = maxRequestSize;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponseDto> handleBusiness(BusinessException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(ErrorResponseDto.of(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleNotValid(MethodArgumentNotValidException exception) {
        return ResponseEntity.status(exception.getStatusCode())
                .body(ErrorResponseDto.of(Optional.ofNullable(exception.getFieldError())
                        .orElseThrow().getDefaultMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleException(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponseDto.of(exception.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleNotReadable(HttpMessageNotReadableException exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponseDto.of(exception.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponseDto> handleMaxUploadSize(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ErrorResponseDto.of(
                        "파일 하나당 최대 " + maxFileSize + ", " + maxRequestSize + "까지 업로드할 수 있습니다."
                ));
    }
}