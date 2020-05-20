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
import org.junit.*

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

@Mock([
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
])
class RoddyBamFile_PropertiesUnitTest implements DomainFactoryCore {

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

    @Before
    void setUp() {
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

    @After
    void tearDown() {
        sample = null
        sampleType = null
        seqType = null
        individual = null
        project = null
        referenceGenome = null
        workPackage = null
        bamFile = null
        pipeline = null
    }

    @Test
    void testGetMergingWorkPackage() {
        assert workPackage == bamFile.mergingWorkPackage
    }

    @Test
    void testGetProject() {
        assert project == bamFile.project
    }

    @Test
    void testGetProcessingPriority() {
        assert processingPriority == bamFile.processingPriority
    }

    @Test
    void testGetIndividual() {
        assert individual == bamFile.individual
    }

    @Test
    void testGetSample() {
        assert sample == bamFile.sample
    }

    @Test
    void testGetSampleType() {
        assert sampleType == bamFile.sampleType
    }

    @Test
    void testGetSeqType() {
        assert seqType == bamFile.seqType
    }

    @Test
    void testGetReferenceGenome() {
        assert referenceGenome == bamFile.referenceGenome
    }
}
