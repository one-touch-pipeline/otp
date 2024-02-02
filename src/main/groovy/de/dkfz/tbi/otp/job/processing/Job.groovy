/*
 * Copyright 2011-2024 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.scheduler.Scheduler

/**
 * A Job is the general interface for any operation which can be executed by the Process Engine.
 *
 * Each unique task has to implement this Interface. A Job is populated with the {@link ProcessingStep}
 * it refers to in the current execution of the {@link JobExecutionPlan} (that is a {@link Process}).
 * As well the Job is populated with all {@link Parameter} it needs to perform the task. This is
 * extracted from the {@link JobDefinition} for this Job in the current {@link JobExecutionPlan}.
 *
 * The actual task of the Job needs to be done in the {@link Job#execute()} method. This method has to
 * implement all the logic, when the control flow exits the method no more operations of the task
 * may be performed! {@link Scheduler} watches the execution of the Job, updating the state of the
 * Job automatically. If the method throws an exception the Job will be set to failed.
 *
 * The Job has to generate a set of output parameters which will be passed to the next Job(s) which
 * will be executed. As well all parameters will be stored in the Job's ProcessingStep. The Job itself
 * may not alter the ProcessingStep at all. The Container takes care of updating the ProcessingStep
 * correctly.
 */
interface Job {

    /**
     * Method to implement for Job execution.
     * The logic of the Job which has to be performed has to go into this method. This method is
     * called by the Container, the Job itself may not call it directly. This would result in an
     * inconsistent state of the application. The Job should consider that this method may be
     * executed in an own Thread. It should not start further Threads in this method as the execution
     * of the Job has to be finished when the control flow of this method ends.
     *
     * It is recommended to keep the Job as small as possible. A Job mixing multiple operations
     * may make it impossible to restart the Job when the system shuts down. That is if operation A
     * succeeded but operation B has not yet been started, it might result in errors to restart the
     * complete Job by executing operation A again. Because of that the Job should only do one task.
     * A second task should be an own implementation of Job with an own {@link JobDefinition}.
     *
     * All information the Job requires will have been passed to the implementation before this method
     * is called. The Job should not gather any more data which has not been passed as a parameter.
     * In general Parameters refer to domain objects identified by class name and unique identifier.
     * It is fine to ask the services for the specific objects represented by the Parameters.
     *
     * The Job should do as less error handling as possible but as much error handling as needed.
     * A severe failure which should end in the Process being stopped should not be caught, but
     * passed on as an Exception to the container. The Bean container will take care of stopping the
     * execution and performing the error handling.
     *
     * Exceptions which should not result in an abort of the process execution have to be caught and
     * not be passed to the outside world as an exception leaving this method will trigger the error
     * handling.
     *
     * @throws Exception The execution of the Job may throw any exception. It is handled by {@link Scheduler#doErrorHandling(Job, Throwable)}.
     */
    void execute() throws Exception

    /**
     * This method returns all the output parameters generated by the Job during execute.
     * The framework will take care of persisting the output parameters after the Job finished
     * successfully in the related ProcessingStep. The output parameters are mapped to the input
     * parameters of the next to be started JobDefinitions.
     *
     * As long as the Job has not yet executed this method has to throw an InvalidStateException.
     * If the Job did not produce any output parameters an empty list has to be returned and not
     * null.
     * @return List of generated output parameters by this job.
     * @throws InvalidStateException In case the Job has not finished the execution.
     */
    Set<Parameter> getOutputParameters() throws InvalidStateException

    /**
     * @return The ProcessingStep describing the execution of this Job.
     */
    ProcessingStep getProcessingStep()

    void setProcessingStep(ProcessingStep processingStep)

    /**
     * Invoked by the Container to indicate that the Job has been started. Called when the execute method
     * has been invoked.
     * Implement if the Job needs to know when execute is started.
     * @throws InvalidStateException In case the Job is not in a state where it could be started.
     */
    void start() throws InvalidStateException

    /**
     * Invoked by the Container to indicate that the Job has finished.
     * Implement if the Job needs to know exactly when the execution finished.
     * @throws InvalidStateException In case the Job is not in a state where it could end.
     */
    void end() throws InvalidStateException
}
