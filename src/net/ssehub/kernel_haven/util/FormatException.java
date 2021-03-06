package net.ssehub.kernel_haven.util;

import net.ssehub.kernel_haven.util.null_checks.Nullable;

/**
 * Exception for unexpected structure of file-content.
 * 
 * @author Adam
 * @author Moritz
 *
 */
public class FormatException extends Exception {

    
    private static final long serialVersionUID = 2081277470741239201L;

    /**
     * Creates a new FormatException.
     */
    public FormatException() {

    }
    
    /**
     * Creates a new FormatException.
     * 
     * @param message The message to display.
     */
    public FormatException(@Nullable String message) {
        super(message);
    }
    
    /**
     * Creates a new FormatException.
     * 
     * @param cause The exception that caused this exception.
     */
    public FormatException(@Nullable Throwable cause) {
        super(cause);
    }
    
    /**
     * Creates a new FormatException.
     * 
     * @param message The message to display.
     * @param cause The exception that caused this exception.
     */
    public FormatException(@Nullable String message, @Nullable Throwable cause) {
        super(message, cause);
    }
    
}
