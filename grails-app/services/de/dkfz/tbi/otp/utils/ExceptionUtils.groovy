package de.dkfz.tbi.otp.utils

import grails.util.Environment
import org.codehaus.groovy.grails.exceptions.DefaultStackTraceFilterer

class ExceptionUtils {

    /**
     * If in {@link Environment#PRODUCTION}, logs the exception, otherwise throws it.
     *
     * This is for uncritical exceptions which should not disrupt service in production, but should make tests fail.
     */
    static void logOrThrow(final def log, final RuntimeException e) {
        if (Environment.current == Environment.PRODUCTION) {
            log.error e.message, e
        } else {
            throw e
        }
    }

    /**
     * Logs the exception, then throws it unless in {@link Environment#PRODUCTION}.
     *
     * This is for uncritical exceptions which should not disrupt service in production, but should make tests fail.
     */
    static void logAndThrowUnlessInProduction(final def log, final RuntimeException e) {
        log.error e.message, e
        if (Environment.current != Environment.PRODUCTION) {
            throw e
        }
    }

    static String getStackTrace(final Throwable t) {
        new DefaultStackTraceFilterer().filter(t)
        final CharArrayWriter buffer = new CharArrayWriter()
        t.printStackTrace(new PrintWriter(buffer))
        return buffer.toString()
    }
}
