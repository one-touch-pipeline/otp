package de.dkfz.tbi.otp.utils.logging

import org.apache.commons.logging.Log

import de.dkfz.tbi.otp.job.processing.ProcessingStep

/**
 * {@link Log} implementation which wraps each log message in a
 * {@link JobLogMessage} and logs it to a real Log.
 *
 * An instance of this class gets injected into each Job by an
 * AST transformation.
 */
class JobLog implements Log {
    /**
     * The ProcessingStep the Job is operating on.
     * Gets included in the JobLogMessage.
     */
    private ProcessingStep processingStep = null
    /**
     * The real log to which the JobLogMessage is logged.
     */
    Log log = null

    JobLog(ProcessingStep processingStep, Log log) {
        this.processingStep = processingStep
        this.log = log
    }

    @Override
    void debug(Object message) {
        if (!log) {
            return
        }
        log.debug(wrap(message))
    }

    @Override
    void debug(Object message, Throwable throwable) {
        if (!log) {
            return
        }
        log.debug(wrap(message), throwable)
    }

    @Override
    void error(Object message) {
        if (!log) {
            return
        }
        log.error(wrap(message))
    }

    @Override
    void error(Object message, Throwable throwable) {
        if (!log) {
            return
        }
        log.error(wrap(message), throwable)
    }

    @Override
    void fatal(Object message) {
        if (!log) {
            return
        }
        log.fatal(wrap(message))
    }

    @Override
    void fatal(Object message, Throwable throwable) {
        if (!log) {
            return
        }
        log.fatal(wrap(message), throwable)
    }

    @Override
    void info(Object message) {
        if (!log) {
            return
        }
        log.info(wrap(message))
    }

    @Override
    void info(Object message, Throwable throwable) {
        if (!log) {
            return
        }
        log.info(wrap(message), throwable)
    }

    @Override
    boolean isDebugEnabled() {
        if (log) {
            return log.isDebugEnabled()
        }
        return false
    }

    @Override
    boolean isErrorEnabled() {
        if (log) {
            return log.isErrorEnabled()
        }
        return false
    }

    @Override
    boolean isFatalEnabled() {
        if (log) {
            return log.isFatalEnabled()
        }
        return false
    }

    @Override
    boolean isInfoEnabled() {
        if (log) {
            return log.isInfoEnabled()
        }
        return false
    }

    @Override
    boolean isTraceEnabled() {
        if (log) {
            return log.isTraceEnabled()
        }
        return false
    }

    @Override
    boolean isWarnEnabled() {
        if (log) {
            return log.isWarnEnabled()
        }
        return false
    }

    @Override
    void trace(Object message) {
        log.trace(wrap(message))
    }

    @Override
    void trace(Object message, Throwable throwable) {
        log.trace(wrap(message), throwable)
    }

    @Override
    void warn(Object message) {
        log.warn(wrap(message))
    }

    @Override
    void warn(Object message, Throwable throwable) {
        log.warn(wrap(message), throwable)
    }

    /**
     * Wraps the given message in a JobLogMessage.
     * @param message The original message to log
     * @return Wrapped message with ProcessingStep
     */
    private JobLogMessage wrap(Object message) {
        return new JobLogMessage(message, processingStep)
    }

    /**
     * Overloaded method for convenience.
     * @param message
     * @param throwable
     * @return
     */
    private JobLogMessage wrap(Object message, Throwable throwable) {
        return new JobLogMessage(message, processingStep, throwable)
    }
}
