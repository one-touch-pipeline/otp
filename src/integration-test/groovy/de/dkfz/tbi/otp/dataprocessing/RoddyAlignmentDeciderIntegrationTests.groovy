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
package de.dkfz.tbi.otp.dataprocessing

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.FileOperationStatus
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.HelperUtils

@Rollback
@Integration
class RoddyAlignmentDeciderIntegrationTests {
    @Autowired
    PanCanAlignmentDecider decider

    @Test
    void testGetPipelineNotWGBS() {
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        Pipeline pipeline = decider.getPipeline(seqTrack)
        assert pipeline.name == Pipeline.Name.PANCAN_ALIGNMENT
        assert pipeline.type == Pipeline.Type.ALIGNMENT
    }

    @Test
    void testGetPipelineWGBS() {
        SeqTrack seqTrack = DomainFactory.createSeqTrack([seqType: DomainFactory.createWholeGenomeBisulfiteSeqType()])
        Pipeline pipeline = decider.getPipeline(seqTrack)
        assert pipeline.name == Pipeline.Name.PANCAN_ALIGNMENT
        assert pipeline.type == Pipeline.Type.ALIGNMENT
    }

    @Test
    void testGetPipelineRNA() {
        SeqTrack seqTrack = DomainFactory.createSeqTrack([seqType: DomainFactory.createRnaPairedSeqType()])
        Pipeline pipeline = decider.getPipeline(seqTrack)
        assert pipeline.name == Pipeline.Name.RODDY_RNA_ALIGNMENT
        assert pipeline.type == Pipeline.Type.ALIGNMENT
    }

    private createAndRunPrepare(boolean bamFileContainsSeqTrack, boolean withdrawn, FileOperationStatus fileOperationStatus, boolean forceAlign) {
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile([
                withdrawn: withdrawn,
                md5sum: fileOperationStatus == FileOperationStatus.PROCESSED ? HelperUtils.randomMd5sum : null,
                fileOperationStatus: fileOperationStatus,
                fileSize: fileOperationStatus == FileOperationStatus.PROCESSED ? 10000 : -1,
        ])

        SeqTrack seqTrack = bamFileContainsSeqTrack ?
                bamFile.seqTracks.iterator().next() :
                DomainFactory.createSeqTrackWithDataFiles(bamFile.workPackage)

        assert bamFile.workPackage.satisfiesCriteria(seqTrack)
        assert !bamFile.workPackage.needsProcessing

        decider.prepareForAlignment(bamFile.workPackage, seqTrack, forceAlign)

        return bamFile.workPackage
    }

    /**
    Expected behaviour of RoddyAlignmentDecider.prepareForAlignment():
    properties checked for the latest bam file |  result
    contains given  withdrawn   fileOperationStatus        |   action
     seq track                                             |
    ---------------------------------------------------------------------
    true            true        DECLARED/NEEDS_PROCESSING      look at previous bam file*
    true            true        INPROGRESS/PROCESSED           needs processing
    true            false       DECLARED/NEEDS_PROCESSING      no op
    true            false       INPROGRESS/PROCESSED           no op
    false           true        DECLARED/NEEDS_PROCESSING      look at previous bam file*
    false           true        INPRORGESS/PROCESSED           needs processing
    false           false       DECLARED/NEEDS_PROCESSING      needs processing
    false           false       INPROGRESS/PROCESSED           needs processing
         non-existent bam file                                 needs processing
    (*if a bam file matches this criteria, the properties of the previous bam file should be used to make the decision)

    */

