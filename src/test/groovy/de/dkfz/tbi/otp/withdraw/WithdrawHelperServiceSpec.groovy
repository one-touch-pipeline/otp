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
package de.dkfz.tbi.otp.withdraw

import grails.test.hibernate.HibernateSpec
import spock.lang.TempDir
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.domainFactory.FastqcDomainFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.AlignmentPipelineFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.filestore.FilestoreService
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun

import java.nio.file.*
import java.nio.file.attribute.PosixFilePermission

class WithdrawHelperServiceSpec extends HibernateSpec implements FastqcDomainFactory, WorkflowSystemDomainFactory {

    private static final List<String> PATH_LIST1 = ['/tmp'].asImmutable()
    private static final List<String> PATH_LIST2 = ['/tmp2'].asImmutable()
    private static final List<String> PATH_LIST_TOGETHER = [
            PATH_LIST1,
            PATH_LIST2,
    ].flatten().asImmutable()

    @TempDir
    Path tempDir

    @Override
    List<Class> getDomainClasses() {
        return [
                DataFile,
                FastqcProcessedFile,
                MergingWorkPackage,
        ]
    }

    @Unroll
    void "createOverviewSummary, when deleteBamFile is #deleteBamFile and deleteAnalysis is #deleteAnalysis, then create expected text"() {
        SeqTrack seqTrack1 = Mock(SeqTrack)
        SeqTrack seqTrack2 = Mock(SeqTrack)
        RoddyBamFile roddyBamFile1 = Mock(RoddyBamFile)
        RoddyBamFile roddyBamFile2 = Mock(RoddyBamFile)
        SingleCellBamFile singleCellBamFile1 = Mock(SingleCellBamFile)
        SingleCellBamFile singleCellBamFile2 = Mock(SingleCellBamFile)
        BamFilePairAnalysis analysis1 = Mock(BamFilePairAnalysis)
        BamFilePairAnalysis analysis2 = Mock(BamFilePairAnalysis)

        WithdrawHelperService service = new WithdrawHelperService([
                withdrawDisplayDomainService: Mock(WithdrawDisplayDomainService) {
                    1 * seqTrackInfo(seqTrack1) >> "seqTrack1"
                    1 * seqTrackInfo(seqTrack2) >> "seqTrack2"
                    1 * bamFileInfo(roddyBamFile1) >> "roddyBamFile1"
                    1 * bamFileInfo(roddyBamFile2) >> "roddyBamFile2"
                    1 * bamFileInfo(singleCellBamFile1) >> "singleCellBamFile1"
                    1 * bamFileInfo(singleCellBamFile2) >> "singleCellBamFile2"
                    1 * analysisInfo(analysis1) >> "analysis1"
                    1 * analysisInfo(analysis2) >> "analysis2"
                    0 * _
                }
        ])

        WithdrawStateHolder holder = new WithdrawStateHolder([
                withdrawParameters: new WithdrawParameters([
                        seqTracksWithComments: [
                                new SeqTrackWithComment(seqTrack1, "withdrawnComment1\nover multiple Lines"),
                                new SeqTrackWithComment(seqTrack2, "withdrawnComment2"),
                        ],
                        deleteBamFile        : deleteBamFile,
                        deleteAnalysis       : deleteAnalysis,
                ]),
                bamFiles: [
                        roddyBamFile1,
                        roddyBamFile2,
                        singleCellBamFile1,
                        singleCellBamFile2,
                ],
                analysis          : [
                        analysis1,
                        analysis2,
                ],
        ])

        String expected = [
                "Withdraw summary",
                WithdrawHelperService.TRIM_LINE,
                "Withdrawing 2 lanes",
                "- seqTrack1\twith comment: \'withdrawnComment1\nover multiple Lines\'",
                "- seqTrack2\twith comment: \'withdrawnComment2\'",
                WithdrawHelperService.TRIM_LINE,
                "${deleteBamFileText} 4 bam file(s)",
                "- roddyBamFile1",
                "- roddyBamFile2",
                "- singleCellBamFile1",
                "- singleCellBamFile2",
                WithdrawHelperService.TRIM_LINE,
                "${deleteAnalysisText} 2 analysis",
                "- analysis1",
                "- analysis2",
                WithdrawHelperService.TRIM_LINE,
        ].join('\n')

        when:
        service.createOverviewSummary(holder)
        String simplified = holder.summary*.trim().findAll().join('\n')

        then:
        simplified == expected

        where:
        deleteBamFile | deleteAnalysis || deleteBamFileText | deleteAnalysisText
        true          | true            | "Deleting"        | "Deleting"
        true          | false           | "Deleting"        | "Deleting"
        false         | true            | "Withdrawing"     | "Deleting"
        false         | false           | "Withdrawing"     | "Withdrawing"
    }

