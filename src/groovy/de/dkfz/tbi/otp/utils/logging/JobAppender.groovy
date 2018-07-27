package de.dkfz.tbi.otp.utils.logging

import java.util.regex.Matcher

import de.dkfz.tbi.otp.job.processing.Job
import de.dkfz.tbi.otp.job.processing.ProcessingStep

import org.apache.log4j.Appender
import org.apache.log4j.AppenderSkeleton
import org.apache.log4j.FileAppender
import org.apache.log4j.spi.LoggingEvent

/**
 * Appender for Log messages of the Jobs.
 *
 * This {@link Appender} takes care of any log message logged by the
 * {@link Job}s. The output of each Job is logged into an own log file.
 * For each {@link Process} a directory is created in the specified log
 * directory with the database Id of the Process as name. For each Job a
 * log file with the Id of its {@link ProcessingStep} as name is created.
 *
 * When a {@link LoggingEvent} has to be logged it is tried to be casted
 * into a {@link JobLogMessage} which contains the information about the
 * {@link ProcessingStep} for which the LoggingEvent occurred. For each
 * ProcessingStep an own Appender is used to which a cloned LoggingEvent
 * containing the original message is forwarded.
 *
 */
class JobAppender extends AppenderSkeleton {
    File logDirectory
    private boolean closed = false
    private Map<Long, Appender> appenders = [:]

    public JobAppender() {
    }

    public JobAppender(boolean isActive) {
        super(isActive)
    }

    @SuppressWarnings("CloseWithoutCloseable")
    @Override
    public void close() {
        appenders.each { k, v ->
            v.close()
        }
        this.closed = true
    }

    @Override
    public boolean requiresLayout() {
        return true
    }

    @Override
    protected void append(LoggingEvent event) {
        if (closed) {
            return
        }
        try {
            String message = event.message.toString()
            Matcher matcher = message =~ /^(\[ProcessingStep (\d+)\] )/
            matcher.find()
            ProcessingStep processingStep = ProcessingStep.getInstance(Long.parseLong(matcher.group(2)))
            FileAppender appender = appenderForProcessingStep(processingStep)
            if (appender) {
                LoggingEvent e = new LoggingEvent(event.fqnOfCategoryClass, event.getLogger(), event.timeStamp, event.getLevel(), message.substring(matcher.group(1).length()), event.threadName, event.throwableInfo, event.ndc, event.locationInfo, event.properties)
                appender.append(e)
            }
        } catch (Exception e) {
            log.error("Could not read the JobLogMessage: ${event.message}", e)
        }
    }

    /**
     * Unregisters the given ProcessingStep from logging.
     * The Appender for this ProcessingStep is closed and removed from the
     * internal list.
     * @param step The ProcessingStep to unregister
     */
    public void unregisterProcessingStep(ProcessingStep step) {
        if (!appenders.containsKey(step.id)) {
            // no appender for this step - nothing todo
            return
        }
        Appender appender = appenders.get(step.id)
        appender.close()
        appenders.remove(step.id)
    }

    /**
     * Retrieves the Appender for the given ProcessingStep.
     *
     * If the Appender had already been created it just retrieves it
     * otherwise it invokes the creation of the Appender.
     * @param step The ProcessingStep for which the Appender should be retrieved
     * @return The Appender, maybe null
     */
    private Appender appenderForProcessingStep(ProcessingStep step) {
        if (appenders.containsKey(step.id)) {
            return appenders.get(step.id)
        }
        return createAppenderForProcessingStep(step)
    }

    /**
     * Creates the Appender for the given ProcessingStep if possible.
     *
     * A FileAppender logging into logDirectory/proccessId/stepId.log. If any
     * directory does not exist, no appender is created.
     * The Appender is set to the name of the ProcessingStep and is equipped with
     * the layout set for this JobAppender.
     *
     * @param step The ProcessingStep for which the Appender should be created.
     * @return The created Appender or null if creation not possible
     */
    private Appender createAppenderForProcessingStep(ProcessingStep step) {
        if (!logDirectory || !logDirectory.isDirectory()) {
            return null
        }
        File dir = new File(logDirectory.getPath(), "${step.process.id}")
        dir.mkdirs()
        if (!dir.exists() || !dir.isDirectory()) {
            return null
        }
        FileAppender appender = new FileAppender(this.layout, "${dir.path}${File.separatorChar}${step.id}.log")
        appender.setName("ProcessingStep ${step.id}")
        appenders.put(step.id, appender)
        return appender
    }

    public void setLogDirectory(File logDirectory) {
        // creates the log directory if it does not yet exist
        if (!logDirectory.exists()) {
            logDirectory.mkdirs()
        }
        if (logDirectory.exists() && logDirectory.isDirectory()) {
            this.logDirectory = logDirectory
        }
    }

}
