package exceptions;

// Exception class for handling duplicate login attempts
public class DuplicateLoginException extends Exception {
    public DuplicateLoginException(String msg) { super(msg); }
}