    void "checkNonExistingDataFiles, when dataFiles exist, then put them not to the summary"() {
        given:
        WithdrawHelperService service = new WithdrawHelperService()

        DataFile dataFile = createDataFile([
                fileExists: true
        ])

        WithdrawStateHolder holder = new WithdrawStateHolder([
                withdrawParameters: new WithdrawParameters([
                        seqTracksWithComments: [
                                new SeqTrackWithComment(dataFile.seqTrack, ""),
                        ],
                ]),
        ])

        when:
        service.checkNonExistingDataFiles(holder)

        then:
        holder.summary.empty
    }

    void "checkNonExistingDataFiles, when dataFiles does not exist and stopOnMissingFiles is set to false, then put them to the summary"() {
        given:
        DataFile dataFile = createDataFile([
                fileExists: false
        ])

        WithdrawHelperService service = new WithdrawHelperService([
                withdrawDisplayDomainService: Mock(WithdrawDisplayDomainService) {
                    1 * dataFileInfo(dataFile) >> "datafile"
                    0 * _
                }
        ])

        WithdrawStateHolder holder = new WithdrawStateHolder([
                withdrawParameters: new WithdrawParameters([
                        seqTracksWithComments: [
                                new SeqTrackWithComment(dataFile.seqTrack, ""),
                        ],
                        stopOnMissingFiles   : false,
                ]),
        ])

        when:
        service.checkNonExistingDataFiles(holder)

        then:
        holder.summary.size() == 3
        holder.summary[1].contains("datafile")
        holder.summary[2].contains(WithdrawHelperService.NOTE_IGNORE_MISSING_FILES)
    }

    void "checkNonExistingDataFiles, when dataFiles does not exist and stopOnMissingFiles is set to true, then throw exception containing them"() {
        given:
        DataFile dataFile = createDataFile([
                fileExists: false
        ])

        WithdrawHelperService service = new WithdrawHelperService([
                withdrawDisplayDomainService: Mock(WithdrawDisplayDomainService) {
                    1 * dataFileInfo(dataFile) >> "datafile"
                    0 * _
                }
        ])

        WithdrawStateHolder holder = new WithdrawStateHolder([
                withdrawParameters: new WithdrawParameters([
                        seqTracksWithComments: [
                                new SeqTrackWithComment(dataFile.seqTrack, "")
                        ],
                        stopOnMissingFiles   : true,
                ]),
        ])

        when:
        service.checkNonExistingDataFiles(holder)

        then:
        WithdrawnException e = thrown()
        e.message.contains("datafile")
    }

    void "checkForAlreadyWithdrawnDatafiles, when no dataFiles already withdrawn, then put them not to the summary"() {
        given:
        WithdrawHelperService service = new WithdrawHelperService()

        DataFile dataFile = createDataFile([
                fileWithdrawn: false,
        ])

        WithdrawStateHolder holder = new WithdrawStateHolder([
                withdrawParameters: new WithdrawParameters([
                        seqTracksWithComments: [
                                new SeqTrackWithComment(dataFile.seqTrack, ""),
                        ],
                ]),
        ])

        when:
        service.checkForAlreadyWithdrawnDatafiles(holder)

        then:
        holder.summary.empty
    }

