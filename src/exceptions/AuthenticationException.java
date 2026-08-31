package exceptions;

// Exception class for handling authentication-related errors
public class AuthenticationException extends Exception {
    public AuthenticationException(String msg) { super(msg); }
}