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
package de.dkfz.tbi.otp.dataswap

import grails.testing.gorm.DataTest
import grails.validation.Validateable
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.*

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.dataswap.data.DataSwapData
import de.dkfz.tbi.otp.dataswap.parameters.DataSwapParameters
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.nio.file.*

class DataSwapServiceSpec extends Specification implements DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                DataFile,
                Project,
                SampleType,
                SeqType,
                Sample,
                Individual,
                SeqTrack,
                ExternallyProcessedMergedBamFile,
                RoddyBamFile,
                AlignmentPass,
                MergingAssignment,
                Comment,
        ]
    }

    @Rule
    TemporaryFolder temporaryFolder

    @Shared
    DataSwapService service

    void setupSpec() {
        service = Spy(DataSwapService)
    }

    DataSwapService setupServiceWithAbstractMethods() {
        return Spy(type: new DataSwapService<DataSwapParameters, DataSwapData<DataSwapParameters>>() {
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
            protected void logSwapData(DataSwapData<DataSwapParameters> data) {
                // Is not functionally tested here
            }

            @Override
            protected void performDataSwap(DataSwapData<DataSwapParameters> data) {
                // Is not functionally tested here
            }

            @Override
            protected void createSwapComments(DataSwapData<DataSwapParameters> data) {
                // Is not functionally tested here
            }
        }.class) as DataSwapService
    }

    void "swap(P parameters), calls methods in the right order"() {
        given:
        DataSwapService service = setupServiceWithAbstractMethods()

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
        DataSwapService service = setupServiceWithAbstractMethods()

        final DataSwapData dataSwapData = new DataSwapData<DataSwapParameters>()

        when:
        service.swap(dataSwapData)

        then:
        1 * service.validateDTO(_ as Validateable) >> null

        then:
        1 * service.logSwapData(_ as DataSwapData<DataSwapParameters>) >> _

        then:
        1 * service.performDataSwap(_ as DataSwapData<DataSwapParameters>) >> _

        then:
        1 * service.createSwapComments(_ as DataSwapData<DataSwapParameters>) >> _
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
    void "collectFileNamesOfDataFiles, when single cell is #singleCell and label is #wellLabel, then return correct list"() {
        given:
        final DataFile dataFile = createDataFile(
                used: false,
                seqTrack: createSeqTrack([
                        seqType            : createSeqType([
                                singleCell: singleCell,
                        ]),
                        singleCellWellLabel: wellLabel,
                ]),
        )

        service.lsdfFilesService = Mock(LsdfFilesService) {
            1 * getFileFinalPath(dataFile) >> 'finalFile'
            1 * getFileViewByPidPath(dataFile) >> 'viewByPidFile'
            wellCount * getWellAllFileViewByPidPath(dataFile) >> 'wellFile'
            0 * _
        }
        service.singleCellService = Mock(SingleCellService) {
            wellCount * singleCellMappingFile(dataFile) >> Paths.get('wellMappingFile')
            wellCount * mappingEntry(dataFile) >> 'entry'
            0 * _
        }

        Map<DataFile, Map<String, ?>> expected = [
                (dataFile): [
                        (DataSwapService.DIRECT_FILE_NAME): 'finalFile',
                        (DataSwapService.VBP_FILE_NAME)   : 'viewByPidFile',
                ],
        ]
        if (wellCount) {
            expected[dataFile] << [
                    (DataSwapService.WELL_FILE_NAME)              : 'wellFile',
                    (DataSwapService.WELL_MAPPING_FILE_NAME)      : Paths.get('wellMappingFile'),
                    (DataSwapService.WELL_MAPPING_FILE_ENTRY_NAME): 'entry',
            ]
        }

        expect:
        expected == service.collectFileNamesOfDataFiles([dataFile])

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
        final DataFile dataFile = createDataFile(
                used: false,
                seqTrack: createSeqTrack([
                        seqType            : createSeqType([
                                singleCell: singleCell,
                        ]),
                        singleCellWellLabel: wellLabel,
                ]),
        )

        expect:
        service.createSingeCellScript(dataFile, [:]) == ''

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

        final String NEW_FINAL_PATH = "newFinalPath"
        final String NEW_PATH = 'newPath'
        final String NEW_ALL_PATH = "${NEW_PATH}/all"
        final String NEW_WELL_PATH = "${NEW_ALL_PATH}/newFile"
        final String NEW_MAPPING_PATH = "${NEW_ALL_PATH}/mapping"
        final String NEW_ENTRY = 'newEntry\tvalue'

        final DataFile dataFile = createDataFile(
                used: false,
                seqTrack: createSeqTrack([
                        seqType            : createSeqType([
                                singleCell: true,
                        ]),
                        singleCellWellLabel: 'WELL',
                ]),
        )

        service.lsdfFilesService = Mock(LsdfFilesService) {
            1 * getFileFinalPath(dataFile) >> NEW_FINAL_PATH
            1 * getWellAllFileViewByPidPath(dataFile) >> NEW_WELL_PATH
            0 * _
        }

        service.singleCellService = Mock(SingleCellService) {
            1 * singleCellMappingFile(dataFile) >> Paths.get(NEW_MAPPING_PATH)
            1 * mappingEntry(dataFile) >> NEW_ENTRY
            0 * _
        }

        final Map<String, ?> oldValues = [
                (DataSwapService.DIRECT_FILE_NAME)            : OLD_FINAL_PATH,
                (DataSwapService.WELL_FILE_NAME)              : OLD_WELL_PATH,
                (DataSwapService.WELL_MAPPING_FILE_NAME)      : Paths.get(OLD_MAPPING_PATH),
                (DataSwapService.WELL_MAPPING_FILE_ENTRY_NAME): OLD_ENTRY,
        ]

        final String expectedScript = """
                                |# Single Cell structure
                                |## recreate link
                                |rm -f '${OLD_WELL_PATH}'
                                |mkdir -p -m 2750 '${NEW_ALL_PATH}'
                                |ln -s '${NEW_FINAL_PATH}' \\\n      '${NEW_WELL_PATH}'
                                |
                                |## remove entry from old mapping file
                                |sed -i '\\#${OLD_ENTRY}#d' ${OLD_MAPPING_PATH}
                                |
                                |## add entry to new mapping file
                                |touch '${NEW_MAPPING_PATH}'
                                |echo '${NEW_ENTRY}' >> '${NEW_MAPPING_PATH}'
                                |
                                |## delete mapping file, if empty
                                |if [ ! -s '${OLD_MAPPING_PATH}' ]
                                |then
                                |    rm '${OLD_MAPPING_PATH}'
                                |fi
                                |""".stripMargin()

        when:
        String script = service.createSingeCellScript(dataFile, oldValues)

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

        final List<Swap<String>> completedSwapList = [
                new Swap(oldValue, newValue),
                new Swap(oldValue, oldValue),
                new Swap(oldValue, oldValue),
        ]

        final String filledLog = "\n    - ${oldValue} --> ${newValue}" +
                "\n    - ${oldValue} --> ${oldValue}" +
                "\n    - ${oldValue} --> ${oldValue}"

        when:
        List<Swap<String>> resultList = service.completeOmittedNewSwapValuesAndLog(swapList, log)

        then:
        resultList == completedSwapList
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
        final Path oldPath = temporaryFolder.newFile().toPath()
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
        final Path oldPath = temporaryFolder.newFile().toPath()
        final Path newPath = temporaryFolder.newFile().toPath()

        final String fileExistsComment = "# new file already exists: '${newPath}'; delete old file\n# rm -f '${oldPath}'\n"

        when:
        String bashCommand = service.generateMaybeMoveBashCommand(oldPath, newPath, null)

        then:
        bashCommand == fileExistsComment
    }

    void "generateMaybeMoveBashCommand, when new file exists and it is the same as the old one create comment"() {
        given:
        final Path oldPath = temporaryFolder.newFile().toPath()

        final String fileExistsComment = "# the old and the new data file ('${oldPath}') are the same, no move needed.\n"

        when:
        String bashCommand = service.generateMaybeMoveBashCommand(oldPath, oldPath, null)

        then:
        bashCommand == fileExistsComment
    }

    void "generateMaybeMoveBashCommand, when old file not exists but new file do create move manually comment"() {
        given:
        final Path oldPath = Paths.get("not_existing_old_file")
        final Path newPath = temporaryFolder.newFile().toPath()

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
        final Path scriptOutputDirectory = temporaryFolder.newFolder("files").toPath()

        service.fileService = Mock(FileService) {
            1 * createOrOverwriteScriptOutputFile(_, _, _) >> Files.createFile(scriptOutputDirectory.resolve("restartAli_${bashScriptName}.groovy"))
        }

        final DataSwapParameters parameters = new DataSwapParameters(
                bashScriptName: bashScriptName,
                scriptOutputDirectory: scriptOutputDirectory
        )

        final DataSwapData dataSwapData = new DataSwapData<DataSwapParameters>(parameters: parameters)

        when:
        service.createGroovyConsoleScriptToRestartAlignments(dataSwapData, realm)

        then:
        scriptOutputDirectory.toFile().listFiles().length != 0
        File alignmentScript = scriptOutputDirectory.resolve("restartAli_${bashScriptName}.groovy").toFile()
        alignmentScript.exists()
        alignmentScript.text == DataSwapService.ALIGNMENT_SCRIPT_HEADER
    }

    void "createGroovyConsoleScriptToRestartAlignments, when seq track given write comment header and seq track comments to script file"() {
        given:
        final Realm realm = createRealm()
        final List<SeqTrack> seqTrackList = (0..1).collect { createSeqTrack() }

        final String bashScriptName = "TEST-SCRIPT"
        final Path scriptOutputDirectory = temporaryFolder.newFolder("files").toPath()

        service.fileService = Mock(FileService) {
            1 * createOrOverwriteScriptOutputFile(_, _, _) >> Files.createFile(scriptOutputDirectory.resolve("restartAli_${bashScriptName}.groovy"))
        }

        final DataSwapParameters parameters = new DataSwapParameters(
                bashScriptName: bashScriptName,
                scriptOutputDirectory: scriptOutputDirectory
        )

        final DataSwapData dataSwapData = new DataSwapData<DataSwapParameters>(parameters: parameters, seqTrackList: seqTrackList)

        final String expectedAlignmentScript = DataSwapService.ALIGNMENT_SCRIPT_HEADER + "    ${seqTrackList[0].id},  //${seqTrackList[0]}\n" +
                "    ${seqTrackList[1].id},  //${seqTrackList[1]}\n"

        when:
        service.createGroovyConsoleScriptToRestartAlignments(dataSwapData, realm)

        then:
        scriptOutputDirectory.toFile().listFiles().length != 0
        File alignmentScript = scriptOutputDirectory.resolve("restartAli_${bashScriptName}.groovy").toFile()
        alignmentScript.exists()
        alignmentScript.text == expectedAlignmentScript
    }

    void "copyAndRemoveFastQcFile, when no checksum files exists create just one command"() {
        given:
        final String oldFilePath = temporaryFolder.newFile()
        final String fileMoveCommand = "# the old and the new data file ('${oldFilePath}') are the same, no move needed.\n\n"

        when:
        // no need to test all scenarios since generateMaybeMoveBashCommand is tested separately
        String bashCommand = service.copyAndRemoveFastQcFile(oldFilePath, oldFilePath, null)

        then:
        bashCommand == fileMoveCommand
    }

    void "copyAndRemoveFastQcFile, when checksum files exists create one for data files and one for checksum files"() {
        given:
        final Path oldFilePath = temporaryFolder.newFile().toPath()
        final Path checksum = temporaryFolder.newFile(oldFilePath.fileName.toString() + ".md5sum").toPath()
        final String fileMoveCommand = "# the old and the new data file ('${oldFilePath}') are the same, no move needed.\n\n" +
                "# the old and the new data file ('${checksum}') are the same, no move needed.\n"

        when:
        // no need to test all scenarios since generateMaybeMoveBashCommand is tested separately
        String bashCommand = service.copyAndRemoveFastQcFile(oldFilePath.toString(), oldFilePath.toString(), null)

        then:
        bashCommand == fileMoveCommand
    }

    @Unroll
    void "createCommentForSwappedDatafiles, when no comments exists call saveComment #size times with #comment"() {
        given:
        final List<DataFile> dataFiles = (1..size).collect { createDataFile() }
        final DataSwapData dataSwapData = new DataSwapData<DataSwapParameters>(dataFiles: dataFiles)

        service.commentService = Mock(CommentService)

        when:
        service.createCommentForSwappedDatafiles(dataSwapData)

        then:
        size * service.commentService.saveComment(_, comment)

        where:
        comment = "Attention: Datafile swapped!"
        size = 5
    }

    @Unroll
    void "createCommentForSwappedDatafiles, when already comments exists call saveComment #size times with #comment"() {
        given:
        final List<DataFile> dataFiles = (1..size).collect {
            createDataFile([
                    comment: new Comment(comment: someComment, modificationDate: new Date(), author: "SomeAuthor")
            ])
        }
        final DataSwapData dataSwapData = new DataSwapData<DataSwapParameters>(dataFiles: dataFiles)

        service.commentService = Mock(CommentService)

        when:
        service.createCommentForSwappedDatafiles(dataSwapData)

        then:
        size * service.commentService.saveComment(_, comment)

        where:
        someComment = "Some comment"
        swapComment = "Attention: Datafile swapped!"
        comment = someComment + "\n" + swapComment
        size = 5
    }

    void "renameDataFiles, when old dataFile is withdrawn and libraryLayout is not SINGLE fail with AssertError"() {
        given:
        final String newDataFileName = 'newDataFileName.gz'

        // domain
        final SeqTrack seqTrack = createSeqTrackWithOneDataFile([seqType: createSeqType([libraryLayout: SequencingReadType.PAIRED])])
        DataFile dataFile = CollectionUtils.exactlyOneElement(DataFile.findAllBySeqTrack(seqTrack))
        dataFile.fileType.vbpPath = "/sequence/"
        dataFile.mateNumber = null
        dataFile.fileWithdrawn = true
        final Path oldFile = temporaryFolder.newFile(dataFile.fileName).toPath()
        final Path oldFileViewByPid = temporaryFolder.newFile('oldViewByPidFile').toPath()
        final Path newFile = Paths.get('somePath').resolve(Paths.get(newDataFileName))
        final Path newFileViewByPid = Paths.get('linking').resolve(Paths.get('newViewByPidFile'))

        // service
        service.lsdfFilesService = Mock(LsdfFilesService) {
            _ * getFileFinalPath(_) >> newFile.toString()
            _ * getFileViewByPidPath(_) >> newFileViewByPid.toString()
        }

        // DTO
        final List<Swap<String>> dataFileSwaps = [new Swap(dataFile.fileName, newDataFileName)]
        StringBuilder log = new StringBuilder()
        final Map<DataFile, Map<String, ?>> oldDataFileNameMap = [
                (dataFile): [
                        (DataSwapService.DIRECT_FILE_NAME): oldFile.toString(),
                        (DataSwapService.VBP_FILE_NAME)   : oldFileViewByPid.toString(),
                ],
        ]

        final DataSwapParameters parameters = new DataSwapParameters(
                dataFileSwaps: dataFileSwaps,
                log: log
        )

        final DataSwapData dataSwapData = new DataSwapData(
                projectSwap: new Swap(dataFile.project, dataFile.project),
                parameters: parameters,
                dataFiles: [dataFile],
                oldDataFileNameMap: oldDataFileNameMap
        )

        when:
        service.renameDataFiles(dataSwapData)

        then:
        thrown AssertionError
    }

    void "renameDataFiles, when old dataFile is withdrawn log it and set mateNumber to 1"() {
        given:
        final String newDataFileName = 'newDataFileName.gz'

        // domain
        final SeqTrack seqTrack = createSeqTrackWithOneDataFile()
        DataFile dataFile = CollectionUtils.exactlyOneElement(DataFile.findAllBySeqTrack(seqTrack))
        dataFile.fileType.vbpPath = "/sequence/"
        dataFile.mateNumber = null
        dataFile.fileWithdrawn = true
        final String oldDataFileName = dataFile.fileName
        final Path oldFile = temporaryFolder.newFile(dataFile.fileName).toPath()
        final Path oldFileViewByPid = temporaryFolder.newFile('oldViewByPidFile').toPath()
        final Path newFile = Paths.get('somePath').resolve(Paths.get(newDataFileName))
        final Path newFileViewByPid = Paths.get('linking').resolve(Paths.get('newViewByPidFile'))

        // service
        service.lsdfFilesService = Mock(LsdfFilesService) {
            _ * getFileFinalPath(_) >> newFile.toString()
            _ * getFileViewByPidPath(_) >> newFileViewByPid.toString()
        }

        // DTO
        final List<Swap<String>> dataFileSwaps = [new Swap(dataFile.fileName, newDataFileName)]
        StringBuilder log = new StringBuilder()
        final Map<DataFile, Map<String, ?>> oldDataFileNameMap = [
                (dataFile): [
                        (DataSwapService.DIRECT_FILE_NAME): oldFile.toString(),
                        (DataSwapService.VBP_FILE_NAME)   : oldFileViewByPid.toString(),
                ],
        ]

        final DataSwapParameters parameters = new DataSwapParameters(
                dataFileSwaps: dataFileSwaps,
                log: log
        )

        final DataSwapData dataSwapData = new DataSwapData(
                projectSwap: new Swap(dataFile.project, dataFile.project),
                parameters: parameters,
                dataFiles: [dataFile],
                oldDataFileNameMap: oldDataFileNameMap
        )

        when:
        service.renameDataFiles(dataSwapData)

        then:
        dataFile.mateNumber == 1
        log.toString() == "\n====> set mate number for withdrawn data file\n    changed ${oldDataFileName} to ${dataFile.fileName}"
    }

    void "renameDataFiles, when old files exists but new files not then move old files to new location and link new"() {
        given:
        final String newDataFileName = 'newDataFileName.gz'

        // domain
        final SeqTrack seqTrack = createSeqTrackWithOneDataFile()
        final DataFile dataFile = CollectionUtils.exactlyOneElement(DataFile.findAllBySeqTrack(seqTrack))
        final String oldDataFileName = dataFile.fileName
        final Path oldFile = temporaryFolder.newFile(dataFile.fileName).toPath()
        final Path oldFileViewByPid = temporaryFolder.newFile('oldViewByPidFile').toPath()
        final Path newFile = Paths.get('somePath').resolve(Paths.get(newDataFileName))
        final Path newFileViewByPid = Paths.get('linking').resolve(Paths.get('newViewByPidFile'))

        // service
        service.lsdfFilesService = Mock(LsdfFilesService) {
            _ * getFileFinalPath(_) >> newFile.toString()
            _ * getFileViewByPidPath(_) >> newFileViewByPid.toString()
        }

        // DTO
        final List<Swap<String>> dataFileSwaps = [new Swap(dataFile.fileName, newDataFileName)]
        StringBuilder log = new StringBuilder()
        final Map<DataFile, Map<String, ?>> oldDataFileNameMap = [
                (dataFile): [
                        (DataSwapService.DIRECT_FILE_NAME): oldFile.toString(),
                        (DataSwapService.VBP_FILE_NAME)   : oldFileViewByPid.toString(),
                ],
        ]

        final DataSwapParameters parameters = new DataSwapParameters(
                dataFileSwaps: dataFileSwaps,
                log: log
        )

        final DataSwapData dataSwapData = new DataSwapData(
                projectSwap: new Swap(dataFile.project, dataFile.project),
                parameters: parameters,
                dataFiles: [dataFile],
                oldDataFileNameMap: oldDataFileNameMap
        )

        final String bashMoveDirectFile = """\n
                                     |# ${dataFile.seqTrack} ${newDataFileName}
                                     |mkdir -p -m 2750 '${newFile.parent}';
                                     |mv '${oldFile}' \\
                                     |   '${newFile}';
                                     |chgrp -h `stat -c '%G' ${newFile.parent}` ${newFile}
                                     |if [ -e '${oldFile}.md5sum' ]; then
                                     |  mv '${oldFile}.md5sum' \\
                                     |     '${newFile}.md5sum';
                                     |  chgrp -h `stat -c '%G' ${newFile.parent}` ${newFile}.md5sum
                                     |fi\n""".stripMargin()

        final String bashMoveVbpFile = """\
                                 |rm -f '${oldFileViewByPid}';
                                 |mkdir -p -m 2750 '${newFileViewByPid.parent}';
                                 |ln -s '${newFile}' \\
                                 |      '${newFileViewByPid}'""".stripMargin()

        String bashScriptToMoveFiles = "${bashMoveDirectFile}\n${bashMoveVbpFile}\n"
        bashScriptToMoveFiles += "\n\n"

        when:
        String script = service.renameDataFiles(dataSwapData)

        then:
        bashScriptToMoveFiles == script
        dataFile.fileName == newDataFileName
        dataFile.vbpFileName == newDataFileName
        log.toString() == "\n    changed ${oldDataFileName} to ${dataFile.fileName}"
    }

    void "renameDataFiles, when old and new data files exists then remove old files and link new"() {
        given:
        final String newDataFileName = 'newDataFileName.gz'

        // domain
        final SeqTrack seqTrack = createSeqTrackWithOneDataFile()
        final DataFile dataFile = CollectionUtils.exactlyOneElement(DataFile.findAllBySeqTrack(seqTrack))
        final String oldDataFileName = dataFile.fileName
        final Path oldFile = temporaryFolder.newFile(dataFile.fileName).toPath()
        final Path oldFileViewByPid = temporaryFolder.newFile('oldViewByPidFile').toPath()
        final Path newFile = temporaryFolder.newFile(newDataFileName).toPath()
        final Path newFileViewByPid = temporaryFolder.newFolder('linking').toPath().resolve(newDataFileName)
        newFileViewByPid.toFile().mkdirs()

        // service
        service.lsdfFilesService = Mock(LsdfFilesService) {
            _ * getFileFinalPath(_) >> newFile.toString()
            _ * getFileViewByPidPath(_) >> newFileViewByPid.toString()
        }

        // DTO
        final List<Swap<String>> dataFileSwaps = [new Swap(dataFile.fileName, newDataFileName)]
        StringBuilder log = new StringBuilder()
        final Map<DataFile, Map<String, ?>> oldDataFileNameMap = [
                (dataFile): [
                        (DataSwapService.DIRECT_FILE_NAME): oldFile.toString(),
                        (DataSwapService.VBP_FILE_NAME)   : oldFileViewByPid.toString(),
                ],
        ]

        final DataSwapParameters parameters = new DataSwapParameters(
                dataFileSwaps: dataFileSwaps,
                log: log
        )

        final DataSwapData dataSwapData = new DataSwapData(
                projectSwap: new Swap(dataFile.project, dataFile.project),
                parameters: parameters,
                dataFiles: [dataFile],
                oldDataFileNameMap: oldDataFileNameMap
        )

        String bashMoveDirectFile = "# rm -f '${oldFile}'"
        String bashMoveVbpFile = """\
                                 |rm -f '${oldFileViewByPid}';
                                 |mkdir -p -m 2750 '${newFileViewByPid.parent}';
                                 |ln -s '${newFile}' \\
                                 |      '${newFileViewByPid}'""".stripMargin()

        String bashScriptToMoveFiles = "${bashMoveDirectFile}\n${bashMoveVbpFile}\n"
        bashScriptToMoveFiles += "\n\n"

        when:
        String script = service.renameDataFiles(dataSwapData)

        then:
        bashScriptToMoveFiles == script
        dataFile.fileName == newDataFileName
        dataFile.vbpFileName == newDataFileName
        log.toString() == "\n    changed ${oldDataFileName} to ${dataFile.fileName}"
    }

    void "renameDataFiles, when old file is singleCell then also create singleCellScript"() {
        given:
        final String newDataFileName = 'newDataFileName.gz'

        // domain
        final SeqTrack seqTrack = createSeqTrackWithOneDataFile([seqType: createSeqType([singleCell: true,]), singleCellWellLabel: 'WELL'], [used: false])
        final DataFile dataFile = CollectionUtils.exactlyOneElement(DataFile.findAllBySeqTrack(seqTrack))
        final String oldDataFileName = dataFile.fileName
        final Path oldFile = temporaryFolder.newFile(dataFile.fileName).toPath()
        final Path oldFileViewByPid = temporaryFolder.newFile('oldViewByPidFile').toPath()
        final Path newFile = temporaryFolder.newFile(newDataFileName).toPath()
        final Path newFileViewByPid = temporaryFolder.newFolder('linking').toPath().resolve(newDataFileName)
        newFileViewByPid.toFile().mkdirs()
        final Path oldWellFile = temporaryFolder.newFile('wellFile').toPath()
        final Path oldWellMappingFile = temporaryFolder.newFile('wellMappingFile').toPath()
        final String oldWellMappingFileEntryName = 'oldWellMappingEntry'
        final Path newWellMappingFile = temporaryFolder.newFile('newWellMappingFile').toPath()
        final String newWellMappingFileEntryName = 'newWellMappingEntry'

        // service
        service.lsdfFilesService = Mock(LsdfFilesService) {
            _ * getFileFinalPath(_) >> newFile.toString()
            _ * getFileViewByPidPath(_) >> newFileViewByPid.toString()
            _ * getWellAllFileViewByPidPath(_) >> newFileViewByPid.toString()
        }

        service.singleCellService = Mock(SingleCellService) {
            _ * singleCellMappingFile(_) >> newWellMappingFile
            _ * mappingEntry(_) >> newWellMappingFileEntryName
        }

        // DTO
        final List<Swap<String>> dataFileSwaps = [new Swap(dataFile.fileName, newDataFileName)]
        StringBuilder log = new StringBuilder()
        final Map<DataFile, Map<String, ?>> oldDataFileNameMap = [
                (dataFile): [
                        (DataSwapService.DIRECT_FILE_NAME)            : oldFile.toString(),
                        (DataSwapService.VBP_FILE_NAME)               : oldFileViewByPid.toString(),
                        (DataSwapService.WELL_FILE_NAME)              : oldWellFile.toString(),
                        (DataSwapService.WELL_MAPPING_FILE_NAME)      : oldWellMappingFile,
                        (DataSwapService.WELL_MAPPING_FILE_ENTRY_NAME): oldWellMappingFileEntryName,
                ],
        ]

        final DataSwapParameters parameters = new DataSwapParameters(
                dataFileSwaps: dataFileSwaps,
                log: log
        )

        final DataSwapData dataSwapData = new DataSwapData(
                projectSwap: new Swap(dataFile.project, dataFile.project),
                parameters: parameters,
                dataFiles: [dataFile],
                oldDataFileNameMap: oldDataFileNameMap
        )

        final String bashMoveDirectFile = "# rm -f '${oldFile}'"
        final String bashMoveVbpFile = """\
                                 |rm -f '${oldFileViewByPid}';
                                 |mkdir -p -m 2750 '${newFileViewByPid.parent}';
                                 |ln -s '${newFile}' \\
                                 |      '${newFileViewByPid}'""".stripMargin()

        String bashScriptToMoveFiles = "${bashMoveDirectFile}\n${bashMoveVbpFile}\n"
        bashScriptToMoveFiles += """
                                 |# Single Cell structure
                                 |## recreate link
                                 |rm -f '${oldWellFile}'
                                 |mkdir -p -m 2750 '${newFileViewByPid.parent}'
                                 |ln -s '${newFile}' \\\n      '${newFileViewByPid}'
                                 |\n## remove entry from old mapping file
                                 |sed -i '\\#${oldWellMappingFileEntryName}#d' ${oldWellMappingFile}
                                 |\n## add entry to new mapping file
                                 |touch '${newWellMappingFile}'
                                 |echo '${newWellMappingFileEntryName}' >> '${newWellMappingFile}'
                                 |\n## delete mapping file, if empty
                                 |if [ ! -s '${oldWellMappingFile}' ]
                                 |then
                                 |    rm '${oldWellMappingFile}'
                                 |fi\n""".stripMargin()
        bashScriptToMoveFiles += "\n\n"

        when:
        String script = service.renameDataFiles(dataSwapData)

        then:
        bashScriptToMoveFiles == script
        dataFile.fileName == newDataFileName
        dataFile.vbpFileName == newDataFileName
        log.toString() == "\n    changed ${oldDataFileName} to ${dataFile.fileName}"
    }

    void "renameDataFiles, when old data files can not be found fail with FileNotFoundException"() {
        given:
        final String newDataFileName = 'newDataFileName.gz'

        // domain
        final SeqTrack seqTrack = createSeqTrackWithOneDataFile()
        final DataFile dataFile = CollectionUtils.exactlyOneElement(DataFile.findAllBySeqTrack(seqTrack))

        // service
        service.lsdfFilesService = Mock(LsdfFilesService) {
            _ * getFileFinalPath(_) >> Paths.get(dataFile.pathName).resolve(newDataFileName)
            _ * getFileViewByPidPath(_) >> Paths.get(dataFile.pathName).resolve(newDataFileName)
        }

        // DTO
        final List<Swap<String>> dataFileSwaps = [new Swap(dataFile.fileName, newDataFileName)]
        StringBuilder log = new StringBuilder()
        final Map<DataFile, Map<String, ?>> oldDataFileNameMap = [
                (dataFile): [
                        (DataSwapService.DIRECT_FILE_NAME): dataFile.fileName,
                        (DataSwapService.VBP_FILE_NAME)   : 'viewByPidFile',
                ],
        ]

        final DataSwapParameters parameters = new DataSwapParameters(
                dataFileSwaps: dataFileSwaps,
                log: log
        )

        final DataSwapData dataSwapData = new DataSwapData(
                projectSwap: new Swap(dataFile.project, dataFile.project),
                parameters: parameters,
                dataFiles: [dataFile],
                oldDataFileNameMap: oldDataFileNameMap
        )

        when:
        service.renameDataFiles(dataSwapData)

        then:
        thrown FileNotFoundException
    }
}
