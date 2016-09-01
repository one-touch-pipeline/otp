package workflows

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.junit.*

import static de.dkfz.tbi.TestCase.*

/**
 * tests for AbstractRoddyJob using PanCanAlignmentWorkflow
 */
@Ignore
class AbstractRoddyJobIntegrationTests extends AbstractPanCanAlignmentWorkflowTests {

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
        } =~ /The project configuration .* could not be found./

        // repair

        setPluginVersion(getPluginVersion(projectConfigFile))

        // run

        restartWorkflowFromFailedStep()

        // check

        checkAllAfterSuccessfulExecution_alignBaseBamAndNewLanes()
    }

    @Test
    void executeRoddy_roddyCallSucceeds_onePbsJobFails_RoddyJobFailedAndSuccessfullyRestarted() {
        String DUMMY_STAT_SIZE_FILE_NAME = "dummy.tab"

        // prepare

        RoddyBamFile firstBamFile = createFirstRoddyBamFile()
        createSeqTrack("readGroup2")

        // create invalid stat file and register it in the database
        // this will make one of pbs jobs fail
        MergingWorkPackage workPackage = firstBamFile.workPackage
        File statDir = referenceGenomeService.pathToChromosomeSizeFilesPerReference(workPackage.project, workPackage.referenceGenome)
        executionService.executeCommand(realm, "chmod g+w ${statDir}")
        File statFile = new File(statDir, DUMMY_STAT_SIZE_FILE_NAME)
        workPackage.refresh()
        workPackage.statSizeFileName = statFile.name
        workPackage.save(flush: true, failOnError: true)

        // run

        assert shouldFail(RuntimeException) {
            execute()
        }.contains("Status code: 15")

        // repair

        workPackage.refresh()
        workPackage.statSizeFileName = getChromosomeStatFileName()
        workPackage.save(flush: true, failOnError: true)

        // run

        restartWorkflowFromFailedStep()

        // check

        checkAllAfterRoddyPbsJobsRestartAndSuccessfulExecution_alignBaseBamAndNewLanes()
    }

    @Override
    SeqType findSeqType() {
        return CollectionUtils.exactlyOneElement(SeqType.findAllWhere(
                name: SeqTypeNames.WHOLE_GENOME.seqTypeName,
                libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED,
        ))
    }
}
