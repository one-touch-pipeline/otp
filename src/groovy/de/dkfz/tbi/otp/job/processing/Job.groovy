package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.job.scheduler.Scheduler

/**
 * A groovy.de.dkfz.tbi.otp.job.processing.Job is the general interface for any operation which can be executed by the Process Engine.
 *
 * Each unique task has to implement this Interface. A groovy.de.dkfz.tbi.otp.job.processing.Job is populated with the {@link ProcessingStep}
 * it refers to in the current execution of the {@link JobExecutionPlan} (that is a {@link Process}).
 * As well the groovy.de.dkfz.tbi.otp.job.processing.Job is populated with all {@link Parameter} it needs to perform the task. This is
 * extracted from the {@link JobDefinition} for this groovy.de.dkfz.tbi.otp.job.processing.Job in the current {@link JobExecutionPlan}.
 *
 * The actual task of the groovy.de.dkfz.tbi.otp.job.processing.Job needs to be done in the {@link execute()} method. This method has to
 * implement all the logic, when the control flow exits the method no more operations of the task
 * may be performed! There is an aspect ({@link Scheduler}) watching the execution of the groovy.de.dkfz.tbi.otp.job.processing.Job, updating the state of the
 * groovy.de.dkfz.tbi.otp.job.processing.Job automatically. If the method throws an exception the groovy.de.dkfz.tbi.otp.job.processing.Job will be set to failed.
 *
 * The groovy.de.dkfz.tbi.otp.job.processing.Job has to generate a set of output parameters which will be passed to the next groovy.de.dkfz.tbi.otp.job.processing.Job(s) which
 * will be executed. As well all parameters will be stored in the groovy.de.dkfz.tbi.otp.job.processing.Job's ProcessingStep. The groovy.de.dkfz.tbi.otp.job.processing.Job itself
 * may not alter the ProcessingStep at all. The Container takes care of updating the ProcessingStep
 * correctly.
 *
 **/
public interface Job {
    /**
     * Method to implement for groovy.de.dkfz.tbi.otp.job.processing.Job execution.
     * The logic of the groovy.de.dkfz.tbi.otp.job.processing.Job which has to be performed has to go into this method. This method is
     * called by the Container, the groovy.de.dkfz.tbi.otp.job.processing.Job itself may not call it directly. This would result in an
     * inconsistent state of the application. The groovy.de.dkfz.tbi.otp.job.processing.Job should consider that this method may be
     * executed in an own Thread. It should not start further Threads in this method as the execution
     * of the groovy.de.dkfz.tbi.otp.job.processing.Job has to be finished when the control flow of this method ends.
     *
     * It is recommended to keep the groovy.de.dkfz.tbi.otp.job.processing.Job as small as possible. A groovy.de.dkfz.tbi.otp.job.processing.Job mixing multiple operations
     * may make it impossible to restart the groovy.de.dkfz.tbi.otp.job.processing.Job when the system shuts down. That is if operation A
     * succeeded but operation B has not yet been started, it might result in errors to restart the
     * complete groovy.de.dkfz.tbi.otp.job.processing.Job by executing operation A again. Because of that the groovy.de.dkfz.tbi.otp.job.processing.Job should only do one task.
     * A second task should be an own implementation of groovy.de.dkfz.tbi.otp.job.processing.Job with an own {@link JobDefinition}.
     *
     * All information the groovy.de.dkfz.tbi.otp.job.processing.Job requires will have been passed to the implementation before this method
     * is called. The groovy.de.dkfz.tbi.otp.job.processing.Job should not gather any more data which has not been passed as a parameter.
     * In general Parameters refer to domain objects identified by class name and unique identifier.
     * It is fine to ask the services for the specific objects represented by the Parameters.
     *
     * The groovy.de.dkfz.tbi.otp.job.processing.Job should do as less error handling as possible but as much error handling as needed.
     * A severe failure which should end in the Process being stopped should not be caught, but
     * passed on as an Exception to the container. The Bean container will take care of stopping the
     * execution and performing the error handling.
     *
     * Exceptions which should not result in an abort of the process execution have to be caught and
     * not be passed to the outside world as an exception leaving this method will trigger the error
     * handling.
     *
     * @throws Exception The execution of the groovy.de.dkfz.tbi.otp.job.processing.Job may throw any exception. It is handled by an Aspect ({@link Scheduler#doErrorHandling(org.aspectj.lang.JoinPoint, Exception)}).
     **/
    public void execute() throws Exception;
    /**
     * This method returns all the output parameters generated by the groovy.de.dkfz.tbi.otp.job.processing.Job during execute.
     * The framework will take care of persisting the output parameters after the groovy.de.dkfz.tbi.otp.job.processing.Job finished
     * successfully in the related ProcessingStep. The output parameters are mapped to the input
     * parameters of the next to be started JobDefinitions.
     *
     * As long as the groovy.de.dkfz.tbi.otp.job.processing.Job has not yet executed this method has to throw an InvalidStateException.
     * If the groovy.de.dkfz.tbi.otp.job.processing.Job did not produce any output parameters an empty list has to be returned and not
     * null.
     * @return List of generated output parameters by this job.
     * @throws InvalidStateException In case the groovy.de.dkfz.tbi.otp.job.processing.Job has not finished the execution.
     **/
    public Set<Parameter> getOutputParameters() throws InvalidStateException;
    /**
     * @return The ProcessingStep describing the execution of this groovy.de.dkfz.tbi.otp.job.processing.Job.
     **/
    public ProcessingStep getProcessingStep();
    /**
     * Invoked by the Container to indicate that the Job has been started. Called when the execute method
     * has been invoked.
     * Implement if the Job needs to know when execute is started.
     * @throws InvalidStateException In case the Job is not in a state where it could be started.
     */
    public void start() throws InvalidStateException;
    /**
     * Invoked by the Container to indicate that the Job has finished.
     * Implement if the Job needs to know exactly when the execution finished.
     * @throws InvalidStateException In case the Job is not in a state where it could end.
     */
    public void end() throws InvalidStateException;

    /**
     * Returns a unique version identifier of this class version.
     *
     * The version is the git SHA hash of the last change. The code of this method in implementing
     * classes gets auto-generated through an AST Transformation.
     *
     * Although the code gets generated a dummy implementation can be added just to make the IDE
     * happy.
     * @return Unique identifier of the source code version of the class.
     **/
    public String getVersion();
}
