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
package de.dkfz.tbi.otp.dataInstallation

import spock.lang.Shared
import spock.lang.Unroll

import de.dkfz.tbi.otp.WorkflowTestCase
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.SessionUtils
import de.dkfz.tbi.otp.workflowTest.dataInstallation.DataInstallationWorkflowSpec

import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

/**
 * @Deprecated replaces by {@link DataInstallationWorkflowSpec}
 */
@Deprecated
class DataInstallationWorkflowTests extends WorkflowTestCase implements DomainFactoryCore {

    LsdfFilesService lsdfFilesService

    SingleCellService singleCellService

    // files to be processed by the tests
    @Shared
    String fastqR1Filepath
    @Shared
    String fastqR2Filepath
    @Shared
    String fastqR1Filename = "example_fileR1.fastq.gz"
    @Shared
    String fastqR2Filename = "example_fileR2.fastq.gz"

    private String md5sum(String filepath) {
        String cmdMd5sum = "md5sum ${filepath}"
        String output = remoteShellHelper.executeCommandReturnProcessOutput(realm, cmdMd5sum).assertExitCodeZeroAndStderrEmpty().stdout
        String md5sum = output.split().first()
        return md5sum
    }

    @Override
    void setup() {
        fastqR1Filepath = "${inputRootDirectory}/fastqFiles/wgs/normal/paired/run1/sequence/gerald_D1VCPACXX_6_R1.fastq.bz2"
        fastqR2Filepath = "${inputRootDirectory}/fastqFiles/wgs/normal/paired/run1/sequence/gerald_D1VCPACXX_6_R2.fastq.bz2"

        File softLinkFastqR1Filepath = new File("${ftpDir}/${fastqR1Filename}")
        File softLinkFastqR2Filepath = new File("${ftpDir}/${fastqR2Filename}")

        SessionUtils.withNewSession {
            createDirectories([new File(ftpDir)])
            linkFileUtils.createAndValidateLinks(
                    [
                            (new File(fastqR1Filepath)): softLinkFastqR1Filepath,
                            (new File(fastqR2Filepath)): softLinkFastqR2Filepath,
                    ], realm)
        }
    }

    DataFile createDataFile(SeqTrack seqTrack, Integer mateNumber, String fastqFilename, String fastqFilepath, FileType fileType) {
        return createDataFile([
                project         : seqTrack.project,
                fileName        : fastqFilename,
                md5sum          : md5sum(fastqFilepath),
                seqTrack        : seqTrack,
                vbpFileName     : fastqFilename,
                fileExists      : false,
                fileLinked      : false,
                fileSize        : 0,
                mateNumber      : mateNumber,
                initialDirectory: ftpDir,
                run             : seqTrack.run,
                fileType        : fileType,
        ])
    }

    @Unroll
    void "test DataInstallation, files have to be copied #copied"() {
        given:
        SeqTrack seqTrack
        SessionUtils.withNewSession {
            seqTrack = createWholeGenomeSetup(copied)
        }

        when:
        execute()

        then:
        checkThatWorkflowWasSuccessful(seqTrack)

        where:
        copied | _
        true   | _
        false  | _
    }

    void "test ChipSeq DataInstallation"() {
        given:
        SeqTrack seqTrack
        SessionUtils.withNewSession {
            seqTrack = createChipSeqSeqTrack()
            createDataFiles(seqTrack)
        }

        when:
        execute()

        then:
        checkThatWorkflowWasSuccessful(seqTrack)
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
        execute()

        then:
        checkThatWorkflowWasSuccessful(seqTrack)
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
            FileType fileType = createFileType()
            Sample sample = createSample()
            seqTracks = (1..3).collect {
                String fileNameR1 = "${it}_${fastqR1Filename}"
                String fileNameR2 = "${it}_${fastqR2Filename}"

                File softLinkFastqR1Filepath = new File("${ftpDir}/${fileNameR1}")
                File softLinkFastqR2Filepath = new File("${ftpDir}/${fileNameR2}")

                linkFileUtils.createAndValidateLinks([
                        (new File(fastqR1Filepath)): softLinkFastqR1Filepath,
                        (new File(fastqR2Filepath)): softLinkFastqR2Filepath,
                ], realm)

                SeqTrack seqTrack = createSeqTrack([
                        run                : run,
                        seqType            : seqType,
                        sample             : sample,
                        singleCellWellLabel: "well_${it}",
                ])

                createDataFile(seqTrack, 1, fileNameR1, softLinkFastqR1Filepath.absolutePath, fileType)
                createDataFile(seqTrack, 2, fileNameR2, softLinkFastqR2Filepath.absolutePath, fileType)
                return seqTrack
            }
        }

