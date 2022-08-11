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

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.tracking.NotificationCreator
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path
import java.nio.file.Paths

class FastqcPrepareJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                DataFile,
                WorkflowStep,
                WorkflowRun,
        ]
    }

    void "test doFurtherPreparation"() {
        given:
        final WorkflowRun run = createWorkflowRun([workflow:
                                                           createWorkflow([
                                                                   name: BashFastQcWorkflow.WORKFLOW
                                                           ])
        ])
        WorkflowStep workflowStep = createWorkflowStep([workflowRun: run])
        SeqTrack seqTrack = createSeqTrackWithTwoDataFile()

        FastqcPrepareJob job = new FastqcPrepareJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            1 * getInputArtefact(workflowStep, BashFastQcWorkflow.INPUT_FASTQ) >> seqTrack
        }
        job.notificationCreator = Mock(NotificationCreator)

        when:
        job.doFurtherPreparation(workflowStep)

        then:
        1 * job.notificationCreator.setStartedForSeqTracks([seqTrack], OtrsTicket.ProcessingStep.FASTQC)
        seqTrack.fastqcState == SeqTrack.DataProcessingState.IN_PROGRESS
    }

    void "test buildWorkDirectoryPath"() {
        given:
        String workDirectory = '/workDirectory'
        Path workDirectoryPath = Paths.get(workDirectory)
        final WorkflowRun run = createWorkflowRun([
                workDirectory: workDirectory,
                workflow     : createWorkflow([
                        name: BashFastQcWorkflow.WORKFLOW,
                ]),
        ])
        WorkflowStep workflowStep = createWorkflowStep([workflowRun: run])

        FastqcPrepareJob job = new FastqcPrepareJob()
        job.fileSystemService = new TestFileSystemService()

        expect:
        workDirectoryPath == job.buildWorkDirectoryPath(workflowStep)
    }
}
