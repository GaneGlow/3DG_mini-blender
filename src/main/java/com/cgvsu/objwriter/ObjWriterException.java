package src.main.java.com.cgvsu.objwriter;

public class ObjWriterException extends RuntimeException {
    public ObjWriterException(String message) {
        super(message);
    }

    public ObjWriterException(String message, Throwable cause) {
        super(message, cause);
    }
}
