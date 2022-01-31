/*
 * Copyright 2011-2020 The OTP authors
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
import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

class ProcessedMergedBamFileUnitSpec extends Specification implements DataTest, DomainUnitTest<ProcessedMergedBamFile> {

    MergingPass mergingPass = null
    MergingSet mergingSet = null
    MergingWorkPackage workPackage = null

    @Override
    Class<?>[] getDomainClassesToMock() {
        return [
                AlignmentPass,
                DataFile,
                Individual,
                FileType,
                LibraryPreparationKit,
                MergingCriteria,
                MergingPass,
                MergingSet,
                MergingWorkPackage,
                MergingSetAssignment,
                Pipeline,
                ProcessingPriority,
                Project,
                ProcessedBamFile,
                ProcessedMergedBamFile,
                Realm,
                ReferenceGenome,
                Run,
                FastqImportInstance,
                Sample,
                SampleType,
                SeqCenter,
                SeqPlatform,
                SeqPlatformGroup,
                SeqPlatformModelLabel,
                SeqTrack,
                SeqType,
                SoftwareTool,
        ]
    }

    void setup() {
        this.workPackage = DomainFactory.createMergingWorkPackage(
                seqType: DomainFactory.createSeqType(),
                pipeline: DomainFactory.createDefaultOtpPipeline(),
        )
        this.workPackage.save(flush: true)

        this.mergingSet = new MergingSet(
                identifier: 1,
                mergingWorkPackage: workPackage)
        this.mergingSet.save(flush: true)

        this.mergingPass = new MergingPass(
                identifier: 1,
                mergingSet: mergingSet)
        this.mergingPass.save(flush: true)
    }

    void testSave() {
        given:
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile(mergingPass)

        expect:
        bamFile.validate()
        bamFile.save(flush: true)
    }

    void testSaveWithNumberOfLanes() {
        given:
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile(mergingPass, [
                numberOfMergedLanes: 3,
        ])

        expect:
        bamFile.validate()
        bamFile.save(flush: true)
    }

    void testConstraints() {
        given:
        // mergingPass must not be null
        ProcessedMergedBamFile bamFile = new ProcessedMergedBamFile(
                type: AbstractBamFile.BamType.SORTED)

        expect:
        !bamFile.validate()
    }

    void testMergingWorkPackageConstraint_NoWorkpackage_ShouldFail() {
        given:
        ProcessedMergedBamFile processedMergedBamFile = DomainFactory.createProcessedMergedBamFileWithoutProcessedBamFile(mergingPass, [
                type       : AbstractBamFile.BamType.MDUP,
                workPackage: null,
        ], false)

        expect:
        TestCase.assertValidateError(processedMergedBamFile, "workPackage", "nullable", null)
    }

    void testIsMostRecentBamFile() {
        given:
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile(mergingPass)
        bamFile.save(flush: true)

        expect:
        bamFile.mostRecentBamFile

        MergingPass secondMergingPass = new MergingPass(
                identifier: 2,
                mergingSet: mergingSet)
        secondMergingPass.save(flush: true)

        ProcessedMergedBamFile secondBamFile = DomainFactory.createProcessedMergedBamFile(secondMergingPass)
        secondBamFile.save(flush: true)

        !bamFile.mostRecentBamFile
        secondBamFile.mostRecentBamFile

        MergingSet secondMergingSet = new MergingSet(
                identifier: 2,
                mergingWorkPackage: workPackage)
        secondMergingSet.save(flush: true)

        MergingPass firstMergingPassOfSecondMergingSet = new MergingPass(
                identifier: 1,
                mergingSet: secondMergingSet)
        firstMergingPassOfSecondMergingSet.save(flush: true)

        ProcessedMergedBamFile firstBamFileOfSecondMergingSet = DomainFactory.createProcessedMergedBamFile(firstMergingPassOfSecondMergingSet)
        firstBamFileOfSecondMergingSet.save(flush: true)

        !secondBamFile.mostRecentBamFile
        firstBamFileOfSecondMergingSet.mostRecentBamFile
    }

    void testGetBamFileName() {
        given:
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile(mergingPass)

        expect:
        "${bamFile.sampleType.name}_${bamFile.individual.pid}_${bamFile.seqType.name}_${bamFile.seqType.libraryLayout}_merged.mdup.bam" == bamFile.bamFileName
    }

    void testFileNameNoSuffix() {
        given:
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile(mergingPass)

        expect:
        "${bamFile.sampleType.name}_${bamFile.individual.pid}_${bamFile.seqType.name}_${bamFile.seqType.libraryLayout}_merged.mdup" == bamFile.fileNameNoSuffix()
    }
}
