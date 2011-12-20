package de.dkfz.tbi.otp.job.processing

import java.util.List

import javax.servlet.ServletContext

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.beans.factory.annotation.Autowired

/**
 * Abstract base class for {@link PbsJob}s.
 * @see PbsJob
 */
abstract public class AbstractPbsJobImpl extends AbstractJobImpl implements PbsJob {

    /**
     * Dependency injection of grails Application
     */
    @Autowired
    GrailsApplication grailsApplication
    /**
     * Dependency injection of Servlet Context
     */
    @Autowired
    ServletContext servletContext

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
    public List<String> sendPbsJob() {
        PbsHelper pbsHelper = new PbsHelper()
        File fileWithPbsId = pbsHelper.sendPbsJob()
        if(!fileWithPbsId.isFile || fileWithPbsId.size() == 0) {
            throw new ProcessingException("File for PBS ids is not existing or empty.")
        }
        List<String> pbsIds = pbsHelper.extractPbsIds(fileWithPbsId)
    }
}
