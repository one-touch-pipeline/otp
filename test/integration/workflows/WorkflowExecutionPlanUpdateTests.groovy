package workflows

import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.SeqTypeNames
import org.junit.*
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils

/**
 *
 * To test if updating of the JobExecutionPlan works the normal workflow test preparations have to be done first.
 * Furthermore in class "AbstractStartJobImpl" in the method "initializeJobExecutionPlan" the line
 * "if (plan == null && Environment.current != Environment.TEST) {" has to be changed to "if (plan == null) {"
 *
 * The idea of the test is that with the old jobExecutionPlan the test would fail since the outputParameter "Realm"
 * would be missing. Since the test do not fail it is shown that the old jobExecutionPlan was updated to the newer one
 * which provides "Realm" as outputParameter.
 */

class WorkflowExecutionPlanUpdateTests extends DataInstallationWorkflowTests {

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
                run("scripts/workflows/OldDataInstallationWorkflow.groovy")
                run("scripts/workflows/DataInstallationWorkflow.groovy")
            }
        }
    }
}
