package de.dkfz.tbi.otp.job.processing

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Annotation for any Job that can be suspended and resumed safely.
 *
 * After a Job gets started it is normally in a limbo state. The Job has performed some processing
 * but is not yet finished. The execution is a critical path. Depending on what the Job performs
 * it is not possible to start the same process again. E.g. a process which triggers a long
 * computation can not be resumed if two running computation processes would interfere with each
 * other.
 *
 * By default the framework assumes that no Job can be resumed. This means when the application
 * performs a non-clean shutdown the administrator has to decide manually for each Job running at
 * the time of the crash whether the Job is still running, has succeeded or failed.
 *
 * This interface can be used by a Job which does not have a critical path and which can always be
 * resumed automatically after a non-clean application shutdown. Examples are jobs which just look
 * for a file and stop processing when the file is available.
 *
 * @see SometimesResumableJob
 *
 **/
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ResumableJob {
}
