package de.dkfz.tbi.otp.job.processing

/**
 * Abstract base class for {@link EndStateAwareJob}s.
 * @see EndStateAwareJob
 */
abstract public class AbstractEndStateAwareJobImpl implements EndStateAwareJob {
    /**
    * Dependency injection for Grails Application
    */
   def grailsApplication
   /**
    * The processing step for this Job
    */
   private ProcessingStep processingStep
   /**
    * The input parameters passed to the Job
    */
   private final Collection<Parameter> inputParameters = new HashSet()
   /**
    * The output parameters generated by the Job
    */
   public final Set<Parameter> outputParameters = new HashSet()
   /**
    * The current execution state of the Job, will be updated
    * by {@link start} and {@link end}. It is used to decide
    * whether to throw an exception if e.g. the output parameters
    * are queried before the execution finished.
    * @see start
    * @see end
    */
   private ExecutionState state = ExecutionState.CREATED
   private ExecutionState endState = null

   /**
    * Empty default constructor. Required by Spring.
    */
   protected AbstractEndStateAwareJobImpl() {
   }

   /**
    * Constructor used by the factory method. Each implementing sub-class gets a matching Constructor injected.
    * @param processingStep The processing step for this Job
    * @param inputParameters The input parameters for this Job
    */
   protected AbstractEndStateAwareJobImpl(ProcessingStep processingStep, Collection<Parameter> inputParameters) {
       this.processingStep = processingStep
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
   protected final void addOutputParameter(String name, String value) {
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
   protected final Collection<Parameter> getInputParameters() {
       return inputParameters
   }

   /**
    *
    * @return The current state of the Job
    */
   protected final ExecutionState getState() {
       return state
   }

   @Override
   public final void start() throws InvalidStateException {
       switch (state) {
       case ExecutionState.CREATED:
           state = ExecutionState.STARTED
           break
       case ExecutionState.SUSPENDED:
           state = ExecutionState.RESUMED
           break
       default:
           throw new InvalidStateException("Cannot start the Job from state " + state)
       }
   }

   @Override
   public final void end() throws InvalidStateException {
       switch (state) {
       case ExecutionState.STARTED:
       case ExecutionState.RESTARTED:
       case ExecutionState.RESUMED:
           state = ExecutionState.FINISHED
           break
       default:
           throw new InvalidStateException("Cannot end the Job from state " + state)
       }
   }

   @Override
   public final Set<Parameter> getOutputParameters() throws InvalidStateException {
       switch (state) {
       case ExecutionState.FINISHED: // fall through
       case ExecutionState.SUCCESS: // fall through
       case ExecutionState.FAILURE:
           return outputParameters
       default:
           throw new InvalidStateException("Cannot acces output parameters from state " + state)
       }
   }

   @Override
   public final ProcessingStep getProcessingStep() {
       return processingStep;
   }

   @Override
   public String getVersion() {
       return "";
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

   public String getProcessParameterValue() {
       ProcessParameter parameter = ProcessParameter.findByProcess(processingStep.process)
       if (!parameter) {
           throw new RuntimeException("Required parameter not found")
       }
       return parameter.value
   }

    /**
     * Can be used by an implementing Job to set the Job as failed.
     * @see succeed
     */
    protected final void fail() {
        this.endState = ExecutionState.FAILURE
    }

    /**
     * Can be used by an implementing Job to set the Job as succeeded.
     * @see fail
     */
    protected final void succeed() {
        this.endState = ExecutionState.SUCCESS
    }

    @Override
    public final ExecutionState getEndState() throws InvalidStateException {
        if (!endState) {
            throw new InvalidStateException("EndState accessed without end state being set")
        }
        if (getState() != ExecutionState.FINISHED && getState() != ExecutionState.SUCCESS) {
            throw new InvalidStateException("EndState accessed but not in finished state")
        }
        return endState
    }
}
