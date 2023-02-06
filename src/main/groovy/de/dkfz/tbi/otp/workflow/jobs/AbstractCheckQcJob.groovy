/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.workflow.jobs

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightNotificationService
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

/**
 * Check qc thresholds given for the workflow.
 */
abstract class AbstractCheckQcJob extends AbstractJob {

    @Autowired
    QcTrafficLightService qcTrafficLightService

    @Autowired
    QcTrafficLightNotificationService qcTrafficLightNotificationService

    @Override
    void execute(WorkflowStep workflowStep) throws Throwable {
        AbstractMergedBamFile bamFile = getAbstractMergedBamFile(workflowStep)
        qcTrafficLightService.setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling(bamFile, bamFile.overallQualityAssessment)
        if (bamFile.qcTrafficLightStatus == AbstractMergedBamFile.QcTrafficLightStatus.WARNING) {
            qcTrafficLightNotificationService.informResultsAreWarned(bamFile)
        }
        workflowStateChangeService.changeStateToSuccess(workflowStep)
    }

    @Override
    final JobStage getJobStage() {
        return JobStage.CHECK_QC
    }

    abstract protected AbstractMergedBamFile getAbstractMergedBamFile(WorkflowStep workflowStep)
}
