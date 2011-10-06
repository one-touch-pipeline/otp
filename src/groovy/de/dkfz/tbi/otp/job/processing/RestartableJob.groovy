package de.dkfz.tbi.otp.job.processing

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Annotation for any groovy.de.dkfz.tbi.otp.job.processing.Job that can be restarted safely.
 *
 * After a groovy.de.dkfz.tbi.otp.job.processing.Job gets started it is normally in a limbo state. The groovy.de.dkfz.tbi.otp.job.processing.Job has performed some processing
 * but is not yet finished. The execution is a critical path. Depending on what the groovy.de.dkfz.tbi.otp.job.processing.Job performs
 * it is not possible to start the same process again. E.g. a process which triggers a long
 * computation can not be restarted if two running computation processes would interfere with each
 * other.
 *
 * By default the framework assumes that no groovy.de.dkfz.tbi.otp.job.processing.Job can be restarted. This means when the application
 * performs a non-clean shutdown the administrator has to decide manually for each groovy.de.dkfz.tbi.otp.job.processing.Job running at
 * the time of the crash whether the groovy.de.dkfz.tbi.otp.job.processing.Job is still running, has succeeded or failed.
 *
 * This interface can be used by a groovy.de.dkfz.tbi.otp.job.processing.Job which does not have a critical path and which can always be
 * restarted automatically after a non-clean application shutdown. Examples are jobs which just look
 * for a file and stop processing when the file is available.
 *
 **/
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RestartableJob {
}
