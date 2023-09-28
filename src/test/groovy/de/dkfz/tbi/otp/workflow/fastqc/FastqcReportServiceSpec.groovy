/*
 * Copyright 2011-2023 The OTP authors
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
import spock.lang.*

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.FastqcDomainFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.filestore.PathOption
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.ProcessOutput
import de.dkfz.tbi.otp.workflowExecution.*

import java.nio.file.*

class FastqcReportServiceSpec extends Specification implements DataTest, FastqcDomainFactory, WorkflowSystemDomainFactory {

    static final String WORKFLOW = WesFastQcWorkflow.WORKFLOW
    static final String INPUT_ROLE = WesFastQcWorkflow.INPUT_FASTQ
    static final String OUTPUT_ROLE = WesFastQcWorkflow.OUTPUT_FASTQC

    FastqcReportService service
    private WorkflowRun run
    private WorkflowStep step
    private WorkflowArtefact artefact
    private SeqTrack seqTrack
    private FileSystem fileSystem = FileSystems.default
    private RawSequenceFile rawSequenceFile1
    private RawSequenceFile rawSequenceFile2
    private FastqcProcessedFile fastqcProcessedFile1
    private FastqcProcessedFile fastqcProcessedFile2
    private Workflow workflow
    private WorkflowVersion version

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

    @TempDir
    Path tempDir
    Path tempOutDir

    private void createData(boolean outputDirAlreadyExists) {
        fileSystem = FileSystems.default
        sourceDir = tempDir.resolve("src")
        targetDir = tempDir.resolve("tgt")
        sourceFastqc1 = sourceDir.resolve('fastq1')
        sourceFastqc2 = sourceDir.resolve('fastq2')
        sourceFastqcMd5sum1 = sourceDir.resolve('md5sum1')
        sourceFastqcMd5sum2 = sourceDir.resolve('md5sum2')
        targetFastqc1 = targetDir.resolve('fastq1')
        targetFastqc2 = targetDir.resolve('fastq2')
        targetFastqcHtml1 = targetDir.resolve('html1')
        targetFastqcHtml2 = targetDir.resolve('html2')

        workflow = createWorkflow([
                name: WORKFLOW
        ])
        version = createWorkflowVersion([
                workflow       : workflow,
                workflowVersion: '0.1.1',
        ])
        run = createWorkflowRun([
                workflow       : workflow,
                workflowVersion: version,
                workDirectory  : tempDir.resolve("workflowrun_${nextId}"),
        ])
        step = createWorkflowStep([
                workflowRun: run,
        ])
        artefact = createWorkflowArtefact([
                producedBy  : run,
                outputRole  : INPUT_ROLE, // "FASTQ"
                artefactType: ArtefactType.FASTQ,
        ])
        seqTrack = createSeqTrack([
                workflowArtefact: artefact,
        ])
        rawSequenceFile1 = createFastqFile([
                seqTrack: seqTrack,
        ])
        rawSequenceFile2 = createFastqFile([
                seqTrack: seqTrack,
        ])
        fastqcProcessedFile1 = createFastqcProcessedFile([
                sequenceFile: rawSequenceFile1,
        ])
        fastqcProcessedFile2 = createFastqcProcessedFile([
                sequenceFile     : rawSequenceFile2,
                workDirectoryName: fastqcProcessedFile1.workDirectoryName,
        ])

        if (outputDirAlreadyExists) {
            tempOutDir = Files.createDirectory(fileSystem.getPath(step.workflowRun.workDirectory))
            CreateFileHelper.createFile(tempOutDir.resolve(targetFastqc1))
            CreateFileHelper.createFile(tempOutDir.resolve(targetFastqc2))
        }

        service = new FastqcReportService()
    }

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                FastqcProcessedFile,
                WorkflowArtefact,
                WorkflowStep,
                Realm,
                ProcessingOption,
        ]
    }

    void "canFastqcReportsBeCopied should return false, if no fastqc processed files are available or no"() {
        given:
        createData(false)

        expect:
        !service.canFastqcReportsBeCopied([])
    }

    void "canFastqcReportsBeCopied should return true/false depending on if fastqc processed files are available or not"() {
        given:
        createData(true)

        CreateFileHelper.createFile(sourceFastqc1)
        CreateFileHelper.createFile(sourceFastqc2)
        CreateFileHelper.createFile(sourceFastqcMd5sum1)
        CreateFileHelper.createFile(sourceFastqcMd5sum2)

        service.fastqcDataFilesService = Mock(FastqcDataFilesService) {
            1 * pathToFastQcResultFromSeqCenter(fastqcProcessedFile1) >> Paths.get("/not_readable")
            0 * _
        }

        expect:
        !service.canFastqcReportsBeCopied([fastqcProcessedFile1, fastqcProcessedFile2])
    }

    @Unroll
    void "canFastqcReportsBeCopied should return true if #m1"() {
        given:
        createData(outputDirAlreadyExists)

        CreateFileHelper.createFile(sourceFastqc1)
        CreateFileHelper.createFile(sourceFastqc2)
        CreateFileHelper.createFile(sourceFastqcMd5sum1)
        CreateFileHelper.createFile(sourceFastqcMd5sum2)

        service.fastqcDataFilesService = Mock(FastqcDataFilesService) {
            1 * pathToFastQcResultFromSeqCenter(fastqcProcessedFile1) >> sourceFastqc1
            1 * pathToFastQcResultFromSeqCenter(fastqcProcessedFile2) >> sourceFastqc2
            0 * _
        }

        expect:
        service.canFastqcReportsBeCopied([fastqcProcessedFile1, fastqcProcessedFile2])

        where:
        m1                     | outputDirAlreadyExists
        "target doesn't exist" | false
        "target exist already" | true
    }

    @Unroll
    void "copyExistingFastqcReports should return true/false depending #m1 #m2 on if fastqc processed files are available or not"() {
        given:
        createData(true)

        CreateFileHelper.createFile(sourceFastqc1)
        CreateFileHelper.createFile(sourceFastqc2)
        CreateFileHelper.createFile(sourceFastqcMd5sum1)
        CreateFileHelper.createFile(sourceFastqcMd5sum2)

        service.fastqcDataFilesService = Mock(FastqcDataFilesService) {
            1 * pathToFastQcResultFromSeqCenter(fastqcProcessedFile1) >> sourceFastqc1
            1 * pathToFastQcResultFromSeqCenter(fastqcProcessedFile2) >> sourceFastqc2
            1 * pathToFastQcResultMd5SumFromSeqCenter(fastqcProcessedFile1) >> sourceFastqcMd5sum1
            1 * pathToFastQcResultMd5SumFromSeqCenter(fastqcProcessedFile2) >> sourceFastqcMd5sum2
            1 * fastqcOutputPath(fastqcProcessedFile1) >> targetFastqc1
            1 * fastqcOutputPath(fastqcProcessedFile2) >> targetFastqc2
            0 * _
        }

        service.fileService = Mock(FileService) {
            2 * convertPermissionsToOctalString(FileService.DEFAULT_FILE_PERMISSION)
            1 * ensureFileIsReadableAndNotEmpty(sourceFastqc1)
            1 * ensureFileIsReadableAndNotEmpty(sourceFastqc2)
            1 * ensureFileIsReadableAndNotEmpty(targetFastqc1)
            1 * ensureFileIsReadableAndNotEmpty(targetFastqc2)
            0 * _
        }

        service.remoteShellHelper = Mock(RemoteShellHelper) {
            2 * executeCommandReturnProcessOutput(_) >> new ProcessOutput("", "", 0)
        }

        expect:
        service.copyExistingFastqcReports([fastqcProcessedFile1, fastqcProcessedFile2], sourceDir)
    }

    @Unroll
    void "copyExistingFastqcReportsNewSystem should return true/false depending #m1 #m2 on if fastqc processed files are available or not"() {
        given:
        createData(true)

        CreateFileHelper.createFile(sourceFastqc1)
        CreateFileHelper.createFile(sourceFastqc2)
        CreateFileHelper.createFile(sourceFastqcMd5sum1)
        CreateFileHelper.createFile(sourceFastqcMd5sum2)

        service.fastqcDataFilesService = Mock(FastqcDataFilesService) {
            1 * pathToFastQcResultFromSeqCenter(fastqcProcessedFile1) >> sourceFastqc1
            1 * pathToFastQcResultFromSeqCenter(fastqcProcessedFile2) >> sourceFastqc2
            1 * pathToFastQcResultMd5SumFromSeqCenter(fastqcProcessedFile1) >> sourceFastqcMd5sum1
            1 * pathToFastQcResultMd5SumFromSeqCenter(fastqcProcessedFile2) >> sourceFastqcMd5sum2
            1 * fastqcOutputPath(fastqcProcessedFile1, PathOption.REAL_PATH) >> targetFastqc1
            1 * fastqcOutputPath(fastqcProcessedFile2, PathOption.REAL_PATH) >> targetFastqc2
            0 * _
        }

        service.fileService = Mock(FileService) {
            2 * createDirectoryRecursivelyAndSetPermissionsViaBash(targetDir)
            1 * ensureFileIsReadableAndNotEmpty(sourceFastqc1)
            1 * ensureFileIsReadableAndNotEmpty(sourceFastqc2)
            1 * ensureFileIsReadableAndNotEmpty(targetFastqc1)
            1 * ensureFileIsReadableAndNotEmpty(targetFastqc2)
            0 * _
        }

        service.remoteShellHelper = Mock(RemoteShellHelper) {
            2 * executeCommandReturnProcessOutput(_) >> new ProcessOutput("", "", 0)
        }

        expect:
        service.copyExistingFastqcReportsNewSystem([fastqcProcessedFile1, fastqcProcessedFile2])
    }
}
