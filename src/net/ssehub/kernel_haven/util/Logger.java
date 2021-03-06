package net.ssehub.kernel_haven.util;

import static net.ssehub.kernel_haven.util.null_checks.NullHelpers.notNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.DefaultSettings;
import net.ssehub.kernel_haven.util.Util.Color;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;

/**
 * A thread-safe singleton logger.
 * 
 * @author Adam
 * @author alice
 * @author moritz
 */
public final class Logger {
    
    /**
     * The available log levels.
     */
    public static enum Level {
        
        /**
         * No messages are logged.
         */
        NONE("none   ", -1, "none   "),
        
        /**
         * Only error messages are logged.
         */
        ERROR("error  ", 0, Color.RED.getAnsiCode() + "error" + Color.RESET.getAnsiCode() + "  "),
        
        /**
         * Error and warning messages are logged.
         */
        WARNING("warning", 1, Color.YELLOW.getAnsiCode() + "warning" + Color.RESET.getAnsiCode() + ""),
        
        /**
         * Error, warning, and status messages are logged.
         * Kind of info logging, but less frequent to show only the current status/progress of the whole analysis.
         */
        STATUS("status ", 2, Color.BLUE.getAnsiCode() + "status" + Color.RESET.getAnsiCode() + " "),
        
        /**
         * Error, warning, status, and info messages are logged.
         */
        INFO("info   ", 3, Color.GREEN.getAnsiCode() + "info" + Color.RESET.getAnsiCode() + "   "),
        
        /**
         * All messages (error, warning, status, info, debug) are logged.
         */
        DEBUG("debug  ", 4, Color.CYAN.getAnsiCode() + "debug" + Color.RESET.getAnsiCode() + "  ");
        
        private @NonNull String str;
        
        private int level;
        
        private @NonNull String ansiString;
        
        /**
         * Creates a log level.
         * 
         * @param str The string representation of this log level.
         * @param level The level. All log levels with values <= this value will be logged.
         * @param ansiString The string representation of this log level with ANSI escape code colors.
         */
        private Level(@NonNull String str, int level, @NonNull String ansiString) {
            this.str = str;
            this.level = level;
            this.ansiString = ansiString;
        }
        
        /**
         * Whether a message with the specified level will be logged, if the logger is set to this level.
         * 
         * @param other The other level to check.
         * @return Whether the other level will be logged if this level is set.
         */
        public boolean shouldLog(@NonNull Level other) {
            return this.level >= other.level;
        }
        
        @Override
        public @NonNull String toString() {
            return notNull(str.trim());
        }
        
        /**
         * Creates a string representation of this level to be used in the log output. All levels will return strings
         * of equal length.
         * 
         * @param ansiColor Whether to add ANSI coloring codes or not.
         * @return A string representation of this level.
         */
        private @NonNull String toLogString(boolean ansiColor) {
            return ansiColor ? ansiString : str;
        }
        
    }

    /**
     * The singleton instance.
     */
    private static @NonNull Logger instance = new Logger();
    
    /**
     * The level at which this logger should start logging.
     */
    private @NonNull Level level;
    
    /**
     * The target to log to.
     */
    private @NonNull ArrayList<@NonNull Target> targets;

    /**
     * The charset used by the logger. All targets have the same charset.
     */
    private @NonNull Charset charset;

    /**
     * File used as target for logging specified in the configuration.
     */
    private @Nullable File logFile;
    
    private @Nullable Boolean forceColor;

    /**
     * A single target of this logger.
     */
    private static final class Target {
        
        private @NonNull OutputStream out;
        
        private boolean useColor;

        /**
         * Creates a new target.
         * 
         * @param out The output stream of this target.
         * @param useColor Whether to use ANSI color codes on this target.
         */
        public Target(@NonNull OutputStream out, boolean useColor) {
            this.out = out;
            this.useColor = useColor;
        }
        
    }

    /**
     * Instantiates a new logger.
     */
    private Logger() {
        this.targets = new ArrayList<>(2);
        addTarget(notNull(System.out));
        
        this.charset = notNull(Charset.forName("UTF-8"));
        this.level = Level.INFO;
    }
    
    /**
     * Gets the singleton instance of Logger.
     *
     * @return the logger
     */
    public static @NonNull Logger get() {
        return instance;
    }

    /**
     * The setup method sets the log level and targets. Overrides any existing targets.
     * 
     * @param config The configuration for the logger; must not be <code>null</code>.
     * @throws SetUpException
     *             Throws the SetUpException when the path to log to is not a
     *             valid directory.
     */

