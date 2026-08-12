package exceptions;

public class DuplicateLoginException extends Exception {
    public DuplicateLoginException(String msg) { super(msg); }
}