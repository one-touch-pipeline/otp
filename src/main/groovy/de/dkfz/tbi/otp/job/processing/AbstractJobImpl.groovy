/*
 * Copyright 2011-2019 The OTP authors
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

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.infrastructure.ClusterJobService
import de.dkfz.tbi.otp.utils.CollectionUtils

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

/**
 * Abstract Base class for all Job implementations.
 *
 * This class can be derived by Job implementations which do not need any further
 * customization. By basing an implementation on this class the Job can concentrate on
 * implementing the execute method only. The abstract class takes care of ensuring that
 * all input parameters are set and offers the derived classes a way to add generated
 * output parameters.
 *
 * An implementing sub class does not even have to define the constructors as those are
 * generated through an AST Transformation.
 *
 * @see Job
 */
@CompileDynamic
@Transactional
abstract class AbstractJobImpl implements Job {

    @Autowired
    JobStatusLoggingService jobStatusLoggingService

    @Autowired
    ClusterJobService clusterJobService

    // Not storing a reference to the {@link ProcessingStep} instance here, because it may be detached by the time it is
    // used again. Accessing it would result in Hibernate proxy errors (OTP-967).
    // See http://openjpa.apache.org/builds/1.0.3/apache-openjpa-1.0.3/docs/manual/jpa_overview_emfactory_perscontext.html.
    // (This job including the reference might live longer than the persistence context which it was retrieved from,
    // in particular if this is a MonitoringJob.)
    /**
     * @see #getProcessingStep()
     */
    private long processingStepId

    private final Set<Parameter> outputParameters = [] as Set

    static enum State {
        /** The Job has been created but not yet started */
        CREATED,
        /** The Job has been started, that is the processing is running */
        STARTED,
        /** The execution of the Job finished */
        FINISHED,
    }

    /**
     * The current execution state of the Job, will be updated
     * by {@link #start} and {@link #end}. It is used to decide
     * whether to throw an exception if e.g. the output parameters
     * are queried before the execution finished.
     * @see #start
     * @see #end
     */
    private State state = State.CREATED

    /**
     * Adds an output parameter to the list of output parameters provided
     * by the Job implementation.
     *
     * If a parameter with the same key already exists, this parameter will be
     * replaced.
     * @param name The name of the ParameterType for which the parameter is going to be added
     * @param value The value of the parameter
     */
    protected void addOutputParameter(String name, String value) {
        Iterator<Parameter> it = outputParameters.iterator()
        while (it.hasNext()) {
            Parameter param = it.next()
            if (param.type.name == name) {
                param.value = value
                return
            }
        }
        // find the ParameterType
        ParameterType type = CollectionUtils.atMostOneElement(ParameterType.findAllByNameAndJobDefinition(name, processingStep.jobDefinition))
        if (!type) {
            throw new ProcessingException("ParameterType missing")
        }
        if (type.parameterUsage != ParameterUsage.OUTPUT) {
            throw new ProcessingException("Not an output parameter")
        }
        outputParameters << new Parameter(type: type, value: value)
    }

    /**
     * @return The current state of the Job
     */
    protected State getState() {
        return state
    }

    @Override
    void start() throws InvalidStateException {
        switch (state) {
            case State.CREATED:
                state = State.STARTED
                break
            default:
                throw new InvalidStateException("Cannot start the Job from state " + state)
        }
    }

    @Override
    void end() throws InvalidStateException {
        switch (state) {
            case State.STARTED:
                state = State.FINISHED
                break
            default:
                throw new InvalidStateException("Cannot end the Job from state " + state)
        }
    }

    @Override
    Set<Parameter> getOutputParameters() throws InvalidStateException {
        switch (state) {
            case State.FINISHED:
                return outputParameters
            default:
                throw new InvalidStateException("Cannot access output parameters from state " + state)
        }
    }

    @Override
    void setProcessingStep(ProcessingStep processingStep) {
        switch (state) {
            case State.CREATED:
                processingStepId = processingStep.id
                break
            default:
                throw new InvalidStateException("Cannot set processingStep from state " + state)
        }
    }

    @Override
    ProcessingStep getProcessingStep() {
        return ProcessingStep.getInstance(processingStepId)
    }

    /**
     * Returns the object which is referenced by the {@link ProcessParameter} for the {@link Process} that this job
     * belongs to. If there is no such process parameter or object, this method will throw an exception.
     */
    @SuppressWarnings("ThrowRuntimeException") // ignored: will be removed with the old workflow system
    ProcessParameterObject getProcessParameterObject() {
        ProcessParameterObject object = processParameter.toObject()
        if (object == null) {
            throw new RuntimeException("Object referenced by ProcessParameter was not found.")
        }
        return object
    }

    ProcessParameterObject getRefreshedProcessParameterObject() {
        ProcessParameterObject object = processParameterObject
        object.refresh()
        return object
    }

    String getProcessParameterValue() {
        return processParameter.value
    }

    ProcessParameter getProcessParameter() {
        return exactlyOneElement(ProcessParameter.findAllByProcess(processingStep.process))
    }

    @SuppressWarnings("ThrowRuntimeException") // ignored: will be removed with the old workflow system
    Collection<ClusterJob> failedOrNotFinishedClusterJobs() {
        // For none multi jobs the LogFiles belong to the otp job sending the cluster job and not to the job validating the cluster job, which can fail.
        // The sending cluster job is two steps before the validating one.
        ProcessingStep sendStep = processingStep.previous?.previous
        if (!sendStep) {
            throw new RuntimeException("No sending processing step found for ${processingStep}")
        }

        Collection<ClusterJob> clusterJobs = ClusterJob.findAllByProcessingStep(sendStep)
        if (!clusterJobs) {
            throw new RuntimeException("No ClusterJobs found for ${sendStep}")
        }

        return jobStatusLoggingService.failedOrNotFinishedClusterJobs(
                sendStep,
                ClusterJobIdentifier.asClusterJobIdentifierList(clusterJobs)
        ).collect { ClusterJobIdentifier identifier ->
            return clusterJobService.getClusterJobByIdentifier(identifier, sendStep)
        }
    }
}
