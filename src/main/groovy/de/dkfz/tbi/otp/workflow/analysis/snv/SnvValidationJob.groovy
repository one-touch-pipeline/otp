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

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyResult
import de.dkfz.tbi.otp.dataprocessing.snvcalling.RoddySnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvWorkFileService
import de.dkfz.tbi.otp.utils.ExecuteRoddyCommandService
import de.dkfz.tbi.otp.workflow.jobs.AbstractRoddyClusterValidationJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

@Component
@Slf4j
class SnvValidationJob extends AbstractRoddyClusterValidationJob implements SnvWorkflowShared {

    @Autowired
    SnvWorkFileService snvWorkFileService

    @Autowired
    ExecuteRoddyCommandService executeRoddyCommandService

    @Autowired
    SnvCallingService snvCallingService

    @Override
    protected List<Path> getExpectedFiles(WorkflowStep workflowStep) {
        RoddySnvCallingInstance instance = getSnvInstance(workflowStep)
        return [snvWorkFileService.getCombinedPlotPath(instance),
                snvWorkFileService.getSnvCallingResult(instance),
                snvWorkFileService.getSnvDeepAnnotationResult(instance),
                snvWorkFileService.getResultRequiredForRunYapsa(instance),
        ]
    }

    @Override
    protected List<Path> getExpectedDirectories(WorkflowStep workflowStep) {
        RoddySnvCallingInstance instance = getSnvInstance(workflowStep)

        return [snvWorkFileService.getWorkExecutionStoreDirectory(instance)] + snvWorkFileService.getWorkExecutionDirectories(instance)
    }

    @Override
    protected RoddyResult getRoddyResult(WorkflowStep workflowStep) {
        return getSnvInstance(workflowStep)
    }

    @Override
    protected void saveResult(WorkflowStep workflowStep) {
    }

    @Override
    protected void doFurtherValidation(WorkflowStep workflowStep) {
        RoddySnvCallingInstance instance = getSnvInstance(workflowStep)

        snvCallingService.validateInputBamFiles(instance)
    }
}
