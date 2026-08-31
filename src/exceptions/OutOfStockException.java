package exceptions;

// Exception class for handling out-of-stock scenarios
public class OutOfStockException extends Exception {
    public OutOfStockException(String msg) { super(msg); }
}