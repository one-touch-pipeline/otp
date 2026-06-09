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

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.shared.SkipWorkflowStepException
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep
import de.dkfz.tbi.otp.workflowExecution.WorkflowStepSkipMessage

class SkipForEmptyRawSequenceFileJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                FastqImportInstance,
                RawSequenceFile,
                Sample,
                SampleType,
                WorkflowStep,
        ]
    }

    SkipForEmptyRawSequenceFileJob job
    WorkflowStep workflowStep

    void setup() {
        workflowStep = createWorkflowStep()
        job = new SkipForEmptyRawSequenceFileJob()
    }

    void "checkRequirements should not throw when no raw sequence files are empty"() {
        given:
        SeqTrack seqTrack = createSeqTrackWithTwoFastqFile()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            1 * getInputArtefacts(workflowStep, SkipForEmptyRawSequenceFileJob.INPUT_FASTQ_ROLE) >> [seqTrack]
        }

        when:
        job.checkRequirements(workflowStep)

        then:
        notThrown(SkipWorkflowStepException)
    }

    void "checkRequirements should throw skip exception when at least one raw sequence file is empty"() {
        given:
        SeqTrack seqTrack = createSeqTrackWithOneFastqFile()
        RawSequenceFile emptyFile = seqTrack.sequenceFiles.first()
        emptyFile.emptyFile = true
        emptyFile.save(flush: true)

        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            1 * getInputArtefacts(workflowStep, SkipForEmptyRawSequenceFileJob.INPUT_FASTQ_ROLE) >> [seqTrack]
        }

        when:
        job.checkRequirements(workflowStep)

        then:
        SkipWorkflowStepException e = thrown(SkipWorkflowStepException)
        e.skipMessage.category == WorkflowStepSkipMessage.Category.EMPTY_FILE
        e.message.contains(emptyFile.fileName)
    }

    void "checkRequirements should throw skip exception when empty files exist across multiple seq tracks"() {
        given:
        SeqTrack seqTrack1 = createSeqTrackWithOneFastqFile()
        SeqTrack seqTrack2 = createSeqTrackWithOneFastqFile()
        RawSequenceFile emptyFile = seqTrack2.sequenceFiles.first()
        emptyFile.emptyFile = true
        emptyFile.save(flush: true)

        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            1 * getInputArtefacts(workflowStep, SkipForEmptyRawSequenceFileJob.INPUT_FASTQ_ROLE) >> [seqTrack1, seqTrack2]
        }

        when:
        job.checkRequirements(workflowStep)

        then:
        SkipWorkflowStepException e = thrown(SkipWorkflowStepException)
        e.skipMessage.category == WorkflowStepSkipMessage.Category.EMPTY_FILE
        e.message.contains(emptyFile.fileName)
    }
}
