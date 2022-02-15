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
package de.dkfz.tbi.otp.alignment.roddy.panCan

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.alignment.roddy.AbstractRoddyAlignmentWorkflowTests
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.job.jobs.roddyAlignment.PanCanStartJob
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.SessionUtils

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

/**
 * test for PanCanAlignment workflow
 */
abstract class AbstractPanCanAlignmentWorkflowTests extends AbstractRoddyAlignmentWorkflowTests {

    @Autowired
    PanCanStartJob panCanStartJob

    void "test no processableObject found"() {
        given:
        RoddyBamFile firstBamFile
        List<SeqTrack> seqTracks
        SessionUtils.withNewTransaction {
            firstBamFile = createFirstRoddyBamFile()
            createSeqTrack("readGroup2")

            seqTracks = SeqTrack.findAllByLaneIdInList(["readGroup1"])

            MergingWorkPackage workPackage = exactlyOneElement(MergingWorkPackage.findAll())
            workPackage.needsProcessing = false
            workPackage.save(flush: true)
        }

        when:
        SessionUtils.withNewTransaction {
            panCanStartJob.execute()
        }

        then:
        SessionUtils.withNewTransaction {
            assert Process.list().size() == 0
            assert RoddyBamFile.findAll().size() == 1
            checkFirstBamFileState(firstBamFile, true, [
                    seqTracks         : seqTracks,
                    containedSeqTracks: seqTracks,
            ])
            assertBamFileFileSystemPropertiesSet(firstBamFile)
            return true
        }
    }

    void "test alignLanesOnly, no baseBam exists, one lane, all fine"() {
        given:
        SessionUtils.withNewTransaction {
            createSeqTrack("readGroup1")
        }

        when:
        execute()

        then:
        verify_AlignLanesOnly_AllFine()
    }

    void "testAlignLanesOnly_NoBaseBamExist_OneLane_bwa_mem_0_7_8_sambamba_0_5_9_allFine"() {
        given:
        SessionUtils.withNewTransaction {
            createSeqTrack("readGroup1")
            MergingWorkPackage mergingWorkPackage = exactlyOneElement(MergingWorkPackage.findAll())
            createProjectConfig(mergingWorkPackage, [
                    bwaMemVersion  : "0.7.8",
                    sambambaVersion: "0.5.9",
                    configVersion  : "v2_0",
            ])
        }

        when:
        execute()

        then:
        verify_AlignLanesOnly_AllFine()
    }

    void "test alignLanesOnly, no baseBam exists, one lane, fastTrack, all fine"() {
        given:
        SessionUtils.withNewTransaction {
            fastTrackSetup()
        }

        when:
        execute()

        then:
        verify_AlignLanesOnly_AllFine()
    }

    void "test alignLanesOnly, no baseBam exists, one lane, with fingerPrinting, all fine"() {
        given:
        SessionUtils.withNewTransaction {
            createSeqTrack("readGroup1")
            setUpFingerPrintingFile()
        }

        when:
        execute()

        then:
        verify_AlignLanesOnly_AllFine()
    }

    void "test alignLanesOnly, no baseBam exists, two lane, all fine"() {
        given:
        SeqTrack firstSeqTrack
        SeqTrack secondSeqTrack

        SessionUtils.withNewTransaction {
            firstSeqTrack = createSeqTrack("readGroup1")
            secondSeqTrack = createSeqTrack("readGroup2")
        }

        when:
        execute()

        then:
        check_alignLanesOnly_NoBaseBamExist_TwoLanes(firstSeqTrack, secondSeqTrack)
    }

    void "test, alignBaseBam and new lanes, all fine"() {
        given:
        SessionUtils.withNewTransaction {
            createFirstRoddyBamFile(useOldStructure)
            createSeqTrack("readGroup2")
        }

        when:
        execute()

        then:
        checkAllAfterSuccessfulExecution_alignBaseBamAndNewLanes()

        where:
        useOldStructure | _
        true            | _
        false           | _
    }

    void "test align with withdrawn base, all fine"() {
        given:
        RoddyBamFile roddyBamFile
        SessionUtils.withNewTransaction {
            roddyBamFile = createFirstRoddyBamFile()
            roddyBamFile.withdrawn = true
            roddyBamFile.save(flush: true)
            roddyBamFile.mergingWorkPackage.needsProcessing = true
            roddyBamFile.mergingWorkPackage.save(flush: true)
        }

        when:
        execute()

        then:
        SessionUtils.withNewTransaction {
            assert !roddyBamFile.workDirectory.exists()
            checkWorkPackageState()

            List<RoddyBamFile> bamFiles = RoddyBamFile.findAll().sort { it.id }
            assert bamFiles.size() == 2
            assert roddyBamFile == bamFiles.first()
            assert !bamFiles[1].baseBamFile
            checkFirstBamFileState(bamFiles[1], true, [identifier: 1])
            assertBamFileFileSystemPropertiesSet(bamFiles[1])
            checkFileSystemState(bamFiles[1])
            return true
        }
    }
}
