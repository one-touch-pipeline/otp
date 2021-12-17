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

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class AbstractBamFileServiceSpec extends Specification implements DataTest {

    AbstractBamFileService abstractBamFileService

    Date createdBefore = new Date() + 1

    @Override
    Class[] getDomainClassesToMock() {
        [
                DataFile,
                FileType,
                ProcessedBamFile,
                MergingCriteria,
                MergingSetAssignment,
                MergingWorkPackage,
                ProcessedMergedBamFile,
        ]
    }

    void setup() {
        abstractBamFileService = new AbstractBamFileService()
    }

    private ProcessedBamFile createTestDataForHasBeenQualityAssessedAndMerged(Map processedBamFileMap = [:], Map mergingSetMap = [:], Map mergingSetAssignmentMap = [:], Map processedMergedBamFileMap = [:]) {
        ProcessedBamFile processedBamFile = DomainFactory.createProcessedBamFile([
                qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
                status                 : AbstractBamFile.State.PROCESSED,
        ] + processedBamFileMap)
        MergingSet mergingSet = DomainFactory.createMergingSet([
                mergingWorkPackage: processedBamFile.mergingWorkPackage,
                status            : MergingSet.State.PROCESSED,
        ] + mergingSetMap)
        DomainFactory.createMergingSetAssignment([
                bamFile   : processedBamFile,
                mergingSet: mergingSet,
        ] + mergingSetAssignmentMap)
        DomainFactory.createProcessedMergedBamFile([
                qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
                mergingPass            : DomainFactory.createMergingPass([
                        mergingSet: mergingSet,
                ]),
                workPackage            : mergingSet.mergingWorkPackage,
        ] + processedMergedBamFileMap)
        return processedBamFile
    }

    void testHasBeenQualityAssessedAndMerged() {
        given:
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged()

        expect:
        abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }

    void testHasBeenQualityAssessedAndMerged_QAIsNotFinished() {
        given:
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged([
                qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.IN_PROGRESS,
        ])

        expect:
        !abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }

    void testHasBeenQualityAssessedAndMerged_StatusIsNotProcessed() {
        given:
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged([
                status: AbstractBamFile.State.INPROGRESS,
        ])

        expect:
        !abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }

    void testHasBeenQualityAssessedAndMerged_BamFileIsNotInMergingSet() {
        given:
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged()
        MergingSetAssignment msa = exactlyOneElement(MergingSetAssignment.findAllByBamFile(processedBamFile))
        msa.bamFile = DomainFactory.createProcessedBamFile(msa.mergingSet.mergingWorkPackage)
        assert msa.save(flush: true)

        expect:
        !abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }

    void testHasBeenQualityAssessedAndMerged_MergingSetIsNorProcessed() {
        given:
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged([:], [
                status: MergingSet.State.INPROGRESS,
        ])

        expect:
        !abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }

    void testHasBeenQualityAssessedAndMerged_ProcessedMergedBamFileHasQANotFinished() {
        given:
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged([:], [:], [:], [
                qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.IN_PROGRESS,
        ])

        expect:
        !abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }

    void testHasBeenQualityAssessedAndMerged_ProcessedMergedBamFileDateIsLater() {
        given:
        createdBefore = new Date() - 1
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged()

        expect:
        !abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }

    void testHasBeenQualityAssessedAndMerged_ProcessedMergedBamFileIsWithdrawn() {
        given:
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged([:], [:], [:], [
                withdrawn: true,
        ])

        expect:
        !abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }

    void testHasBeenQualityAssessedAndMerged_ProcessedMergedBamFileAndBamFileAreWithdrawn() {
        given:
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged([
                withdrawn: true], [:], [:], [withdrawn: true])

        expect:
        abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }

    void testHasBeenQualityAssessedAndMerged_ProcessedMergedBamFileOtherMergingSet() {
        given:
        MergingPass mergingPass = DomainFactory.createMergingPass()
        ProcessedBamFile processedBamFile = createTestDataForHasBeenQualityAssessedAndMerged([:], [:], [:], [
                mergingPass: mergingPass,
                workPackage: mergingPass.mergingWorkPackage,
        ])

        expect:
        !abstractBamFileService.hasBeenQualityAssessedAndMerged(processedBamFile, createdBefore)
    }
}
