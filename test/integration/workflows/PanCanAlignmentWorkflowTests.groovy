package workflows

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.jobs.roddyAlignment.*
import de.dkfz.tbi.otp.job.processing.*
import org.junit.*
import org.springframework.beans.factory.annotation.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

/**
 * test for PanCanAlignment workflow
 */
abstract class PanCanAlignmentWorkflowTests extends AbstractPanCanAlignmentWorkflowTests {

    @Autowired
    PanCanStartJob panCanStartJob


    @Test
    void testNoProcessableObjectFound() {

        // prepare
        RoddyBamFile firstBamFile = createFirstRoddyBamFile()
        createSeqTrack("readGroup2")

        MergingWorkPackage workPackage = exactlyOneElement(MergingWorkPackage.findAll())
        workPackage.needsProcessing = false
        workPackage.save(flush: true, failOnError: true)

        // run
        panCanStartJob.execute()

        // check
        assert 0 == Process.list().size()
        assert 1 == RoddyBamFile.findAll().size()
        checkFirstBamFileState(firstBamFile, true)
        assertBamFileFileSystemPropertiesSet(firstBamFile)
    }

    @Test
    void testAlignLanesOnly_NoBaseBamExist_OneLane_allFine() {

        // prepare
        createSeqTrack("readGroup1")

        executeAndVerify_AlignLanesOnly_AllFine()
    }

    @Test
    void testAlignLanesOnly_NoBaseBamExist_OneLane_FastTrack_allFine() {

        fastTrackSetup()

        executeAndVerify_AlignLanesOnly_AllFine()
    }

    @Ignore ("convey")
    @Test
    void testConveyAlignLanesOnly_NoBaseBamExist_OneLane_FastTrack_allFine() {

        // config must point to project-config with convey options
        resetProjectConfig(conveyProjectConfigFile)

        fastTrackSetup()

        executeAndVerify_AlignLanesOnly_AllFine()
    }


    @Test
    void testAlignLanesOnly_NoBaseBamExist_TwoLanes_allFine() {
        alignLanesOnly_NoBaseBamExist_TwoLanes()
    }

    @Ignore("convey")
    @Test
    void testConveyAlignLanesOnly_NoBaseBamExist_TwoLanes_allFine() {
        // config must point to project-config with convey options
        resetProjectConfig(conveyProjectConfigFile)
        alignLanesOnly_NoBaseBamExist_TwoLanes()
    }

    @Ignore('OTP-2192')
    @Test
    void testAlignBaseBamAndNewLanes_allFine() {
        alignBaseBamAndNewLanesHelper(false)
    }

    @Ignore('OTP-2192')
    @Test
    void testAlignBaseBamAndNewLanes_allFine_oldStructure() {
        alignBaseBamAndNewLanesHelper(true)
    }

    protected void alignBaseBamAndNewLanesHelper(boolean useOldStructure) {
        // prepare
        createFirstRoddyBamFile(useOldStructure)
        createSeqTrack("readGroup2")

        // run
        execute()

        // check
        checkAllAfterSuccessfulExecution_alignBaseBamAndNewLanes()
    }

    @Test
    void testAlignWithWithdrawnBase_allFine() {
        // prepare
        RoddyBamFile roddyBamFile = createFirstRoddyBamFile()
        roddyBamFile.withdrawn = true
        roddyBamFile.save(flush: true, failOnError: true)
        roddyBamFile.mergingWorkPackage.needsProcessing = true
        roddyBamFile.mergingWorkPackage.save(flush: true, failOnError: true)

        // run
        execute()

        // check
        assert !roddyBamFile.workDirectory.exists()
        checkWorkPackageState()

        List<RoddyBamFile> bamFiles = RoddyBamFile.findAll().sort {it.id}
        assert 2 == bamFiles.size()
        assert roddyBamFile == bamFiles.first()
        assert !bamFiles[1].baseBamFile
        checkFirstBamFileState(bamFiles[1], true, [identifier: 1])
        assertBamFileFileSystemPropertiesSet(bamFiles[1])
        checkFileSystemState(bamFiles[1])
    }
}
