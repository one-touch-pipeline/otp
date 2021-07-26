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
package de.dkfz.tbi.otp.workflowTest.dataInstallation

import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.SessionUtils
import de.dkfz.tbi.otp.workflow.datainstallation.DataInstallationInitializationService
import de.dkfz.tbi.otp.workflow.datainstallation.DataInstallationWorkflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowTest.AbstractWorkflowSpec

import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

/**
 * Workflow test for the Fastq import.
 *
 * Each test needs less then 2 minutes (If cluster is free).
 */
class DataInstallationWorkflowSpec extends AbstractWorkflowSpec {

    static final private String FASTQ_R1_ORIGINAL_FILENAME = "gerald_D1VCPACXX_6_R1.fastq.bz2"
    static final private String FASTQ_R2_ORIGINAL_FILENAME = "gerald_D1VCPACXX_6_R2.fastq.bz2"
    static final private String FASTQ_R1_FILENAME = "example_fileR1.fastq.gz"
    static final private String FASTQ_R2_FILENAME = "example_fileR2.fastq.gz"
    static final private String DIRECTORY_IN_INPUT = "fastqFiles/wgs/normal/paired/run1/sequence"

    DataInstallationInitializationService dataInstallationInitializationService
    LsdfFilesService lsdfFilesService
    SingleCellService singleCellService

    private Path fastqR1Filepath
    private Path fastqR2Filepath

    private FileType fileType

    @Override
    Duration getRunningTimeout() {
        return Duration.ofMinutes(60)
    }

    @Override
    String getWorkflowName() {
        return DataInstallationWorkflow.WORKFLOW
    }

    final Class<DataInstallationWorkflow> workflowComponentClass = DataInstallationWorkflow

    @Override
    void setup() {
        SessionUtils.withNewSession {
            fileType = createFileType()

            fastqR1Filepath = prepareFileSystemForFile(FASTQ_R1_FILENAME, FASTQ_R1_ORIGINAL_FILENAME)
            fastqR2Filepath = prepareFileSystemForFile(FASTQ_R2_FILENAME, FASTQ_R2_ORIGINAL_FILENAME)
        }
    }

    private Path prepareFileSystemForFile(String fileName, String orgName) {
        Path path = inputDataDirectory.resolve(DIRECTORY_IN_INPUT).resolve(orgName)
        Path link = additionalDataDirectory.resolve(fileName)
        fileService.createLink(link, path, realm)
        return path
    }

    private String md5sum(Path filepath) {
        String cmdMd5sum = "md5sum ${filepath}"
        String output = remoteShellHelper.executeCommandReturnProcessOutput(realm, cmdMd5sum).assertExitCodeZeroAndStderrEmpty().stdout
        String md5sum = output.split().first()
        return md5sum
    }

    private DataFile createDataFile(SeqTrack seqTrack, Integer mateNumber, String fastqFilename, Path fastqFilePath, Map map = [:]) {
        return createDataFile([
                mateNumber      : mateNumber,
                seqTrack        : seqTrack,
                project         : seqTrack.project,
                fileName        : fastqFilename,
                vbpFileName     : fastqFilename,
                md5sum          : md5sum(fastqFilePath),
                fileExists      : false,
                fileLinked      : false,
                fileSize        : 0,
                initialDirectory: additionalDataDirectory.toString(),
                run             : seqTrack.run,
                fileType        : fileType,
        ] + map)
    }

    private void createDataFiles(SeqTrack seqTrack, Map map = [:]) {
        createDataFile(seqTrack, 1, FASTQ_R1_FILENAME, fastqR1Filepath, map)
        createDataFile(seqTrack, 2, FASTQ_R2_FILENAME, fastqR2Filepath, map)
    }

    private SeqTrack createWholeGenomeSetup(boolean linkedExternally = false) {
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()
        SeqTrack seqTrack = createSeqTrack([seqType: seqType, linkedExternally: linkedExternally])
        createDataFiles(seqTrack)
        return seqTrack
    }

    protected void prepareAndExecute(int expectedWorkflows = 1) {
        SessionUtils.withNewSession {
            FastqImportInstance fastqImportInstance = createFastqImportInstance([
                    dataFiles: DataFile.list(),
            ])
            List<WorkflowRun> workflowRuns = dataInstallationInitializationService.createWorkflowRuns(fastqImportInstance)
            assert workflowRuns.size() == expectedWorkflows
        }
        execute(expectedWorkflows)
    }