    void "checkForAlreadyWithdrawnDatafiles, when dataFiles already withdrawn and stopOnAlreadyWithdrawnData is set to false, then put them to the summary"() {
        given:
        DataFile dataFile = createDataFile([
                fileWithdrawn: true,
        ])

        WithdrawHelperService service = new WithdrawHelperService([
                withdrawDisplayDomainService: Mock(WithdrawDisplayDomainService) {
                    1 * dataFileInfo(dataFile, true) >> "datafile"
                    0 * _
                }
        ])

        WithdrawStateHolder holder = new WithdrawStateHolder([
                withdrawParameters: new WithdrawParameters([
                        seqTracksWithComments     : [
                                new SeqTrackWithComment(dataFile.seqTrack, ""),
                        ],
                        stopOnAlreadyWithdrawnData: false,
                ]),
        ])

        when:
        service.checkForAlreadyWithdrawnDatafiles(holder)

        then:
        holder.summary.size() == 3
        holder.summary[1].contains("datafile")
        holder.summary[2].contains(WithdrawHelperService.NOTE_IGNORE_ALREADY_WITHDRAWN)
    }

    void "checkForAlreadyWithdrawnDatafiles, when dataFiles already withdrawn and stopOnAlreadyWithdrawnData is set to true, then throw exception containing them"() {
        given:
        DataFile dataFile = createDataFile([
                fileWithdrawn: true,
        ])

        WithdrawHelperService service = new WithdrawHelperService([
                withdrawDisplayDomainService: Mock(WithdrawDisplayDomainService) {
                    1 * dataFileInfo(dataFile, true) >> "datafile"
                    0 * _
                }
        ])

        WithdrawStateHolder holder = new WithdrawStateHolder([
                withdrawParameters: new WithdrawParameters([
                        seqTracksWithComments     : [
                                new SeqTrackWithComment(dataFile.seqTrack, ""),
                        ],
                        stopOnAlreadyWithdrawnData: true,
                ]),
        ])

        when:
        service.checkForAlreadyWithdrawnDatafiles(holder)

        then:
        WithdrawnException e = thrown()
        e.message.contains("datafile")
    }

    @Unroll
    void "handleAnalysis, when deleteAnalysis is #deleteAnalysis and deleteBamFile is #deleteBamFile, then call expected method of withdrawAnalysisService and list directory in correct property"() {
        given:
        BamFilePairAnalysis analysis = Mock(BamFilePairAnalysis)

        WithdrawHelperService service = new WithdrawHelperService()

        service.withdrawAnalysisService = Mock(WithdrawAnalysisService) {
            countWithdraw * withdrawObjects([analysis])
            countDelete * deleteObjects([analysis])
            1 * collectPaths([analysis]) >> PATH_LIST1
        }

        WithdrawStateHolder holder = new WithdrawStateHolder([
                withdrawParameters: new WithdrawParameters([
                        deleteAnalysis: deleteAnalysis,
                        deleteBamFile : deleteBamFile,
                ]),
                analysis          : [analysis,],
        ])

        when:
        service.handleAnalysis(holder)

        then:
        holder.pathsToChangeGroup == pathsToChangeGroup
        holder.pathsToDelete == pathsToDelete

        where:
        deleteBamFile | deleteAnalysis || countWithdraw | countDelete | pathsToChangeGroup | pathsToDelete
        false         | false          || 1             | 0           | PATH_LIST1         | []
        false         | true           || 0             | 1           | []                 | PATH_LIST1
        true          | false          || 0             | 1           | []                 | PATH_LIST1
        true          | true           || 0             | 1           | []                 | PATH_LIST1
    }

