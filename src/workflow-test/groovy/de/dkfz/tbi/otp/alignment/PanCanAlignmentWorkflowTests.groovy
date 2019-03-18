/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.dkfz.tbi.otp.alignment

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.job.jobs.roddyAlignment.PanCanStartJob
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.ngsdata.SeqTrack

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

/**
 * test for PanCanAlignment workflow
 */
abstract class PanCanAlignmentWorkflowTests extends AbstractRoddyAlignmentWorkflowTests {

    @Autowired
    PanCanStartJob panCanStartJob


    @Test
    void testNoProcessableObjectFound() {

        // prepare
        RoddyBamFile firstBamFile = createFirstRoddyBamFile()
        createSeqTrack("readGroup2")

        List<SeqTrack> seqTracks = SeqTrack.findAllByLaneIdInList(["readGroup1"])

        MergingWorkPackage workPackage = exactlyOneElement(MergingWorkPackage.findAll())
        workPackage.needsProcessing = false
        workPackage.save(flush: true, failOnError: true)

        // run
        panCanStartJob.execute()

        // check
        assert 0 == Process.list().size()
        assert 1 == RoddyBamFile.findAll().size()
        checkFirstBamFileState(firstBamFile, true, [
                seqTracks         : seqTracks,
                containedSeqTracks: seqTracks,
        ])
        assertBamFileFileSystemPropertiesSet(firstBamFile)
    }

    @Test
    void testAlignLanesOnly_NoBaseBamExist_OneLane_allFine() {

        // prepare
        createSeqTrack("readGroup1")

        executeAndVerify_AlignLanesOnly_AllFine()
    }

    @Test
    void testAlignLanesOnly_NoBaseBamExist_OneLane_workflow_1_0_182_1_allFine() {

        // prepare
        createSeqTrack("readGroup1")

        MergingWorkPackage mergingWorkPackage = exactlyOneElement(MergingWorkPackage.findAll())

        createProjectConfigForQualityControlWorkflow(mergingWorkPackage)

        executeAndVerify_AlignLanesOnly_AllFine()
    }

    private createProjectConfigForQualityControlWorkflow(MergingWorkPackage mergingWorkPackage) {
        createProjectConfig(mergingWorkPackage, [
                pluginName       : "QualityControlWorkflows",
                pluginVersion    : "1.2.182",
                baseProjectConfig: "otpPanCanAlignmentWorkflow-1.3",
                configVersion    : "v2_0",
        ])
    }

    @Test
    void testAlignLanesOnly_NoBaseBamExist_OneLane_bwa_mem_0_7_8_sambamba_0_5_9_allFine() {

        // prepare
        createSeqTrack("readGroup1")

        MergingWorkPackage mergingWorkPackage = exactlyOneElement(MergingWorkPackage.findAll())

        createProjectConfig(mergingWorkPackage, [
                bwaMemVersion  : "0.7.8",
                sambambaVersion: "0.5.9",
                configVersion  : "v2_0",
        ])
        executeAndVerify_AlignLanesOnly_AllFine()
    }


    @Test
    void testAlignLanesOnly_NoBaseBamExist_OneLane_FastTrack_allFine() {

        fastTrackSetup()

        executeAndVerify_AlignLanesOnly_AllFine()
    }


    @Test
    void testAlignLanesOnly_NoBaseBamExist_OneLane_WithFingerPrinting_allFine() {

        // prepare
        createSeqTrack("readGroup1")
        setUpFingerPrintingFile()

        executeAndVerify_AlignLanesOnly_AllFine()
    }


    @Test
    void testAlignLanesOnly_NoBaseBamExist_TwoLanes_allFine() {
        alignLanesOnly_NoBaseBamExist_TwoLanes()
    }


    @Test
    void testAlignBaseBamAndNewLanes_allFine() {
        alignBaseBamAndNewLanesHelper(false)
    }

    @Test
    void testAlignBaseBamAndNewLanes_workflow_1_0_182_1_allFine() {
        MergingWorkPackage mergingWorkPackage = exactlyOneElement(MergingWorkPackage.findAll())

        createProjectConfigForQualityControlWorkflow(mergingWorkPackage)

        alignBaseBamAndNewLanesHelper(false)
    }

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

        List<RoddyBamFile> bamFiles = RoddyBamFile.findAll().sort { it.id }
        assert 2 == bamFiles.size()
        assert roddyBamFile == bamFiles.first()
        assert !bamFiles[1].baseBamFile
        checkFirstBamFileState(bamFiles[1], true, [identifier: 1])
        assertBamFileFileSystemPropertiesSet(bamFiles[1])
        checkFileSystemState(bamFiles[1])
    }
}
