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
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.CreateLinkOption
import de.dkfz.tbi.otp.infrastructure.FileService
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
    private DataFile dataFile1
    private DataFile dataFile2
    private Path source1
    private Path source2
    private Path target1
    private Path target2

    static final String WORKFLOW = DataInstallationWorkflow.WORKFLOW
    static final String OUTPUT_ROLE = DataInstallationWorkflow.OUTPUT_FASTQ

    @Override
    Class[] getDomainClassesToMock() {
        return [
                DataFile,
                WorkflowArtefact,
                WorkflowStep,
        ]
    }

    private void createData(boolean linkedExternally) {
        fileSystem = FileSystems.default
        run = createWorkflowRun([
                workflow: createWorkflow([
                        name: WORKFLOW,
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
                linkedExternally: linkedExternally,
        ])
        dataFile1 = createDataFile([
                seqTrack: seqTrack,
        ])
        dataFile2 = createDataFile([
                seqTrack: seqTrack,
        ])
        source1 = TestCase.uniqueNonExistentPath.toPath()
        source2 = TestCase.uniqueNonExistentPath.toPath()
        target1 = TestCase.uniqueNonExistentPath.toPath()
        target2 = TestCase.uniqueNonExistentPath.toPath()

        job = new CopyOrLinkFastqsOfLaneJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(step, OUTPUT_ROLE, WORKFLOW) >> seqTrack
            0 * _
        }
        job.fileSystemService = new TestFileSystemService()
        job.lsdfFilesService = Mock(LsdfFilesService) {
            1 * getFileInitialPathAsPath(dataFile1) >> source1
            1 * getFileInitialPathAsPath(dataFile2) >> source2
            1 * getFileFinalPathAsPath(dataFile1) >> target1
            1 * getFileFinalPathAsPath(dataFile2) >> target2
            0 * _
        }
    }

    void "createScripts, when SeqTrack should be linked, then create links and return empty list"() {
        given:
        createData(true)

        job.fileService = Mock(FileService) {
            1 * createLink(target1, source1, _, CreateLinkOption.DELETE_EXISTING_FILE)
            1 * createLink(target2, source2, _, CreateLinkOption.DELETE_EXISTING_FILE)
            0 * _
        }
        job.logService = Mock(LogService)

        when:
        List<String> scripts = job.createScripts(step)

        then:
        scripts == []
    }

    void "createScripts, when SeqTrack should be copied, then do not create links and return scripts"() {
        given:
        createData(false)

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
