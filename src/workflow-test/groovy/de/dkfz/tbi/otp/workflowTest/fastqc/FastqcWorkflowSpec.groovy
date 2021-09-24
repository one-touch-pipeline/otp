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
import de.dkfz.tbi.otp.workflow.fastqc.FastqcWorkflow
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

    FastqcDataFilesService fastqcDataFilesService
    FastqcDecider fastqcDecider
    LsdfFilesService lsdfFilesService

    private Path expectedFastqc
    private DataFile dataFile
    private SeqTrack seqTrack
    private WorkflowArtefact workflowArtefact

    void setupWorkflow(String fileExtension) {
        Path sourceFastq = inputDataDirectory.resolve("${INPUT_FILE}${fileExtension}")
        expectedFastqc = inputDataDirectory.resolve(EXPECTED_RESULT_FILE)

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
                fileName: "asdf.fastq.${fileExtension}",
                vbpFileName: "asdf.fastq.${fileExtension}",
                seqTrack: seqTrack,
                run: run,
                initialDirectory: workflowResultDirectory.resolve("ftp").resolve(run.name),
    )

        fileService.createLink(lsdfFilesService.getFileViewByPidPathAsPath(dataFile), sourceFastq, realm)
        fileService.createLink(lsdfFilesService.getFileFinalPathAsPath(dataFile), sourceFastq, realm)
    }

    void "test FastQcWorkflow, when FastQC result file is available"() {
        given:
        SessionUtils.withNewSession {
            setupWorkflow('gz')
            Path initialPath = lsdfFilesService.getFileInitialPathAsPath(dataFile, remoteFileSystem).parent
            String fastqcFileName = fastqcDataFilesService.fastqcFileName(dataFile)
            fileService.createLink(initialPath.resolve(fastqcFileName), expectedFastqc, realm)
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
        SessionUtils.withNewSession {
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
        SessionUtils.withNewSession {
            ZipFile expectedResult = new ZipFile(fileService.toFile(expectedFastqc))
            ZipFile actualResult = new ZipFile(fastqcDataFilesService.fastqcOutputFile(dataFile))

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
        SessionUtils.withNewSession {
            FastqcProcessedFile fastqcProcessedFile = CollectionUtils.exactlyOneElement(FastqcProcessedFile.all)

            assert fastqcProcessedFile.fileExists
            assert fastqcProcessedFile.contentUploaded
            assert fastqcProcessedFile.dataFile == dataFile
        }
    }

    private void validateFastQcFileContent() {
        SessionUtils.withNewSession {
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
        return FastqcWorkflow.WORKFLOW
    }

    final Class<FastqcWorkflow> workflowComponentClass = FastqcWorkflow
}
