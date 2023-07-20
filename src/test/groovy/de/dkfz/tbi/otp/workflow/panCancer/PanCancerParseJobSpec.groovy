/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.workflow.panCancer

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPancanFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStateChangeService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

class PanCancerParseJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory, RoddyPancanFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowStep,
                Pipeline,
                LibraryPreparationKit,
                SampleType,
                Sample,
                ReferenceGenomeProjectSeqType,
                FileType,
                FastqImportInstance,
                MergingWorkPackage,
                RoddyWorkflowConfig,
                RoddyBamFile,
                FastqFile,
        ]
    }

    void "test execution"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        RoddyBamFile roddyBamFile = createRoddyBamFile(RoddyBamFile)

        PanCancerParseJob job = Spy(PanCancerParseJob) {
            1 * getRoddyBamFile(workflowStep) >> roddyBamFile
        }
        job.abstractQualityAssessmentService = Mock(AbstractQualityAssessmentService)
        job.qcTrafficLightService = Mock(QcTrafficLightService)
        job.workflowStateChangeService = Mock(WorkflowStateChangeService)

        when:
        job.execute(workflowStep)

        then:
        1 * job.abstractQualityAssessmentService.parseRoddySingleLaneQaStatistics(roddyBamFile)
        1 * job.abstractQualityAssessmentService.parseRoddyMergedBamQaStatistics(roddyBamFile)
        1 * job.abstractQualityAssessmentService.saveCoverageToRoddyBamFile(roddyBamFile)
        1 * job.qcTrafficLightService.setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling(roddyBamFile, _)
        1 * job.workflowStateChangeService.changeStateToSuccess(workflowStep)
    }
}
