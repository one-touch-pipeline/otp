package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.processing.AbstractMaybeSubmitWaitValidateJob
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob.NextAction
import de.dkfz.tbi.otp.job.processing.ExecutionHelperService
import de.dkfz.tbi.otp.ngsdata.Realm

import static de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys.REALM
import static de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys.SCRIPT

/**
 * A Job that takes an script via an input parameter and executes it on the PBS cluster.
 */
class ClusterScriptExecutorJob extends AbstractMaybeSubmitWaitValidateJob {

    ExecutionHelperService executionHelperService

    @Override
    protected NextAction maybeSubmit() throws Throwable {

        String script = getParameterValueOrClass("${SCRIPT}")
        assert script != null : 'No script passed as input parameter'

        String realmID = getParameterValueOrClass("${REALM}")
        assert realmID != null: 'No realm passed as input parameter'

        if (script.empty && realmID.empty) {
            // Silently ignore empty scripts and move on to the next job.
            return NextAction.SUCCEED
        } else if (script.empty || realmID.empty) {
            throw new RuntimeException("Either both ${SCRIPT} and ${REALM} must be provided or none.")
        }

        Realm realm = Realm.findById(Long.parseLong(realmID))
        assert realm: "No realm found for id ${realmID}"

        executionHelperService.sendScript(realm, script) // Will NOT add output parameters
        return NextAction.WAIT_FOR_CLUSTER_JOBS
    }

    /**
     * Dummy validation function. The validation about the job being run successfully is done in the super-class.
     *
     * @throws Throwable will never be thrown, as there is no implementation
     */
    @Override
    protected void validate() throws Throwable {
        // This function body in intentionally left blank.
    }
}
