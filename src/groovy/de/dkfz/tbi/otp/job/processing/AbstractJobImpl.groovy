package de.dkfz.tbi.otp.job.processing

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

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
abstract class AbstractJobImpl implements Job {

    def grailsApplication

    // Not storing a reference to the {@link ProcessingStep} instance here, because it may be detached by the time it is
    // used again. Accessing it would result in Hibernate proxy errors (OTP-967).
    // See http://openjpa.apache.org/builds/1.0.3/apache-openjpa-1.0.3/docs/manual/jpa_overview_emfactory_perscontext.html.
    // (This job including the reference might live longer than the persistence context which it was retrieved from,
    // in particular if this is a MonitoringJob.)
    /**
     * @see #getProcessingStep()
     */
    private long processingStepId

    private final Collection<Parameter> inputParameters = new HashSet()

    private final Set<Parameter> outputParameters = new HashSet()

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
     * by {@link start} and {@link end}. It is used to decide
     * whether to throw an exception if e.g. the output parameters
     * are queried before the execution finished.
     * @see start
     * @see end
     */
    private State state = State.CREATED

    /**
     * Empty default constructor. Required by Spring.
     */
    protected AbstractJobImpl() {
    }

    /**
     * Constructor used by the factory method. Each implementing sub-class gets a matching Constructor injected.
     * @param processingStep The processing step for this Job
     * @param inputParameters The input parameters for this Job
     */
    protected AbstractJobImpl(ProcessingStep processingStep, Collection<Parameter> inputParameters) {
        processingStepId = processingStep.id
        if (inputParameters) {
            this.inputParameters.addAll(inputParameters)
        }
    }

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
        ParameterType type = ParameterType.findByNameAndJobDefinition(name, processingStep.jobDefinition)
        if (!type) {
            throw new ProcessingException("ParameterType missing")
        }
        if (type.parameterUsage != ParameterUsage.OUTPUT) {
            throw new ProcessingException("Not an output parameter")
        }
        outputParameters << new Parameter(type: type, value: value)
    }

    /**
     *
     * @return List of input parameters set to the job
     */
    protected Collection<Parameter> getInputParameters() {
        return inputParameters
    }

    /**
     *
     * @return The current state of the Job
     */
    protected State getState() {
        return state
    }

    @Override
    public void start() throws InvalidStateException {
        switch (state) {
            case State.CREATED:
                state = State.STARTED
                break
            default:
                throw new InvalidStateException("Cannot start the Job from state " + state)
        }
    }

    @Override
    public void end() throws InvalidStateException {
        switch (state) {
            case State.STARTED:
                state = State.FINISHED
                break
            default:
                throw new InvalidStateException("Cannot end the Job from state " + state)
        }
    }

    @Override
    public Set<Parameter> getOutputParameters() throws InvalidStateException {
        switch (state) {
            case State.FINISHED:
                return outputParameters
            default:
                throw new InvalidStateException("Cannot access output parameters from state " + state)
        }

    }

    /**
     * Retrieves the value of the specified output parameter.
     *
     * If the return value is <code>null</code>, this could either mean that the parameter is not set or
     * the value of the parameter is actually <code>null</code>.
     *
     * @param param the name of the output parameter
     * @return the value of the output parameter, or <code>null</code> if the parameter is not set.
     */
    public String getOutputParameterValue(final String param) {
        Parameter p = outputParameters?.find {
            it.type.name == param
        }
        p?.value
    }

    @Override
    public ProcessingStep getProcessingStep() {
        return ProcessingStep.getInstance(processingStepId)
    }

    @Override
    public String getVersion() {
        return ""
    }

    /**
     * Returns the parameter value or the associated class.
     *
     * The parameter value is the value of the instance the job has an instance of.
     * If the associated parameter type has the className value set this one is returned
     * instead of the parameter value. Therefore the return type of the method is generic.
     * @param typeName The type name of the type to be returned a value of
     * @return The parameter value or the class of the instance.
     * @throws RuntimeException In case the parameter could not be found.
     */
    public <T> T getParameterValueOrClass(String typeName) {
        Parameter parameter = processingStep.input.find { it.type.name == typeName }
        if (!parameter) {
            throw new RuntimeException("Required parameter not found")
        }
        if(parameter.type.className) {
            return grailsApplication.getClassForName(parameter.type.className).get(parameter.value.toLong())
        }
        return parameter.value
    }

    /**
     * Returns the object which is referenced by the {@link ProcessParameter} for the {@link Process} that this job
     * belongs to. If there is no such process parameter or object, this method will throw an exception.
     */
    public def getProcessParameterObject() {
        def object = exactlyOneElement(ProcessParameter.findAllByProcess(processingStep.process)).toObject()
        if (object == null) {
            throw new RuntimeException("Object referenced by ProcessParameter was not found.")
        }
        return object
    }

    public String getProcessParameterValue() {
        ProcessParameter parameter = ProcessParameter.findByProcess(processingStep.process)
        if (!parameter) {
            throw new RuntimeException("Required parameter not found")
        }
        return parameter.value
    }
}
