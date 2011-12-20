package de.dkfz.tbi.otp.job.processing

import javax.servlet.ServletContext

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.beans.factory.annotation.Autowired

/**
 * Abstract base class for {@link ValidatingJob}s.
 * @see ValidatingJob
 */
abstract public class AbstractValidatingJobImpl extends AbstractEndStateAwareJobImpl implements ValidatingJob {

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
    public AbstractValidatingJobImpl() {
    }
    public AbstractValidatingJobImpl(ProcessingStep processingStep, Collection<Parameter> inputParameters) {
        super(processingStep, inputParameters)
    }

    @Override
    public List<ProcessingStep> getValidatorFor() {
        return getProcessingStep().all.toList()
    }

    private PbsHelper pbsHelper = new PbsHelper()

    private Map<String, Boolean> validate(List<String> pbsIds) {
        if(!pbsIds) {
            return [:]
        }
        Map<String, Boolean> stats
        for(String pbsId : pbsIds) {
            String qstat = "qstat -i ${pbsId}"
            File tmpStat= pbsHelper.sshConnect(null, qstat)
            if(tmpStat.size() == 0 || !tmpStat.isFile) {
                throw new ProcessingException("Temporary file to contain qstat could not be written properly.")
            }
            Boolean running = isRunning(tmpStat)
            stats.put(running, pbsId)
        }
        return stats
    }

    private boolean isRunning(File file) {
        def pattern = grailsApplication.config.otp.pbs.pattern.running
        file.eachLine { String line ->
            if(pattern.matcher(line)) {
                return true
            }
        }
        return false
    }
}