    public void setup(@NonNull Configuration config) throws SetUpException {
        synchronized (targets) {
            targets.clear();
            logFile = null;
            level = config.getValue(DefaultSettings.LOG_LEVEL);
            forceColor = config.getValue(DefaultSettings.LOG_FORCE_COLOR);
            
            if (config.getValue(DefaultSettings.LOG_CONSOLE)) {
                addTarget(notNull(System.out));
            }
            
            if (config.getValue(DefaultSettings.LOG_FILE)) {
                logFile = new File(config.getValue(DefaultSettings.LOG_DIR), 
                        Timestamp.INSTANCE.getFilename("KernelHaven", "log"));
                try {
                    addTarget(new FileOutputStream(logFile));
                } catch (FileNotFoundException e) {
                    throw new SetUpException(e);
                }
            }
        }
        
        ProgressLogger.setInterval(config.getValue(DefaultSettings.LOG_PROGRESS_INTERVAL));
    }
    
    /**
     * Returns the list of targets for this logger.
     * 
     * @return An unmodifiable list of the targets of this logger.
     */
    public @NonNull List<@NonNull OutputStream> getTargets() {
        synchronized (targets) {
            List<@NonNull OutputStream> result = new ArrayList<>(targets.size());
            for (Target target : targets) {
                result.add(target.out);
            }
            return notNull(Collections.unmodifiableList(result));
        }
    }
    
    /**
     * Removes a target from the list of log targets.
     * 
     * @param index The index of the target to remove (see {@link #getTargets()} for indices).
     * 
     * @throws IndexOutOfBoundsException If the index is out of bounds.
     */
    public void removeTarget(int index) throws IndexOutOfBoundsException {
        synchronized (targets) {
            targets.remove(index);
        }
    }
    
    /**
     * Removes all targets that this logger currently logs to.
     */
    public void clearAllTargets() {
        synchronized (targets) {
            targets.clear();
        }
    }
    
    /**
     * Adds a target for this logger. This logger will always obtain a lock (via synchronized(target)) on this object
     * before writing to it.
     * 
     * @param target The target to add to this logger.
     */
    public void addTarget(@NonNull OutputStream target) {
        synchronized (targets) {
            targets.add(new Target(target, useColor(target)));
        }
    }

    /**
     * Overwrite the current log level.
     * 
     * @param level The new log level.
     */
    public void setLevel(@NonNull Level level) {
        this.level = level;
    }

    /**
     * Creates a "header" prefix for log lines. The lines contain the specified
     * log level, the name of the current thread and the time.
     * 
     * @param level The log level that will be used. Must not be null.
     * @param useColors Whether ANSI color codes should be used.
     * @return A string in the format "[level] [time] [threadName] "
     */
    private @NonNull String constructHeader(@NonNull Level level, boolean useColors) {
        StringBuilder hdr = new StringBuilder();
        
        String timestamp = new Timestamp().getTimestamp();
        
        String levelStr = level.toLogString(useColors);
        
        String threadName = Thread.currentThread().getName();
        if (useColors) {
            threadName = Color.WHITE.getAnsiCode() + threadName + Color.RESET.getAnsiCode();
        }
        
        hdr
            .append('[')
            .append(timestamp)
            .append("] [")
            .append(levelStr)
            .append("] [")
            .append(threadName).append("] ");
        return notNull(hdr.toString());
    }
    
    /**
     * Checks first whether to log the message and only iff the message shall be logged, it will concatenate
     * the single parts and log the complete message into a single line.
     * @param level The log level to be written. Must not be <tt>null</tt>.
     * @param messageParts The message to be logged, for all elements {@link Object#toString()} is called to concatenate
     *     the message.
     */
    private void log(@NonNull Level level, @Nullable Object /*@NonNull*/ ... messageParts) {
        if (this.level.shouldLog(level) && null != messageParts) {
            StringBuilder messageLine = new StringBuilder();
            for (Object object : messageParts) {
                if (object instanceof Object[]) {
                    Object[] innerArray = (Object[]) object;
                    for (Object innerObject : innerArray) {
                        messageLine.append(innerObject != null ? innerObject.toString() : "null");
                    }
                } else {
                    messageLine.append(object != null ? object.toString() : "null");
                }
            }
            
            log(level, messageLine.toString());
        }
    }
    
