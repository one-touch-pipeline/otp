package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys
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

    ExecutionService executionService

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
     * @param qsubParameter The parameter which are needed for some qsub commands and can not be included in the text parameter.
     * The qsubParameter must be in JSON format!
     * @return what the server sends back
     * @throws NullPointerException if a job identifier is provided, but no PBS option is defined for this
     *          job identifier and the cluster ({@link Realm#cluster}) of the {@link Realm}
     * @see PbsOptionMergingService#mergePbsOptions(Realm, String)
     */
    public String sendScript(Realm realm, String text, String jobIdentifier = null, String qsubParameters = "") {
        String pbsResponse = executionService.executeJob(realm, text, jobIdentifier, qsubParameters)
        List<String> extractedPbsIds = executionService.extractPbsIds(pbsResponse)
        if (extractedPbsIds.size() != 1) {
            throw new ProcessingException("Could not extract a unique PBS ID. The received response is: " + pbsResponse)
        }
        return extractedPbsIds.get(0)
    }

    /**
     * Executes a job on a specified realm. This is a convenience method that accepts a {@link Closure} that
     * returns a {@link java.lang.String} and calls {@link #sendScript(Realm, String, String)}.
     *
     * It will also add all required job output parameters for the @{link WatchdogJob} when called from a {@link Job}.
     *
     * @param realm The realm which identifies the host
     * @param createScript The closure that creates the script to be sent
     * @return what the server sends back
     */
    public String sendScript(Realm realm, String jobIdentifier = null, Closure createScript) {

        def isCalledFromJob = (createScript.delegate instanceof AbstractJobImpl)

        String pbsId = sendScript(realm, createScript(), jobIdentifier)

        if (isCalledFromJob) {
            // "Automagically" add required job parameters
            AbstractJobImpl job = (AbstractJobImpl)createScript.delegate

            String outputParamValue = job.getOutputParameterValue("${JobParameterKeys.PBS_ID_LIST}")
            String jobIdList = outputParamValue ? "${outputParamValue},${pbsId}" : pbsId

            job.addOutputParameter("${JobParameterKeys.PBS_ID_LIST}", jobIdList)
            job.addOutputParameter("${JobParameterKeys.REALM}", "${realm.id}")
        }
        return pbsId
    }
}
