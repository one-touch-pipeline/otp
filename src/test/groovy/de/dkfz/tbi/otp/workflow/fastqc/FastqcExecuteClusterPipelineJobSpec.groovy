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
import spock.lang.*

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.FastqcDomainFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.filestore.PathOption
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataWorkFileService
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CreateFileHelper
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

    private void createData(boolean outputDirAlreadyExists, boolean copyFile) {
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

        int callOfDelete = outputDirAlreadyExists ? 1 : 0
        int logEntryCount = (outputDirAlreadyExists ? 2 : 1) + (copyFile ? 0 : 1)

        workflow = createWorkflow([
                name: WORKFLOW
        ])
        version = createWorkflowVersion([
                apiVersion: createWorkflowApiVersion(workflow: workflow),
                workflowVersion   : '0.1.1',
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

        job = new FastqcExecuteClusterPipelineJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getInputArtefact(step, INPUT_ROLE) >> seqTrack
            _ * getOutputArtefacts(step, OUTPUT_ROLE) >> [fastqcProcessedFile1, fastqcProcessedFile2]
            0 * _
        }

        job.fileSystemService = new TestFileSystemService()
        job.fileService = Mock(FileService) {
            _ * ensureFileIsReadableAndNotEmpty(_, _)
            callOfDelete * deleteDirectoryRecursively(_)
            callOfDelete * createDirectoryRecursivelyAndSetPermissionsViaBash(_, _)
            0 * _
        }

        job.logService = Mock(LogService) {
            logEntryCount * addSimpleLogEntry(_, _)
        }
        findOrCreateProcessingOption(
                name: ProcessingOption.OptionName.COMMAND_ENABLE_MODULE,
                value: 'module load',
        )
        findOrCreateProcessingOption(
                name: ProcessingOption.OptionName.COMMAND_FASTQC,
                value: 'fastqc',
        )
    }

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                FastqcProcessedFile,
                WorkflowArtefact,
                WorkflowStep,
                ProcessingOption,
        ]
    }

    @Unroll
    void "test, when fastqc reports can be copied #m1, then #m2 copy fastqc"() {
        given:
        createData(outputDirAlreadyExists, true)

        CreateFileHelper.createFile(sourceFastqc1)
        CreateFileHelper.createFile(sourceFastqc2)
        CreateFileHelper.createFile(sourceFastqcMd5sum1)
        CreateFileHelper.createFile(sourceFastqcMd5sum2)

        job.fastqcReportService = Mock(FastqcReportService) {
            1 * canFastqcReportsBeCopied([fastqcProcessedFile1, fastqcProcessedFile2]) >> true
            1 * copyExistingFastqcReports([fastqcProcessedFile1, fastqcProcessedFile2])
            0 * _
        }

        job.fastqcDataFilesService = Mock(FastqcDataFilesService) {
            1 * fastqcOutputDirectory(fastqcProcessedFile1, PathOption.REAL_PATH) >> targetDir
            0 * _
        }

        when:
        List<String> scripts = job.createScripts(step)

        then:
        scripts.size() == 0

        where:
        m1                          | m2                     | outputDirAlreadyExists
        ""                          | ""                     | false
        " and target exist already" | "delete existing and " | true
    }

    @Unroll
    void "test, if fastqc reports can not be copied #m1, then #m2 create copy fastqc scripts"() {
        given:
        createData(outputDirAlreadyExists, false)

        final String cmd_activation_fastqc = "cmd module load fastqc"
        final String cmd_fastqc = "cmd fastqc"

        job.fastqcDataFilesService = Mock(FastqcDataFilesService) {
            1 * fastqcOutputDirectory(fastqcProcessedFile1, PathOption.REAL_PATH) >> targetDir
            0 * _
        }
        job.fastqcReportService = Mock(FastqcReportService) {
            1 * canFastqcReportsBeCopied([fastqcProcessedFile1, fastqcProcessedFile2]) >> false
            0 * _
        }
        job.rawSequenceDataWorkFileService = Mock(RawSequenceDataWorkFileService) {
            1 * getFilePath(rawSequenceFile1) >> Paths.get('fastq1')
            1 * getFilePath(rawSequenceFile2) >> Paths.get('fastq2')
            0 * _
        }

        job.processingOptionService = Mock(ProcessingOptionService) {
            _ * findOptionAsString(ProcessingOption.OptionName.COMMAND_ENABLE_MODULE) >> cmd_activation_fastqc
            _ * findOptionAsString(ProcessingOption.OptionName.COMMAND_FASTQC) >> cmd_fastqc
        }

        when:
        List<String> scripts = job.createScripts(step)

        then:
        scripts.size() == 2
        scripts.each { String it ->
            assert it.contains(cmd_activation_fastqc + ' fastqc/' + version.workflowVersion)
            assert it.contains(cmd_fastqc)
        }

        where:
        m1                          | m2                     | outputDirAlreadyExists
        ""                          | ""                     | false
        " and target exist already" | "delete existing and " | true
    }
}
