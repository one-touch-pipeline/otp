package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.ProcessHelperService

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
     *
     * @param realm The realm which identifies the host
     * @param text The script to be run a pbs system
     * @param qsubParameters The parameters which are needed for some qsub commands and can not be included in the text parameter.
     * The qsubParameters must be in JSON format!
     * @return what the server sends back
     * @throws NullPointerException if a job identifier is provided, but no PBS option is defined for this
     *          job identifier and the cluster ({@link Realm#cluster}) of the {@link Realm}
     */
    public String sendScript(Realm realm, String text, String qsubParameters = "") {
        String pbsResponse = executionService.executeJob(realm, text, qsubParameters)
        List<String> extractedPbsIds = executionService.extractPbsIds(pbsResponse)
        if (extractedPbsIds.size() != 1) {
            throw new ProcessingException("Could not extract a unique PBS ID. The received response is: " + pbsResponse)
        }
        return extractedPbsIds.get(0)
    }

    /**
     * Executes a job on a specified realm. This is a convenience method that accepts a {@link Closure} that
     * returns a {@link java.lang.String} and calls {@link #sendScript(Realm, String)}.
     *
     * It will also add all required job output parameters for the @{link WatchdogJob} when called from a {@link Job}.
     *
     * @param realm The realm which identifies the host
     * @param createScript The closure that creates the script to be sent
     * @return what the server sends back
     *
     * @deprecated Create/use a subclass of {@link AbstractMultiJob}, then cluster job IDs no longer have to be passed
     * between jobs.
     */
    @Deprecated
    public String sendScript(Realm realm, Closure createScript) {

        def isCalledFromJob = (createScript.delegate instanceof AbstractJobImpl)

        String pbsId = sendScript(realm, createScript())

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



    String getGroup(File directory) {
        assert directory: 'directory may not be null'
        ProcessHelperService.executeAndAssertExitCodeAndErrorOutAndReturnStdout("stat -c '%G' ${directory}")
    }

    String setGroup(Realm realm, File directory, String group) {
        assert realm: 'realm may not be null'
        assert directory: 'directory may not be null'
        assert group: 'group may not be null'
        executionService.executeCommand(realm, "chgrp ${group} ${directory}")
    }

    String setPermission(Realm realm, File directory, String permission) {
        assert realm: 'realm may not be null'
        assert directory: 'directory may not be null'
        assert permission: 'permission may not be null'
        executionService.executeCommand(realm, "chmod  ${permission} ${directory}")
    }
}
