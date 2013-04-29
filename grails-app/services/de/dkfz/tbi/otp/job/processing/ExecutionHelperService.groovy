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

    /**
     * Executes a job on a specified realm. It is a helper to simplify the using of {@link ExecutionService}.
     * It uses {@link ExecutionService#executeJob} to send the job and {@link ExecutionService#extractPbsIds} to
     * extract the cluster job id.
     * It uses indirect {@link PbsOptionMergingService#mergePbsOptions(Realm, String)}
     * with the given realm and jobIdentifier to create the merged PBS options String to pass to qsub command.
     * If a job key is given, a {@link ProcessingOption} for the job identifier and the cluster defined by the realm
     * has to exist, otherwise a {@link NullPointerException} is thrown. The JSON format is described in
     * {@link PbsOptionMergingService}.
     *
     * @param realm The realm which identifies the host
     * @param text The script to be run a pbs system
     * @param jobIdentifier the name of a job to take job-cluster specific parameters
     * @return what the server sends back
     * @throws NullPointerException if a job identifier is provided, but no PBS option is defined for this
     *          job identifier and the cluster ({@link Realm#cluster}) of the {@link Realm}
     * @see PbsOptionMergingService#mergePbsOptions(Realm, String)
     */
    public String sendScript(Realm realm, String text, String jobIdentifier = null) {
        String pbsResponse = executionService.executeJob(realm, text, jobIdentifier)
        List<String> extractedPbsIds = executionService.extractPbsIds(pbsResponse)
        if (extractedPbsIds.size() != 1) {
            log.debug "Number of PBS jobs is = ${extractedPbsIds.size()}"
        }
        return extractedPbsIds.get(0)
    }
}
