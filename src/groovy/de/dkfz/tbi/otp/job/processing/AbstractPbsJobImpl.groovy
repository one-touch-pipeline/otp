package de.dkfz.tbi.otp.job.processing

/**
 * Abstract base class for {@link PbsJob}s.
 * @see PbsJob
 */
abstract public class AbstractPbsJobImpl extends AbstractJobImpl implements PbsJob {
    /**
     * Dependency injection of pbs service
     */
    def pbsService

    /**
     * Default empty constructor
     */
    public AbstractPbsJobImpl() {}
    public AbstractPbsJobImpl(ProcessingStep processingStep, Collection<Parameter> inputParameters) {
        super(processingStep, inputParameters)
    }

    /**
     * Triggers connection to PBS via ssh and returns List of PBS ids
     * 
     * To connect parameters set in properties file are used.
     * @return List of String with PBS ids
     */
    protected List<String> sendPbsJob() {
        File fileWithPbsId = pbsService.sendPbsJob()
        if (!fileWithPbsId.isFile || fileWithPbsId.size() == 0) {
            throw new ProcessingException("File for PBS ids is not existing or empty.")
        }
        List<String> pbsIds = pbsService.extractPbsIds(fileWithPbsId)
    }
}