    @Unroll
    void "test DataInstallation, files have to be linkedExternally #linkedExternally"() {
        given:
        SessionUtils.withNewSession {
            createWholeGenomeSetup(linkedExternally)
        }

        when:
        prepareAndExecute()

        then:
        checkThatWorkflowWasSuccessful()

        where:
        linkedExternally | _
        true             | _
        false            | _
    }

    void "test ChipSeq DataInstallation"() {
        given:
        SeqTrack seqTrack
        SessionUtils.withNewSession {
            seqTrack = createChipSeqSeqTrack()
            createDataFiles(seqTrack)
        }

        when:
        prepareAndExecute()

        then:
        checkThatWorkflowWasSuccessful()
    }

    void "test single cell import without well"() {
        given:
        SeqTrack seqTrack
        SessionUtils.withNewSession {
            seqTrack = createSeqTrack([
                    seqType            : createSeqType([
                            libraryLayout: SequencingReadType.PAIRED,
                            singleCell   : true,
                    ]),
                    singleCellWellLabel: null,
            ])
            createDataFiles(seqTrack)
        }

        when:
        prepareAndExecute()

        then:
        checkThatWorkflowWasSuccessful()
    }

    void "test single cell import with well"() {
        given:
        List<SeqTrack> seqTracks
        SessionUtils.withNewSession {
            SeqType seqType = createSeqType([
                    libraryLayout: SequencingReadType.PAIRED,
                    singleCell   : true,
            ])
            Run run = createRun()
            SoftwareTool softwareTool = createSoftwareTool()
            Sample sample = createSample()

            seqTracks = (1..3).collect {
                String fileNameR1 = "${it}_${FASTQ_R1_FILENAME}"
                String fileNameR2 = "${it}_${FASTQ_R2_FILENAME}"
                Path linkR1 = prepareFileSystemForFile(fileNameR1, FASTQ_R1_ORIGINAL_FILENAME)
                Path linkR2 = prepareFileSystemForFile(fileNameR2, FASTQ_R2_ORIGINAL_FILENAME)

                SeqTrack seqTrack = createSeqTrack([
                        run                : run,
                        seqType            : seqType,
                        sample             : sample,
                        pipelineVersion    : softwareTool,
                        singleCellWellLabel: "well_${it}",
                ])

                createDataFile(seqTrack, 1, fileNameR1, linkR1)
                createDataFile(seqTrack, 2, fileNameR2, linkR2)
                return seqTrack
            }
        }

        when:
        prepareAndExecute(seqTracks.size())

        then:
        checkThatWorkflowWasSuccessful()
        checkWellBasedLinksAreCreatedSuccessful()
        checkMappingFileAreCreatedSuccessful()
    }

    /**
     * check that DataFiles are copied and completed successfully.
     */
    protected void checkThatWorkflowWasSuccessful() {
        SessionUtils.withNewSession {
            SeqTrack.list().each { SeqTrack seqTrack ->
                assert seqTrack.dataInstallationState == SeqTrack.DataProcessingState.FINISHED
                assert SeqTrack.DataProcessingState.NOT_STARTED == seqTrack.fastqcState
            }
            DataFile.list().collectMany { DataFile dataFile ->
                assert dataFile.fileExists
                assert dataFile.fileLinked
                assert dataFile.fileSize > 0
                [
                        lsdfFilesService.getFileFinalPath(dataFile),
                        lsdfFilesService.getFileViewByPidPath(dataFile),
                ]
            }.each {
                Path path = remoteFileSystem.getPath(it)
                assert Files.exists(path)
            }
        }
    }

    /**
     * check that links in the well structure was created successfully
     */
    protected void checkWellBasedLinksAreCreatedSuccessful() {
        SessionUtils.withNewSession {
            DataFile.list().collect {
                lsdfFilesService.getWellAllFileViewByPidPath(it)
            }.each {
                assert new File(it).exists()
            }
        }
    }

    /**
     * check that well mapping file was created successfully with correct content
     */
    protected void checkMappingFileAreCreatedSuccessful() {
        SessionUtils.withNewSession {
            List<DataFile> dataFiles = DataFile.list()

            //check mapping file exists
            Path mappingFile = CollectionUtils.exactlyOneElement(dataFiles.collect {
                singleCellService.singleCellMappingFile(it)
            }.unique())
            assert Files.exists(mappingFile)

            //check mappingFileContext
            String mappingFileContent = mappingFile.text
            assert mappingFileContent.split('\n').size() == dataFiles.size()
            dataFiles.each {
                assert mappingFileContent.contains(singleCellService.mappingEntry(it))
            }
        }
    }
}
