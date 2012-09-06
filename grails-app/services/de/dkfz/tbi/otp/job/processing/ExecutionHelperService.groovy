package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.ngsdata.*

/**
 * This service is a layer between ExecutionService and jobs
 *
 * Execution service is focused on more basics aspects of sending jobs
 * and executing commands. In particular it will be changed if protocols
 * change. This service is responsible for more abstract operations which
 * implementation is independent of the protocol details
 *
 *
 */
class ExecutionHelperService {

    def executionService

    public String sendScript(Realm realm, String text) {
        String pbsResponse = executionService.executeJob(realm, text)
        List<String> extractedPbsIds = executionService.extractPbsIds(pbsResponse)
        if (extractedPbsIds.size() != 1) {
            log.debug "Number of PBS jobs is = ${extractedPbsIds.size()}"
        }
        return extractedPbsIds.get(0)
    }
}
