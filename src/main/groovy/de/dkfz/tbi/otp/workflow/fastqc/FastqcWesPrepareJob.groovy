/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.workflow.fastqc

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.FastqcDataFilesService
import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.tracking.NotificationCreator
import de.dkfz.tbi.otp.tracking.Ticket
import de.dkfz.tbi.otp.utils.LinkEntry
import de.dkfz.tbi.otp.workflow.jobs.AbstractPrepareJob
import de.dkfz.tbi.otp.workflowExecution.FastqcReportService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

@Component
@Slf4j
class FastqcWesPrepareJob extends AbstractPrepareJob implements FastqcShared {

    @Autowired
    NotificationCreator notificationCreator

    @Autowired
    FastqcDataFilesService fastqcDataFilesService

    @Autowired
    FastqcReportService fastqcReportService

    @Override
    protected boolean shouldWorkDirectoryBeProtected() {
        return true
    }

    @Override
    protected void doFurtherPreparation(WorkflowStep workflowStep) {
        SeqTrack seqTrack = getSeqTrack(workflowStep)
        notificationCreator.setStartedForSeqTracks([seqTrack], Ticket.ProcessingStep.FASTQC)
        seqTrack.fastqcState = SeqTrack.DataProcessingState.IN_PROGRESS
        seqTrack.save(flush: true)

        List<FastqcProcessedFile> fastqcProcessedFiles = getFastqcProcessedFiles(workflowStep)
        if (fastqcReportService.canFastqcReportsBeCopied(fastqcProcessedFiles)) {
            logService.addSimpleLogEntry(workflowStep, "fastqc reports found, mark object to copy them")
            fastqcProcessedFiles.each { FastqcProcessedFile fastqcProcessedFile ->
                fastqcProcessedFile.fileCopied = true
                fastqcProcessedFile.pathInWorkFolder = fastqcDataFilesService.fastqcFileName(fastqcProcessedFile)
                fastqcProcessedFile.save(flush: true)
            }
        } else {
            fastqcProcessedFiles.each { FastqcProcessedFile fastqcProcessedFile ->
                String name = fastqcDataFilesService.fastqcFileName(fastqcProcessedFile)
                fastqcProcessedFile.pathInWorkFolder = "${fastqcProcessedFile.sequenceFile.fileName}_reports/${name}"
                fastqcProcessedFile.fileCopied = false
                fastqcProcessedFile.save(flush: true)
            }
        }
    }

    @Override
    protected Path buildWorkDirectoryPath(WorkflowStep workflowStep) {
        return null
    }

    @Override
    protected Collection<LinkEntry> generateMapForLinking(WorkflowStep workflowStep) {
        return []
    }
}
