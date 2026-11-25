package my.edu.taylors.ap.data;

/**
 * Checked exception for any failure reading or writing the JSON data files.
 * Callers must decide what to do: retry, show the user a message, or fail the tool call.
 */
public class DataAccessException extends Exception {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataAccessException(String message) {
        super(message);
    }
}
