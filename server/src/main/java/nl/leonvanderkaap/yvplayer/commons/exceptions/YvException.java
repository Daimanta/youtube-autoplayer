package nl.leonvanderkaap.yvplayer.commons.exceptions;

public abstract class YvException extends RuntimeException{
    public YvException(String message) {
        super(message);
    }

    public abstract int getErrorCode();
}
