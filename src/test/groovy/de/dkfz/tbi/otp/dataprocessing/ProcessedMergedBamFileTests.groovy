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

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.junit.*

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

@TestFor(ProcessedMergedBamFile)
@Mock([
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
])
class ProcessedMergedBamFileTests {

    MergingPass mergingPass = null
    MergingSet mergingSet = null
    MergingWorkPackage workPackage = null

    @Before
    void setUp() {
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

    @After
    void tearDown() {
        mergingPass = null
        mergingSet = null
        workPackage = null
    }

    @Test
    void testSave() {
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile(mergingPass)
        assert  bamFile.validate()
        bamFile.save(flush: true)
    }

    @Test
    void testSaveWithNumberOfLanes() {
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile(mergingPass, [
                numberOfMergedLanes: 3,
        ])
        assert  bamFile.validate()
        bamFile.save(flush: true)
    }

    @Test
    void testConstraints() {
        // mergingPass must not be null
        ProcessedMergedBamFile bamFile = new ProcessedMergedBamFile(
                type: AbstractBamFile.BamType.SORTED)
        assert  !bamFile.validate()
    }

    @Test
    void testMergingWorkPackageConstraint_NoWorkpackage_ShouldFail() {
        ProcessedMergedBamFile processedMergedBamFile = DomainFactory.createProcessedMergedBamFileWithoutProcessedBamFile(mergingPass, [
                type       : AbstractBamFile.BamType.MDUP,
                workPackage: null,
        ], false)
        TestCase.assertValidateError(processedMergedBamFile, "workPackage", "nullable", null)
    }

    @Test
    void testIsMostRecentBamFile() {
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile(mergingPass)
        bamFile.save(flush: true)

        assert bamFile.mostRecentBamFile

        MergingPass secondMergingPass = new MergingPass(
                identifier: 2,
                mergingSet: mergingSet)
        secondMergingPass.save(flush: true)

        ProcessedMergedBamFile secondBamFile = DomainFactory.createProcessedMergedBamFile(secondMergingPass)
        secondBamFile.save(flush: true)

        assert !bamFile.mostRecentBamFile
        assert secondBamFile.mostRecentBamFile

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

        assert !secondBamFile.mostRecentBamFile
        assert firstBamFileOfSecondMergingSet.mostRecentBamFile
    }

    @Test
    void testGetBamFileName() {
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile(mergingPass)

        assert "${bamFile.sampleType.name}_${bamFile.individual.pid}_${bamFile.seqType.name}_${bamFile.seqType.libraryLayout}_merged.mdup.bam" == bamFile.bamFileName
    }

    @Test
    void testFileNameNoSuffix() {
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile(mergingPass)
        assert "${bamFile.sampleType.name}_${bamFile.individual.pid}_${bamFile.seqType.name}_${bamFile.seqType.libraryLayout}_merged.mdup" == bamFile.fileNameNoSuffix()
    }
}
