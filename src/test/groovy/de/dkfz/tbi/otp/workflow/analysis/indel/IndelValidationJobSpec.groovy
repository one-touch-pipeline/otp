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
package de.dkfz.tbi.otp.workflow.analysis.indel

import grails.testing.gorm.DataTest
import spock.lang.*

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingInstance
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingService
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelWorkFileService
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyResult
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.domainFactory.pipelines.analysis.IndelDomainFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.analysis.AbstractAnalysisWorkflow
import de.dkfz.tbi.otp.workflowExecution.*

import java.nio.file.Path

class IndelValidationJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {
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
                SampleType,
                SampleTypePerProject,
                SeqCenter,
                SamplePair,
                SeqPlatform,
                SeqPlatformGroup,
                SeqTrack,
                IndelCallingInstance,
                SeqType,
                ReferenceGenome,
                ReferenceGenomeEntry,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
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

    IndelValidationJob job
    WorkflowStep workflowStep
    IndelCallingInstance instance

    String analysisOutput = AbstractAnalysisWorkflow.ANALYSIS_OUTPUT

    void setup() {
        workflowStep = createWorkflowStep([workflowRun: createWorkflowRun([workflow: findOrCreateWorkflow(IndelWorkflow.WORKFLOW)])])
        job = new IndelValidationJob([
                indelWorkFileService      : Mock(IndelWorkFileService),
                indelCallingService       : Mock(IndelCallingService),
                concreteArtefactService   : Mock(ConcreteArtefactService),
        ])
        instance = IndelDomainFactory.INSTANCE.createInstance(IndelDomainFactory.INSTANCE.createSamplePairWithExternallyProcessedBamFiles(), [
                processingState: AnalysisProcessingStates.IN_PROGRESS,
        ])
    }

    void "getRoddyResult, should return indel instance"() {
        when:
        RoddyResult result = job.getRoddyResult(workflowStep)

        then:
        result == instance
        1 * job.concreteArtefactService.getOutputArtefact(workflowStep, analysisOutput) >> instance
    }

    void "getExpectedFiles, should get all files"() {
        given:
        Path combinedPlot = tempDir.resolve('combinedPlot')
        Path indelQcJson = tempDir.resolve('indelQcJson')
        Path sampleSwapJson = tempDir.resolve('sampleSwapJson')
        Path resultFile1 = tempDir.resolve('resultFile1')
        Path resultFile2 = tempDir.resolve('resultFile2')

        when:
        List<Path> resultPaths = job.getExpectedFiles(workflowStep)

        then:
        TestCase.assertContainSame(resultPaths, [combinedPlot, indelQcJson, sampleSwapJson, resultFile1, resultFile2])

        and:
        1 * job.concreteArtefactService.getOutputArtefact(workflowStep, analysisOutput) >> instance
        1 * job.indelWorkFileService.getCombinedPlotPath(instance) >> combinedPlot
        1 * job.indelWorkFileService.getIndelQcJsonFile(instance) >> indelQcJson
        1 * job.indelWorkFileService.getSampleSwapJsonFile(instance) >> sampleSwapJson
        1 * job.indelWorkFileService.getResultFilePathsToValidate(instance) >> [resultFile1, resultFile2]
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
        1 * job.indelWorkFileService.getWorkExecutionStoreDirectory(instance) >> storeDirectory
        1 * job.indelWorkFileService.getWorkExecutionDirectories(instance) >> [dir1, dir2]
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
        1 * job.indelCallingService.validateInputBamFiles(instance)
    }
}
