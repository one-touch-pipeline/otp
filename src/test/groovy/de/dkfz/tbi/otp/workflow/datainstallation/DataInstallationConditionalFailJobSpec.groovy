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
package de.dkfz.tbi.otp.workflow.datainstallation

import grails.testing.gorm.DataTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflow.shared.WorkflowException
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

class DataInstallationConditionalFailJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    @Rule
    TemporaryFolder temporaryFolder

    @Override
    Class[] getDomainClassesToMock() {
        return [
                DataFile,
                Sample,
                SampleType,
                WorkflowStep,
        ]
    }

    void "test check, succeeds"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        SeqTrack seqTrack = createSeqTrackWithTwoDataFile()

        Path path = temporaryFolder.newFile().toPath()
        path.text = "non-empty"

        DataInstallationConditionalFailJob job = Spy(DataInstallationConditionalFailJob) {
            getSeqTrack(workflowStep) >> seqTrack
        }
        job.fileSystemService = new TestFileSystemService()
        job.lsdfFilesService = Mock(LsdfFilesService) {
            getFileInitialPathAsPath(_) >> path
        }

        when:
        job.check(workflowStep)

        then:
        notThrown(WorkflowException)
    }

    void "test check, fails because seqTrack has no dataFiles"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        SeqTrack seqTrack = createSeqTrack()

        DataInstallationConditionalFailJob job = Spy(DataInstallationConditionalFailJob) {
            getSeqTrack(workflowStep) >> seqTrack
        }

        when:
        job.check(workflowStep)

        then:
        WorkflowException e = thrown(WorkflowException)
        e.message.contains("has no dataFiles")
    }

    void "test check, fails because physical files are missing"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        SeqTrack seqTrack = createSeqTrackWithTwoDataFile()

        DataInstallationConditionalFailJob job = Spy(DataInstallationConditionalFailJob) {
            getSeqTrack(workflowStep) >> seqTrack
        }
        job.fileSystemService = new TestFileSystemService()
        job.lsdfFilesService = Mock(LsdfFilesService) {
            getFileInitialPathAsPath(_) >> TestCase.uniqueNonExistentPath.toPath()
            0 * _
        }

        when:
        job.check(workflowStep)

        then:
        WorkflowException e = thrown(WorkflowException)
        e.message.contains("files are missing")
    }
}
