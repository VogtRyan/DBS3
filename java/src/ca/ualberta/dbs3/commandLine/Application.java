/*
 * Copyright (c) 2010-2023 Ryan Vogt <rvogt@ualberta.ca>
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION
 * OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN
 * CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ca.ualberta.dbs3.commandLine;

/**
 * The <code>Application</code> singleton class represents a command line
 * application. This class contains a number of convenience methods for command
 * line applications to process errors and interact with the underlying
 * operating system.
 */
public abstract class Application {
    /**
     * Whether or not to display full stack traces.
     */
    private static final boolean FULL_STACK = false;

    /**
     * The singleton application instance.
     */
    private static Application singleton = null;

    /**
     * Creates a new <code>Application</code> and installs a standard error
     * handler as the default error handler for all threads.
     *
     * @throws IllegalStateException if more than one <code>Application</code>
     *         object is created.
     */
    protected Application() {
        if (Application.singleton != null)
            throw new IllegalStateException("More than one application");
        Application.singleton = this;
        ErrorHandler eh = new ErrorHandler();
        Thread.setDefaultUncaughtExceptionHandler(eh);
    }

    /**
     * Gets the value of the given environmental variable.
     *
     * @param name the environmental variable name.
     * @return the value of the environmental variable, or <code>null</code> if
     *         the variable does not exist or if the environment cannot be
     *         queried.
     */
    public static String getEnvironment(String name) {
        String ret = null;
        try {
            ret = System.getenv(name);
        } catch (Exception e) {
        }
        return ret;
    }

    /**
     * Converts the given throwable error into a string message about the
     * error.
     *
     * @param e the throwable error.
     * @return a string representation of the error.
     */
    public static String throwableToString(Throwable e) {
        return Application.throwableToString(e, true);
    }

    /**
     * Converts the given throwable error into a string message about the
     * error.
     *
     * @param e the throwable error.
     * @param location whether to include information about the error location.
     * @return a string representation of the error.
     */
    public static String throwableToString(Throwable e, boolean location) {
        String output = "Error: " + e.getClass().getSimpleName();
        String msg = e.getMessage();
        if (msg != null)
            output = output + " - " + msg;

        if (location) {
            StackTraceElement[] st = e.getStackTrace();
            for (StackTraceElement ste : st) {
                output = output + "\n       " + ste.getClassName() + ":"
                        + ste.getMethodName();
                String file = ste.getFileName();
                if (file != null)
                    output = output + " at " + file + ":"
                            + ste.getLineNumber();
                if (!Application.FULL_STACK)
                    break;
            }
        }
        return output;
    }

    /**
     * The <code>ErrorHandler</code> class represents a default method of
     * handling uncaught exceptions thrown from command-line programs by
     * printing abbreviated details to standard error.
     */
    private static class ErrorHandler
            implements Thread.UncaughtExceptionHandler {
        /**
         * Creates a new <code>ErrorHandler</code>.
         */
        public ErrorHandler() {}

        /**
         * Invoked when the given thread terminates due to an uncaught
         * exception.
         *
         * @param t the thread that is terminated.
         * @param e the throwable error that caused the termination.
         */
        public void uncaughtException(Thread t, Throwable e) {
            String str = Application.throwableToString(e);
            System.err.println(str);
        }
    }
}
