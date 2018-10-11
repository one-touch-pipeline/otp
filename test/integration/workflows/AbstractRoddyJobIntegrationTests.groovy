package workflows

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.job.processing.ExecutionState
import de.dkfz.tbi.otp.job.processing.ProcessingStepUpdate
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.junit.*

import static de.dkfz.tbi.TestCase.*

/**
 * tests for AbstractRoddyJob using PanCanAlignmentWorkflow
 */
@Ignore
class AbstractRoddyJobIntegrationTests extends AbstractRoddyAlignmentWorkflowTests {

    @Test
    void executeRoddy_roddyCallSucceeds_noClusterJobsSent_RoddyJobFailedAndSuccessfullyRestarted() {

        // prepare
        createFirstRoddyBamFile()
        createSeqTrack("readGroup2")

        RoddyWorkflowConfig config = CollectionUtils.exactlyOneElement(RoddyWorkflowConfig.findAll())
        String pluginVersion = config.pluginVersion

        // setting not existing plugin must make roddy exit without sending any cluster jobs
        config.pluginVersion = HelperUtils.getUniqueString()
        config.save(flush: true)

        // run
        assert shouldFail(RuntimeException) {
            execute()
        } =~ /The project configuration .* could not be found./

        // repair
        config.pluginVersion = pluginVersion
        config.save(flush: true)

        // run
        restartWorkflowFromFailedStep()

        // check
        checkAllAfterSuccessfulExecution_alignBaseBamAndNewLanes()
    }

    @Test
    void executeRoddy_roddyCallSucceeds_oneClusterJobFails_RoddyJobFailedAndSuccessfullyRestarted() {
        String DUMMY_STAT_SIZE_FILE_NAME = "dummy.tab"

        // prepare

        RoddyBamFile firstBamFile = createFirstRoddyBamFile()
        createSeqTrack("readGroup2")

        // create invalid stat file and register it in the database
        // this will make one of the cluster jobs fail
        MergingWorkPackage workPackage = firstBamFile.workPackage
        File statDir = referenceGenomeService.pathToChromosomeSizeFilesPerReference(workPackage.referenceGenome)
        remoteShellHelper.executeCommand(realm, "chmod g+w ${statDir}")
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

        checkAllAfterRoddyClusterJobsRestartAndSuccessfulExecution_alignBaseBamAndNewLanes()
    }


    @Override
    void checkForFailedClusterJobs() {
        assert ClusterJob.all.every { it.jobLog != null }
    }


    @Override
    SeqType findSeqType() {
        return CollectionUtils.exactlyOneElement(SeqType.findAllWhere(
                name: SeqTypeNames.WHOLE_GENOME.seqTypeName,
                libraryLayout: LibraryLayout.PAIRED,
        ))
    }
}
