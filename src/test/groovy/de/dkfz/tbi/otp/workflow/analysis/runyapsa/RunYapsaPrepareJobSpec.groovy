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
package de.dkfz.tbi.otp.workflow.analysis.runyapsa

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaWorkFileService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.domainFactory.pipelines.analysis.RunYapsaDomainFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path
import java.nio.file.Paths

class RunYapsaPrepareJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowStep,
                WorkflowRun,
                Pipeline,
                LibraryPreparationKit,
                SampleType,
                Sample,
                MergingWorkPackage,
                ReferenceGenomeProjectSeqType,
                FileType,
                FastqImportInstance,
                FastqFile,
                RoddyWorkflowConfig,
                SampleTypePerProject,
                RoddyBamFile,
                ProcessingThresholds,
                ExternalMergingWorkPackage,
                ExternallyProcessedBamFile,
                SamplePair,
                RunYapsaInstance,
        ]
    }

    void "buildWorkDirectoryPath, should return work directory"() {
        given:
        RunYapsaDomainFactory runYapsaDomainFactory = RunYapsaDomainFactory.INSTANCE

        Path workDirectoryPath = Paths.get('/path')
        RunYapsaInstance runYapsaCallingInstance = runYapsaDomainFactory.createInstance(runYapsaDomainFactory.createSamplePairWithExternallyProcessedBamFiles())
        WorkflowStep workflowStep = createWorkflowStep([workflowRun: createWorkflowRun([workflow: findOrCreateWorkflow(RunYapsaWorkflow.WORKFLOW)])])
        RunYapsaPrepareJob job = new RunYapsaPrepareJob([
                runYapsaWorkFileService: Mock(RunYapsaWorkFileService),
                concreteArtefactService: Mock(ConcreteArtefactService),
        ])

        when:
        Path resultPath = job.buildWorkDirectoryPath(workflowStep)

        then:
        resultPath == workDirectoryPath
        1 * job.runYapsaWorkFileService.getDirectoryPath(runYapsaCallingInstance) >> workDirectoryPath
        1 * job.concreteArtefactService.getOutputArtefact(workflowStep, _) >> runYapsaCallingInstance
    }

    void "generateMapForLinking, should return empty list"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        RunYapsaPrepareJob job = new RunYapsaPrepareJob()

        expect:
        job.generateMapForLinking(workflowStep) == []
    }

    void "doFurtherPreparation, should do nothing"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        RunYapsaPrepareJob job = new RunYapsaPrepareJob()

        when:
        job.doFurtherPreparation(workflowStep)

        then:
        0 * _
    }
}
