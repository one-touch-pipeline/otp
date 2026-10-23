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
package de.dkfz.tbi.otp.workflow.alignment.cellRanger

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.AlignmentInfo
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerAlignmentInfoService
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.utils.MessageSourceService
import de.dkfz.tbi.otp.workflow.jobs.AbstractCreateNotificationJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

/**
 * Creates the configuration part of the notification for the CellRanger workflow.
 *
 * The used program and its version are provided by the {@link CellRangerAlignmentInfoService}.
 */
@Component
@Slf4j
class CellRangerCreateNotificationJob extends AbstractCreateNotificationJob implements CellRangerShared {

    private final MessageSourceService messageSourceService
    private final CellRangerAlignmentInfoService cellRangerAlignmentInfoService

    CellRangerCreateNotificationJob(MessageSourceService messageSourceService, CellRangerAlignmentInfoService cellRangerAlignmentInfoService) {
        this.messageSourceService = messageSourceService
        this.cellRangerAlignmentInfoService = cellRangerAlignmentInfoService
    }

    @Override
    String createNotificationText(WorkflowStep workflowStep) {
        SingleCellBamFile bamFile = getBamFile(workflowStep)
        AlignmentInfo alignmentInfo = cellRangerAlignmentInfoService.getAlignmentInfo(workflowStep.workflowRun)

        Map<String, Object> alignmentSpecificAttributes = alignmentInfo.alignmentSpecificMessageAttributes
        return messageSourceService.createMessage("notification.template.alignment.processing", [
                seqType           : bamFile.seqType.displayNameWithLibraryLayout,
                referenceGenome   : bamFile.referenceGenome,
                alignmentProgram  : alignmentInfo.alignmentProgram,
                alignmentParameter: alignmentInfo.alignmentParameter,
        ]) + messageSourceService.createMessage(alignmentSpecificAttributes.code as String, alignmentSpecificAttributes.params as Map)
    }
}
