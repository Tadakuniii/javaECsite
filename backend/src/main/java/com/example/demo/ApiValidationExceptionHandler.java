package com.example.demo;

import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiValidationExceptionHandler {
    /** 同時登録によるID重複にも、既存IDとしての応答を返す。 */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handleDuplicateRegistration(DataIntegrityViolationException exception) {
        return Map.of("status", "EXISTING_ID", "message", "入力されたユーザIDは登録済です");
    }

    /** 登録ルール違反を、画面に表示できる400のエラー応答へ変換する。 */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleInvalidRegistration(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(violation -> violation.getMessage()).sorted().findFirst().orElse("入力内容を確認してください");
        return Map.of("status", "VALIDATION_ERROR", "message", message);
    }
}
