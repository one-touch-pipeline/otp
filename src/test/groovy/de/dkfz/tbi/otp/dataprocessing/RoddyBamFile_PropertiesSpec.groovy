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
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

class RoddyBamFile_PropertiesSpec extends Specification implements DataTest, DomainFactoryCore {

    ProcessingPriority processingPriority
    SampleType sampleType
    Sample sample
    SeqType seqType
    Individual individual
    Project project
    ReferenceGenome referenceGenome
    MergingWorkPackage workPackage
    Pipeline pipeline
    RoddyBamFile bamFile

    @Override
    Class<?>[] getDomainClassesToMock() {
        return [
                AbstractMergedBamFile,
                DataFile,
                FileType,
                Individual,
                LibraryPreparationKit,
                MergingCriteria,
                MergingWorkPackage,
                Pipeline,
                ProcessingPriority,
                Project,
                Realm,
                ReferenceGenome,
                RoddyBamFile,
                RoddyWorkflowConfig,
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
        processingPriority = createProcessingPriority()
        sampleType = DomainFactory.createSampleType()
        project = DomainFactory.createProject(processingPriority: processingPriority)
        individual = DomainFactory.createIndividual(project: project)
        sample = DomainFactory.createSample(sampleType: sampleType, individual: individual)
        referenceGenome = DomainFactory.createReferenceGenome()
        seqType = DomainFactory.createSeqType()
        pipeline = DomainFactory.createPanCanPipeline()
        workPackage = DomainFactory.createMergingWorkPackage(
                sample: sample,
                referenceGenome: referenceGenome,
                seqType: seqType,
                pipeline: pipeline,
        )
        bamFile = DomainFactory.createRoddyBamFile(workPackage: workPackage)
    }

    void testGetMergingWorkPackage() {
        expect:
        workPackage == bamFile.mergingWorkPackage
    }

    void testGetProject() {
        expect:
        project == bamFile.project
    }

    void testGetProcessingPriority() {
        expect:
        processingPriority == bamFile.processingPriority
    }

    void testGetIndividual() {
        expect:
        individual == bamFile.individual
    }

    void testGetSample() {
        expect:
        sample == bamFile.sample
    }

    void testGetSampleType() {
        expect:
        sampleType == bamFile.sampleType
    }

    void testGetSeqType() {
        expect:
        seqType == bamFile.seqType
    }

    void testGetReferenceGenome() {
        expect:
        referenceGenome == bamFile.referenceGenome
    }
}
