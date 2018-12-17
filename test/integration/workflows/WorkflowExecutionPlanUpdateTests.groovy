package workflows

import org.junit.Ignore
import org.junit.Test

import de.dkfz.tbi.otp.ngsdata.SeqTrack


/*
 * The idea of the test is that with the old jobExecutionPlan the test would fail since the outputParameter "Realm"
 * would be missing. Since the test do not fail it is shown that the old jobExecutionPlan was updated to the newer one
 * which provides "Realm" as outputParameter.
 */

@Ignore
class WorkflowExecutionPlanUpdateTests extends DataInstallationWorkflowTests {

    @Test
    void testUpdateOfJobExecutionPlan() {
        SeqTrack seqTrack = createWholeGenomeSetup()

        execute()

        checkThatWorkflowWasSuccessful(seqTrack)
    }

    @Override
    List<String> getWorkflowScripts() {
        return [
                "scripts/workflows/OldDataInstallationWorkflow.groovy",
                "scripts/workflows/DataInstallationWorkflow.groovy",
        ]
    }
}
