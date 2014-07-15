package de.dkfz.tbi.otp.job.processing

import org.springframework.beans.factory.annotation.Autowired

/**
 * Abstract base class for {@link PbsJob}s.
 * @see PbsJob
 */
abstract public class AbstractPbsJobImpl extends AbstractJobImpl implements PbsJob {

    /**
     * Triggers connection to PBS via ssh and returns List of PBS ids
     *
     * To connect parameters set in properties file are used.
     * @return List of String with PBS ids
     */
    protected List<String> sendPbsJob(String cmd = null) {
        File fileWithPbsId = pbsService.sendPbsJob(cmd)
        if (!fileWithPbsId.isFile || fileWithPbsId.size() == 0) {
            throw new ProcessingException("File for PBS ids is not existing or empty.")
        }
        return pbsService.extractPbsIds(fileWithPbsId)
    }
}
