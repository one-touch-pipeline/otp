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
package de.dkfz.tbi.otp.workflowTest.fastqc

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.FastqcDataFilesService
import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.SessionUtils
import de.dkfz.tbi.otp.workflow.fastqc.BashFastQcWorkflow
import de.dkfz.tbi.otp.workflowExecution.ArtefactType
import de.dkfz.tbi.otp.workflowExecution.Workflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowApiVersion
import de.dkfz.tbi.otp.workflowExecution.WorkflowArtefact
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowVersion
import de.dkfz.tbi.otp.workflowExecution.WorkflowVersionSelector
import de.dkfz.tbi.otp.workflowExecution.decider.FastqcDecider
import de.dkfz.tbi.otp.workflowTest.AbstractWorkflowSpec

import java.nio.file.Path
import java.time.Duration
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class FastqcWorkflowSpec extends AbstractWorkflowSpec {

    // @Slf4j does not work with Spock containing tests and produces problems in closures
    @SuppressWarnings('PropertyName')
    final static Logger log = LoggerFactory.getLogger(FastqcWorkflowSpec)

    private static final String INPUT_FILE = "fastqFiles/fastqc/input_fastqc.fastq."
    private static final String EXPECTED_RESULT_FILE = "fastqFiles/fastqc/asdf_fastqc.zip"
    private static final String BASE_NAME = "asdf.fastq"

    Class<BashFastQcWorkflow> workflowComponentClass = BashFastQcWorkflow

    FastqcDataFilesService fastqcDataFilesService
    FastqcDecider fastqcDecider
    LsdfFilesService lsdfFilesService

    private Path expectedFastqc
    private RawSequenceFile rawSequenceFile
    private SeqType seqType
    private SeqTrack seqTrack
    private WorkflowArtefact workflowArtefact

    private Workflow workflow
    private WorkflowVersion workflowVersion

    void setupWorkflow(String fileExtension) {
        log.debug("Start setup ${this.class.simpleName}")
        Path sourceFastq = referenceDataDirectory.resolve("${INPUT_FILE}${fileExtension}")
        expectedFastqc = referenceDataDirectory.resolve(EXPECTED_RESULT_FILE)

        Run run = createRun()

        workflowArtefact = createWorkflowArtefact(
                state: WorkflowArtefact.State.SUCCESS,
                artefactType: ArtefactType.FASTQ,
                displayName: "display name",
                outputRole: "FASTQ",
                producedBy: createWorkflowRun(priority: processingPriority, state: WorkflowRun.State.LEGACY),
        )

        seqType = SeqTypeService.rnaSingleSeqType

        seqTrack = createSeqTrack(
                fastqcState: SeqTrack.DataProcessingState.NOT_STARTED,
                seqType: seqType,
                run: run,
                workflowArtefact: workflowArtefact,
        )

        rawSequenceFile = createSequenceDataFile(
                fileExists: true,
                fileSize: 1,
                fileName: "${BASE_NAME}.${fileExtension}",
                vbpFileName: "${BASE_NAME}.${fileExtension}",
                seqTrack: seqTrack,
                run: run,
                initialDirectory: workingDirectory.resolve("ftp").resolve(run.name),
                fastqImportInstance: fastqImportInstance,
        )
        log.info("Domain data created")

        fileService.createLink(lsdfFilesService.getFileFinalPathAsPath(rawSequenceFile), sourceFastq)
        fileService.createLink(lsdfFilesService.getFileViewByPidPathAsPath(rawSequenceFile), lsdfFilesService.getFileFinalPathAsPath(rawSequenceFile))
        log.info("File system prepared")

        workflow = CollectionUtils.exactlyOneElement(Workflow.findAllByName(BashFastQcWorkflow.WORKFLOW))
        log.info("Fetch workflow Fastqc ${workflow}")

        WorkflowApiVersion wav = CollectionUtils.exactlyOneElement(WorkflowApiVersion.findAllByWorkflow(workflow, [sort: 'id', order: 'desc', max: 1]))
        workflowVersion = CollectionUtils.exactlyOneElement(WorkflowVersion.findAllByApiVersion(wav, [sort: 'id', order: 'desc', max: 1]))
        log.info("Fetch workflow version ${workflowVersion}")

        WorkflowVersionSelector workflowVersionSelector = createWorkflowVersionSelector([
                project        : seqTrack.project,
                seqType        : null,
                workflowVersion: workflowVersion,
        ])
        log.info("Create selectedProjectSeqTypeWorkflowVersion ${workflowVersionSelector}")

        log.debug("Finish setup ${this.class.simpleName}")
    }

    void "test FastQcWorkflow, when FastQC result file is available"() {
        given:
        SessionUtils.withTransaction {
            setupWorkflow('gz')
            Path initialPath = lsdfFilesService.getFileInitialPathAsPath(rawSequenceFile).parent
            fileService.createLink(initialPath.resolve(expectedFastqc.fileName), expectedFastqc)
            fastqcDecider.decide([workflowArtefact])
        }

        when:
        execute(1, 1)

        then:
        checkExistenceOfResultsFiles()
        validateFastqcProcessedFile()
        validateFastQcFileContent()
    }

    @Unroll
    void "test FastQcWorkflow, when FastQC result file is not available and extension is #extension"() {
        given:
        SessionUtils.withTransaction {
            setupWorkflow(extension)
            fastqcDecider.decide([workflowArtefact])
        }

        when:
        execute(1, 1)

        then:
        checkExistenceOfResultsFiles()
        validateFastqcProcessedFile()
        validateFastQcFileContent()

        where:
        extension | _
        'gz'      | _
        'tar.gz'  | _
        'bz2'     | _
        'tar.bz2' | _
    }

    private void checkExistenceOfResultsFiles() {
        SessionUtils.withTransaction {
            FastqcProcessedFile fastqcProcessedFile = CollectionUtils.atMostOneElement(FastqcProcessedFile.findAllBySequenceFile(rawSequenceFile))
            ZipFile expectedResult = new ZipFile(fileService.toFile(expectedFastqc))
            ZipFile actualResult = new ZipFile(fastqcDataFilesService.fastqcOutputPath(fastqcProcessedFile).toString())

            List<String> actualFiles = []
            actualResult.entries().each { ZipEntry entry ->
                actualFiles.add(entry.name)
            }

            expectedResult.entries().each { ZipEntry entry ->
                assert actualFiles.contains(entry.name)
                actualFiles.remove(entry.name)
            }
            assert actualFiles.isEmpty()
        }
    }

    private void validateFastqcProcessedFile() {
        SessionUtils.withTransaction {
            FastqcProcessedFile fastqcProcessedFile = CollectionUtils.exactlyOneElement(FastqcProcessedFile.all)

            assert fastqcProcessedFile.workflowArtefact
            assert fastqcProcessedFile.fileExists
            assert fastqcProcessedFile.contentUploaded
            assert fastqcProcessedFile.sequenceFile == rawSequenceFile
        }
    }

    private void validateFastQcFileContent() {
        SessionUtils.withTransaction {
            rawSequenceFile.refresh()
            assert rawSequenceFile.sequenceLength
            assert rawSequenceFile.nReads
            seqTrack = SeqTrack.get(seqTrack.id) // .refresh() does not work
            assert seqTrack.nBasePairs
        }
    }

    @Override
    Duration getRunningTimeout() {
        return Duration.ofHours(5)
    }

    @Override
    String getWorkflowName() {
        return BashFastQcWorkflow.WORKFLOW
    }
}
