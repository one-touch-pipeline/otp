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

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingInstance
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingService
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelWorkFileService
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyResult
import de.dkfz.tbi.otp.utils.ExecuteRoddyCommandService
import de.dkfz.tbi.otp.workflow.jobs.AbstractRoddyClusterValidationJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

@Component
@Slf4j
class IndelValidationJob extends AbstractRoddyClusterValidationJob implements IndelWorkflowShared {

    @Autowired
    IndelWorkFileService indelWorkFileService

    @Autowired
    ExecuteRoddyCommandService executeRoddyCommandService

    @Autowired
    IndelCallingService indelCallingService

    @Override
    protected List<Path> getExpectedFiles(WorkflowStep workflowStep) {
        IndelCallingInstance instance = getIndelInstance(workflowStep)
        return [indelWorkFileService.getCombinedPlotPath(instance),
                indelWorkFileService.getIndelQcJsonFile(instance),
                indelWorkFileService.getSampleSwapJsonFile(instance)] + indelWorkFileService.getResultFilePathsToValidate(instance)
    }

    @Override
    protected List<Path> getExpectedDirectories(WorkflowStep workflowStep) {
        IndelCallingInstance instance = getIndelInstance(workflowStep)

        return [indelWorkFileService.getWorkExecutionStoreDirectory(instance)] + indelWorkFileService.getWorkExecutionDirectories(instance)
    }

    @Override
    protected RoddyResult getRoddyResult(WorkflowStep workflowStep) {
        return getIndelInstance(workflowStep)
    }

    @Override
    protected void saveResult(WorkflowStep workflowStep) {
    }

    @Override
    protected void doFurtherValidation(WorkflowStep workflowStep) {
        IndelCallingInstance instance = getIndelInstance(workflowStep)

        indelCallingService.validateInputBamFiles(instance)
    }
}
