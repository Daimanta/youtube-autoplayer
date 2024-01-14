package nl.leonvanderkaap.yvplayer.commons.exceptions;

import lombok.Getter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.Serializable;

@ControllerAdvice
public class ExceptionHandlers {

    @ExceptionHandler
    public ResponseEntity<ReturnedError> handleYvException(YvException yvException) {
        return ResponseEntity.status(yvException.getErrorCode()).body(new ReturnedError(yvException.getMessage()));
    }

    @Getter
    public static class ReturnedError implements Serializable {
        private String message;

        public ReturnedError(String message) {
            this.message = message;
        }
    }
}
