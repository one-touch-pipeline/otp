/*
 * Copyright 2011-2021 The OTP authors
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

import de.dkfz.tbi.otp.dataprocessing.FastqcDataFilesService
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsqc.FastqcUploadService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStateChangeService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

class FastqcParseJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                DataFile,
                Realm,
                SeqTrack,
                WorkflowStep,
        ]
    }

    void "test execute"() {
        given:
        SeqTrack seqTrack = createSeqTrackWithTwoDataFile()
        WorkflowStep workflowStep = createWorkflowStep()

        FastqcParseJob job = Spy(FastqcParseJob) {
            1 * getSeqTrack(workflowStep) >> seqTrack
        }
        job.fastqcDataFilesService = Mock(FastqcDataFilesService)
        job.fastqcUploadService = Mock(FastqcUploadService)
        job.workflowStateChangeService = Mock(WorkflowStateChangeService)

        job.seqTrackService = new SeqTrackService()
        job.seqTrackService.fileTypeService = new FileTypeService()

        when:
        job.execute(workflowStep)

        then:
        1 * job.fastqcDataFilesService.getAndUpdateFastqcProcessedFile(seqTrack.dataFiles.first(), _)
        1 * job.fastqcDataFilesService.getAndUpdateFastqcProcessedFile(seqTrack.dataFiles.last(), _)
        2 * job.fastqcUploadService.uploadFastQCFileContentsToDataBase(_)
        1 * job.workflowStateChangeService.changeStateToSuccess(workflowStep)
    }
}