    @Test
    void testPrepareForAlignment_bamFileContainsSeqTrackWithdrawnTrueFileOperationStatusDeclared_shouldSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(true, true, FileOperationStatus.DECLARED, false)
        assert workPackage.needsProcessing
    }

    @Test
    void testPrepareForAlignment_bamFileContainsSeqTrackWithdrawnTrueFileOperationStatusNeedsProcessing_shouldSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(true, true, FileOperationStatus.NEEDS_PROCESSING, false)
        assert workPackage.needsProcessing
    }

    @Test
    void testPrepareForAlignment_bamFileContainsSeqTrackWithdrawnTrueFileOperationStatusInProgress_shouldSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(true, true, FileOperationStatus.INPROGRESS, false)
        assert workPackage.needsProcessing
    }

    @Test
    void testPrepareForAlignment_bamFileContainsSeqTrackWithdrawnTrueFileOperationStatusProcessed_shouldSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(true, true, FileOperationStatus.PROCESSED, false)
        assert workPackage.needsProcessing
    }

    @Test
    void testPrepareForAlignment_bamFileContainsSeqTrackWithdrawnFalseFileOperationStatusDeclared_shouldNotSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(true, false, FileOperationStatus.DECLARED, false)
        assert !workPackage.needsProcessing
    }

    @Test
    void testPrepareForAlignment_bamFileContainsSeqTrackWithdrawnFalseFileOperationStatusNeedsProcessing_shouldNotSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(true, false, FileOperationStatus.NEEDS_PROCESSING, false)
        assert !workPackage.needsProcessing
    }

    @Test
    void testPrepareForAlignment_bamFileContainsSeqTrackWithdrawnFalseFileOperationStatusInProgress_shouldNotSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(true, false, FileOperationStatus.INPROGRESS, false)
        assert !workPackage.needsProcessing
    }

    @Test
    void testPrepareForAlignment_bamFileContainsSeqTrackWithdrawnFalseFileOperationStatusProcessed_shouldNotSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(true, false, FileOperationStatus.PROCESSED, false)
        assert !workPackage.needsProcessing
    }

    @Test
    void testPrepareForAlignment_bamFileDoesntContainSeqTrackWithdrawnTrueFileOperationStatusDeclared_shouldSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(false, true, FileOperationStatus.DECLARED, false)
        assert workPackage.needsProcessing
    }

    @Test
    void testPrepareForAlignment_bamFileDoesntContainSeqTrackWithdrawnTrueFileOperationStatusNeedsProcessing_shouldSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(false, true, FileOperationStatus.NEEDS_PROCESSING, false)
        assert workPackage.needsProcessing
    }

    @Test
    void testPrepareForAlignment_bamFileDoesntContainSeqTrackWithdrawnTrueFileOperationStatusInProgress_shouldSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(false, true, FileOperationStatus.INPROGRESS, false)
        assert workPackage.needsProcessing
    }

    @Test
    void testPrepareForAlignment_bamFileDoesntContainSeqTrackWithdrawnTrueFileOperationStatusProcessed_shouldSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(false, true, FileOperationStatus.PROCESSED, false)
        assert workPackage.needsProcessing
    }

    @Test
    void testPrepareForAlignment_bamFileDoesntContainSeqTrackWithdrawnFalseFileOperationStatusDeclared_shouldSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(false, false, FileOperationStatus.DECLARED, false)
        assert workPackage.needsProcessing
    }

    @Test
    void testPrepareForAlignment_bamFileDoesntContainSeqTrackWithdrawnFalseFileOperationStatusNeedsProcessing_shouldSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(false, false, FileOperationStatus.NEEDS_PROCESSING, false)
        assert workPackage.needsProcessing
    }

    @Test
    void testPrepareForAlignment_bamFileDoesntContainSeqTrackWithdrawnFalseFileOperationStatusInProgress_shouldSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(false, false, FileOperationStatus.INPROGRESS, false)
        assert workPackage.needsProcessing
    }

    @Test
    void testPrepareForAlignment_bamFileDoesntContainSeqTrackWithdrawnFalseFileOperationStatusProcessed_shouldSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(false, false, FileOperationStatus.PROCESSED, false)
        assert workPackage.needsProcessing
    }

    @Test
    void testPrepareForAlignment_noBamFileFound_shouldSetNeedsProcessing() {
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        DomainFactory.createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)

        Pipeline pipeline = decider.getPipeline(seqTrack)

        MergingWorkPackage workPackage = DomainFactory.createMergingWorkPackage(
                MergingWorkPackage.getMergingProperties(seqTrack) +
                [
                pipeline: pipeline,
                statSizeFileName: pipeline.name == Pipeline.Name.PANCAN_ALIGNMENT ? DomainFactory.DEFAULT_TAB_FILE_NAME : null,
                ]
        )
        workPackage.save(flush: true)

        decider.prepareForAlignment(workPackage, seqTrack, false)

        assert workPackage.needsProcessing
    }

    void prepareGetLatestExistingValidBamFile_latestShouldBeFound(boolean withdrawn, boolean md5sumNotNull) {
        RoddyBamFile bamFile1 = DomainFactory.createRoddyBamFile(
                withdrawn: false,
        )
        DomainFactory.createRoddyBamFile(
                withdrawn: false,
                identifier: bamFile1.identifier + 1,
                workPackage: bamFile1.workPackage,
                seqTracks: [bamFile1.seqTracks.iterator().next()] as Set<SeqTrack>,
                config: bamFile1.config,
        )
        RoddyBamFile bamFile3 = DomainFactory.createRoddyBamFile([
                withdrawn: withdrawn,
                md5sum: md5sumNotNull ? HelperUtils.randomMd5sum : null,
                identifier: bamFile1.identifier + 2,
                workPackage: bamFile1.workPackage,
                seqTracks: [bamFile1.seqTracks.iterator().next()] as Set<SeqTrack>,
                fileOperationStatus: md5sumNotNull ? FileOperationStatus.PROCESSED : FileOperationStatus.DECLARED,
                fileSize: md5sumNotNull ? 10000 : -1,
                config: bamFile1.config,
                ]
        )

        def bamFileResult = decider.getLatestBamFileWhichHasBeenOrCouldBeCopied(bamFile1.workPackage)

        assert bamFileResult.id == bamFile3.id
    }

    @Test
    void testGetLatestExistingValidBamFile_latestShouldBeFound() {
        prepareGetLatestExistingValidBamFile_latestShouldBeFound(true, true)
        prepareGetLatestExistingValidBamFile_latestShouldBeFound(false, false)
        prepareGetLatestExistingValidBamFile_latestShouldBeFound(false, true)
    }

    void prepareGetLatestExistingValidBamFile_secondShouldBeFound(boolean withdrawn, boolean md5sumNotNull) {
        RoddyBamFile bamFile1 = DomainFactory.createRoddyBamFile(
                withdrawn: false,
        )
        RoddyBamFile bamFile2 = DomainFactory.createRoddyBamFile([
                withdrawn: withdrawn,
                md5sum: md5sumNotNull ? HelperUtils.randomMd5sum : null,
                identifier: bamFile1.identifier + 1,
                workPackage: bamFile1.workPackage,
                seqTracks: [bamFile1.seqTracks.iterator().next()] as Set<SeqTrack>,
                fileOperationStatus: md5sumNotNull ? FileOperationStatus.PROCESSED : FileOperationStatus.DECLARED,
                fileSize: md5sumNotNull ? 10000 : -1,
                config: bamFile1.config,
                ]
        )
        DomainFactory.createRoddyBamFile(
                withdrawn: true,
                md5sum: null,
                fileOperationStatus: FileOperationStatus.DECLARED,
                fileSize: -1,
                identifier: bamFile1.identifier + 2,
                workPackage: bamFile1.workPackage,
                seqTracks: [bamFile1.seqTracks.iterator().next()] as Set<SeqTrack>,
                config: bamFile1.config,
        )

        def bamFileResult = decider.getLatestBamFileWhichHasBeenOrCouldBeCopied(bamFile1.workPackage)

        assert bamFileResult.id == bamFile2.id
    }

    @Test
    void testGetLatestExistingValidBamFile_secondShouldBeFound() {
        prepareGetLatestExistingValidBamFile_secondShouldBeFound(true, true)
        prepareGetLatestExistingValidBamFile_secondShouldBeFound(false, false)
        prepareGetLatestExistingValidBamFile_secondShouldBeFound(false, true)
    }

    @Test
    void prepareGetLatestExistingValidBamFile_nothingShouldBeFound() {
        RoddyBamFile bamFile1 = DomainFactory.createRoddyBamFile(
                withdrawn: true,
                md5sum: null,
                fileOperationStatus: FileOperationStatus.DECLARED,
                fileSize: -1,
        )
        DomainFactory.createRoddyBamFile(
                withdrawn: true,
                md5sum: null,
                fileOperationStatus: FileOperationStatus.DECLARED,
                fileSize: -1,
                identifier: bamFile1.identifier + 1,
                workPackage: bamFile1.workPackage,
                seqTracks: [bamFile1.seqTracks.iterator().next()] as Set<SeqTrack>,
                config: bamFile1.config,
        )
        DomainFactory.createRoddyBamFile(
                withdrawn: true,
                md5sum: null,
                fileOperationStatus: FileOperationStatus.DECLARED,
                fileSize: -1,
                identifier: bamFile1.identifier + 2,
                workPackage: bamFile1.workPackage,
                seqTracks: [bamFile1.seqTracks.iterator().next()] as Set<SeqTrack>,
                config: bamFile1.config,
        )

        def bamFileResult = decider.getLatestBamFileWhichHasBeenOrCouldBeCopied(bamFile1.workPackage)

        assert bamFileResult == null
    }

    @Test
    void testPrepareForAlignment_forceAlignment_shouldNotSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(true, false, FileOperationStatus.DECLARED, true)
        assert !workPackage.needsProcessing
    }

    @Test
    void test_ensureConfigurationIsComplete_whenConfigurationIsComplete_shouldReturnNormally() {
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        DomainFactory.createReferenceGenomeProjectSeqType(
                project: seqTrack.project,
                seqType: seqTrack.seqType,
        )
        DomainFactory.createRoddyWorkflowConfig(
                project: seqTrack.project,
                seqType: seqTrack.seqType,
                pipeline: decider.getPipeline(seqTrack),
        )

        decider.ensureConfigurationIsComplete(seqTrack)
    }

    @Test
    void test_ensureConfigurationIsComplete_whenReferenceGenomeIsMissing_shouldThrow() {
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        DomainFactory.createRoddyWorkflowConfig(
                project: seqTrack.project,
                seqType: seqTrack.seqType,
                pipeline: decider.getPipeline(seqTrack),
        )

        TestCase.shouldFailWithMessageContaining(RuntimeException, "Reference genome is not configured") {
            decider.ensureConfigurationIsComplete(seqTrack)
        }
    }

    @Test
    void testCanPipelineAlign_whenEverythingIsOkay_shouldReturnTrue() {
        DomainFactory.createRoddyAlignableSeqTypes()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()
        SeqTrack seqTrack = DomainFactory.createSeqTrack(seqType: seqType)
        DomainFactory.createRoddyWorkflowConfig(project: seqTrack.project, seqType:  seqType)

        assert decider.canPipelineAlign(seqTrack)
    }

    @Test
    void testCanPipelineAlign_whenWrongLibraryLayout_shouldReturnFalse() {
        DomainFactory.createRoddyAlignableSeqTypes()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType(SequencingReadType.MATE_PAIR)
        SeqTrack seqTrack = DomainFactory.createSeqTrack(seqType: seqType)
        DomainFactory.createRoddyWorkflowConfig(project: seqTrack.project, seqType:  seqType)

        assert !decider.canPipelineAlign(seqTrack)
        assert !seqTrack.logMessages
    }

    @Test
    void testCanPipelineAlign_whenWrongSeqType_shouldReturnFalse() {
        DomainFactory.createRoddyAlignableSeqTypes()
        SeqType seqType = DomainFactory.createSeqType(
                name: 'INVALID_NAME',
                libraryLayout: SequencingReadType.PAIRED
        )
        SeqTrack seqTrack = DomainFactory.createSeqTrack(seqType: seqType)
        DomainFactory.createRoddyWorkflowConfig(project: seqTrack.project, seqType:  seqType)

        assert !decider.canPipelineAlign(seqTrack)
        assert !seqTrack.logMessages
    }

    @Test
    void testCanPipelineAlign_whenConfigIsMissing_shouldReturnFalse() {
        DomainFactory.createRoddyAlignableSeqTypes()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()
        SeqTrack seqTrack = DomainFactory.createSeqTrack(seqType: seqType)

        assert !decider.canPipelineAlign(seqTrack)
        assert CollectionUtils.exactlyOneElement(seqTrack.logMessages).message == "RoddyWorkflowConfig is missing for ${seqTrack.project} ${seqTrack.seqType} ${Pipeline.Name.PANCAN_ALIGNMENT.name()}.".toString()
    }
}
