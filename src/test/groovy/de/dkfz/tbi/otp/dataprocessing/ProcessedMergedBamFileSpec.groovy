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

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

class ProcessedMergedBamFileSpec extends Specification implements DataTest, DomainUnitTest<ProcessedMergedBamFile>, DomainFactoryCore {

    ProcessingPriority processingPriority
    SampleType sampleType
    Sample sample
    SeqType seqType
    Individual individual
    Project project
    ReferenceGenome referenceGenome
    MergingWorkPackage workPackage
    ProcessedMergedBamFile bamFile

    @Override
    Class<?>[] getDomainClassesToMock() {
        return [
                LibraryPreparationKit,
                MergingWorkPackage,
                MergingPass,
                MergingSet,
                SeqPlatform,
                SeqPlatformGroup,
                SeqCenter,
                SeqType,
                SeqTrack,
                SampleType,
                Pipeline,
                ProcessingPriority,
                Project,
                ProcessedMergedBamFile,
                Individual,
                Sample,
                SoftwareTool,
                ReferenceGenome,
                FastqImportInstance,
                FileType,
                DataFile,
                Run,
                Realm,
        ]
    }

    void setupTest() {
        processingPriority = createProcessingPriority()
        sampleType = DomainFactory.createSampleType()
        project = DomainFactory.createProject(processingPriority: processingPriority)
        individual = DomainFactory.createIndividual(project: project)
        sample = DomainFactory.createSample(sampleType: sampleType, individual: individual)
        referenceGenome = DomainFactory.createReferenceGenome()
        seqType = DomainFactory.createSeqType()
        workPackage = DomainFactory.createMergingWorkPackage(sample: sample, referenceGenome: referenceGenome, seqType: seqType, pipeline: DomainFactory.createDefaultOtpPipeline())
        MergingSet mergingSet = DomainFactory.createMergingSet(mergingWorkPackage: workPackage)
        MergingPass mergingPass = DomainFactory.createMergingPass(mergingSet: mergingSet)
        bamFile = DomainFactory.createProcessedMergedBamFileWithoutProcessedBamFile(mergingPass, [:], false)
    }

    void testGetMergingWorkPackage() {
        given:
        setupTest()

        expect:
        workPackage == bamFile.mergingWorkPackage
    }

    void testGetProject() {
        given:
        setupTest()

        expect:
        project == bamFile.project
    }

    void testGetProcessingPriority() {
        given:
        setupTest()

        expect:
        processingPriority == bamFile.processingPriority
    }

    void testGetIndividual() {
        given:
        setupTest()

        expect:
        individual == bamFile.individual
    }

    void testGetSample() {
        given:
        setupTest()

        expect:
        sample == bamFile.sample
    }

    void testGetSampleType() {
        given:
        setupTest()

        expect:
        sampleType == bamFile.sampleType
    }

    void testGetSeqType() {
        given:
        setupTest()

        expect:
        seqType == bamFile.seqType
    }

    void testGetReferenceGenome() {
        given:
        setupTest()

        expect:
        referenceGenome == bamFile.referenceGenome
    }
}