    /**
     * Writes a single log entry consisting of the specified lines with the
     * specified log level to the target. Internally, a lock on {@link #target}
     * is acquired to ensure that messages are not splitted in a multi-threaded
     * environment.
     * 
     * @param level
     *            The log level to be written. Must not be null.
     * @param lines
     *            The lines that are written together as one log entry. Must not
     *            be null.
     */
    private void log(@NonNull Level level, String /*@NonNull*/ ... lines) {
        // TODO: commented out @NonNull annotation because checkstyle can't parse it
        
        if (!this.level.shouldLog(level)) {
            return;
        }
        String header = constructHeader(level, false);
        String indent = "";
        if (lines.length > 1) {
            char[] whitespaces = new char[header.length()];
            Arrays.fill(whitespaces, ' ');
            indent = new String(whitespaces);
        }
        byte[] headerBytes = header.getBytes(charset);

        StringBuilder str = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i != 0) {
                str.append(indent);
            }
            str.append(lines[i]).append('\n');
        }
        
        byte[] bytes = str.toString().getBytes(charset);

        synchronized (this.targets) {
            for (Target target : this.targets) {
    
                synchronized (target.out) {
                    try {
                        if (target.useColor) {
                            // no need to cache this header, since only one target will be System.out
                            target.out.write(constructHeader(level, true).getBytes(charset));
                        } else {
                            target.out.write(headerBytes);
                        }
                        target.out.write(bytes);
                        target.out.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    
    /**
     * Determines whether ANSI color codes should be used for the given target stream.
     * 
     * @param target The stream to log to.
     * 
     * @return Whether ANSI color codes should be used.
     */
    private boolean useColor(OutputStream target) {
        boolean result;
        
        Boolean forceColor = this.forceColor;
        if (forceColor != null) {
            result = forceColor && target == System.out;
            
        } else {
            result = Util.isTTY(target);
        }
        
        return result;
    }

    /**
     * Logs a log entry with the log level "info".
     * 
     * @param lines
     *            The content of the log entry. Must not be null.
     */
    public void logInfo(String /*@NonNull*/ ... lines) {
        // TODO: commented out @NonNull annotation because checkstyle can't parse it
        log(Level.INFO, lines);
    }
    
    /**
     * Logs a log entry with the log level "info".
     * 
     * @param messageParts The content of the log entry.
     */
    public void logInfo2(@Nullable Object /*@NonNull*/ ... messageParts) {
        // TODO: commented out @NonNull annotation because checkstyle can't parse it
        log(Level.INFO, messageParts);
    }
    
    /**
     * Logs a log entry with the log level "info".
     * 
     * @param lines
     *            The content of the log entry. Must not be null.
     */
    public void logStatusLines(String /*@NonNull*/ ... lines) {
        // TODO: commented out @NonNull annotation because checkstyle can't parse it
        log(Level.STATUS, lines);
    }
    
    /**
     * Logs a log entry with the log level "info".
     * 
     * @param messageParts The content of the log entry.
     */
    public void logStatus(@Nullable Object /*@NonNull*/ ... messageParts) {
        // TODO: commented out @NonNull annotation because checkstyle can't parse it
        log(Level.STATUS, messageParts);
    }

    /**
     * Logs a log entry with the log level "debug".
     * 
     * @param lines
     *            The content of the log entry. Must not be null.
     */
    public void logDebug(String /*@NonNull*/ ... lines) {
        // TODO: commented out @NonNull annotation because checkstyle can't parse it
        log(Level.DEBUG, lines);
    }
    
    /**
     * Logs a log entry with the log level "debug".
     * 
     * @param messageParts The content of the log entry.
     */
    public void logDebug2(@Nullable Object /*@NonNull*/ ... messageParts) {
        // TODO: commented out @NonNull annotation because checkstyle can't parse it
        log(Level.DEBUG, messageParts);
    }

    /**
     * Logs a log entry with the log level "warning".
     * 
     * @param lines
     *            The content of the log entry.
     */
    public void logWarning(String /*@NonNull*/ ... lines) {
        // TODO: commented out @NonNull annotation because checkstyle can't parse it
        log(Level.WARNING, lines);
    }
    
    /**
     * Logs a log entry with the log level "warning".
     * 
     * @param messageParts The content of the log entry.
     */
    public void logWarning2(@Nullable Object /*@NonNull*/ ... messageParts) {
        // TODO: commented out @NonNull annotation because checkstyle can't parse it
        log(Level.WARNING, messageParts);
    }

    /**
     * Logs a log entry with the log level "error".
     * 
     * @param lines
     *            The content of the log entry. Must not be null.
     */
    public void logError(String /*@NonNull*/ ... lines) {
        // TODO: commented out @NonNull annotation because checkstyle can't parse it
        log(Level.ERROR, lines);
    }
    
    /**
     * Logs a log entry with the log level "error".
     * 
     * @param messageParts The content of the log entry.
     */
    public void logError2(@Nullable Object /*@NonNull*/ ... messageParts) {
        // TODO: commented out @NonNull annotation because checkstyle can't parse it
        log(Level.ERROR, messageParts);
    }

    /**
     * Converts a given exception to a string and adds the lines to the list.
     * The string will contain the stack trace, much like
     * {@link Throwable#printStackTrace()}. Additionally, the causing exceptions
     * are converted into strings, too.
     * 
     * @param exc
     *            The exception to convert into a string. Must not be null.
     * @param lines
     *            The output target. The lines are appended to this list. Must
     *            not be null.
     */
    private void exceptionToString(@NonNull Throwable exc, @NonNull List<String> lines) {
        String[] headerLines = exc.toString().split("\n");
        for (String headerLine : headerLines) {
            lines.add(headerLine);
        }

        StackTraceElement[] stack = exc.getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            lines.add("    at " + stack[i].toString());
        }

        Throwable cause = exc.getCause();
        if (cause != null) {
            lines.add("Caused by:");
            exceptionToString(cause, lines);
        }
    }
    
    /**
     * Creates a log entry from the given comment and exception.
     * 
     * @param level The log level to log at.
     * @param comment 
     *            A comment that is displayed above the exception. A ":" is
     *            appended to it by this method. Must not be null.
     * @param exc
     *            The exception to log. A stack trace will be logged.
     */
    private void logException(@NonNull Level level, @NonNull String comment, @Nullable Throwable exc) {
        List<String> lines;
        if (exc != null) {
            lines = new ArrayList<>(exc.getStackTrace().length + 2);
            lines.add(comment + ":");
            exceptionToString(exc, lines);
            
        } else {
            lines = new ArrayList<>(1);
            lines.add(comment + ": <exception is null>");
        }
        log(level, notNull(lines.toArray(new String[0])));
    }

    /**
     * Creates a log entry from the given comment and exception. The log level
     * is "error".
     * 
     * @param comment
     *            A comment that is displayed above the exception. A ":" is
     *            appended to it by this method. Must not be null.
     * @param exc
     *            The exception to log. A stack trace will be logged.
     */
    public void logException(@NonNull String comment, @Nullable Throwable exc) {
        logException(Level.ERROR, comment, exc);
    }
    
    /**
     * Creates a log entry from the given comment and exception. The log level
     * is "debug".
     * 
     * @param comment
     *            A comment that is displayed above the exception. A ":" is
     *            appended to it by this method. Must not be null.
     * @param exc
     *            The exception to log. A stack trace will be logged.
     */
    public void logExceptionDebug(@NonNull String comment, @Nullable Throwable exc) {
        logException(Level.DEBUG, comment, exc);
    }
    
    /**
     * Creates a log entry from the given comment and exception. The log level
     * is "warning".
     * 
     * @param comment
     *            A comment that is displayed above the exception. A ":" is
     *            appended to it by this method. Must not be null.
     * @param exc
     *            The exception to log. A stack trace will be logged.
     */
    public void logExceptionWarning(@NonNull String comment, @Nullable Throwable exc) {
        logException(Level.WARNING, comment, exc);
    }
    
    /**
     * Creates a log entry from the given comment and exception. The log level
     * is "info".
     * 
     * @param comment
     *            A comment that is displayed above the exception. A ":" is
     *            appended to it by this method. Must not be null.
     * @param exc
     *            The exception to log. A stack trace will be logged.
     */
    public void logExceptionInfo(@NonNull String comment, @Nullable Throwable exc) {
        logException(Level.INFO, comment, exc);
    }
    
    /**
     * Prints a stack trace of the current thread to the log.
     * @param level The log level to be written. Must not be <tt>null</tt>.
     * @param text The title of the message to be printed (introduces the stack trace).
     */
    public void logStackTrace(@NonNull Level level, @NonNull String text) {
        if (this.level.shouldLog(level)) {
            // Creates the Stack Trace (copied from: Thread.currentThread().dumpStack())
            Exception stackTrace = new Exception(text);
            StringWriter sWriter = new StringWriter();
            PrintWriter pWriter = new PrintWriter(sWriter);
            stackTrace.printStackTrace(pWriter);
            
            // Delete exception from part from Stack Trace:
            String result = notNull(sWriter.toString());
            int startPos = 21; // "java.lang.Exception: ".length()
            if (result.length() > startPos) {
                result = notNull(result.substring(startPos));
            }
            log(level, result.split("\n"));
        }
    }
    
    /**
     * Gets the target logging file specified in the configuration..
     * 
     * @return the file used as logging target. May be null if not logging to a file.
     */
    public @Nullable File getLogFile() {
        return logFile;
    }
    
}
