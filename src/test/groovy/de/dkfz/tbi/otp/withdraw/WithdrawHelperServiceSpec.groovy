/*
 * Copyright 2011-2026 The OTP authors
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
import de.dkfz.tbi.otp.filestore.BaseFolder
import de.dkfz.tbi.otp.filestore.FilestoreService
import de.dkfz.tbi.otp.filestore.PathOption
import de.dkfz.tbi.otp.filestore.WorkFolder
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataAllWellFileService
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataViewFileService
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataWorkFileService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.workflowExecution.Workflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowArtefact
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.ArtefactType

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
                RawSequenceFile,
                FastqFile,
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

    void "checkNonExistingRawSequenceFiles, when dataFiles exist, then put them not to the summary"() {
        given:
        WithdrawHelperService service = new WithdrawHelperService()

        RawSequenceFile fastqFile = createFastqFile([
                fileExists: true
        ])

        WithdrawStateHolder holder = new WithdrawStateHolder([
                withdrawParameters: new WithdrawParameters([
                        seqTracksWithComments: [
                                new SeqTrackWithComment(fastqFile.seqTrack, ""),
                        ],
                ]),
        ])

        when:
        service.checkNonExistingRawSequenceFiles(holder)

        then:
        holder.summary.empty
    }

    void "checkNonExistingRawSequenceFiles, when dataFiles does not exist and stopOnMissingFiles is set to false, then put them to the summary"() {
        given:
        RawSequenceFile fastqFile = createFastqFile([
                fileExists: false
        ])

        WithdrawHelperService service = new WithdrawHelperService([
                withdrawDisplayDomainService: Mock(WithdrawDisplayDomainService) {
                    1 * rawSequenceFileInfo(fastqFile) >> "datafile"
                    0 * _
                }
        ])

        WithdrawStateHolder holder = new WithdrawStateHolder([
                withdrawParameters: new WithdrawParameters([
                        seqTracksWithComments: [
                                new SeqTrackWithComment(fastqFile.seqTrack, ""),
                        ],
                        stopOnMissingFiles   : false,
                ]),
        ])

        when:
        service.checkNonExistingRawSequenceFiles(holder)

        then:
        holder.summary.size() == 3
        holder.summary[1].contains("datafile")
        holder.summary[2].contains(WithdrawHelperService.NOTE_IGNORE_MISSING_FILES)
    }

    void "checkNonExistingRawSequenceFiles, when dataFiles does not exist and stopOnMissingFiles is set to true, then throw exception containing them"() {
        given:
        RawSequenceFile fastqFile = createFastqFile([
                fileExists: false
        ])

        WithdrawHelperService service = new WithdrawHelperService([
                withdrawDisplayDomainService: Mock(WithdrawDisplayDomainService) {
                    1 * rawSequenceFileInfo(fastqFile) >> "datafile"
                    0 * _
                }
        ])

        WithdrawStateHolder holder = new WithdrawStateHolder([
                withdrawParameters: new WithdrawParameters([
                        seqTracksWithComments: [
                                new SeqTrackWithComment(fastqFile.seqTrack, "")
                        ],
                        stopOnMissingFiles   : true,
                ]),
        ])

        when:
        service.checkNonExistingRawSequenceFiles(holder)

        then:
        WithdrawnException e = thrown()
        e.message.contains("datafile")
    }

    void "checkForAlreadyWithdrawnRawSequenceFiles, when no dataFiles already withdrawn, then put them not to the summary"() {
        given:
        WithdrawHelperService service = new WithdrawHelperService()

        RawSequenceFile fastqFile = createFastqFile([
                fileWithdrawn: false,
        ])

        WithdrawStateHolder holder = new WithdrawStateHolder([
                withdrawParameters: new WithdrawParameters([
                        seqTracksWithComments: [
                                new SeqTrackWithComment(fastqFile.seqTrack, ""),
                        ],
                ]),
        ])

        when:
        service.checkForAlreadyWithdrawnRawSequenceFiles(holder)

        then:
        holder.summary.empty
    }

    void "checkForAlreadyWithdrawnRawSequenceFiles, when dataFiles already withdrawn and stopOnAlreadyWithdrawnData is set to false, then put them to the summary"() {
        given:
        RawSequenceFile fastqFile = createFastqFile([
                fileWithdrawn: true,
        ])

        WithdrawHelperService service = new WithdrawHelperService([
                withdrawDisplayDomainService: Mock(WithdrawDisplayDomainService) {
                    1 * rawSequenceFileInfo(fastqFile, true) >> "datafile"
                    0 * _
                }
        ])

        WithdrawStateHolder holder = new WithdrawStateHolder([
                withdrawParameters: new WithdrawParameters([
                        seqTracksWithComments     : [
                                new SeqTrackWithComment(fastqFile.seqTrack, ""),
                        ],
                        stopOnAlreadyWithdrawnData: false,
                ]),
        ])

        when:
        service.checkForAlreadyWithdrawnRawSequenceFiles(holder)

        then:
        holder.summary.size() == 3
        holder.summary[1].contains("datafile")
        holder.summary[2].contains(WithdrawHelperService.NOTE_IGNORE_ALREADY_WITHDRAWN)
    }

    void "checkForAlreadyWithdrawnRawSequenceFiles, when dataFiles already withdrawn and stopOnAlreadyWithdrawnData is set to true, then throw exception containing them"() {
        given:
        RawSequenceFile fastqFile = createFastqFile([
                fileWithdrawn: true,
        ])

        WithdrawHelperService service = new WithdrawHelperService([
                withdrawDisplayDomainService: Mock(WithdrawDisplayDomainService) {
                    1 * rawSequenceFileInfo(fastqFile, true) >> "datafile"
                    0 * _
                }
        ])

        WithdrawStateHolder holder = new WithdrawStateHolder([
                withdrawParameters: new WithdrawParameters([
                        seqTracksWithComments     : [
                                new SeqTrackWithComment(fastqFile.seqTrack, ""),
                        ],
                        stopOnAlreadyWithdrawnData: true,
                ]),
        ])

        when:
        service.checkForAlreadyWithdrawnRawSequenceFiles(holder)

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

        Map<AbstractWithdrawBamFileService, List<AbstractBamFile>> bamFileMap = [
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

    void "handleRawSequenceFiles, when datafiles given, then collect the needed path to the correct list"() {
        given:
        String withdrawnCommentNormal = "withdrawnComment \nover\nmultiple lines"
        String withdrawnCommentWithdrawn = "withdrawnComment\nfor withdrawnDataFile"
        String withdrawnCommentSingleCell = "withdrawnComment\nfor singleCellDataFile"
        final Path finalPathNormal = CreateFileHelper.createFile(tempDir.resolve("finalNormal"))
        final Path finalPathSingleCell = CreateFileHelper.createFile(tempDir.resolve("finalSingleCell"))
        final Path uuidPath = CreateFileHelper.createFile(tempDir.resolve("uuid"))
        final String viewByPidPathNormal = "${File.separator}tmp${File.separator}viewByPidNormal"
        final String viewByPidPathSingleCell = "${File.separator}tmp${File.separator}viewByPidSingleCell"
        final String wellPathSingleCell = "${File.separator}tmp${File.separator}wellSingleCell"
        final Path fastqcPath = tempDir.resolve("fastqc")
        Files.createDirectories(fastqcPath)
        final Path finalMd5sumNormal = CreateFileHelper.createFile(tempDir.resolve("finalMd5sum"))
        final Path finalMd5sumSingleCell = CreateFileHelper.createFile(tempDir.resolve("finalMd5sumSingleCell"))

        final Path fastqcZip = CreateFileHelper.createFile(fastqcPath.resolve("fastqc.zip"))
        final Path fastqcMd5sum = CreateFileHelper.createFile(fastqcPath.resolve("fastqc.zip.md5sum"))
        final Path fastqcHtml = CreateFileHelper.createFile(fastqcPath.resolve("fastqc.html"))
        final Path singleCellFastqcZip = CreateFileHelper.createFile(fastqcPath.resolve("singlecell_fastqc.zip"))
        final Path singleCellFastqcMd5sum = CreateFileHelper.createFile(fastqcPath.resolve("singlecell_fastqc.zip.md5sum"))
        final Path singleCellFastqcHtml = CreateFileHelper.createFile(fastqcPath.resolve("singlecell_fastqc.html"))

        RawSequenceFile fastqFile = createFastqFile()
        FastqcProcessedFile fastqcProcessedFile = createFastqcProcessedFile([
                sequenceFile: fastqFile,
                workflowArtefact: null,  // Old workflow - no artefact
        ])
        RawSequenceFile withdrawnFastqFile = createFastqFile([fileWithdrawn: true])
        RawSequenceFile singleCellFastqFile = createSequenceDataFile([
                seqTrack: createSeqTrack([
                        seqType            : createSeqType([
                                singleCell: true,
                        ]),
                        singleCellWellLabel: 'someLabel',
                ])
        ])
        FastqcProcessedFile singleCellFastqcProcessedFile = createFastqcProcessedFile([
                sequenceFile: singleCellFastqFile,
                workflowArtefact: null,  // Old workflow - no artefact
        ])
        MergingWorkPackage mergingWorkPackage = AlignmentPipelineFactory.RoddyPanCancerFactoryInstance.INSTANCE.createMergingWorkPackage([
                seqTracks: [fastqFile.seqTrack] as Set,
                seqType  : fastqFile.seqTrack.seqType,
        ])

        WithdrawHelperService service = new WithdrawHelperService()

        service.filestoreService = Mock(FilestoreService)
        service.fastqcDataFilesService = Mock(FastqcDataFilesService)
        service.rawSequenceDataWorkFileService = Mock(RawSequenceDataWorkFileService)
        service.rawSequenceDataViewFileService = Mock(RawSequenceDataViewFileService)
        service.rawSequenceDataAllWellFileService = Mock(RawSequenceDataAllWellFileService)
        WithdrawStateHolder holder = new WithdrawStateHolder([
                withdrawParameters: new WithdrawParameters([
                        seqTracksWithComments: [
                                new SeqTrackWithComment(fastqFile.seqTrack, withdrawnCommentNormal),
                                new SeqTrackWithComment(withdrawnFastqFile.seqTrack, withdrawnCommentWithdrawn),
                                new SeqTrackWithComment(singleCellFastqFile.seqTrack, withdrawnCommentSingleCell),
                        ],
                ]),
        ])
        List<String> pathsToChangeGroup = [
                finalPathNormal.toString(),
                finalMd5sumNormal.toString(),
                finalPathSingleCell.toString(),
                finalMd5sumSingleCell.toString(),
                fastqcPath.toString(),
                uuidPath.toString(),
        ]
        List<String> pathsToDelete = [
                viewByPidPathNormal,
                viewByPidPathSingleCell,
                wellPathSingleCell,
        ]

        when:
        service.handleRawSequenceFiles(holder)

        then:
        1 * service.rawSequenceDataWorkFileService.getFilePath(fastqFile) >> finalPathNormal
        1 * service.rawSequenceDataWorkFileService.getMd5sumPath(fastqFile) >> finalMd5sumNormal
        1 * service.rawSequenceDataViewFileService.getFilePath(fastqFile) >> Paths.get(viewByPidPathNormal)
        1 * service.rawSequenceDataWorkFileService.getFilePath(singleCellFastqFile) >> finalPathSingleCell
        1 * service.rawSequenceDataWorkFileService.getMd5sumPath(singleCellFastqFile) >> finalMd5sumSingleCell
        1 * service.rawSequenceDataViewFileService.getFilePath(singleCellFastqFile) >> Paths.get(viewByPidPathSingleCell)
        1 * service.rawSequenceDataAllWellFileService.getFilePath(singleCellFastqFile) >> Paths.get(wellPathSingleCell)

        1 * service.fastqcDataFilesService.fastqcOutputDirectory(fastqcProcessedFile) >> fastqcPath
        1 * service.fastqcDataFilesService.fastqcOutputDirectory(fastqcProcessedFile, PathOption.REAL_PATH) >> uuidPath
        1 * service.fastqcDataFilesService.fastqcOutputDirectory(singleCellFastqcProcessedFile) >> fastqcPath
        1 * service.fastqcDataFilesService.fastqcOutputDirectory(singleCellFastqcProcessedFile, PathOption.REAL_PATH) >> uuidPath

        1 * service.fastqcDataFilesService.fastqcOutputPath(fastqcProcessedFile) >> fastqcZip
        1 * service.fastqcDataFilesService.fastqcOutputMd5sumPath(fastqcProcessedFile) >> fastqcMd5sum
        1 * service.fastqcDataFilesService.fastqcHtmlPath(fastqcProcessedFile) >> fastqcHtml
        1 * service.fastqcDataFilesService.fastqcOutputPath(singleCellFastqcProcessedFile) >> singleCellFastqcZip
        1 * service.fastqcDataFilesService.fastqcOutputMd5sumPath(singleCellFastqcProcessedFile) >> singleCellFastqcMd5sum
        1 * service.fastqcDataFilesService.fastqcHtmlPath(singleCellFastqcProcessedFile) >> singleCellFastqcHtml

        0 * service.fastqcDataFilesService._

        and:
        TestCase.assertContainSame(holder.pathsToChangeGroup, pathsToChangeGroup)
        TestCase.assertContainSame(holder.pathsToDelete, pathsToDelete)

        Set<String> expectedPermissionPaths = [
                finalPathNormal.toString(),
                finalMd5sumNormal.toString(),
                finalPathSingleCell.toString(),
                finalMd5sumSingleCell.toString(),
                fastqcZip.toString(),
                fastqcMd5sum.toString(),
                fastqcHtml.toString(),
                singleCellFastqcZip.toString(),
                singleCellFastqcMd5sum.toString(),
                singleCellFastqcHtml.toString(),
        ] as Set
        TestCase.assertContainSame(holder.pathsToChangePermissions, expectedPermissionPaths)

        with(fastqFile) {
            assert fileWithdrawn
            assert withdrawnDate != null
            assert withdrawnComment == withdrawnCommentNormal
        }

        with(singleCellFastqFile) {
            assert fileWithdrawn
            assert withdrawnDate != null
            assert withdrawnComment == withdrawnCommentSingleCell
        }

        with(withdrawnFastqFile) {
            assert fileWithdrawn
            assert withdrawnComment != withdrawnCommentWithdrawn
        }

        mergingWorkPackage.seqTracks.empty
    }

    void "createAndWriteBashScript, if paths given, write expected script"() {
        given:
        String withdrawnGroup = "withdrawnGroup"
        String pathToDelete = "/tmp/file1"
        String pathToChangeGroup = "/tmp/file2"
        String pathToChangePermission = "/tmp/fastq1.gz"

        String scriptName = 'script.sh'
        File scriptFolder = new File('/tmp/script')
        Path withdrawnScript = scriptFolder.toPath().resolve('withdrawn').resolve(scriptName)
        FileSystem fileSystem = FileSystems.default

        WithdrawHelperService service = new WithdrawHelperService()
        service.processingOptionService = Mock(ProcessingOptionService) {
            1 * findOptionAsString(ProcessingOption.OptionName.WITHDRAWN_UNIX_GROUP) >> withdrawnGroup
            _ * findOptionAsString(ProcessingOption.OptionName.OTP_USER_LINUX_GROUP) >> 'test-group'
            0 * _
        }
        service.configService = new TestConfigService([
                (OtpProperty.PATH_SCRIPTS_OUTPUT): scriptFolder.path,
        ])
        service.fileService = Mock(FileService) {
            1 * toPath(scriptFolder, fileSystem) >> scriptFolder.toPath()
            1 * deleteDirectoryRecursively(withdrawnScript)
            1 * createFileWithContent(withdrawnScript, _, _, FileService.OWNER_READ_WRITE_GROUP_READ_WRITE_FILE_PERMISSION) >> { Path path, String content, String unixGroup, Set<PosixFilePermission> filePermission ->
                assert content.startsWith(FileService.BASH_HEADER)
                assert content.contains("rm --recursive --force --verbose ${pathToDelete}" as String)
                assert content.contains("chgrp --recursive --verbose ${withdrawnGroup} ${pathToChangeGroup}" as String)
                assert content.contains("chmod 440 ${pathToChangePermission}" as String)
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
                pathsToChangePermissions: [pathToChangePermission] as Set,
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

        Set<String> pathsToChangePermissions = [
                "/tmp/fastq1.gz",
                "/tmp/fastq2.gz.md5sum",
                "/tmp/fastqc.zip",
        ]

        WithdrawHelperService service = new WithdrawHelperService()
        service.processingOptionService = Mock(ProcessingOptionService) {
            1 * findOptionAsString(ProcessingOption.OptionName.WITHDRAWN_UNIX_GROUP) >> withdrawnGroup
            _ * findOptionAsString(ProcessingOption.OptionName.OTP_USER_LINUX_GROUP) >> 'test-group'
        }

        WithdrawStateHolder holder = new WithdrawStateHolder([
                withdrawParameters: new WithdrawParameters(),
                pathsToDelete     : pathsToDelete,
                pathsToChangeGroup: pathsToChangeGroup,
                pathsToChangePermissions: pathsToChangePermissions,
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

        pathsToChangePermissions.each {
            assert script.contains("chmod 440 ${it}" as String)
        }
    }

    void "tests handleRawSequenceFiles, when old workflow data given, then collect paths for permission changes"() {
        given:
        RawSequenceFile oldWorkflowFile = createFastqFile()
        oldWorkflowFile.seqTrack.workflowArtefact = null // Simulate old workflow data

        final Path oldFilePath = CreateFileHelper.createFile(tempDir.resolve("oldFastq.gz"))
        final Path oldMd5Path = CreateFileHelper.createFile(tempDir.resolve("oldFastq.gz.md5sum"))

        WithdrawHelperService service = new WithdrawHelperService()
        service.rawSequenceDataWorkFileService = Mock(RawSequenceDataWorkFileService) {
            1 * getFilePath(oldWorkflowFile) >> oldFilePath
            1 * getMd5sumPath(oldWorkflowFile) >> oldMd5Path
        }
        service.rawSequenceDataViewFileService = Mock(RawSequenceDataViewFileService) {
            1 * getFilePath(oldWorkflowFile) >> Paths.get("/tmp/view")
        }
        service.fastqcDataFilesService = Mock(FastqcDataFilesService)

        WithdrawStateHolder holder = new WithdrawStateHolder([
                withdrawParameters: new WithdrawParameters([
                        seqTracksWithComments: [new SeqTrackWithComment(oldWorkflowFile.seqTrack, "comment")],
                ]),
        ])

        when:
        service.handleRawSequenceFiles(holder)

        then:
        holder.pathsToChangePermissions.contains(oldFilePath.toString())
        holder.pathsToChangePermissions.contains(oldMd5Path.toString())
        holder.pathsToChangeGroup.size() > 0
    }

    void "tests handleRawSequenceFiles, when new workflow data given, then skip permission changes"() {
        given:
        RawSequenceFile newWorkflowFile = createFastqFile()
        WorkflowRun workflowRun = createWorkflowRun(workFolder: createWorkFolder())
        newWorkflowFile.seqTrack.workflowArtefact = createWorkflowArtefact(producedBy: workflowRun)

        final Path newFilePath = CreateFileHelper.createFile(tempDir.resolve("newFastq.gz"))
        final Path newMd5Path = CreateFileHelper.createFile(tempDir.resolve("newFastq.gz.md5sum"))

        WithdrawHelperService service = new WithdrawHelperService()
        service.rawSequenceDataWorkFileService = Mock(RawSequenceDataWorkFileService) {
            1 * getFilePath(newWorkflowFile) >> newFilePath
            1 * getMd5sumPath(newWorkflowFile) >> newMd5Path
        }
        service.rawSequenceDataViewFileService = Mock(RawSequenceDataViewFileService) {
            1 * getFilePath(newWorkflowFile) >> Paths.get("/tmp/view")
        }
        service.fastqcDataFilesService = Mock(FastqcDataFilesService)

        WithdrawStateHolder holder = new WithdrawStateHolder([
                withdrawParameters: new WithdrawParameters([
                        seqTracksWithComments: [new SeqTrackWithComment(newWorkflowFile.seqTrack, "comment")],
                ]),
        ])

        when:
        service.handleRawSequenceFiles(holder)

        then:
        holder.pathsToChangePermissions.empty
        holder.pathsToChangeGroup.size() > 0
    }

    void "handleRawSequenceFiles, when fastq new workflow but fastqc old workflow, then change fastqc permissions only"() {
        given:
        RawSequenceFile newWorkflowFastqFile = createFastqFile([fileWithdrawn: false])
        BaseFolder baseFolder = new BaseFolder(
                path: "/tmp/test-base-folder-${System.currentTimeMillis()}",
                writable: true
        )
        baseFolder.save(flush: true)
        WorkFolder workFolder = new WorkFolder(
                baseFolder: baseFolder,
                uuid: UUID.randomUUID(),
                size: 0
        )
        workFolder.save(flush: true)

        Workflow workflow = new Workflow(
                name: "TestWorkflow-${System.currentTimeMillis()}",
                enabled: true,
                maxParallelWorkflows: 1
        )
        workflow.save(flush: true)

        WorkflowRun workflowRun = new WorkflowRun(
                workFolder: workFolder,
                state: WorkflowRun.State.SUCCESS,
                displayName: "Test Workflow Run",
                shortDisplayName: "TestRun",
                project: newWorkflowFastqFile.project,
                workflow: workflow,
                priority: newWorkflowFastqFile.seqTrack.sample.individual.project.processingPriority
        )
        workflowRun.save(flush: true)

        WorkflowArtefact workflowArtefact = new WorkflowArtefact(
                producedBy: workflowRun,
                state: WorkflowArtefact.State.SUCCESS,
                displayName: "Test Workflow Artefact",
                artefactType: ArtefactType.FASTQ,
                outputRole: "test"
        )
        workflowArtefact.save(flush: true)

        newWorkflowFastqFile.seqTrack.workflowArtefact = workflowArtefact
        newWorkflowFastqFile.seqTrack.save(flush: true)

        FastqcProcessedFile oldWorkflowFastqcFile = createFastqcProcessedFile([
                sequenceFile: newWorkflowFastqFile,
                workflowArtefact: null,
        ])

        final Path newFastqPath = CreateFileHelper.createFile(tempDir.resolve("newFastq.gz"))
        final Path newMd5Path = CreateFileHelper.createFile(tempDir.resolve("newFastq.gz.md5sum"))

        final Path fastqcDir = tempDir.resolve("fastqc")
        Files.createDirectories(fastqcDir)
        final Path oldFastqcZip = CreateFileHelper.createFile(fastqcDir.resolve("oldFastqc.zip"))
        final Path oldFastqcMd5 = CreateFileHelper.createFile(fastqcDir.resolve("oldFastqc.zip.md5sum"))
        final Path oldFastqcHtml = CreateFileHelper.createFile(fastqcDir.resolve("oldFastqc.html"))

        WithdrawHelperService service = new WithdrawHelperService()

        service.rawSequenceDataWorkFileService = Mock(RawSequenceDataWorkFileService) {
            1 * getFilePath(newWorkflowFastqFile) >> newFastqPath
            1 * getMd5sumPath(newWorkflowFastqFile) >> newMd5Path
        }
        service.rawSequenceDataViewFileService = Mock(RawSequenceDataViewFileService) {
            1 * getFilePath(newWorkflowFastqFile) >> Paths.get("/tmp/view")
        }
        service.fastqcDataFilesService = Mock(FastqcDataFilesService) {
            1 * fastqcOutputDirectory(oldWorkflowFastqcFile) >> fastqcDir
            1 * fastqcOutputDirectory(oldWorkflowFastqcFile, PathOption.REAL_PATH) >> fastqcDir
            1 * fastqcOutputPath(oldWorkflowFastqcFile) >> oldFastqcZip
            1 * fastqcOutputMd5sumPath(oldWorkflowFastqcFile) >> oldFastqcMd5
            1 * fastqcHtmlPath(oldWorkflowFastqcFile) >> oldFastqcHtml
        }

        WithdrawStateHolder holder = new WithdrawStateHolder([
                withdrawParameters: new WithdrawParameters([
                        seqTracksWithComments: [new SeqTrackWithComment(newWorkflowFastqFile.seqTrack, "test comment")],
                ]),
        ])

        when:
        service.handleRawSequenceFiles(holder)

        then:
        !holder.pathsToChangePermissions.contains(newFastqPath.toString())
        !holder.pathsToChangePermissions.contains(newMd5Path.toString())

        holder.pathsToChangePermissions.contains(oldFastqcZip.toString())
        holder.pathsToChangePermissions.contains(oldFastqcMd5.toString())
        holder.pathsToChangePermissions.contains(oldFastqcHtml.toString())

        holder.pathsToChangeGroup.size() > 0

        newWorkflowFastqFile.fileWithdrawn
        newWorkflowFastqFile.withdrawnDate != null
    }
}
