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
package de.dkfz.tbi.otp.workflow.fastqc

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.FastqcDataFilesService
import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile
import de.dkfz.tbi.otp.domainFactory.FastqcDomainFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.NotificationCreator
import de.dkfz.tbi.otp.tracking.Ticket
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflowExecution.*

class FastqcWesPrepareJobSpec extends Specification implements DataTest, FastqcDomainFactory, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                FastqcProcessedFile,
                RawSequenceFile,
                WorkflowStep,
                WorkflowRun,
        ]
    }

    @Unroll
    void "doFurtherPreparation: when canBeCopied is #canBeCopied, then do expected checks and adapt fastqcProcessedFiles correctly"() {
        given:
        final WorkflowRun run = createWorkflowRun([
                workflow:
                        createWorkflow([
                                name: BashFastQcWorkflow.WORKFLOW
                        ])
        ])
        WorkflowStep workflowStep = createWorkflowStep([workflowRun: run])
        SeqTrack seqTrack = createSeqTrackWithTwoFastqFile()
        FastqcProcessedFile fastqcProcessedFile = createFastqcProcessedFile([
                sequenceFile: createFastqFile([
                        seqTrack: seqTrack,
                        fileName: 'fastqFile.R1.fastq.gz',
                ]),
        ])

        FastqcWesPrepareJob job = new FastqcWesPrepareJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            1 * getInputArtefact(workflowStep, BashFastQcWorkflow.INPUT_FASTQ) >> seqTrack
            1 * getOutputArtefacts(workflowStep, BashFastQcWorkflow.OUTPUT_FASTQC) >> [fastqcProcessedFile]
            0 * _
        }
        job.fastqcReportService = Mock(FastqcReportService) {
            1 * canFastqcReportsBeCopied([fastqcProcessedFile]) >> canBeCopied
            0 * _
        }
        job.fastqcDataFilesService = Mock(FastqcDataFilesService) {
            1 * fastqcFileName(fastqcProcessedFile) >> 'name.zip'
            0 * _
        }
        job.notificationCreator = Mock(NotificationCreator)
        job.logService = Mock(LogService)

        when:
        job.doFurtherPreparation(workflowStep)

        then:
        1 * job.notificationCreator.setStartedForSeqTracks([seqTrack], Ticket.ProcessingStep.FASTQC)
        seqTrack.fastqcState == SeqTrack.DataProcessingState.IN_PROGRESS
        fastqcProcessedFile.fileCopied == canBeCopied
        fastqcProcessedFile.pathInWorkFolder == pathInWorkFolder

        where:
        canBeCopied || pathInWorkFolder
        true        || 'name.zip'
        false       || 'fastqFile.R1.fastq.gz_reports/name.zip'
    }

    void "buildWorkDirectoryPath, should return null"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        FastqcWesPrepareJob job = new FastqcWesPrepareJob()

        expect:
        job.buildWorkDirectoryPath(workflowStep) == null
    }

    void "test generateMapForLinking, should return empty list"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        FastqcWesPrepareJob job = new FastqcWesPrepareJob()

        expect:
        job.generateMapForLinking(workflowStep) == []
    }
}
