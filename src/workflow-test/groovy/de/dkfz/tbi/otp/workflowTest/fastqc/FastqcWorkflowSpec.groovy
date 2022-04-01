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
package de.dkfz.tbi.otp.workflowTest.fastqc

import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.FastqcDataFilesService
import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.SessionUtils
import de.dkfz.tbi.otp.workflow.fastqc.BashFastQcWorkflow
import de.dkfz.tbi.otp.workflowExecution.ArtefactType
import de.dkfz.tbi.otp.workflowExecution.WorkflowArtefact
import de.dkfz.tbi.otp.workflowExecution.decider.FastqcDecider
import de.dkfz.tbi.otp.workflowTest.AbstractWorkflowSpec

import java.nio.file.Path
import java.time.Duration
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class FastqcWorkflowSpec extends AbstractWorkflowSpec {

    private static final String INPUT_FILE = "fastqFiles/fastqc/input_fastqc.fastq."
    private static final String EXPECTED_RESULT_FILE = "fastqFiles/fastqc/asdf_fastqc.zip"
    private static final String BASE_NAME = "asdf.fastq"

    FastqcDataFilesService fastqcDataFilesService
    FastqcDecider fastqcDecider
    LsdfFilesService lsdfFilesService

    private Path expectedFastqc
    private DataFile dataFile
    private SeqTrack seqTrack
    private WorkflowArtefact workflowArtefact

    void setupWorkflow(String fileExtension) {
        Path sourceFastq = referenceDataDirectory.resolve("${INPUT_FILE}${fileExtension}")
        expectedFastqc = referenceDataDirectory.resolve(EXPECTED_RESULT_FILE)

        Run run = createRun()

        seqTrack = createSeqTrack(
                fastqcState: SeqTrack.DataProcessingState.NOT_STARTED,
                seqType: SeqTypeService.rnaSingleSeqType,
                run: run,
        )
        workflowArtefact = createWorkflowArtefact(
                state: WorkflowArtefact.State.SUCCESS,
                artefactType: ArtefactType.FASTQ,
                displayName: "display name",
                outputRole: "FASTQ",
                producedBy: createWorkflowRun(priority: processingPriority),
        )
        seqTrack.workflowArtefact = workflowArtefact
        seqTrack.save(flush: true)

        dataFile = createSequenceDataFile(
                fileExists: true,
                fileSize: 1,
                fileName: "${BASE_NAME}.${fileExtension}",
                vbpFileName: "${BASE_NAME}.${fileExtension}",
                seqTrack: seqTrack,
                run: run,
                initialDirectory: workingDirectory.resolve("ftp").resolve(run.name),
        )

        fileService.createLink(lsdfFilesService.getFileViewByPidPathAsPath(dataFile), sourceFastq, realm)
        fileService.createLink(lsdfFilesService.getFileFinalPathAsPath(dataFile), sourceFastq, realm)
    }

    void "test FastQcWorkflow, when FastQC result file is available"() {
        given:
        SessionUtils.withTransaction {
            setupWorkflow('gz')
            Path initialPath = lsdfFilesService.getFileInitialPathAsPath(dataFile).parent
            fileService.createLink(initialPath.resolve("${BASE_NAME}.zip"), expectedFastqc, realm)
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
            FastqcProcessedFile fastqcProcessedFile = CollectionUtils.atMostOneElement(FastqcProcessedFile.findAllByDataFile(dataFile))
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

            assert fastqcProcessedFile.fileExists
            assert fastqcProcessedFile.contentUploaded
            assert fastqcProcessedFile.dataFile == dataFile
        }
    }

    private void validateFastQcFileContent() {
        SessionUtils.withTransaction {
            dataFile.refresh()
            assert null != dataFile.sequenceLength
            assert null != dataFile.nReads
            seqTrack.refresh()
            assert seqTrack.nBasePairs
        }
    }

    @Override
    Duration getRunningTimeout() {
        return Duration.ofMinutes(20)
    }

    @Override
    String getWorkflowName() {
        return BashFastQcWorkflow.WORKFLOW
    }

    final Class<BashFastQcWorkflow> workflowComponentClass = BashFastQcWorkflow
}
