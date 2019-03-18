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

import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestFor
import org.junit.Before
import org.junit.Test

import de.dkfz.tbi.otp.ngsdata.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

@Build([
    DataFile,
    FileType,
    ProcessedBamFile,
    MergingCriteria,
    MergingSetAssignment,
    MergingWorkPackage,
    ProcessedMergedBamFile,
])
@TestFor(AbstractBamFileService)
class AbstractBamFileServiceUnitTests {

    AbstractBamFileService abstractBamFileService

    Date createdBefore = new Date().plus(1)



    @Before
    void setUp() {
        abstractBamFileService = new AbstractBamFileService()
    }



    private ProcessedBamFile createTestDataForHasBeenQualityAssessedAndMerged(Map processedBamFileMap = [:], Map mergingSetMap = [:], Map mergingSetAssignmentMap = [:], Map processedMergedBamFileMap = [:]) {
        ProcessedBamFile processedBamFile = ProcessedBamFile.build([
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
            status: AbstractBamFile.State.PROCESSED,
        ] + processedBamFileMap)
        MergingSet mergingSet = MergingSet.build([
            mergingWorkPackage: processedBamFile.mergingWorkPackage,
            status: MergingSet.State.PROCESSED,
        ] + mergingSetMap)
        MergingSetAssignment.build([
            bamFile: processedBamFile,
            mergingSet: mergingSet,
        ] + mergingSetAssignmentMap)
        ProcessedMergedBamFile.build([
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
            mergingPass: DomainFactory.createMergingPass([
                mergingSet: mergingSet,
            ]),
            workPackage: mergingSet.mergingWorkPackage,
        ] + processedMergedBamFileMap)
        return processedBamFile
    }


    @Test
    void testHasBeenQualityAssessedAndMerged() {
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged()

        assert abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }

    @Test
    void testHasBeenQualityAssessedAndMerged_QAIsNotFinished() {
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged([
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.IN_PROGRESS,
        ])

        assert !abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }

    @Test
    void testHasBeenQualityAssessedAndMerged_StatusIsNotProcessed() {
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged([
            status: AbstractBamFile.State.INPROGRESS,
        ])

        assert !abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }

    @Test
    void testHasBeenQualityAssessedAndMerged_BamFileIsNotInMergingSet() {
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged()
        MergingSetAssignment msa = exactlyOneElement(MergingSetAssignment.findAllByBamFile(processedBamFile))
        msa.bamFile = DomainFactory.createProcessedBamFile(msa.mergingSet.mergingWorkPackage)
        assert msa.save(failOnError: true)

        assert !abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }

    @Test
    void testHasBeenQualityAssessedAndMerged_MergingSetIsNorProcessed() {
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged([:], [
            status: MergingSet.State.INPROGRESS,
        ])

        assert !abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }

    @Test
    void testHasBeenQualityAssessedAndMerged_ProcessedMergedBamFileHasQANotFinished() {
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged([:], [:], [:], [
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.IN_PROGRESS,
        ])

        assert !abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }

    @Test
    void testHasBeenQualityAssessedAndMerged_ProcessedMergedBamFileDateIsLater() {
        createdBefore = new Date().minus(1)
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged()

        assert !abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }

    @Test
    void testHasBeenQualityAssessedAndMerged_ProcessedMergedBamFileIsWithdrawn() {
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged([:], [:], [:], [
            withdrawn: true,
        ])

        assert !abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }

    @Test
    void testHasBeenQualityAssessedAndMerged_ProcessedMergedBamFileAndBamFileAreWithdrawn() {
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged([
            withdrawn: true], [:], [:], [withdrawn: true])

        assert abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }

    @Test
    void testHasBeenQualityAssessedAndMerged_ProcessedMergedBamFileOtherMergingSet() {
        MergingPass mergingPass = MergingPass.build()
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged([:], [:], [:], [
            mergingPass: mergingPass,
            workPackage: mergingPass.mergingWorkPackage,
        ])

        assert !abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }
}
