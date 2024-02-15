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

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataWorkFileService
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflowExecution.*

import java.nio.file.*

class CopyOrLinkFastqsOfLaneJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    private CopyOrLinkFastqsOfLaneJob job
    private WorkflowRun run
    private WorkflowStep step
    private WorkflowArtefact artefact
    private SeqTrack seqTrack
    private FileSystem fileSystem = FileSystems.default
    private RawSequenceFile rawSequenceFile1
    private RawSequenceFile rawSequenceFile2
    private Path source1
    private Path source2
    private Path target1
    private Path target2

    static final String OUTPUT_ROLE = DataInstallationWorkflow.OUTPUT_FASTQ

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                RawSequenceFile,
                WorkflowArtefact,
                WorkflowStep,
        ]
    }

    private void createData() {
        fileSystem = FileSystems.default
        run = createWorkflowRun([
                workflow: createWorkflow([
                        name: DataInstallationWorkflow.WORKFLOW,
                ]),
        ])
        step = createWorkflowStep([
                workflowRun: run,
        ])
        artefact = createWorkflowArtefact([
                producedBy  : run,
                outputRole  : OUTPUT_ROLE,
                artefactType: ArtefactType.FASTQ,
        ])
        seqTrack = createSeqTrack([
                workflowArtefact: artefact,
                linkedExternally: false,
        ])
        rawSequenceFile1 = createFastqFile([
                seqTrack: seqTrack,
        ])
        rawSequenceFile2 = createFastqFile([
                seqTrack: seqTrack,
        ])
        source1 = TestCase.uniqueNonExistentPath.toPath()
        source2 = TestCase.uniqueNonExistentPath.toPath()
        target1 = TestCase.uniqueNonExistentPath.toPath()
        target2 = TestCase.uniqueNonExistentPath.toPath()

        job = new CopyOrLinkFastqsOfLaneJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(step, OUTPUT_ROLE) >> seqTrack
            0 * _
        }
        job.fileSystemService = new TestFileSystemService()
        job.lsdfFilesService = Mock(LsdfFilesService) {
            1 * getFileInitialPathAsPath(rawSequenceFile1) >> source1
            1 * getFileInitialPathAsPath(rawSequenceFile2) >> source2
            0 * _
        }
        job.rawSequenceDataWorkFileService = Mock(RawSequenceDataWorkFileService) {
            1 * getFilePath(rawSequenceFile1) >> target1
            1 * getFilePath(rawSequenceFile2) >> target2
            0 * _
        }
    }

    void "createScripts, when called, then return scripts"() {
        given:
        createData()

        job.fileService = Mock(FileService) {
            0 * _
        }
        job.checksumFileService = Mock(ChecksumFileService) {
            2 * md5FileName(_) >> TestCase.uniqueNonExistentPath.toPath()
        }

        when:
        List<String> scripts = job.createScripts(step)

        then:
        scripts.size() == 2

        scripts.each {
            assert it ==~ """
                |cd .*
                |if \\[ -e ".*" \\]; then
                |    echo "File .* already exists."
                |    rm .*\\*
                |fi
                |cp .* .*
                |md5sum .* > .*
                |chgrp -h ${seqTrack.project.unixGroup} .* .*
                |chmod 440 .* .*
                |""".stripMargin()
        }
    }
}
