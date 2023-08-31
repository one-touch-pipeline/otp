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
package de.dkfz.tbi.otp.workflow.alignment

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.workflow.alignment.panCancer.PanCancerShared
import de.dkfz.tbi.otp.workflow.jobs.AbstractFragmentJob
import de.dkfz.tbi.otp.workflowExecution.SingleSelectSelectorExtendedCriteria
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

@Component
@Slf4j
class RoddyAlignmentFragmentJob extends AbstractFragmentJob implements PanCancerShared {

    @Override
    protected List<SingleSelectSelectorExtendedCriteria> fetchSelectors(WorkflowStep workflowStep) {
        RoddyBamFile bamFile = getRoddyBamFile(workflowStep)
        return bamFile.containedSeqTracks*.libraryPreparationKit.unique().sort {
            it?.name
        }.collect {
            new SingleSelectSelectorExtendedCriteria(
                    workflowStep.workflowRun.workflowVersion.workflow,
                    workflowStep.workflowRun.workflowVersion,
                    bamFile.project,
                    bamFile.seqType,
                    bamFile.referenceGenome,
                    it,
            )
        }
    }
}