    @Unroll
    void "handleBamFiles, when deleteBamFile is #deleteBamFile, then call expected method of withdrawBamFileService and list directory in correct property"() {
        given:
        RoddyBamFile roddyBamFile = new RoddyBamFile()
        SingleCellBamFile singleCellBamFile = new SingleCellBamFile()

        WithdrawHelperService service = new WithdrawHelperService()

        RoddyBamFileWithdrawService roddyBamFileWithdrawService = Mock(RoddyBamFileWithdrawService) {
            countWithdraw * withdrawObjects([roddyBamFile])
            countDelete * deleteObjects([roddyBamFile])
            1 * collectPaths([roddyBamFile]) >> PATH_LIST1
        }
        CellRangerBamFileWithdrawService cellRangerBamFileWithdrawService = Mock(CellRangerBamFileWithdrawService) {
            countWithdraw * withdrawObjects([singleCellBamFile])
            countDelete * deleteObjects([singleCellBamFile])
            1 * collectPaths([singleCellBamFile]) >> PATH_LIST2
        }

        Map<WithdrawBamFileService, List<AbstractBamFile>> bamFileMap = [
                (roddyBamFileWithdrawService)     : [roddyBamFile],
                (cellRangerBamFileWithdrawService): [singleCellBamFile],
        ]

        WithdrawStateHolder holder = new WithdrawStateHolder([
                withdrawParameters: new WithdrawParameters([
                        deleteBamFile: deleteBamFile,
                ]),
        ])

        when:
        service.handleBamFiles(holder, bamFileMap)

        then:
        holder.pathsToChangeGroup == pathsToChangeGroup
        holder.pathsToDelete == pathsToDelete

        where:
        deleteBamFile || countWithdraw | countDelete | pathsToChangeGroup | pathsToDelete
        false         || 1             | 0           | PATH_LIST_TOGETHER | []
        true          || 0             | 1           | []                 | PATH_LIST_TOGETHER
    }

