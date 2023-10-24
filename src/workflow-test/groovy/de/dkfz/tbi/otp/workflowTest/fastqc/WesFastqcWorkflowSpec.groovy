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

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.FastqcDataFilesService
import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.SessionUtils
import de.dkfz.tbi.otp.workflow.fastqc.BashFastQcWorkflow
import de.dkfz.tbi.otp.workflow.fastqc.WesFastQcWorkflow
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.decider.FastqcDecider
import de.dkfz.tbi.otp.workflowTest.AbstractDecidedWorkflowSpec

import java.nio.file.Path
import java.time.Duration
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class WesFastqcWorkflowSpec extends AbstractDecidedWorkflowSpec {

    //@Slf4j does not work with Spock containing tests and produces problems in closures
    @SuppressWarnings('PropertyName')
    final static Logger log = LoggerFactory.getLogger(WesFastqcWorkflowSpec)

    private static final String INPUT_FILE = "fastqFiles/fastqc/input_fastqc.fastq."
    private static final String EXPECTED_RESULT_FILE = "fastqFiles/fastqc/asdf_fastqc.zip"
    private static final List<String> BASE_NAMES = (1..2).collect {
        "asdf.R${it}.fastq"
    }.asImmutable()

    private static final List<String> EXPECTED_ZIP_ENTRIES = [
            "",
            "Icons/",
            "Images/",
            "Icons/fastqc_icon.png",
            "Icons/warning.png",
            "Icons/error.png",
            "Icons/tick.png",
            "summary.txt",
            "Images/per_base_quality.png",
            "Images/per_tile_quality.png",
            "Images/per_sequence_quality.png",
            "Images/per_base_sequence_content.png",
            "Images/per_sequence_gc_content.png",
            "Images/per_base_n_content.png",
            "Images/sequence_length_distribution.png",
            "Images/duplication_levels.png",
            "Images/adapter_content.png",
            "fastqc_report.html",
            "fastqc_data.txt",
            "fastqc.fo",
    ].asImmutable()

    private static final List<String> EXPECTED_ZIP_ENTRIES_COPIED = (EXPECTED_ZIP_ENTRIES + ["Images/kmer_profiles.png"]).collect {
        "asdf_fastqc/${it}".toString()
    }.asImmutable()

    private static final List<String> EXPECTED_ZIP_ENTRIES_WES = EXPECTED_ZIP_ENTRIES.collect {
        "stdin_fastqc/${it}".toString()
    }.asImmutable()

    Class<WesFastQcWorkflow> workflowComponentClass = WesFastQcWorkflow

    FastqcDataFilesService fastqcDataFilesService
    FastqcDecider fastqcDecider
    LsdfFilesService lsdfFilesService

    private Path expectedFastqc
    private final List<RawSequenceFile> allRawSequenceFiles = []

    private Workflow workflow
    private WorkflowVersion workflowVersion

    void setupWorkflow(String fileExtension, int count) {
        log.debug("Start setup ${this.class.simpleName}")
        Path sourceFastq = referenceDataDirectory.resolve("${INPUT_FILE}${fileExtension}")
        expectedFastqc = referenceDataDirectory.resolve(EXPECTED_RESULT_FILE)

        Sample sample = createSample()

        (1..count).each {
            Run run = createRun()
            WorkflowArtefact workflowArtefact = createWorkflowArtefact([
                    state       : WorkflowArtefact.State.SUCCESS,
                    artefactType: ArtefactType.FASTQ,
                    displayName : "display name",
                    outputRole  : "FASTQ",
                    producedBy  : createWorkflowRun(priority: processingPriority, state: WorkflowRun.State.LEGACY),
            ])

            SeqType seqType = SeqTypeService.wholeGenomePairedSeqType

            SeqTrack seqTrack = createSeqTrack([
                    fastqcState     : SeqTrack.DataProcessingState.NOT_STARTED,
                    seqType         : seqType,
                    run             : run,
                    sample          : sample,
                    workflowArtefact: workflowArtefact,
            ])

            List<RawSequenceFile> rawSequenceFiles = BASE_NAMES.collect {
                createSequenceDataFile([
                        fileExists         : true,
                        fileSize           : 1,
                        fileName           : "${it}.${fileExtension}",
                        vbpFileName        : "${it}.${fileExtension}",
                        seqTrack           : seqTrack,
                        run                : run,
                        initialDirectory   : workingDirectory.resolve("ftp").resolve(run.name),
                        fastqImportInstance: fastqImportInstance,
                ])
            }
            allRawSequenceFiles.addAll(rawSequenceFiles)
        }
        log.info("Domain data created")

        allRawSequenceFiles.each {
            fileService.createLink(lsdfFilesService.getFileFinalPathAsPath(it), sourceFastq)
            fileService.createLink(lsdfFilesService.getFileViewByPidPathAsPath(it), lsdfFilesService.getFileFinalPathAsPath(it))
        }
        log.info("File system prepared")

        workflow = CollectionUtils.exactlyOneElement(Workflow.findAllByName(WesFastQcWorkflow.WORKFLOW))
        log.info("Fetch workflow: ${workflow}")

        workflowVersion = CollectionUtils.exactlyOneElement(WorkflowVersion.findAllByWorkflow(workflow, [sort: 'id', order: 'desc', max: 1]))
        log.info("Fetch workflow version: ${workflowVersion}")

        WorkflowVersionSelector workflowVersionSelector = createWorkflowVersionSelector([
                project        : sample.project,
                seqType        : null,
                workflowVersion: workflowVersion,
        ])
        log.info("Create selectedProjectSeqTypeWorkflowVersion ${workflowVersionSelector}")

        log.debug("Finish setup ${this.class.simpleName}")
    }

    void "test FastQcWorkflow, when FastQC result file is available"() {
        given:
        SessionUtils.withTransaction {
            setupWorkflow('gz', 1)
            Path initialPath = lsdfFilesService.getFileInitialPathAsPath(allRawSequenceFiles.first()).parent
            allRawSequenceFiles.each {
                fileService.createLink(initialPath.resolve(it.fileName.replace('.fastq.gz', '_fastqc.zip')), expectedFastqc)
            }
            decide(1, BASE_NAMES.size())
        }

        when:
        execute(1, 1)

        then:
        checkExistenceOfResultsFiles(EXPECTED_ZIP_ENTRIES_COPIED)
        validateFastqcProcessedFile()
        validateFastQcFileContent()
    }

    @Unroll
    void "test FastQcWorkflow, when FastQC result file is not available and extension is #extension and #parallel parallel"() {
        given:
        SessionUtils.withTransaction {
            setupWorkflow(extension, parallel)
            decide(parallel, parallel * BASE_NAMES.size())
        }

        when:
        execute(parallel, parallel)

        then:
        checkExistenceOfResultsFiles(EXPECTED_ZIP_ENTRIES_WES)
        validateFastqcProcessedFile()
        validateFastQcFileContent()

        where:
        extension | parallel
        'gz'      | 1
        'tar.gz'  | 1
        'bz2'     | 1
        'tar.bz2' | 1
        'gz'      | 3
    }

    private void checkExistenceOfResultsFiles(List<String> expectedZipEntries) {
        SessionUtils.withTransaction {
            allRawSequenceFiles.each { RawSequenceFile rawSequenceFile ->
                FastqcProcessedFile fastqcProcessedFile = CollectionUtils.atMostOneElement(FastqcProcessedFile.findAllBySequenceFile(rawSequenceFile))
                ZipFile actualResult = new ZipFile(fastqcDataFilesService.fastqcOutputPath(fastqcProcessedFile).toString())

                List<String> actualFiles = []
                actualResult.entries().each { ZipEntry entry ->
                    actualFiles.add(entry.name)
                }

                TestCase.assertContainSame(actualFiles, expectedZipEntries)
            }
        }
    }

    private void validateFastqcProcessedFile() {
        SessionUtils.withTransaction {
            allRawSequenceFiles.each { RawSequenceFile rawSequenceFile ->
                FastqcProcessedFile fastqcProcessedFile = CollectionUtils.atMostOneElement(FastqcProcessedFile.findAllBySequenceFile(rawSequenceFile))

                assert fastqcProcessedFile.workflowArtefact
                assert fastqcProcessedFile.fileExists
                assert fastqcProcessedFile.contentUploaded
            }
        }
    }

    private void validateFastQcFileContent() {
        SessionUtils.withTransaction {
            allRawSequenceFiles.each { RawSequenceFile rawSequenceFileParam ->
                RawSequenceFile rawSequenceFile = RawSequenceFile.get(rawSequenceFileParam.id)
                assert rawSequenceFile.sequenceLength
                assert rawSequenceFile.nReads
                assert rawSequenceFile.seqTrack.nBasePairs
            }
        }
    }

    @Override
    protected FastqcDecider getDecider() {
        return fastqcDecider
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
