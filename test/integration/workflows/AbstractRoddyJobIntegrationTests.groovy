package workflows

import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.utils.HelperUtils
import org.junit.Ignore

import static de.dkfz.tbi.TestCase.shouldFail
import org.junit.Test

/**
 * tests for AbstractRoddyJob using PanCanAlignmentWorkflow
 */
class AbstractRoddyJobIntegrationTests extends AbstractPanCanAlignmentWorkflowTests {

    @Ignore
    @Test
    void executeRoddy_roddyCallFails_noPbsJobsSent_RoddyJobFailedAndSuccessfullyRestarted() {

        // prepare

        createFirstRoddyBamFile()
        createSeqTrack("readGroup2")
        resetProjectConfig(roddyFailsProjectConfig)

        // run

        assert shouldFail(RuntimeException) {
            execute()
        }.contains("The exit value is not 0")

        // repair

        resetProjectConfig(projectConfigFile)

        // run

        restartWorkflowFromFailedStep()

        // check

        checkAllAfterSuccessfulExecution_alignBaseBamAndNewLanes()
    }

    @Ignore
    @Test
    void executeRoddy_roddyCallSucceeds_noPbsJobsSent_RoddyJobFailedAndSuccessfullyRestarted() {

        // prepare

        createFirstRoddyBamFile()
        createSeqTrack("readGroup2")
        // setting not existing plugin must make roddy exit without sending any PBS jobs
        setPluginVersion(HelperUtils.getUniqueString())

        // run

        assert shouldFail(RuntimeException) {
            execute()
        }.contains("Roddy output contains no information about output directories")

        // repair

        setPluginVersion(getPluginVersion(projectConfigFile))

        // run

        restartWorkflowFromFailedStep()

        // check

        checkAllAfterSuccessfulExecution_alignBaseBamAndNewLanes()
    }

    @Ignore
    @Test
    void executeRoddy_roddyCallSucceeds_onePbsJobFails_RoddyJobFailedAndSuccessfullyRestarted() {

        // prepare

        RoddyBamFile firstBamFile = createFirstRoddyBamFile()
        createSeqTrack("readGroup2")

        // create invalid stat file and register it in the database
        // this will make one of pbs jobs fail
        MergingWorkPackage workPackage = firstBamFile.workPackage
        File statDir = referenceGenomeService.pathToChromosomeSizeFilesPerReference(workPackage.project, workPackage.referenceGenome)
        File statFile = new File(statDir, "invalid-stat-${HelperUtils.getUniqueString()}.tab")
        statFile << HelperUtils.getUniqueString()
        workPackage.refresh()
        workPackage.statSizeFileName = statFile.name
        workPackage.save(flush: true, failOnError: true)

        // run

        assert shouldFail(RuntimeException) {
            execute()
        }.contains("Status code: 15")

        // repair

        workPackage.refresh()
        workPackage.statSizeFileName = CHROMOSOME_STAT_FILE_NAME
        workPackage.save(flush: true, failOnError: true)

        // run

        restartWorkflowFromFailedStep()

        // check

        checkAllAfterRoddyPbsJobsRestartAndSuccessfulExecution_alignBaseBamAndNewLanes()
    }
}