    void "handleDataFiles, when datafiles given, then collect the needed path to the correct list"() {
        given:
        String withdrawnCommentNormal = "withdrawnComment \nover\nmultiple lines"
        String withdrawnCommentWithdrawn = "withdrawnComment\nfor withdrawnDataFile"
        String withdrawnCommentSingleCell = "withdrawnComment\nfor singleCellDataFile"
        final Path finalPathNormal = CreateFileHelper.createFile(tempDir.resolve("finalNormal"))
        final Path finalPathSingleCell = CreateFileHelper.createFile(tempDir.resolve("finalSingleCell"))
        final Path uuidPathNormal = CreateFileHelper.createFile(tempDir.resolve("uuidNormal"))
        final Path uuidPathSingleCell = CreateFileHelper.createFile(tempDir.resolve("uuidSingleCell"))
        final String viewByPidPathNormal = "/tmp/viewByPidNormal"
        final String viewByPidPathSingleCell = "/tmp/viewByPidSingleCell"
        final String wellPathSingleCell = "/tmp/wellSingleCell"
        final Path fastqcPathNormal = CreateFileHelper.createFile(tempDir.resolve("fastqcNormal"))
        final Path fastqcPathSingleCell = CreateFileHelper.createFile(tempDir.resolve("fastqcSingleCell"))
        final Path finalMd5sumNormal = CreateFileHelper.createFile(tempDir.resolve("finalMd5sum"))
        final Path finalMd5sumSingleCell = CreateFileHelper.createFile(tempDir.resolve("finalMd5sumSingleCell"))
        final WorkflowRun fastqcRun = createWorkflowRun(workFolder: createWorkFolder())
        final WorkflowRun fastqcSingleCellRun = createWorkflowRun(workFolder: createWorkFolder())

        DataFile dataFile = createDataFile()
        FastqcProcessedFile fastqcProcessedFile = createFastqcProcessedFile([
                dataFile: dataFile,
                workflowArtefact: createWorkflowArtefact(producedBy: fastqcRun),
        ])
        DataFile withdrawnDataFile = createDataFile([fileWithdrawn: true])
        DataFile singleCellDataFile = createDataFile([
                seqTrack: createSeqTrack([
                        seqType            : createSeqType([
                                singleCell: true,
                        ]),
                        singleCellWellLabel: 'someLabel',
                ])
        ])
        FastqcProcessedFile singleCellFastqcProcessedFile = createFastqcProcessedFile([
                dataFile: singleCellDataFile,
                workflowArtefact: createWorkflowArtefact(producedBy: fastqcSingleCellRun),
        ])
        MergingWorkPackage mergingWorkPackage = AlignmentPipelineFactory.RoddyPancanFactoryInstance.INSTANCE.createMergingWorkPackage([
                seqTracks: [dataFile.seqTrack] as Set,
                seqType  : dataFile.seqTrack.seqType,
        ])

        WithdrawHelperService service = new WithdrawHelperService()

        service.filestoreService = Mock(FilestoreService)
        service.lsdfFilesService = Mock(LsdfFilesService)
        service.fastqcDataFilesService = Mock(FastqcDataFilesService)
        WithdrawStateHolder holder = new WithdrawStateHolder([
                withdrawParameters: new WithdrawParameters([
                        seqTracksWithComments: [
                                new SeqTrackWithComment(dataFile.seqTrack, withdrawnCommentNormal),
                                new SeqTrackWithComment(withdrawnDataFile.seqTrack, withdrawnCommentWithdrawn),
                                new SeqTrackWithComment(singleCellDataFile.seqTrack, withdrawnCommentSingleCell),
                        ],
                ]),
        ])
        List<String> pathsToChangeGroup = [
                finalPathNormal.toString(),
                finalMd5sumNormal.toString(),
                tempDir.toString(),
                uuidPathNormal.toString(),
                finalPathSingleCell.toString(),
                finalMd5sumSingleCell.toString(),
                tempDir.toString(),
                uuidPathSingleCell.toString(),
        ]
        List<String> pathsToDelete = [
                viewByPidPathNormal,
                viewByPidPathSingleCell,
                wellPathSingleCell,
        ]

        when:
        service.handleDataFiles(holder)

        then:
        1 * service.lsdfFilesService.getFileFinalPathAsPath(dataFile) >> finalPathNormal
        1 * service.lsdfFilesService.getFileMd5sumFinalPathAsPath(dataFile) >> finalMd5sumNormal
        1 * service.lsdfFilesService.getFileViewByPidPath(dataFile) >> viewByPidPathNormal
        1 * service.lsdfFilesService.getFileFinalPathAsPath(singleCellDataFile) >> finalPathSingleCell
        1 * service.lsdfFilesService.getFileMd5sumFinalPathAsPath(singleCellDataFile) >> finalMd5sumSingleCell
        1 * service.lsdfFilesService.getFileViewByPidPath(singleCellDataFile) >> viewByPidPathSingleCell
        1 * service.lsdfFilesService.getWellAllFileViewByPidPath(singleCellDataFile) >> wellPathSingleCell
        0 * service.lsdfFilesService._

        1 * service.fastqcDataFilesService.fastqcOutputPath(fastqcProcessedFile) >> fastqcPathNormal
        1 * service.fastqcDataFilesService.fastqcOutputPath(singleCellFastqcProcessedFile) >> fastqcPathSingleCell
        0 * service.fastqcDataFilesService._

        1 * service.filestoreService.getWorkFolderPath(fastqcRun) >> uuidPathNormal
        1 * service.filestoreService.getWorkFolderPath(fastqcSingleCellRun) >> uuidPathSingleCell

        and:
        TestCase.assertContainSame(holder.pathsToChangeGroup, pathsToChangeGroup)
        TestCase.assertContainSame(holder.pathsToDelete, pathsToDelete)

        with(dataFile) {
            fileWithdrawn == true
            withdrawnDate != null
            withdrawnComment == withdrawnCommentNormal
        }

        with(singleCellDataFile) {
            fileWithdrawn == true
            withdrawnDate != null
            withdrawnComment == withdrawnCommentSingleCell
        }

        with(withdrawnDataFile) {
            fileWithdrawn == true
            withdrawnComment != withdrawnCommentWithdrawn
        }

        mergingWorkPackage.seqTracks.empty
    }

