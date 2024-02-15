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
package de.dkfz.tbi.otp.workflow.datainstallation

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.RawSequenceFile
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.LinkEntry
import de.dkfz.tbi.otp.workflow.jobs.AbstractLinkJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

@Component
@Slf4j
class DataInstallationPidLinkJob extends AbstractLinkJob implements DataInstallationShared {

    @Override
    protected List<LinkEntry> getLinkMap(WorkflowStep workflowStep) {
        SeqTrack seqTrack = getSeqTrack(workflowStep)

        return seqTrack.sequenceFiles.collect { RawSequenceFile rawSequenceFile ->
            Path target = rawSequenceDataWorkFileService.getFilePath(rawSequenceFile)
            Path link = rawSequenceDataViewFileService.getFilePath(rawSequenceFile)
            return new LinkEntry(target: target, link: link)
        }
    }

    @Override
    protected void doFurtherWork(WorkflowStep workflowStep) { }

    @Override
    protected void saveResult(WorkflowStep workflowStep) {
        SeqTrack seqTrack = getSeqTrack(workflowStep)
        seqTrack.sequenceFiles.each { RawSequenceFile rawSequenceFile ->
            rawSequenceFile.fileLinked = true
            rawSequenceFile.dateLastChecked = new Date()
            rawSequenceFile.save(flush: true)
        }
        seqTrack.dataInstallationState = SeqTrack.DataProcessingState.FINISHED
        seqTrack.fastqcState = SeqTrack.DataProcessingState.NOT_STARTED
        assert seqTrack.save(flush: true)
    }
}
