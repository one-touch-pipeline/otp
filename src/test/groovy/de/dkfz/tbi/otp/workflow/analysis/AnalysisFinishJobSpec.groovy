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
package de.dkfz.tbi.otp.workflow.analysis

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingInstance
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.RoddySnvCallingInstance
import de.dkfz.tbi.otp.domainFactory.pipelines.analysis.IndelDomainFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.FastqFile
import de.dkfz.tbi.otp.ngsdata.FastqImportInstance
import de.dkfz.tbi.otp.ngsdata.SampleTypePerProject
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.analysis.snv.SnvWorkflow
import de.dkfz.tbi.otp.workflowExecution.*

class AnalysisFinishJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                MergingWorkPackage,
                FastqImportInstance,
                FastqFile,
                RoddySnvCallingInstance,
                SampleTypePerProject,
                RoddyWorkflowConfig,
                RoddyBamFile,
                IndelCallingInstance,
        ]
    }

    WorkflowStep workflowStep
    BamFilePairAnalysis analysis
    AnalysisFinishJob job

    void "updateDomains should update analysis"() {
        given:
        workflowStep = createWorkflowStep([workflowRun: createWorkflowRun([
                workflow: findOrCreateWorkflow(SnvWorkflow.WORKFLOW, [beanName: SnvWorkflow.simpleName.uncapitalize()]),
        ])])
        analysis = IndelDomainFactory.INSTANCE.createInstanceWithRoddyBamFiles()
        job = new AnalysisFinishJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            1 * getOutputArtefact(workflowStep, AbstractAnalysisWorkflow.ANALYSIS_OUTPUT) >> analysis
            0 * _
        }

        when:
        job.updateDomains(workflowStep)

        then:
        analysis.processingState == AnalysisProcessingStates.FINISHED
    }
}