    void "createAndWriteBashScript, if paths given, write expected script"() {
        given:
        String withdrawnGroup = "withdrawnGroup"
        String pathToDelete = "/tmp/file1"
        String pathToChangeGroup = "/tmp/file2"

        String scriptName = 'script.sh'
        File scriptFolder = new File('/tmp/script')
        Path withdrawnScript = scriptFolder.toPath().resolve('withdrawn').resolve(scriptName)
        FileSystem fileSystem = FileSystems.default

        WithdrawHelperService service = new WithdrawHelperService()
        service.processingOptionService = Mock(ProcessingOptionService) {
            1 * findOptionAsString(ProcessingOption.OptionName.WITHDRAWN_UNIX_GROUP) >> withdrawnGroup
            0 * _
        }
        service.configService = new TestConfigService([
                (OtpProperty.PATH_SCRIPTS_OUTPUT): scriptFolder.path,
        ])
        service.fileService = Mock(FileService) {
            1 * toPath(scriptFolder, fileSystem) >> scriptFolder.toPath()
            1 * deleteDirectoryRecursively(withdrawnScript)
            1 * createFileWithContentOnDefaultRealm(withdrawnScript, _, FileService.OWNER_READ_WRITE_GROUP_READ_WRITE_FILE_PERMISSION) >> { Path path, String content, Set<PosixFilePermission> filePermission ->
                assert content.startsWith(FileService.BASH_HEADER)
                assert content.contains("rm --recursive --force --verbose ${pathToDelete}" as String)
                assert content.contains("chgrp --recursive --verbose ${withdrawnGroup} ${pathToChangeGroup}" as String)
            }
            0 * _
        }

        WithdrawStateHolder holder = new WithdrawStateHolder([
                withdrawParameters: new WithdrawParameters([
                        fileName: scriptName,
                ]),
                remoteFileSystem  : fileSystem,
                pathsToDelete     : [pathToDelete],
                pathsToChangeGroup: [pathToChangeGroup],
        ])

        when:
        Path outputFile = service.createAndWriteBashScript(holder)

        then:
        holder.summary.join('\n').contains(outputFile.toString())
    }

    void "createScript, if paths given, create expected script"() {
        given:
        String withdrawnGroup = "withdrawnGroup"

        List<String> pathsToDelete = [
                "/tmp/dir1",
                "/tmp/dir2",
                "/tmp/file1",
        ]

        List<String> pathsToChangeGroup = [
                "/tmp/dir3",
                "/tmp/dir4",
                "/tmp/file3",
        ]

        WithdrawHelperService service = new WithdrawHelperService()
        service.processingOptionService = Mock(ProcessingOptionService) {
            1 * findOptionAsString(ProcessingOption.OptionName.WITHDRAWN_UNIX_GROUP) >> withdrawnGroup
        }

        WithdrawStateHolder holder = new WithdrawStateHolder([
                withdrawParameters: new WithdrawParameters(),
                pathsToDelete     : pathsToDelete,
                pathsToChangeGroup: pathsToChangeGroup,
        ])

        when:
        String script = service.createBashScript(holder)

        then:
        script.startsWith(FileService.BASH_HEADER)

        pathsToDelete.each {
            assert script.contains("rm --recursive --force --verbose ${it}" as String)
        }

        pathsToChangeGroup.each {
            assert script.contains("chgrp --recursive --verbose ${withdrawnGroup} ${it}" as String)
        }
    }
}
