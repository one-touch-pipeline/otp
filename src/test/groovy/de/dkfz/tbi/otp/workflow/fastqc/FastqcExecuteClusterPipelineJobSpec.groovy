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
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.FastqcDomainFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.ProcessOutput
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflowExecution.*

import java.nio.file.*

class FastqcExecuteClusterPipelineJobSpec extends Specification implements DataTest, FastqcDomainFactory, WorkflowSystemDomainFactory {

    static final String WORKFLOW = BashFastQcWorkflow.WORKFLOW
    static final String INPUT_ROLE = BashFastQcWorkflow.INPUT_FASTQ
    static final String OUTPUT_ROLE = BashFastQcWorkflow.OUTPUT_FASTQC

    private FastqcExecuteClusterPipelineJob job
    private WorkflowRun run
    private WorkflowStep step
    private WorkflowArtefact artefact
    private SeqTrack seqTrack
    private FileSystem fileSystem = FileSystems.default
    private DataFile dataFile1
    private DataFile dataFile2
    private FastqcProcessedFile fastqcProcessedFile1
    private FastqcProcessedFile fastqcProcessedFile2

    private Path sourceDir
    private Path targetDir
    private Path sourceFastqc1
    private Path sourceFastqc2
    private Path sourceFastqcMd5sum1
    private Path sourceFastqcMd5sum2
    private Path targetFastqc1
    private Path targetFastqc2
    private Path targetFastqcHtml1
    private Path targetFastqcHtml2

    @Rule
    TemporaryFolder temporaryFolder

    private void createData(boolean targetAlreadyExist) {
        fileSystem = FileSystems.default
        sourceDir = temporaryFolder.newFolder("src").toPath()
        targetDir = temporaryFolder.newFolder("tgt").toPath()
        sourceFastqc1 = sourceDir.resolve('fastq1')
        sourceFastqc2 = sourceDir.resolve('fastq2')
        sourceFastqcMd5sum1 = sourceDir.resolve('md5sum1')
        sourceFastqcMd5sum2 = sourceDir.resolve('md5sum2')
        targetFastqc1 = targetDir.resolve('fastq1')
        targetFastqc2 = targetDir.resolve('fastq2')
        targetFastqcHtml1 = targetDir.resolve('html1')
        targetFastqcHtml2 = targetDir.resolve('html2')

        if (targetAlreadyExist) {
            CreateFileHelper.createFile(targetFastqc1)
            CreateFileHelper.createFile(targetFastqc2)
        }
        int callOfDelete = targetAlreadyExist ? 1 : 0
        int logEntryCount = targetAlreadyExist ? 3 : 1

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
        fastqcProcessedFile1 = createFastqcProcessedFile([
                dataFile: dataFile1,
        ])
        fastqcProcessedFile2 = createFastqcProcessedFile([
                dataFile         : dataFile2,
                workDirectoryName: fastqcProcessedFile1.workDirectoryName,
        ])

        job = new FastqcExecuteClusterPipelineJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getInputArtefact(step, INPUT_ROLE) >> seqTrack
            _ * getOutputArtefacts(step, OUTPUT_ROLE) >> [fastqcProcessedFile1, fastqcProcessedFile2]
            0 * _
        }
        job.fileSystemService = new TestFileSystemService()
        job.fileService = Mock(FileService) {
            callOfDelete * deleteDirectoryRecursively(targetFastqc1)
            callOfDelete * deleteDirectoryRecursively(targetFastqc2)
            _ * ensureFileIsReadableAndNotEmpty(_)
            _ * convertPermissionsToOctalString(_)
            0 * _
        }
        job.logService = Mock(LogService) {
            logEntryCount * addSimpleLogEntry(_, _)
        }
    }

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqcProcessedFile,
                WorkflowArtefact,
                WorkflowStep,
        ]
    }

    @Unroll
    void "test, when fastqc reports can be copied #m1, then #m2 copy fastqc"() {
        given:
        createData(targetAlreadyExist)

        CreateFileHelper.createFile(sourceFastqc1)
        CreateFileHelper.createFile(sourceFastqc2)
        CreateFileHelper.createFile(sourceFastqcMd5sum1)
        CreateFileHelper.createFile(sourceFastqcMd5sum2)

        job.fastqcDataFilesService = Mock(FastqcDataFilesService) {
            2 * pathToFastQcResultFromSeqCenter(fastqcProcessedFile1) >> sourceFastqc1
            2 * pathToFastQcResultFromSeqCenter(fastqcProcessedFile2) >> sourceFastqc2
            1 * pathToFastQcResultMd5SumFromSeqCenter(fastqcProcessedFile1) >> sourceFastqcMd5sum1
            1 * pathToFastQcResultMd5SumFromSeqCenter(fastqcProcessedFile2) >> sourceFastqcMd5sum2
            2 * fastqcOutputPath(fastqcProcessedFile1) >> targetFastqc1
            2 * fastqcOutputPath(fastqcProcessedFile2) >> targetFastqc2
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

        when:
        List<String> scripts = job.createScripts(step)

        then:
        scripts.size() == 0

        where:
        m1                          | m2                     | targetAlreadyExist
        ""                          | ""                     | false
        " and target exist already" | "delete existing and " | true
    }

    @Unroll
    void "test, if fastqc reports can not be copied #m1, then #m2 create copy fastqc scripts"() {
        given:
        createData(targetAlreadyExist)

        final String cmd_module_loader = "cmd load module loader"
        final String cmd_activation_fastqc = "cmd activate fastqc"
        final String cmd_fastqc = "cmd fastqc"

        job.fastqcDataFilesService = Mock(FastqcDataFilesService) {
            1 * pathToFastQcResultFromSeqCenter(fastqcProcessedFile1) >> sourceFastqc1
            0 * pathToFastQcResultFromSeqCenter(fastqcProcessedFile2) >> sourceFastqc2
            2 * fastqcOutputPath(fastqcProcessedFile1) >> targetFastqc1
            2 * fastqcOutputPath(fastqcProcessedFile2) >> targetFastqc2
            1 * fastqcHtmlPath(fastqcProcessedFile1) >> targetFastqcHtml1
            1 * fastqcHtmlPath(fastqcProcessedFile2) >> targetFastqcHtml2
            0 * _
        }
        job.lsdfFilesService = Mock(LsdfFilesService) {
            1 * getFileFinalPath(dataFile1) >> Paths.get('fastq1')
            1 * getFileFinalPath(dataFile2) >> Paths.get('fastq2')
            0 * _
        }

        job.processingOptionService = Mock(ProcessingOptionService) {
            _ * findOptionAsString(ProcessingOption.OptionName.COMMAND_LOAD_MODULE_LOADER) >> cmd_module_loader
            _ * findOptionAsString(ProcessingOption.OptionName.COMMAND_ACTIVATION_FASTQC) >> cmd_activation_fastqc
            _ * findOptionAsString(ProcessingOption.OptionName.COMMAND_FASTQC) >> cmd_fastqc
        }

        when:
        List<String> scripts = job.createScripts(step)

        then:
        scripts.size() == 2
        scripts.each { String it ->
            assert it.contains(cmd_module_loader)
            assert it.contains(cmd_activation_fastqc)
            assert it.contains(cmd_fastqc)
        }

        where:
        m1                          | m2                     | targetAlreadyExist
        ""                          | ""                     | false
        " and target exist already" | "delete existing and " | true
    }
}
