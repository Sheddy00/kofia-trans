package hei.school.kofiatrans.exception;

/** The request is valid but clashes with existing data (duplicate plate, resource still referenced...). */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
