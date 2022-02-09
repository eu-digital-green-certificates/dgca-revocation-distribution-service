package europa.ec.dgc.revocationdistribution.exception;

public class TokenValidationException extends RuntimeException{
    public int getStatus() {
        return status;
    }

    private final int status;

    public TokenValidationException(String message, Throwable inner) {

        super(message, inner);
        status = 500;
    }

    public TokenValidationException(String message) {

        super(message);
        status = 500;
    }

    public TokenValidationException(String message, Throwable inner, int status) {
        super(message, inner);
        this.status = status;
    }

    public TokenValidationException(String message, int status) {
        super(message);
        this.status = status;
    }
}
