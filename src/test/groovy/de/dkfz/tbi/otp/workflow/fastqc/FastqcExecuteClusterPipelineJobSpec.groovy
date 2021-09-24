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
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.ProcessOutput
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflowExecution.*

import java.nio.file.*

class FastqcExecuteClusterPipelineJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    private FastqcExecuteClusterPipelineJob job
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
    private Path sourceDir
    private Path targetDir

    static final String WORKFLOW = FastqcWorkflow.WORKFLOW
    static final String INPUT_ROLE = FastqcWorkflow.INPUT_FASTQ

    private void createData() {
        fileSystem = FileSystems.default
        sourceDir = temporaryFolder.newFolder("src").toPath()
        targetDir = temporaryFolder.newFolder("tgt").toPath()

        Path tmp

        tmp = temporaryFolder.newFile().toPath()
        tmp.write("bla")
        source1 = sourceDir.resolve(tmp)
        Files.move(tmp, source1)

        tmp = temporaryFolder.newFile().toPath()
        tmp.write("bla")
        source2 = sourceDir.resolve(tmp)
        Files.move(tmp, source2)

        tmp = temporaryFolder.newFile().toPath()
        tmp.write("bla")
        target1 = targetDir.resolve(tmp)
        Files.move(tmp, target1)

        tmp = temporaryFolder.newFile().toPath()
        tmp.write("bla")
        target2 = targetDir.resolve(tmp)
        Files.move(tmp, target2)

        run = createWorkflowRun([
                workflow: createWorkflow([
                        name: WORKFLOW
                ]),
        ])
        step = createWorkflowStep([
                workflowRun: run,
        ])
        artefact = createWorkflowArtefact([
                producedBy  : run,
                outputRole  : INPUT_ROLE, //"FASTQ"
                artefactType: ArtefactType.FASTQ,
        ])
        seqTrack = createSeqTrack([
                workflowArtefact: artefact,
        ])
        dataFile1 = createDataFile([
                seqTrack: seqTrack,
        ])
        dataFile2 = createDataFile([
                seqTrack: seqTrack,
        ])

        job = new FastqcExecuteClusterPipelineJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getInputArtefact(step, INPUT_ROLE, WORKFLOW) >> seqTrack
            0 * _
        }
        job.fileSystemService = Mock(FileSystemService) {
            _ * getRemoteFileSystem(_) >> fileSystem
            0 * _
        }
    }

    @Override
    Class[] getDomainClassesToMock() {
        return [
                SeqTrack,
                DataFile,
                WorkflowArtefact,
                WorkflowStep,
        ]
    }

    @Rule
    TemporaryFolder temporaryFolder

    void "test, if fastqc reports can be copied. no scripts are created"() {
        given:
        createData()

        job.fastqcDataFilesService = Mock(FastqcDataFilesService) {
            1 * fastqcOutputDirectory(_) >> targetDir
            1 * pathToFastQcResultMd5SumFromSeqCenter(_, dataFile1) >> target1
            1 * pathToFastQcResultMd5SumFromSeqCenter(_, dataFile2) >> target2
            2 * fastqcOutputPath(dataFile1) >> target1
            2 * fastqcOutputPath(dataFile2) >> target2
            0 * _
        }
        job.seqTrackService = Mock(SeqTrackService) {
            1 * getSequenceFilesForSeqTrack(seqTrack) >> [dataFile1, dataFile2]
            0 * _
        }
        job.fileService = Mock(FileService) {
            2 * deleteDirectoryRecursively(_)
            _ * ensureFileIsReadableAndNotEmpty(_)
            _ * convertPermissionsToOctalString(_)
            0 * _
        }
        job.remoteShellHelper = Mock(RemoteShellHelper) {
            2 * executeCommandReturnProcessOutput(_, _) >> { Realm realm, String copyAndMd5sumCommand ->
                assert copyAndMd5sumCommand.contains("cd")
                assert copyAndMd5sumCommand.contains("md5sum")
                return new ProcessOutput("Ok", "", 0)
            }
            0 * _
        }
        job.logService = Mock(LogService) {
            3 * addSimpleLogEntry(_, _)
        }

        when:
        List<String> scripts = job.createScripts(step)

        then:
        2 * job.fastqcDataFilesService.pathToFastQcResultFromSeqCenter(fileSystem as FileSystem, dataFile1) >> target1
        2 * job.fastqcDataFilesService.pathToFastQcResultFromSeqCenter(fileSystem as FileSystem, dataFile2) >> target2

        scripts.size() == 0
    }

    void "test, if fastqc reports can not be copied. correct scripts are created for each data file"() {
        given:
        createData()

        final String cmd_module_loader = "cmd load module loader"
        final String cmd_activation_fastqc = "cmd activate fastqc"
        final String cmd_fastqc = "cmd fastqc"

        job.fastqcDataFilesService = Mock(FastqcDataFilesService) {
            1 * fastqcOutputDirectory(_) >> targetDir
            2 * fastqcOutputPath(dataFile1) >> target1
            2 * fastqcOutputPath(dataFile2) >> target2
            1 * fastqcHtmlPath(dataFile1) >> target1
            1 * fastqcHtmlPath(dataFile2) >> target2
            0 * _
        }
        job.seqTrackService = Mock(SeqTrackService) {
            1 * getSequenceFilesForSeqTrack(seqTrack) >> [dataFile1, dataFile2]
            0 * _
        }
        job.fileService = Mock(FileService) {
            2 * deleteDirectoryRecursively(_)
            _ * ensureFileIsReadableAndNotEmpty(_)
            _ * convertPermissionsToOctalString(_)
            0 * _
        }
        job.lsdfFilesService = Mock(LsdfFilesService) {
            1 * getFileFinalPath(dataFile1) >> target1.toString()
            1 * getFileFinalPath(dataFile2) >> target2.toString()
            0 * _
        }
        job.logService = Mock(LogService) {
            3 * addSimpleLogEntry(_, _)
        }
        job.processingOptionService = Mock(ProcessingOptionService) {
            _ * findOptionAsString(ProcessingOption.OptionName.COMMAND_LOAD_MODULE_LOADER) >> cmd_module_loader
            _ * findOptionAsString(ProcessingOption.OptionName.COMMAND_ACTIVATION_FASTQC) >> cmd_activation_fastqc
            _ * findOptionAsString(ProcessingOption.OptionName.COMMAND_FASTQC) >> cmd_fastqc
        }

        when:
        List<String> scripts = job.createScripts(step)

        then:
        1 * job.fastqcDataFilesService.pathToFastQcResultFromSeqCenter(fileSystem as FileSystem, dataFile1) >> TestCase.uniqueNonExistentPath.toPath()

        scripts.size() == 2
        scripts.each { String it ->
            assert it.contains(cmd_module_loader)
            assert it.contains(cmd_activation_fastqc)
            assert it.contains(cmd_fastqc)
        }
    }

    //false positives, since rule can not recognize calling class
    @SuppressWarnings('ExplicitFlushForDeleteRule')
    void "test, if no result files exist and fastqc reports can not be copied. correct scripts are created for each data file"() {
        given:
        createData()

        //remove the seqTrack result files, so that no deletion needed
        Files.delete(target1)
        Files.delete(target2)

        final String cmd_module_loader = "cmd load module loader"
        final String cmd_activation_fastqc = "cmd activate fastqc"
        final String cmd_fastqc = "cmd fastqc"

        job.fastqcDataFilesService = Mock(FastqcDataFilesService) {
            1 * fastqcOutputDirectory(_) >> targetDir
            2 * fastqcOutputPath(dataFile1) >> target1
            2 * fastqcOutputPath(dataFile2) >> target2
            1 * fastqcHtmlPath(dataFile1) >> target1
            1 * fastqcHtmlPath(dataFile2) >> target2
            0 * _
        }
        job.seqTrackService = Mock(SeqTrackService) {
            1 * getSequenceFilesForSeqTrack(seqTrack) >> [dataFile1, dataFile2]
            0 * _
        }
        job.fileService = Mock(FileService) {
            0 * deleteDirectoryRecursively(_)
            _ * ensureFileIsReadableAndNotEmpty(_)
            _ * convertPermissionsToOctalString(_)
            0 * _
        }
        job.lsdfFilesService = Mock(LsdfFilesService) {
            1 * getFileFinalPath(dataFile1) >> target1.toString()
            1 * getFileFinalPath(dataFile2) >> target2.toString()
            0 * _
        }
        job.logService = Mock(LogService) {
            1 * addSimpleLogEntry(_, "Creating cluster scripts")
        }
        job.processingOptionService = Mock(ProcessingOptionService) {
            _ * findOptionAsString(ProcessingOption.OptionName.COMMAND_LOAD_MODULE_LOADER) >> cmd_module_loader
            _ * findOptionAsString(ProcessingOption.OptionName.COMMAND_ACTIVATION_FASTQC) >> cmd_activation_fastqc
            _ * findOptionAsString(ProcessingOption.OptionName.COMMAND_FASTQC) >> cmd_fastqc
        }

        when:
        List<String> scripts = job.createScripts(step)

        then:
        // @see FastqcExecuteClusterPipelineJob.canFastQcReportsBeCopied
        // In every() loop the first returns false will end the loop
        1 * job.fastqcDataFilesService.pathToFastQcResultFromSeqCenter(fileSystem as FileSystem, dataFile1) >> TestCase.uniqueNonExistentPath.toPath()

        scripts.size() == 2
        scripts.each { String it ->
            assert it.contains(cmd_module_loader)
            assert it.contains(cmd_activation_fastqc)
            assert it.contains(cmd_fastqc)
        }
    }
}
