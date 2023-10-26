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
package de.dkfz.tbi.otp.dataswap

import grails.testing.gorm.DataTest
import grails.validation.Validateable
import spock.lang.*

import de.dkfz.tbi.otp.Comment
import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.AnalysisDeletionService
import de.dkfz.tbi.otp.dataswap.data.DataSwapData
import de.dkfz.tbi.otp.dataswap.parameters.DataSwapParameters
import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPancanFactory
import de.dkfz.tbi.otp.filestore.PathOption
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.exceptions.FileNotFoundException

import java.nio.file.*

class AbstractDataSwapServiceSpec extends Specification implements DataTest, RoddyPancanFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                RawSequenceFile,
                Project,
                SampleType,
                SeqType,
                Sample,
                Individual,
                SeqTrack,
                ExternallyProcessedBamFile,
                RoddyBamFile,
                Comment,
                BamFilePairAnalysis,
                ReferenceGenomeProjectSeqType,
                SampleTypePerProject,
                AceseqInstance,
                AceseqQc,
                MergingWorkPackage,
                FastqFile,
        ]
    }

    @TempDir
    Path tempDir

    @Shared
    AbstractDataSwapService service

    void setupSpec() {
        service = Spy(AbstractDataSwapService)
    }

    AbstractDataSwapService setupServiceWithAbstractMethods() {
        return Spy(type: new AbstractDataSwapService<DataSwapParameters, DataSwapData<DataSwapParameters>>() {
            @Override
            protected void logSwapParameters(DataSwapParameters params) {
                // Is not functionally tested here
            }

            @Override
            protected void completeOmittedNewSwapValuesAndLog(DataSwapParameters params) {
                // Is not functionally tested here
            }

            @Override
            protected DataSwapData<DataSwapParameters> buildDataDTO(DataSwapParameters params) {
                // Is not functionally tested here
                return null
            }

            @Override
            protected void performDataSwap(DataSwapData<DataSwapParameters> data) {
                // Is not functionally tested here
            }

            @Override
            protected void createSwapComments(DataSwapData<DataSwapParameters> data) {
                // Is not functionally tested here
            }

            @Override
            protected void cleanupLeftOvers(DataSwapData<DataSwapParameters> data) {
                // Is not functionally tested here
            }
        }.class) as AbstractDataSwapService
    }

    void "swap(P parameters), calls methods in the right order"() {
        given:
        AbstractDataSwapService service = setupServiceWithAbstractMethods()

        final DataSwapParameters parameters = new DataSwapParameters()

        final DataSwapData dataSwapData = new DataSwapData<DataSwapParameters>()

        when:
        service.swap(parameters)

        then:
        1 * service.validateDTO(_ as Validateable) >> null

        then:
        1 * service.logSwapParameters(_ as DataSwapParameters) >> _

        then:
        1 * service.completeOmittedNewSwapValuesAndLog(_ as DataSwapParameters) >> _

        then:
        1 * service.buildDataDTO(_ as DataSwapParameters) >> dataSwapData

        then:
        1 * service.swap(dataSwapData) >> null
    }

    void "swap(D data), calls methods in the right order"() {
        given:
        AbstractDataSwapService service = setupServiceWithAbstractMethods()

        final DataSwapData dataSwapData = new DataSwapData<DataSwapParameters>()

        when:
        service.swap(dataSwapData)

        then:
        1 * service.validateDTO(_ as Validateable) >> null

        then:
        1 * service.checkThatNoAnalysisIsRunning(_ as DataSwapData<DataSwapParameters>) >> null

        then:
        1 * service.createGroovyConsoleScriptToRestartAlignments(_ as DataSwapData<DataSwapParameters>) >> null

        then:
        1 * service.markSeqTracksAsSwappedAndDeleteDependingObjects(_ as DataSwapData<DataSwapParameters>) >> null

        then:
        1 * service.performDataSwap(_ as DataSwapData<DataSwapParameters>) >> _

        then:
        1 * service.createSwapComments(_ as DataSwapData<DataSwapParameters>) >> _

        then:
        1 * service.cleanupLeftOvers(_ as DataSwapData<DataSwapParameters>) >> _

        then:
        1 * service.createMoveFilesScript(_ as DataSwapData<DataSwapParameters>) >> null
    }

    void "validateDTO, when validateable is valid should not throw an exception"() {
        given:
        Validateable validateable = Mock(Validateable) {
            _ * validate(*_) >> true
        }

        when:
        service.validateDTO(validateable)

        then:
        noExceptionThrown()
    }

    void "validateDTO, when validateable is not valid should not throw an AssertionError"() {
        given:
        Validateable validateable = Mock(Validateable) {
            _ * validate(*_) >> false
        }

        when:
        service.validateDTO(validateable)

        then:
        thrown(AssertionError)
    }

    @Unroll
    void "collectFileNamesOfRawSequenceFiles, when single cell is #singleCell and label is #wellLabel, then return correct list"() {
        given:
        final RawSequenceFile rawSequenceFile = createFastqFile(
                used: false,
                seqTrack: createSeqTrack([
                        seqType            : createSeqType([
                                singleCell: singleCell,
                        ]),
                        singleCellWellLabel: wellLabel,
                ]),
        )

        service.lsdfFilesService = Mock(LsdfFilesService) {
            1 * getFileFinalPathAsPath(rawSequenceFile) >> Paths.get('finalFile')
            1 * getFileViewByPidPathAsPath(rawSequenceFile) >> Paths.get('viewByPidFile')
            wellCount * getFileViewByPidPathAsPath(rawSequenceFile, WellDirectory.ALL_WELL) >> Paths.get('wellFile')
            0 * _
        }
        service.singleCellService = Mock(SingleCellService) {
            wellCount * singleCellMappingFile(rawSequenceFile) >> Paths.get('wellMappingFile')
            wellCount * mappingEntry(rawSequenceFile) >> 'entry'
            0 * _
        }

        Map<RawSequenceFile, Map<String, ?>> expected = [
                (rawSequenceFile): [
                        (AbstractDataSwapService.DIRECT_FILE_NAME): 'finalFile',
                        (AbstractDataSwapService.VBP_FILE_NAME)   : 'viewByPidFile',
                ],
        ]
        if (wellCount) {
            expected[rawSequenceFile] << [
                    (AbstractDataSwapService.WELL_FILE_NAME)              : 'wellFile',
                    (AbstractDataSwapService.WELL_MAPPING_FILE_NAME)      : 'wellMappingFile',
                    (AbstractDataSwapService.WELL_MAPPING_FILE_ENTRY_NAME): 'entry',
            ]
        }

        expect:
        expected == service.collectFileNamesOfRawSequenceFiles([rawSequenceFile])

        where:
        singleCell | wellLabel || wellCount
        false      | ''        || 0
        true       | ''        || 0
        false      | 'WELL'    || 0
        true       | 'WELL'    || 1
    }

    @Unroll
    void "createSingeCellScript, when single cell is #singleCell and label is #wellLabel, then return empty list"() {
        given:
        final RawSequenceFile rawSequenceFile = createFastqFile(
                used: false,
                seqTrack: createSeqTrack([
                        seqType            : createSeqType([
                                singleCell: singleCell,
                        ]),
                        singleCellWellLabel: wellLabel,
                ]),
        )

        expect:
        service.createSingeCellScript(rawSequenceFile, [:]) == ''

        where:
        singleCell | wellLabel
        false      | ''
        true       | ''
        false      | 'WELL'
    }

    void "createSingeCellScript, when seqType is single cell and well label given, create script containing expected commands"() {
        given:
        final String OLD_FINAL_PATH = "oldFinalPath"
        final String OLD_PATH = 'oldPath'
        final String OLD_ALL_PATH = "${OLD_PATH}/all"
        final String OLD_WELL_PATH = "${OLD_ALL_PATH}/oldFile"
        final String OLD_MAPPING_PATH = "${OLD_ALL_PATH}/mapping"
        final String OLD_ENTRY = 'oldEntry\tvalue'

        final Path NEW_FINAL_PATH = Paths.get("newFinalPath")
        final String NEW_PATH = 'newPath'
        final String NEW_ALL_PATH = "${NEW_PATH}/all"
        final Path NEW_WELL_PATH = Paths.get("${NEW_ALL_PATH}/newFile")
        final String NEW_MAPPING_PATH = "${NEW_ALL_PATH}/mapping"
        final String NEW_ENTRY = 'newEntry\tvalue'

        final RawSequenceFile rawSequenceFile = createFastqFile(
                used: false,
                seqTrack: createSeqTrack([
                        seqType            : createSeqType([
                                singleCell: true,
                        ]),
                        singleCellWellLabel: 'WELL',
                ]),
        )

        service.lsdfFilesService = Mock(LsdfFilesService) {
            1 * getFileFinalPathAsPath(rawSequenceFile) >> NEW_FINAL_PATH
            1 * getFileViewByPidPathAsPath(rawSequenceFile, WellDirectory.ALL_WELL) >> NEW_WELL_PATH
            0 * _
        }

        service.singleCellService = Mock(SingleCellService) {
            1 * singleCellMappingFile(rawSequenceFile) >> Paths.get(NEW_MAPPING_PATH)
            1 * mappingEntry(rawSequenceFile) >> NEW_ENTRY
            0 * _
        }

        final Map<String, String> oldValues = [
                (AbstractDataSwapService.DIRECT_FILE_NAME)            : OLD_FINAL_PATH,
                (AbstractDataSwapService.WELL_FILE_NAME)              : OLD_WELL_PATH,
                (AbstractDataSwapService.WELL_MAPPING_FILE_NAME)      : OLD_MAPPING_PATH,
                (AbstractDataSwapService.WELL_MAPPING_FILE_ENTRY_NAME): OLD_ENTRY,
        ]

        final String expectedScript = """
                                |# Single Cell structure
                                |## recreate link
                                |rm -f '${OLD_WELL_PATH}'
                                |mkdir -p -m 2750 '${NEW_ALL_PATH}'
                                |ln -sr '${NEW_FINAL_PATH}' \\\n      '${NEW_WELL_PATH}'
                                |
                                |## remove entry from old mapping file
                                |chmod 640 '${OLD_MAPPING_PATH}'
                                |sed -i '\\#${OLD_ENTRY}#d' ${OLD_MAPPING_PATH}
                                |chmod 440 '${OLD_MAPPING_PATH}'
                                |
                                |## add entry to new mapping file
                                |touch '${NEW_MAPPING_PATH}'
                                |chgrp '${rawSequenceFile.project.unixGroup}' '${NEW_MAPPING_PATH}'
                                |chmod 640 '${NEW_MAPPING_PATH}'
                                |echo '${NEW_ENTRY}' >> '${NEW_MAPPING_PATH}'
                                |chmod 440 '${NEW_MAPPING_PATH}'
                                |
                                |## delete mapping file, if empty
                                |if [ ! -s '${OLD_MAPPING_PATH}' ]
                                |then
                                |    rm -f '${OLD_MAPPING_PATH}'
                                |fi
                                |""".stripMargin()

        when:
        String script = service.createSingeCellScript(rawSequenceFile, oldValues)

        then:
        expectedScript == script
    }

    void "completeOmittedNewSwapValuesAndLog, when new values are omitted complete them if old values"() {
        given:
        final String oldValue = "old_value"
        final String newValue = "new_value"
        final String omittedNull = null
        final String omittedEmpty = ""

        StringBuilder log = new StringBuilder()

        final List<Swap<String>> swapList = [
                new Swap(oldValue, newValue),
                new Swap(oldValue, omittedNull),
                new Swap(oldValue, omittedEmpty),
        ]

        final String filledLog = "\n    - ${oldValue} --> ${newValue}" +
                "\n    - ${oldValue} --> ${oldValue}" +
                "\n    - ${oldValue} --> ${oldValue}"

        when:
        List<Swap<String>> resultList = service.completeOmittedNewSwapValuesAndLog(swapList, log)

        then:
        resultList[0].old == oldValue
        resultList[0].new == newValue
        resultList[1].old == oldValue
        resultList[1].new == oldValue
        resultList[2].old == oldValue
        resultList[2].new == oldValue
        filledLog == log.toString()
    }

    void "completeOmittedNewSwapValuesAndLog, when any old value is omitted throws IllegalArgumentException"() {
        given:
        final String oldValue = "old_value"
        final String newValue = "new_value"
        final String omittedNull = null
        final String omittedEmpty = ""

        StringBuilder log = new StringBuilder()

        final List<Swap<String>> swapList = [
                new Swap(oldValue, newValue),
                new Swap(omittedNull, newValue),
                new Swap(oldValue, omittedEmpty),
        ]

        when:
        service.completeOmittedNewSwapValuesAndLog(swapList, log)

        then:
        thrown IllegalArgumentException
    }

    void "generateMaybeMoveBashCommand, fail with FileNotFoundException if both new and old file not found and failOnMissingFiles is true"() {
        given:
        final Path oldPath = Paths.get("not_existing_old_file")
        final Path newPath = Paths.get("not_existing_new_file")

        final DataSwapParameters parameters = new DataSwapParameters(
                log: new StringBuilder(),
                failOnMissingFiles: true
        )

        final DataSwapData dataSwapData = new DataSwapData<DataSwapParameters>(parameters: parameters)

        when:
        service.generateMaybeMoveBashCommand(oldPath, newPath, dataSwapData)

        then:
        thrown(FileNotFoundException)
    }

    void "generateMaybeMoveBashCommand, just log if both new and old file not found and failOnMissingFiles is false"() {
        given:
        final Path oldPath = Paths.get("not_existing_old_file")
        final Path newPath = Paths.get("not_existing_new_file")

        StringBuilder log = new StringBuilder()

        final DataSwapParameters parameters = new DataSwapParameters(
                log: log,
                failOnMissingFiles: false
        )

        final DataSwapData dataSwapData = new DataSwapData<DataSwapParameters>(parameters: parameters)

        when:
        String bashCommand = service.generateMaybeMoveBashCommand(oldPath, newPath, dataSwapData)

        then:
        log.toString()
        notThrown(FileNotFoundException)
        !bashCommand
    }

    void "generateMaybeMoveBashCommand, when old file exists and new file not create file move bash command"() {
        given:
        final Path oldPath = CreateFileHelper.createFile(tempDir.resolve("old"))
        final Path newPath = Paths.get("/parent/not_existing_new_file")

        final String fileMoveCommand = """
                                       mkdir -p -m 2750 '${newPath.parent}';
                                       mv '${oldPath}' \\
                                          '${newPath}';
                                       chgrp -h `stat -c '%G' ${newPath.parent}` ${newPath}\n
                                       """.stripIndent()

        when:
        String bashCommand = service.generateMaybeMoveBashCommand(oldPath, newPath, null)

        then:
        bashCommand == fileMoveCommand
    }

    void "generateMaybeMoveBashCommand, when new file exists and it is not the same as the old one create comment to remove it"() {
        given:
        final Path oldPath = CreateFileHelper.createFile(tempDir.resolve("old"))
        final Path newPath = CreateFileHelper.createFile(tempDir.resolve("new"))

        final String fileExistsComment = "# new file already exists: '${newPath}'; delete old file\n rm -f '${oldPath}'\n"

        when:
        String bashCommand = service.generateMaybeMoveBashCommand(oldPath, newPath, null)

        then:
        bashCommand == fileExistsComment
    }

    void "generateMaybeMoveBashCommand, when new file exists and it is the same as the old one create comment"() {
        given:
        final Path oldPath = CreateFileHelper.createFile(tempDir.resolve("old"))

        final String fileExistsComment = "# the old and the new data file ('${oldPath}') are the same, no move needed.\n"

        when:
        String bashCommand = service.generateMaybeMoveBashCommand(oldPath, oldPath, null)

        then:
        bashCommand == fileExistsComment
    }

    void "generateMaybeMoveBashCommand, when old file not exists but new file do create move manually comment"() {
        given:
        final Path oldPath = Paths.get("not_existing_old_file")
        final Path newPath = CreateFileHelper.createFile(tempDir.resolve("new"))

        final String fileExistsComment = "# no old file, and ${newPath} is already at the correct position (apparently put there manually?)\n"

        when:
        String bashCommand = service.generateMaybeMoveBashCommand(oldPath, newPath, null)

        then:
        bashCommand == fileExistsComment
    }

    void "createGroovyConsoleScriptToRestartAlignments, when no seq track given just write comment header to script file"() {
        given:
        final Realm realm = createRealm()

        final String bashScriptName = "TEST-SCRIPT"
        final Path scriptOutputDirectory = tempDir.resolve("files")

        Files.createDirectory(scriptOutputDirectory)

        service.configService = Mock(ConfigService) {
            1 * getDefaultRealm() >> realm
        }
        service.fileService = Mock(FileService) {
            1 * createOrOverwriteScriptOutputFile(_, _, _) >> Files.createFile(scriptOutputDirectory.resolve("restartAli_${bashScriptName}.groovy"))
        }

        final DataSwapParameters parameters = new DataSwapParameters(
                bashScriptName: bashScriptName,
                scriptOutputDirectory: scriptOutputDirectory
        )

        final DataSwapData dataSwapData = new DataSwapData<DataSwapParameters>(parameters: parameters)

        when:
        service.createGroovyConsoleScriptToRestartAlignments(dataSwapData)

        then:
        scriptOutputDirectory.toFile().listFiles().length != 0
        File alignmentScript = scriptOutputDirectory.resolve("restartAli_${bashScriptName}.groovy").toFile()
        alignmentScript.exists()
        alignmentScript.text == AbstractDataSwapService.ALIGNMENT_SCRIPT_HEADER
    }

    void "createGroovyConsoleScriptToRestartAlignments, when seq track given write comment header and seq track comments to script file"() {
        given:
        final Realm realm = createRealm()
        final List<SeqTrack> seqTrackList = (0..1).collect { createSeqTrack() }

        final String bashScriptName = "TEST-SCRIPT"
        final Path scriptOutputDirectory = tempDir.resolve("files")

        Files.createDirectory(scriptOutputDirectory)

        service.configService = Mock(ConfigService) {
            1 * getDefaultRealm() >> realm
        }
        service.fileService = Mock(FileService) {
            1 * createOrOverwriteScriptOutputFile(_, _, _) >> Files.createFile(scriptOutputDirectory.resolve("restartAli_${bashScriptName}.groovy"))
        }

        final DataSwapParameters parameters = new DataSwapParameters(
                bashScriptName: bashScriptName,
                scriptOutputDirectory: scriptOutputDirectory
        )

        final DataSwapData dataSwapData = new DataSwapData<DataSwapParameters>(parameters: parameters, seqTrackList: seqTrackList)

        final String expectedAlignmentScript = AbstractDataSwapService.ALIGNMENT_SCRIPT_HEADER + "    ${seqTrackList[0].id},  //${seqTrackList[0]}\n" +
                "    ${seqTrackList[1].id},  //${seqTrackList[1]}\n"

        when:
        service.createGroovyConsoleScriptToRestartAlignments(dataSwapData)

        then:
        scriptOutputDirectory.toFile().listFiles().length != 0
        File alignmentScript = scriptOutputDirectory.resolve("restartAli_${bashScriptName}.groovy").toFile()
        alignmentScript.exists()
        alignmentScript.text == expectedAlignmentScript
    }

    void "copyAndRemoveFastQcFile, when no checksum files exists create just one command"() {
        given:
        final String oldFilePath = Files.createFile(tempDir.resolve("test.txt"))
        final String fileMoveCommand = "# the old and the new data file ('${oldFilePath}') are the same, no move needed.\n\n"

        when:
        // no need to test all scenarios since generateMaybeMoveBashCommand is tested separately
        String bashCommand = service.copyAndRemoveFastQcFile(oldFilePath, oldFilePath, null)

        then:
        bashCommand == fileMoveCommand
    }

    void "copyAndRemoveFastQcFile, when checksum files exists create one for data files and one for checksum files"() {
        given:
        final Path oldFilePath = CreateFileHelper.createFile(tempDir.resolve("old.txt"))
        final Path checksum = CreateFileHelper.createFile(tempDir.resolve(oldFilePath.fileName.toString() + ".md5sum"))
        final String fileMoveCommand = "# the old and the new data file ('${oldFilePath}') are the same, no move needed.\n\n" +
                "# the old and the new data file ('${checksum}') are the same, no move needed.\n"

        when:
        // no need to test all scenarios since generateMaybeMoveBashCommand is tested separately
        String bashCommand = service.copyAndRemoveFastQcFile(oldFilePath.toString(), oldFilePath.toString(), null)

        then:
        bashCommand == fileMoveCommand
    }

    @Unroll
    void "createCommentForSwappedRawSequenceFiles, when no comments exists call saveComment #size times with #comment"() {
        given:
        final List<RawSequenceFile> rawSequenceFiles = (1..size).collect { createFastqFile() }
        final DataSwapData dataSwapData = new DataSwapData<DataSwapParameters>(rawSequenceFiles: rawSequenceFiles)

        service.commentService = Mock(CommentService)

        when:
        service.createCommentForSwappedRawSequenceFiles(dataSwapData)

        then:
        size * service.commentService.saveComment(_, comment)

        where:
        comment = "Attention: Datafile swapped!"
        size = 5
    }

    @Unroll
    void "createCommentForSwappedRawSequenceFiles, when already comments exists call saveComment #size times with #comment"() {
        given:
        final List<RawSequenceFile> rawSequenceFiles = (1..size).collect {
            createFastqFile([
                    comment: new Comment(comment: someComment, modificationDate: new Date(), author: "SomeAuthor")
            ])
        }
        final DataSwapData dataSwapData = new DataSwapData<DataSwapParameters>(rawSequenceFiles: rawSequenceFiles)

        service.commentService = Mock(CommentService)

        when:
        service.createCommentForSwappedRawSequenceFiles(dataSwapData)

        then:
        size * service.commentService.saveComment(_, comment)

        where:
        someComment = "Some comment"
        swapComment = "Attention: Datafile swapped!"
        comment = someComment + "\n" + swapComment
        size = 5
    }

    void "renameRawSequenceFiles, when old dataFile is withdrawn and libraryLayout is not SINGLE fail with AssertError"() {
        given:
        final String newRawSequenceFileName = 'newDataFileName.gz'

        // domain
        final SeqTrack seqTrack = createSeqTrackWithOneFastqFile([seqType: createSeqType([libraryLayout: SequencingReadType.PAIRED])])
        RawSequenceFile rawSequenceFile = CollectionUtils.exactlyOneElement(RawSequenceFile.findAllBySeqTrack(seqTrack))
        rawSequenceFile.fileType.vbpPath = "/sequence/"
        rawSequenceFile.mateNumber = null
        rawSequenceFile.fileWithdrawn = true
        final Path oldFile = tempDir.resolve(rawSequenceFile.fileName)
        final Path oldFileViewByPid = tempDir.resolve('oldViewByPidFile')
        final Path newFile = Paths.get('somePath').resolve(Paths.get(newRawSequenceFileName))
        final Path newFileViewByPid = Paths.get('linking').resolve(Paths.get('newViewByPidFile'))

        // service
        service.lsdfFilesService = Mock(LsdfFilesService) {
            _ * getFileFinalPath(_) >> newFile.toString()
            _ * getFileViewByPidPath(_) >> newFileViewByPid.toString()
        }

        // DTO
        final List<Swap<String>> rawSequenceFileSwaps = [new Swap(rawSequenceFile.fileName, newRawSequenceFileName)]
        StringBuilder log = new StringBuilder()
        final Map<RawSequenceFile, Map<String, ?>> oldRawSequenceFileNameMap = [
                (rawSequenceFile): [
                        (AbstractDataSwapService.DIRECT_FILE_NAME): oldFile.toString(),
                        (AbstractDataSwapService.VBP_FILE_NAME)   : oldFileViewByPid.toString(),
                ],
        ]

        final DataSwapParameters parameters = new DataSwapParameters(
                rawSequenceFileSwaps: rawSequenceFileSwaps,
                log: log
        )

        final DataSwapData dataSwapData = new DataSwapData(
                projectSwap: new Swap(rawSequenceFile.project, rawSequenceFile.project),
                parameters: parameters,
                rawSequenceFiles: [rawSequenceFile],
                oldRawSequenceFileNameMap: oldRawSequenceFileNameMap
        )

        when:
        service.renameRawSequenceFiles(dataSwapData)

        then:
        thrown AssertionError
    }

    void "renameRawSequenceFiles, when old dataFile is withdrawn log it and set mateNumber to 1"() {
        given:
        final String newRawSequenceFileName = 'newDataFileName.gz'

        // domain
        final SeqTrack seqTrack = createSeqTrackWithOneFastqFile([seqType: createSeqType([libraryLayout: SequencingReadType.SINGLE])])
        RawSequenceFile rawSequenceFile = CollectionUtils.exactlyOneElement(RawSequenceFile.findAllBySeqTrack(seqTrack))
        rawSequenceFile.fileType.vbpPath = "/sequence/"
        rawSequenceFile.mateNumber = null
        rawSequenceFile.fileWithdrawn = true
        final String oldRawSequenceFileName = rawSequenceFile.fileName
        final Path oldFile = CreateFileHelper.createFile(tempDir.resolve(rawSequenceFile.fileName))
        final Path oldFileViewByPid = CreateFileHelper.createFile(tempDir.resolve('oldViewByPidFile'))
        final Path newFile = Paths.get('somePath').resolve(Paths.get(newRawSequenceFileName))
        final Path newFileViewByPid = Paths.get('linking').resolve(Paths.get('newViewByPidFile'))

        // service
        service.lsdfFilesService = Mock(LsdfFilesService) {
            _ * getFileFinalPathAsPath(_) >> newFile
            _ * getFileViewByPidPathAsPath(_) >> newFileViewByPid
        }
        service.fileSystemService = Mock(FileSystemService) {
            getRemoteFileSystem() >> FileSystems.default
        }

        // DTO
        final List<Swap<String>> rawSequenceFileSwaps = [new Swap(rawSequenceFile.fileName, newRawSequenceFileName)]
        StringBuilder log = new StringBuilder()
        final Map<RawSequenceFile, Map<String, ?>> oldRawSequenceFileNameMap = [
                (rawSequenceFile): [
                        (AbstractDataSwapService.DIRECT_FILE_NAME): oldFile.toString(),
                        (AbstractDataSwapService.VBP_FILE_NAME)   : oldFileViewByPid.toString(),
                ],
        ]

        final DataSwapParameters parameters = new DataSwapParameters(
                rawSequenceFileSwaps: rawSequenceFileSwaps,
                log: log
        )

        final DataSwapData dataSwapData = new DataSwapData(
                projectSwap: new Swap(rawSequenceFile.project, rawSequenceFile.project),
                parameters: parameters,
                rawSequenceFiles: [rawSequenceFile],
                oldRawSequenceFileNameMap: oldRawSequenceFileNameMap
        )

        when:
        service.renameRawSequenceFiles(dataSwapData)

        then:
        rawSequenceFile.mateNumber == 1
        log.toString() == "\n====> set mate number for withdrawn data file\n    changed ${oldRawSequenceFileName} to ${rawSequenceFile.fileName}"
    }

    @Unroll
    void "renameRawSequenceFiles, when old files exists but new files not then move old files to new location and link new with #seqTrackAmount SeqTrack(s)"() {
        given:

        // domain
        final Project project = createProject()
        final Sample sample = createSample([individual: createIndividual([project: project,]),])
        final List<RawSequenceFile> rawSequenceFiles = []

        for (int i : 1..seqTrackAmount) {
            final SeqTrack seqTrack = createSeqTrackWithTwoFastqFile(
                    [sample: sample,],
                    [fileName: "DataFileFileName_${i}_R1.gz", project: project,],
                    [fileName: "DataFileFileName_${i}_R2.gz", project: project,],
            )

            final List<RawSequenceFile> rawSequenceFilesPerSeqTrack = RawSequenceFile.findAllBySeqTrack(seqTrack)
            rawSequenceFiles.addAll(rawSequenceFilesPerSeqTrack)
        }
        final Map<RawSequenceFile, Map<String, ?>> rawSequenceFilePaths = createPathsForRawSequenceFiles(rawSequenceFiles, true, false)

        // service
        service.lsdfFilesService = Mock(LsdfFilesService) {
            _ * getFileFinalPathAsPath(_) >> { RawSequenceFile rawSequenceFile, PathOption... options ->
                Paths.get(rawSequenceFilePaths[rawSequenceFile].newPath.toString()) }
            _ * getFileViewByPidPathAsPath(_) >> { RawSequenceFile rawSequenceFile, PathOption... options ->
                Paths.get(rawSequenceFilePaths[rawSequenceFile].newVbpPath.toString()) }
        }
        service.fileSystemService = Mock(FileSystemService) {
            _ * getRemoteFileSystem() >> FileSystems.default
        }

        // DTO
        final List<Swap<String>> rawSequenceFileSwaps = rawSequenceFiles.collect { new Swap(rawSequenceFilePaths[it].oldFileName, rawSequenceFilePaths[it].newFileName) }
        StringBuilder log = new StringBuilder()
        final Map<RawSequenceFile, Map<String, String>> oldRawSequenceFileNameMap = rawSequenceFiles.collectEntries {
            [(it): [
                    (AbstractDataSwapService.DIRECT_FILE_NAME): rawSequenceFilePaths[it].oldPath.toString(),
                    (AbstractDataSwapService.VBP_FILE_NAME)   : rawSequenceFilePaths[it].oldVbpPath.toString(),
            ]]
        }

        final DataSwapParameters parameters = new DataSwapParameters(
                rawSequenceFileSwaps: rawSequenceFileSwaps,
                log: log
        )

        final DataSwapData dataSwapData = new DataSwapData(
                projectSwap: new Swap(project, project),
                parameters: parameters,
                rawSequenceFiles: rawSequenceFiles,
                oldRawSequenceFileNameMap: oldRawSequenceFileNameMap
        )

        String bashScriptToMoveFiles = ""
        rawSequenceFiles.each {
            final String bashMoveDirectFile = """\n
                                     |# ${it.seqTrack} ${rawSequenceFilePaths[it].newFileName}
                                     |mkdir -p -m 2750 '${rawSequenceFilePaths[it].newPath.parent}';
                                     |mv '${rawSequenceFilePaths[it].oldPath}' \\
                                     |   '${rawSequenceFilePaths[it].newPath}';
                                     |chgrp -h `stat -c '%G' ${rawSequenceFilePaths[it].newPath.parent}` ${rawSequenceFilePaths[it].newPath}
                                     |if [ -e '${rawSequenceFilePaths[it].oldPath}.md5sum' ]; then
                                     |  mv '${rawSequenceFilePaths[it].oldPath}.md5sum' \\
                                     |     '${rawSequenceFilePaths[it].newPath}.md5sum';
                                     |  chgrp -h `stat -c '%G' ${rawSequenceFilePaths[it].newPath.parent}` ${rawSequenceFilePaths[it].newPath}.md5sum
                                     |fi\n""".stripMargin()

            final String bashMoveVbpFile = """\
                                 |rm -f '${rawSequenceFilePaths[it].oldVbpPath}';
                                 |mkdir -p -m 2750 '${rawSequenceFilePaths[it].newVbpPath.parent}';
                                 |ln -sr '${rawSequenceFilePaths[it].newPath}' \\
                                 |      '${rawSequenceFilePaths[it].newVbpPath}'""".stripMargin()

            bashScriptToMoveFiles += "${bashMoveDirectFile}\n${bashMoveVbpFile}\n"
            bashScriptToMoveFiles += "\n\n"
        }

        String expectedLog = ""
        rawSequenceFiles.each {
            expectedLog += "\n    changed ${rawSequenceFilePaths[it].oldFileName} to ${rawSequenceFilePaths[it].newFileName}"
        }

        when:
        String script = service.renameRawSequenceFiles(dataSwapData)

        then:
        bashScriptToMoveFiles == script
        rawSequenceFiles*.fileName == rawSequenceFilePaths.values().newFileName
        rawSequenceFiles*.vbpFileName == rawSequenceFilePaths.values().newFileName
        log.toString() == expectedLog

        where:
        seqTrackAmount << [1, 3]
    }

    void "checkThatNoAnalysisIsRunning, when analysis is still progress then it should throw an AssertionError"() {
        given:
        final List<BamFilePairAnalysis> bamFilePairAnalyses = (1..3).collect {
            DomainFactory.createAceseqInstanceWithRoddyBamFiles(processingState: AnalysisProcessingStates.IN_PROGRESS)
        }
        final List<RoddyBamFile> roddyBamFiles = (bamFilePairAnalyses*.sampleType1BamFile + bamFilePairAnalyses*.sampleType2BamFile).sort { it.dateCreated }
        final DataSwapData dataSwapData = new DataSwapData<DataSwapParameters>([
                seqTrackList: roddyBamFiles*.seqTracks.flatten(),
        ])
        service.analysisDeletionService = Mock(AnalysisDeletionService)

        when:
        service.checkThatNoAnalysisIsRunning(dataSwapData)

        then:
        1 * service.analysisDeletionService.assertThatNoWorkflowsAreRunning(_) >> { throw new AssertionError() }

        and:
        thrown(AssertionError)
    }

    void "checkThatNoAnalysisIsRunning, when analysis is not in progress then it should throw no AssertionError"() {
        given:
        final List<BamFilePairAnalysis> bamFilePairAnalyses = (1..3).collect {
            DomainFactory.createAceseqInstanceWithRoddyBamFiles(processingState: AnalysisProcessingStates.FINISHED)
        }
        final List<RoddyBamFile> roddyBamFiles = (bamFilePairAnalyses*.sampleType1BamFile + bamFilePairAnalyses*.sampleType2BamFile).sort { it.dateCreated }
        final DataSwapData dataSwapData = new DataSwapData<DataSwapParameters>([
                seqTrackList: roddyBamFiles*.seqTracks.flatten(),
        ])
        service.analysisDeletionService = Mock(AnalysisDeletionService)

        when:
        service.checkThatNoAnalysisIsRunning(dataSwapData)

        then:
        1 * service.analysisDeletionService.assertThatNoWorkflowsAreRunning(_) >> _

        and:
        notThrown(AssertionError)
    }

    @Unroll
    void "renameRawSequenceFiles, when old and new data files exists then remove old files and link new with #seqTrackAmount SeqTrack(s)"() {
        given:

        // domain
        final Project project = createProject()
        final Sample sample = createSample([individual: createIndividual([project: project,]),])
        final List<RawSequenceFile> rawSequenceFileList = []

        for (int i : 1..seqTrackAmount) {
            final SeqTrack seqTrack = createSeqTrackWithTwoFastqFile(
                    [sample: sample,],
                    [fileName: "DataFileFileName_${i}_R1.gz", project: project,],
                    [fileName: "DataFileFileName_${i}_R2.gz", project: project,]
            )
            final List<RawSequenceFile> rawSequenceFilesPerSeqTrack = RawSequenceFile.findAllBySeqTrack(seqTrack)
            rawSequenceFileList.addAll(rawSequenceFilesPerSeqTrack)
        }
        final Map<RawSequenceFile, Map<String, ?>> rawSequenceFilePaths = createPathsForRawSequenceFiles(rawSequenceFileList)

        // service
        service.lsdfFilesService = Mock(LsdfFilesService) {
            _ * getFileFinalPathAsPath(_) >> { RawSequenceFile rawSequenceFile, PathOption... options ->
                Paths.get(rawSequenceFilePaths[rawSequenceFile].newPath.toString()) }
            _ * getFileViewByPidPathAsPath(_) >> { RawSequenceFile rawSequenceFile, PathOption... options ->
                Paths.get(rawSequenceFilePaths[rawSequenceFile].newVbpPath.toString()) }
        }
        service.fileSystemService = Mock(FileSystemService) {
            _ * getRemoteFileSystem() >> FileSystems.default
        }

        // DTO
        final List<Swap<String>> rawSequenceFileSwaps = rawSequenceFileList.collect { new Swap(rawSequenceFilePaths[it].oldFileName, rawSequenceFilePaths[it].newFileName) }
        StringBuilder log = new StringBuilder()
        final Map<RawSequenceFile, Map<String, String>> oldRawSequenceFileNameMap = rawSequenceFileList.collectEntries {
            [(it): [
                    (AbstractDataSwapService.DIRECT_FILE_NAME): rawSequenceFilePaths[it].oldPath.toString(),
                    (AbstractDataSwapService.VBP_FILE_NAME)   : rawSequenceFilePaths[it].oldVbpPath.toString(),
            ]]
        }

        final DataSwapParameters parameters = new DataSwapParameters(
                rawSequenceFileSwaps: rawSequenceFileSwaps,
                log: log
        )

        final DataSwapData dataSwapData = new DataSwapData(
                projectSwap: new Swap(project, project),
                parameters: parameters,
                rawSequenceFiles: rawSequenceFileList,
                oldRawSequenceFileNameMap: oldRawSequenceFileNameMap
        )

        String bashScriptToMoveFiles = ""
        rawSequenceFileList.each {
            final String bashMoveDirectFile = "rm -f '${rawSequenceFilePaths[it].oldPath}'"
            final String bashMoveVbpFile = """\
                                 |rm -f '${rawSequenceFilePaths[it].oldVbpPath}';
                                 |mkdir -p -m 2750 '${rawSequenceFilePaths[it].newVbpPath.parent}';
                                 |ln -sr '${rawSequenceFilePaths[it].newPath}' \\
                                 |      '${rawSequenceFilePaths[it].newVbpPath}'""".stripMargin()

            bashScriptToMoveFiles += "${bashMoveDirectFile}\n${bashMoveVbpFile}\n"
            bashScriptToMoveFiles += "\n\n"
        }

        String expectedLog = ""
        rawSequenceFileList.each {
            expectedLog += "\n    changed ${rawSequenceFilePaths[it].oldFileName} to ${rawSequenceFilePaths[it].newFileName}"
        }

        when:
        String script = service.renameRawSequenceFiles(dataSwapData)

        then:
        bashScriptToMoveFiles == script
        rawSequenceFileList*.fileName == rawSequenceFilePaths.values().newFileName
        rawSequenceFileList*.vbpFileName == rawSequenceFilePaths.values().newFileName
        log.toString() == expectedLog

        where:
        seqTrackAmount << [1, 3]
    }

    @Unroll
    void "renameRawSequenceFiles, when old file is singleCell then also create singleCellScript with #seqTrackAmount SeqTrack(s)"() {
        given:

        // domain
        final Project project = createProject()
        final Sample sample = createSample([individual: createIndividual([project: project])])
        final List<RawSequenceFile> rawSequenceFileList = []

        for (int i : 1..seqTrackAmount) {
            final SeqTrack seqTrack = createSeqTrackWithTwoFastqFile(
                    [sample: sample, seqType: createSeqType([singleCell: true,]), singleCellWellLabel: 'WELL',],
                    [fileName: "DataFileFileName_${i}_R1.gz", project: project, used: false,],
                    [fileName: "DataFileFileName_${i}_R2.gz", project: project, used: false,]
            )
            final List<RawSequenceFile> rawSequenceFilesPerSeqTrack = RawSequenceFile.findAllBySeqTrack(seqTrack)
            rawSequenceFileList.addAll(rawSequenceFilesPerSeqTrack)
        }

        final Map<RawSequenceFile, Map<String, ?>> rawSequenceFilePaths = createPathsForRawSequenceFiles(rawSequenceFileList)
        rawSequenceFilePaths.each {
            it.value.put('oldWellFile', tempDir.resolve("${it.key.fileName}_wellFile"))
            it.value.put('wellMappingFile', tempDir.resolve("${it.key.fileName}_oldWellMappingFile"))
            it.value.put('oldWellMappingEntry', "${it.key.fileName}_oldWellMappingEntry")
            it.value.put('newWellMappingFile', tempDir.resolve("${it.key.fileName}_newWellMappingFile"))
            it.value.put('newWellMappingEntry', "${it.key.fileName}_newWellMappingEntry")
        }

        // service
        service.lsdfFilesService = Mock(LsdfFilesService) {
            _ * getFileFinalPathAsPath(_) >> { RawSequenceFile rawSequenceFile, PathOption... options ->
                Paths.get(rawSequenceFilePaths[rawSequenceFile].newPath.toString()) }
            _ * getFileViewByPidPathAsPath(_) >> { RawSequenceFile rawSequenceFile, PathOption... options ->
                Paths.get(rawSequenceFilePaths[rawSequenceFile].newVbpPath.toString()) }
            _ * getFileViewByPidPathAsPath(_, WellDirectory.ALL_WELL) >> { RawSequenceFile rawSequenceFile, _, PathOption... options ->
                Paths.get(rawSequenceFilePaths[rawSequenceFile].newVbpPath.toString()) }
        }

        service.singleCellService = Mock(SingleCellService) {
            _ * singleCellMappingFile(_) >> { RawSequenceFile rawSequenceFile -> rawSequenceFilePaths[rawSequenceFile].newWellMappingFile }
            _ * mappingEntry(_) >> { RawSequenceFile rawSequenceFile -> rawSequenceFilePaths[rawSequenceFile].newWellMappingFileEntryName }
        }
        service.fileSystemService = Mock(FileSystemService) {
            _ * getRemoteFileSystem() >> FileSystems.default
        }

        final List<Swap<String>> rawSequenceFileSwaps = rawSequenceFileList.collect { new Swap(rawSequenceFilePaths[it].oldFileName, rawSequenceFilePaths[it].newFileName) }
        StringBuilder log = new StringBuilder()
        final Map<RawSequenceFile, Map<String, String>> oldRawSequenceFileNameMap = rawSequenceFileList.collectEntries {
            [(it): [
                    (AbstractDataSwapService.DIRECT_FILE_NAME)            : rawSequenceFilePaths[it].oldPath.toString(),
                    (AbstractDataSwapService.VBP_FILE_NAME)               : rawSequenceFilePaths[it].oldVbpPath.toString(),
                    (AbstractDataSwapService.WELL_FILE_NAME)              : rawSequenceFilePaths[it].oldWellFile.toString(),
                    (AbstractDataSwapService.WELL_MAPPING_FILE_NAME)      : rawSequenceFilePaths[it].oldWellMappingFile,
                    (AbstractDataSwapService.WELL_MAPPING_FILE_ENTRY_NAME): rawSequenceFilePaths[it].oldWellMappingFileEntryName,
            ]]
        }

        final DataSwapParameters parameters = new DataSwapParameters(
                rawSequenceFileSwaps: rawSequenceFileSwaps,
                log: log
        )

        final DataSwapData dataSwapData = new DataSwapData(
                projectSwap: new Swap(project, project),
                parameters: parameters,
                rawSequenceFiles: rawSequenceFileList,
                oldRawSequenceFileNameMap: oldRawSequenceFileNameMap
        )

        String bashScriptToMoveFiles = ""
        rawSequenceFileList.each {
            final String bashMoveDirectFile = "rm -f '${rawSequenceFilePaths[it].oldPath}'"
            final String bashMoveVbpFile = """\
                                 |rm -f '${rawSequenceFilePaths[it].oldVbpPath}';
                                 |mkdir -p -m 2750 '${rawSequenceFilePaths[it].newVbpPath.parent}';
                                 |ln -sr '${rawSequenceFilePaths[it].newPath}' \\
                                 |      '${rawSequenceFilePaths[it].newVbpPath}'""".stripMargin()

            bashScriptToMoveFiles += "${bashMoveDirectFile}\n${bashMoveVbpFile}\n"
            bashScriptToMoveFiles += """
                                 |# Single Cell structure
                                 |## recreate link
                                 |rm -f '${rawSequenceFilePaths[it].oldWellFile}'
                                 |mkdir -p -m 2750 '${rawSequenceFilePaths[it].newVbpPath.parent}'
                                 |ln -sr '${rawSequenceFilePaths[it].newPath}' \\\n      '${rawSequenceFilePaths[it].newVbpPath}'
                                 |\n## remove entry from old mapping file
                                 |chmod 640 '${rawSequenceFilePaths[it].oldWellMappingFileEntryName}'
                                 |sed -i '\\#${rawSequenceFilePaths[it].oldWellMappingFileEntryName}#d' ${rawSequenceFilePaths[it].oldWellMappingFile}
                                 |chmod 440 '${rawSequenceFilePaths[it].oldWellMappingFileEntryName}'
                                 |\n## add entry to new mapping file
                                 |touch '${rawSequenceFilePaths[it].newWellMappingFile}'
                                 |chgrp '${it.project.unixGroup}' '${rawSequenceFilePaths[it].newWellMappingFile}'
                                 |chmod 640 '${rawSequenceFilePaths[it].newWellMappingFile}'
                                 |echo '${rawSequenceFilePaths[it].newWellMappingFileEntryName}' >> '${rawSequenceFilePaths[it].newWellMappingFile}'
                                 |chmod 440 '${rawSequenceFilePaths[it].newWellMappingFile}'
                                 |\n## delete mapping file, if empty
                                 |if [ ! -s '${rawSequenceFilePaths[it].oldWellMappingFile}' ]
                                 |then
                                 |    rm -f '${rawSequenceFilePaths[it].oldWellMappingFile}'
                                 |fi\n""".stripMargin()
            bashScriptToMoveFiles += "\n\n"
        }

        String expectedLog = ""
        rawSequenceFileList.each {
            expectedLog += "\n    changed ${rawSequenceFilePaths[it].oldFileName} to ${rawSequenceFilePaths[it].newFileName}"
        }

        when:
        String script = service.renameRawSequenceFiles(dataSwapData)

        then:
        bashScriptToMoveFiles == script
        rawSequenceFileList*.fileName == rawSequenceFilePaths.values().newFileName
        rawSequenceFileList*.vbpFileName == rawSequenceFilePaths.values().newFileName
        log.toString() == expectedLog

        where:
        seqTrackAmount << [1, 3]
    }

    void "renameRawSequenceFiles, when old data files can not be found fail with FileNotFoundException"() {
        given:
        final String newRawSequenceFileName = 'newDataFileName.gz'

        // domain
        final SeqTrack seqTrack = createSeqTrackWithOneFastqFile()
        final RawSequenceFile rawSequenceFile = CollectionUtils.exactlyOneElement(RawSequenceFile.findAllBySeqTrack(seqTrack))

        // service
        service.lsdfFilesService = Mock(LsdfFilesService) {
            _ * getFileFinalPathAsPath(_) >> Paths.get(rawSequenceFile.pathName).resolve(newRawSequenceFileName)
            _ * getFileViewByPidPathAsPath(_) >> Paths.get(rawSequenceFile.pathName).resolve(newRawSequenceFileName)
        }
        service.fileSystemService = Mock(FileSystemService) {
            _ * getRemoteFileSystem() >> FileSystems.default
        }

        // DTO
        final List<Swap<String>> rawSequenceFileSwaps = [new Swap(rawSequenceFile.fileName, newRawSequenceFileName)]
        StringBuilder log = new StringBuilder()
        final Map<RawSequenceFile, Map<String, ?>> oldRawSequenceFileNameMap = [
                (rawSequenceFile): [
                        (AbstractDataSwapService.DIRECT_FILE_NAME): rawSequenceFile.fileName,
                        (AbstractDataSwapService.VBP_FILE_NAME)   : 'viewByPidFile',
                ],
        ]

        final DataSwapParameters parameters = new DataSwapParameters(
                rawSequenceFileSwaps: rawSequenceFileSwaps,
                log: log
        )

        final DataSwapData dataSwapData = new DataSwapData(
                projectSwap: new Swap(rawSequenceFile.project, rawSequenceFile.project),
                parameters: parameters,
                rawSequenceFiles: [rawSequenceFile],
                oldRawSequenceFileNameMap: oldRawSequenceFileNameMap
        )

        when:
        service.renameRawSequenceFiles(dataSwapData)

        then:
        thrown FileNotFoundException
    }

    @Unroll
    void "cleanupLeftOverSamples, should create cleanup commands for samples and individual data if it has no samples left: #futherSample"() {
        given:
        service.sampleService = Mock(SampleService)
        final Individual individual = createIndividual()
        if (futherSample) {
            createSample(individual: individual)
        }
        final Path vbpPath = Paths.get("/vbpPath/")
        final Path sampleDir = Paths.get("/samplePath")

        final DataSwapData dataSwapData = new DataSwapData(
                individualSwap: new Swap(individual, null),
                cleanupIndividualPaths: [vbpPath],
                cleanupSampleTypePaths: [sampleDir],
        )

        when:
        service.cleanupLeftOverSamples(dataSwapData)

        then:
        futherSample && Individual.count || !Individual.count
        String bashScriptSnippet = dataSwapData.moveFilesCommands.join("\n")
        bashScriptSnippet.contains("rm -rf ${sampleDir}")

        String cleanupIndividualCommand = "rm -rf ${vbpPath}\n"
        futherSample && !cleanupIndividualCommand || cleanupIndividualCommand

        and:
        1 * service.sampleService.getSamplesByIndividual(_) >> []

        where:
        futherSample << [true, false]
    }

    @Unroll
    void "cleanupLeftOverIndividual, should create cleanup bash commands for individual and also delete it when cleanupDatabase is set"() {
        given:
        final Individual individual = createIndividual()
        final Path vbpPath = Paths.get("/vbpPath/")

        final DataSwapData dataSwapData = new DataSwapData(
                individualSwap: new Swap(individual, null),
                cleanupIndividualPaths: [vbpPath],
        )

        when:
        service.cleanupLeftOverIndividual(dataSwapData, cleanupDatabase)

        then:
        cleanupDatabase && !Individual.count || Individual.count
        String bashScriptSnippet = dataSwapData.moveFilesCommands.join("\n")
        bashScriptSnippet.contains("rm -rf ${vbpPath}")

        where:
        cleanupDatabase << [true, false]
    }

    private static Path createNonExistingFilePath(String fileName) {
        return Paths.get("not_existing").resolve(Paths.get(fileName))
    }

    private Path createExistingFilePath(String fileName) {
        return CreateFileHelper.createFile(tempDir.resolve(fileName))
    }

    private Map<RawSequenceFile, Map<String, ?>> createPathsForRawSequenceFiles(List<RawSequenceFile> rawSequenceFiles, boolean oldFilesExists = true, boolean newFilesExists = true) {
        return rawSequenceFiles.collectEntries {
            [(it): [
                    "oldFileName": it.fileName,
                    "oldPath"    : oldFilesExists ? createExistingFilePath(it.fileName) : createNonExistingFilePath(it.fileName),
                    "oldVbpPath" : oldFilesExists ? createExistingFilePath("Vbp${it.fileName}") : createNonExistingFilePath("Vbp${it.fileName}"),
                    "newFileName": "New${it.fileName}",
                    "newPath"    : newFilesExists ? createExistingFilePath("New${it.fileName}") : createNonExistingFilePath("New${it.fileName}"),
                    "newVbpPath" : newFilesExists ? createExistingFilePath("NewVbp${it.fileName}") : createNonExistingFilePath("NewVbp${it.fileName}"),
            ]]
        }
    }
}
