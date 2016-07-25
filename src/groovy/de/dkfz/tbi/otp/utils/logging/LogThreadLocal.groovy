package de.dkfz.tbi.otp.utils.logging

import static org.springframework.util.Assert.*
import org.apache.commons.logging.Log
import org.apache.commons.logging.impl.SimpleLog

/**
 * Holds a {@link Log} instance in a {@link Thread}
 *
 *
 */
class LogThreadLocal {

    /**
     * The thread local holder for {@link Log}
     */
    private static final ThreadLocal<Log> jobLogHolder = new ThreadLocal<Log>()

    /**
     * return the {@link Log} connected to this {@link Thread}.
     * If no {@link Log} is connected <code>null</code> is returned.
     *
     * @return the connected {@link Log} or null, if no {@link Log} is connected
     */
    public static Log getThreadLog() {
        return jobLogHolder.get()
    }

    /**
     * set the given {@link Log} to this {@link Thread}
     *
     * <p><strong>Make sure that you call {@link #removeThreadLog()} when done (best in a finally block).</strong></p>
     *
     * @param log the {@link Log} to add to this {@link Thread}
     */
    public static void setThreadLog(Log log) {
        notNull(log, "The log may not be null")
        jobLogHolder.set(log)
    }

    /**
     * remove the {@link Log} connected to this {@link Thread}, if any.
     */
    public static void removeThreadLog() {
        jobLogHolder.remove()
    }

    public static void withThreadLog(Appendable out, Closure code) {
        assert out != null
        assert code != null
        assert threadLog == null
        threadLog = new SimpleLog('ThreadLog') {
            // The write method has been introduced in commons-logging version 1.0.4
            @Override
            protected void write(StringBuffer buffer) {
                out.append(buffer)
                out.append('\n')
            }
        }
        try {
            code()
        } finally {
            removeThreadLog()
        }
    }
}
