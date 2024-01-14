package nl.leonvanderkaap.yvplayer.commons.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class InternalErrorException extends YvException{

    public InternalErrorException(String message) {
        super(message);
    }

    @Override
    public int getErrorCode() {
        return 500;
    }
}
