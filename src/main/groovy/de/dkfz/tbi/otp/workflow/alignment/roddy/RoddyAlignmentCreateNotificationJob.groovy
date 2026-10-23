/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.workflow.alignment.roddy

import com.fasterxml.jackson.core.JsonProcessingException
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.AlignmentInfo
import de.dkfz.tbi.otp.dataprocessing.RoddyAlignmentInfoService
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.utils.MessageSourceService
import de.dkfz.tbi.otp.workflow.alignment.AlignmentWorkflowShared
import de.dkfz.tbi.otp.workflow.jobs.AbstractCreateNotificationJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

/**
 * Creates the configuration part of the notification for the Roddy alignment workflows (PanCancer, WGBS and RNA).
 *
 * The used tools and parameters are derived from the combined config of the workflow run by the {@link RoddyAlignmentInfoService},
 * so the selection of the alignment and merging tool is the same as for every other alignment information shown by OTP.
 */
@Component
@Slf4j
class RoddyAlignmentCreateNotificationJob extends AbstractCreateNotificationJob implements AlignmentWorkflowShared<RoddyBamFile> {

    @Autowired
    MessageSourceService messageSourceService

    @Autowired
    RoddyAlignmentInfoService roddyAlignmentInfoService

    @Override
    String createNotificationText(WorkflowStep workflowStep) {
        WorkflowRun workflowRun = workflowStep.workflowRun
        if (!workflowRun.combinedConfig) {
            throw new IllegalArgumentException("CombinedConfig in workflowRun ${workflowRun} is null")
        }

        RoddyBamFile roddyBamFile = getBamFile(workflowStep)
        AlignmentInfo alignmentInfo
        try {
            alignmentInfo = roddyAlignmentInfoService.getAlignmentInfo(workflowRun, roddyBamFile)
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to parse combinedConfig in workflowRun ${workflowRun}. " +
                    "Please ensure it is a valid JSON string.", e)
        }

        Map<String, Object> alignmentSpecificAttributes = alignmentInfo.alignmentSpecificMessageAttributes
        return messageSourceService.createMessage("notification.template.alignment.processing", [
                seqType           : roddyBamFile.seqType.displayNameWithLibraryLayout,
                referenceGenome   : roddyBamFile.referenceGenome,
                alignmentProgram  : alignmentInfo.alignmentProgram,
                alignmentParameter: alignmentInfo.alignmentParameter,
        ]) + messageSourceService.createMessage(alignmentSpecificAttributes.code as String, alignmentSpecificAttributes.params as Map)
    }
}
