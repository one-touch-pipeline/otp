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

import de.dkfz.tbi.otp.dataprocessing.ExternalMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedBamFile
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.ProcessingThresholds
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.RoddySnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvWorkFileService
import de.dkfz.tbi.otp.domainFactory.pipelines.analysis.SnvDomainFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.FastqFile
import de.dkfz.tbi.otp.ngsdata.FastqImportInstance
import de.dkfz.tbi.otp.ngsdata.FileType
import de.dkfz.tbi.otp.ngsdata.LibraryPreparationKit
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeProjectSeqType
import de.dkfz.tbi.otp.ngsdata.Sample
import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.ngsdata.SampleTypePerProject
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path
import java.nio.file.Paths

class SnvPrepareJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

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
                RoddySnvCallingInstance,
                SampleTypePerProject,
                RoddyBamFile,
                ProcessingThresholds,
                ExternalMergingWorkPackage,
                ExternallyProcessedBamFile,
        ]
    }

    void "buildWorkDirectoryPath, should return work directory"() {
        given:
        SnvDomainFactory snvDomainFactory = SnvDomainFactory.INSTANCE

        Path workDirectoryPath = Paths.get('/path')
        RoddySnvCallingInstance snvCallingInstance = snvDomainFactory.createInstance(snvDomainFactory.createSamplePairWithExternallyProcessedBamFiles())
        WorkflowStep workflowStep = createWorkflowStep([workflowRun: createWorkflowRun([workflow: findOrCreateWorkflow(SnvWorkflow.WORKFLOW)])])
        SnvPrepareJob job = new SnvPrepareJob([
                snvWorkFileService     : Mock(SnvWorkFileService),
                concreteArtefactService: Mock(ConcreteArtefactService),
        ])

        when:
        Path resultPath = job.buildWorkDirectoryPath(workflowStep)

        then:
        resultPath == workDirectoryPath
        1 * job.snvWorkFileService.getDirectoryPath(snvCallingInstance) >> workDirectoryPath
        1 * job.concreteArtefactService.getOutputArtefact(workflowStep, _) >> snvCallingInstance
    }

    void "generateMapForLinking, should return empty list"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        SnvPrepareJob job = new SnvPrepareJob()

        expect:
        job.generateMapForLinking(workflowStep) == []
    }

    void "doFurtherPreparation, should do nothing"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        SnvPrepareJob job = new SnvPrepareJob()

        when:
        job.doFurtherPreparation(workflowStep)

        then:
        0 * _
    }
}
