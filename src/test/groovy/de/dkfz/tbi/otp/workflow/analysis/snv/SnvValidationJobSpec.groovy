/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.workflow.analysis.snv

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.AnalysisProcessingStates
import de.dkfz.tbi.otp.dataprocessing.ExternalMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedBamFile
import de.dkfz.tbi.otp.dataprocessing.MergingCriteria
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.ProcessingThresholds
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyResult
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.RoddySnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvWorkFileService
import de.dkfz.tbi.otp.domainFactory.pipelines.analysis.SnvDomainFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.FastqFile
import de.dkfz.tbi.otp.ngsdata.FastqImportInstance
import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.ngsdata.LibraryPreparationKit
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeProjectSeqType
import de.dkfz.tbi.otp.ngsdata.Run
import de.dkfz.tbi.otp.ngsdata.Sample
import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.ngsdata.SampleTypePerProject
import de.dkfz.tbi.otp.ngsdata.SeqCenter
import de.dkfz.tbi.otp.ngsdata.SeqPlatform
import de.dkfz.tbi.otp.ngsdata.SeqPlatformGroup
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.analysis.AbstractAnalysisWorkflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

class SnvValidationJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {
    @Override
    Class[] getDomainClassesToMock() {
        return [
                AbstractBamFile,
                FastqFile,
                Individual,
                LibraryPreparationKit,
                MergingCriteria,
                MergingWorkPackage,
                Pipeline,
                Project,
                Sample,
                SamplePair,
                SampleType,
                SampleTypePerProject,
                SeqCenter,
                SeqPlatform,
                SeqPlatformGroup,
                SeqTrack,
                SeqType,
                ReferenceGenome,
                ReferenceGenomeEntry,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
                RoddySnvCallingInstance,
                RoddyWorkflowConfig,
                Run,
                FastqImportInstance,
                ExternalMergingWorkPackage,
                ExternallyProcessedBamFile,
                ProcessingThresholds,
        ]
    }

    @TempDir
    Path tempDir

    SnvValidationJob job
    WorkflowStep workflowStep
    RoddySnvCallingInstance instance

    String analysisOutput = AbstractAnalysisWorkflow.ANALYSIS_OUTPUT

    void setup() {
        workflowStep = createWorkflowStep([workflowRun: createWorkflowRun([workflow: findOrCreateWorkflow(SnvWorkflow.WORKFLOW)])])
        job = new SnvValidationJob([
                snvWorkFileService        : Mock(SnvWorkFileService),
                concreteArtefactService   : Mock(ConcreteArtefactService),
                snvCallingService         : Mock(SnvCallingService),
        ])
        instance = SnvDomainFactory.INSTANCE.createInstance(SnvDomainFactory.INSTANCE.createSamplePairWithExternallyProcessedBamFiles(), [
                processingState: AnalysisProcessingStates.IN_PROGRESS,
        ])
    }

    void "getRoddyResult, should return snv instance"() {
        when:
        RoddyResult result = job.getRoddyResult(workflowStep)

        then:
        result == instance
        1 * job.concreteArtefactService.getOutputArtefact(workflowStep, analysisOutput) >> instance
    }

    void "getExpectedFiles, should get all files"() {
        given:
        Path combinedPlot = tempDir.resolve('combinedPlot')
        Path snvCallingResult = tempDir.resolve('snvCallingResult')
        Path snvDeepAnnotationResult = tempDir.resolve('snvDeepAnnotationResult')
        Path resultRequiredForRunYapsa = tempDir.resolve('resultRequiredForRunYapsa')

        when:
        List<Path> resultPaths = job.getExpectedFiles(workflowStep)

        then:
        TestCase.assertContainSame(resultPaths, [combinedPlot, snvCallingResult, snvDeepAnnotationResult, resultRequiredForRunYapsa])

        and:
        1 * job.concreteArtefactService.getOutputArtefact(workflowStep, analysisOutput) >> instance
        1 * job.snvWorkFileService.getCombinedPlotPath(instance) >> combinedPlot
        1 * job.snvWorkFileService.getSnvCallingResult(instance) >> snvCallingResult
        1 * job.snvWorkFileService.getSnvDeepAnnotationResult(instance) >> snvDeepAnnotationResult
        1 * job.snvWorkFileService.getResultRequiredForRunYapsa(instance) >> resultRequiredForRunYapsa
    }

    void "getExpectedDirectories, should return expected directories"() {
        given:
        Path storeDirectory = tempDir.resolve('storeDirectory')
        Path dir1 = tempDir.resolve('dir1')
        Path dir2 = tempDir.resolve('dir2')

        when:
        List<Path> resultPaths = job.getExpectedDirectories(workflowStep)

        then:
        TestCase.assertContainSame(resultPaths, [dir1, dir2, storeDirectory])

        and:
        1 * job.concreteArtefactService.getOutputArtefact(workflowStep, analysisOutput) >> instance
        1 * job.snvWorkFileService.getWorkExecutionStoreDirectory(instance) >> storeDirectory
        1 * job.snvWorkFileService.getWorkExecutionDirectories(instance) >> [dir1, dir2]
    }

    void "saveResult, should do nothing"() {
        when:
        job.saveResult(workflowStep)

        then:
        0 * _
    }

    void "doFurtherValidation, should validate input bam"() {
        when:
        job.doFurtherValidation(workflowStep)

        then:
        1 * job.concreteArtefactService.getOutputArtefact(workflowStep, analysisOutput) >> instance
        1 * job.snvCallingService.validateInputBamFiles(instance)
    }
}
