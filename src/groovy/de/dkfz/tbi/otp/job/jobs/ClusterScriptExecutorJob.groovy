package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.job.processing.AbstractOtpJob
import de.dkfz.tbi.otp.job.processing.ExecutionHelperService
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.HelperUtils
import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob.NextAction
import static de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys.REALM
import static de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys.SCRIPT

/**
 * A Job that takes an script via an input parameter and executes it on the PBS cluster.
 */
class ClusterScriptExecutorJob extends AbstractOtpJob {

    @Autowired
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

        // copy script because some scripts are too long for the Jsch library
        assert realm.stagingRootPath
        File stagingPath = new File(realm.stagingRootPath)
        assert stagingPath.isAbsolute()
        File scriptFolder = new File(stagingPath, "clusterScriptExecutorScripts")
        scriptFolder.mkdirs()
        File scriptFile = new File(scriptFolder, "${processingStep.id}-${HelperUtils.uniqueString}.sh")
        scriptFile.withWriter {
            it.write(script)
        }

        script = """
bash ${scriptFile.absolutePath}
"""
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
