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
package de.dkfz.tbi.otp.workflow.jobs

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.RawSequenceFile
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.shared.SkipWorkflowStepException
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep
import de.dkfz.tbi.otp.workflowExecution.WorkflowStepSkipMessage

/**
 * Skips the current workflow if any of the input raw sequence files was previously flagged as empty
 * by {@link de.dkfz.tbi.otp.workflow.datainstallation.CheckFastqFileEmptyJob}.
 *
 * Used in FastQC and alignment workflows to prevent processing of lanes with empty FASTQ files.
 */
@Component
@Slf4j
class SkipForEmptyRawSequenceFileJob extends AbstractConditionalSkipJob {

    static final String INPUT_FASTQ_ROLE = "FASTQ"

    @Autowired
    ConcreteArtefactService concreteArtefactService

    @Override
    void checkRequirements(WorkflowStep workflowStep) throws SkipWorkflowStepException {
        List<SeqTrack> seqTracks = concreteArtefactService.<SeqTrack> getInputArtefacts(workflowStep, INPUT_FASTQ_ROLE)
        List<RawSequenceFile> emptyFiles = seqTracks.collectMany { SeqTrack seqTrack ->
            seqTrack.sequenceFiles.findAll { RawSequenceFile rawSequenceFile ->
                rawSequenceFile.emptyFile
            } as List<RawSequenceFile>
        }

        if (emptyFiles) {
            throw new SkipWorkflowStepException(new WorkflowStepSkipMessage([
                    message : "Skipping workflow because the following raw sequence files are empty: " +
                            "${emptyFiles*.fileName.join(', ')}",
                    category: WorkflowStepSkipMessage.Category.EMPTY_FILE,
            ]))
        }
    }
}