        when:
        execute(seqTracks.size())

        then:
        seqTracks.each { SeqTrack seqTrack ->
            checkThatWorkflowWasSuccessful(seqTrack)
            checkWellBasedLinksAreCreatedSuccessful(seqTrack)
        }
        checkMappingFileAreCreatedSuccessful(seqTracks)
    }

    void "test DataInstallation with FastTrack"() {
        given:
        SeqTrack seqTrack
        SessionUtils.withNewSession {
            seqTrack = createWholeGenomeSetup()
            updateProcessingPriorityToFastrack()
        }

        when:
        execute()

        then:
        checkThatWorkflowWasSuccessful(seqTrack)
    }

    protected void checkThatWorkflowWasSuccessful(SeqTrack seqTrack) {
        SessionUtils.withNewSession {
            seqTrack.refresh()
            assert seqTrack.dataInstallationState == SeqTrack.DataProcessingState.FINISHED
            assert SeqTrack.DataProcessingState.NOT_STARTED == seqTrack.fastqcState

            seqTrack.dataFiles.collectMany {
                it.refresh()
                assert it.fileExists
                assert it.fileLinked
                assert it.fileSize > 0
                [
                        lsdfFilesService.getFileFinalPath(it),
                        lsdfFilesService.getFileViewByPidPath(it),
                ]
            }.each {
                assert new File(it).exists()
            }
        }
    }

    protected void checkWellBasedLinksAreCreatedSuccessful(SeqTrack seqTrack) {
        SessionUtils.withNewSession {
            List<DataFile> dataFiles = seqTrack.dataFiles

            //check links
            dataFiles.collect {
                lsdfFilesService.getWellAllFileViewByPidPath(it)
            }.each {
                assert new File(it).exists()
            }
        }
    }

    protected void checkMappingFileAreCreatedSuccessful(List<SeqTrack> seqTracks) {
        SessionUtils.withNewSession {
            List<DataFile> dataFiles = seqTracks*.dataFiles.flatten()

            //check mapping file exists
            Path mappingFile = CollectionUtils.exactlyOneElement(dataFiles.collect {
                singleCellService.singleCellMappingFile(it)
            }.unique())
            assert Files.exists(mappingFile)

            //check mappingFileContext
            String mappingFileContent = mappingFile.text
            assert mappingFileContent.split('\n').size() == 2 * seqTracks.size()
            dataFiles.each {
                assert mappingFileContent.contains(singleCellService.mappingEntry(it))
            }
        }
    }

    private void createDataFiles(SeqTrack seqTrack) {
        FileType fileType = createFileType()
        createDataFile(seqTrack, 1, fastqR1Filename, fastqR1Filepath, fileType)
        createDataFile(seqTrack, 2, fastqR2Filename, fastqR2Filepath, fileType)
    }

    protected SeqTrack createWholeGenomeSetup(boolean linkedExternally = false) {
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()
        SeqTrack seqTrack = createSeqTrack([seqType: seqType, linkedExternally: linkedExternally])
        createDataFiles(seqTrack)
        return seqTrack
    }

    @Override
    List<String> getWorkflowScripts() {
        return ["scripts/workflows/DataInstallationWorkflow.groovy"]
    }

    @Override
    Duration getTimeout() {
        return Duration.ofMinutes(30)
    }
}
