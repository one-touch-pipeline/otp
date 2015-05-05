package workflows

import de.dkfz.tbi.otp.job.jobs.dataInstallation.DataInstallationStartJob
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.SeqTypeNames
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.junit.Ignore
import org.junit.Test

/**
 *
 * The idea of the test is that with the old jobExecutionPlan the test would fail since the outputParameter "Realm"
 * would be missing. Since the test do not fail it is shown that the old jobExecutionPlan was updated to the newer one
 * which provides "Realm" as outputParameter.
 */

class WorkflowExecutionPlanUpdateTests extends DataInstallationWorkflowTests {

    //required for getStartJobRunnable()
    DataInstallationStartJob dataInstallationStartJob


    @Ignore
    @Test
    void testUpdateOfJobExecutionPlan() {
        SeqTrack seqTrack = furtherDataBaseSetup()

        jobExecutionPlanSetup()
        waitUntilWorkflowFinishesWithoutFailure(TIMEOUT)

        checkThatWorkflowWasSuccessful(seqTrack)
    }

    private SeqTrack furtherDataBaseSetup() {
        SeqType seqType = createSeqType(SeqTypeNames.WHOLE_GENOME.seqTypeName, "SeqTypeDir")

        SeqTrack seqTrack = createSeqTrack(seqType)
        createDataFiles(seqTrack)
        assert DataFile.findAllBySeqTrack(seqTrack)

        return seqTrack
    }

    private void jobExecutionPlanSetup() {
        SpringSecurityUtils.doWithAuth("admin") {
            JobExecutionPlan.withTransaction {
                runScript("scripts/workflows/OldDataInstallationWorkflow.groovy")
                runScript("scripts/workflows/DataInstallationWorkflow.groovy")
            }
        }
    }
}
