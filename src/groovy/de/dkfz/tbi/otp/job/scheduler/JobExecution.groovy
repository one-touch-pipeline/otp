package de.dkfz.tbi.otp.job.scheduler

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Annotation used on the {@link Job} Execution method to notify the {@link Scheduler} advice.
 *
 * The Annotation is injected through an AST Transformation so that there is no need
 * to add the annotation manually on Job Implementations.
 *
 * @see Scheduler
 * @see Job
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JobExecution {
}
