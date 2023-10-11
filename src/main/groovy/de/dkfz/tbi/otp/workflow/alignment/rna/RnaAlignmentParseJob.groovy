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
package de.dkfz.tbi.otp.workflow.alignment.rna

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightService
import de.dkfz.tbi.otp.workflow.jobs.AbstractJob
import de.dkfz.tbi.otp.workflow.jobs.JobStage
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

@Component
@Slf4j
class RnaAlignmentParseJob extends AbstractJob implements RnaAlignmentShared {

    @Autowired
    AbstractQualityAssessmentService abstractQualityAssessmentService

    @Autowired
    QcTrafficLightService qcTrafficLightService

    @Override
    void execute(WorkflowStep workflowStep) throws Throwable {
        final RoddyBamFile bamFile = getRoddyBamFile(workflowStep)

        RnaQualityAssessment rnaQa = abstractQualityAssessmentService.parseRnaRoddyBamFileQaStatistics(bamFile)
        bamFile.qualityAssessmentStatus = AbstractBamFile.QaProcessingStatus.FINISHED
        qcTrafficLightService.setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling(bamFile, rnaQa)
        bamFile.save(flush: true)

        workflowStateChangeService.changeStateToSuccess(workflowStep)
    }

    @Override
    JobStage getJobStage() {
        return JobStage.PARSE
    }
}
