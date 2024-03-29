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
package de.dkfz.tbi.otp.dataswap

import grails.gorm.transactions.Transactional
import grails.validation.Validateable
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataAllWellFileService
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataViewFileService
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataWorkFileService
import de.dkfz.tbi.otp.utils.exceptions.FileNotFoundException
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.AnalysisDeletionService
import de.dkfz.tbi.otp.dataswap.data.DataSwapData
import de.dkfz.tbi.otp.dataswap.parameters.DataSwapParameters
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.DeletionService

import java.nio.file.*

/**
 * Abstract class for an data swap service. Defines basic steps necessary to perform some sort of an data swap.
 * Used as base of {@link SampleSwapService}.
 *
 * @param < P >  of type DataSwapParameters build in the scripts containing only the names/ids of the entities to swap.
 * @param < D >  of type DataSwapData<P extends DataSwapParameters> build in {@link #buildDataDTO} containing all entities
 *        necessary to perform swap.
 */
@SuppressWarnings("JavaIoPackageAccess")
@CompileDynamic
@Transactional
abstract class AbstractDataSwapService<P extends DataSwapParameters, D extends DataSwapData<P>> {

    static final String DIRECT_FILE_NAME = "directFileName"
    static final String VBP_FILE_NAME = "vbpFileName"
    static final String WELL_FILE_NAME = "wellFileName"
    static final String WELL_MAPPING_FILE_NAME = "wellMappingFileName"
    static final String WELL_MAPPING_FILE_ENTRY_NAME = "wellMappingFileEntry"

    static final String BASH_HEADER = """\
            #!/bin/bash

            #PLEASE CHECK THE COMMANDS CAREFULLY BEFORE RUNNING THE SCRIPT

            set -e
            set -v

            """.stripIndent()

    static final String ALIGNMENT_SCRIPT_HEADER = "// ids of seqtracks which should be triggered with 'TriggerAlignment.groovy' for alignment\n\n"

    CommentService commentService
    FastqcDataFilesService fastqcDataFilesService
    ConfigService configService
    SeqTrackService seqTrackService
    FileService fileService
    DeletionService deletionService
    SingleCellService singleCellService
    IndividualService individualService
    AnalysisDeletionService analysisDeletionService
    FileSystemService fileSystemService
    SampleService sampleService
    RawSequenceDataWorkFileService rawSequenceDataWorkFileService
    RawSequenceDataViewFileService rawSequenceDataViewFileService
    RawSequenceDataAllWellFileService rawSequenceDataAllWellFileService

    /**
     * Logs various arguments of DataSwapParameters in DataSwapParameters.log that can be examined later in the script output.
     *
     * @param parameters build in the scripts containing only the names/ids of the entities to swap.
     */
    protected abstract void logSwapParameters(P parameters)

    /**
     * Define which list gets its swaps completed, logs it and delegates to completeOmittedNewSwapValuesAndLog(List<Swap<String>> swapList, StringBuilder log).
     *
     * @param parameters build in the scripts containing only the names/ids of the entities to swap.
     */
    protected abstract void completeOmittedNewSwapValuesAndLog(P parameters)

    /**
     * Gathers the real data specified in DataSwapParameters DTO and creates a new DataSwapData DTO from it.
     *
     * @param parameters build in the scripts containing only the names/ids of the entities to swap.
     * @return D of type DataSwapData<P extends DataSwapParameters> containing all entities necessary to perform a swap.
     */
    protected abstract D buildDataDTO(P parameters)

    /**
     * Performs the actual data swap, changing database entries and creates scripts to move the datafiles.
     *
     * @param data build by buildDataDTO(P parameters) containing all entities necessary to perform a swap.
     */
    protected abstract void performDataSwap(D data)

    /**
     * Creates comments for the data swap and save them together with the swapped entities in the database.
     *
     * @param data build by buildDataDTO(P parameters) containing all entities necessary to perform a swap.
     */
    protected abstract void createSwapComments(D data)

    /**
     * Clean up left over empty data and create bash commands for empty file structures after a swap.
     *
     * @param data build by buildDataDTO(P parameters) containing all entities necessary to perform a swap.
     */
    protected abstract void cleanupLeftOvers(D data)

    /**
     * Takes a {@link grails.validation.Validateable} and perform a validation.
     *
     * @param validateable object to validate
     * @throws AssertionError with validation errors if Validateable is not valid
     */
    void validateDTO(Validateable validateable) throws AssertionError {
        if (!validateable.validate()) {
            throw new AssertionError(validateable.errors)
        }
    }

    /**
     * Perform preparation steps and convert the DataSwapParameters into the DataSwapData DTO.
     * Finally delegates the DataSwapData to swap(D data). It calls various abstracts methods which must
     * be implemented by a concrete service.
     *
     * this method is only used in the data swap scripts!!
     *
     * @param parameters build in the scripts containing only the names/ids of the entities to swap.
     * @throws IOException if file operations are failing.
     * @throws AssertionError if parameters constraints are broken.
     */
    void swap(P parameters) throws IOException, AssertionError {
        validateDTO(parameters)
        logSwapParameters(parameters)
        completeOmittedNewSwapValuesAndLog(parameters)
        swap(buildDataDTO(parameters))
    }

    /**
     * Perform data validation, script creation and the data swap with the real entities by the given DataSwapData DTO.
     * It calls various abstracts methods which must be implemented by a concrete service.
     *
     * this method should not be called in the data swap scripts!!
     *
     * @param data build by swap(P parameters) containing all entities necessary to perform swap.
     * @throws IOException if file operations are failing.
     * @throws AssertionError if parameters constraints are broken.
     */
    protected void swap(D data) throws IOException, AssertionError {
        validateDTO(data)
        checkThatNoAnalysisIsRunning(data)
        createGroovyConsoleScriptToRestartAlignments(data)
        markSeqTracksAsSwappedAndDeleteDependingObjects(data)
        performDataSwap(data)
        createSwapComments(data)
        cleanupLeftOvers(data)
        createMoveFilesScript(data)
    }

    /**
     * validates a move and returns the bash-command to move an old file to a new location.
     */
    String generateMaybeMoveBashCommand(Path oldPath, Path newPath, DataSwapData data) {
        String bashCommand = ""
        if (Files.exists(oldPath)) {
            if (Files.exists(newPath)) {
                if (Files.isSameFile(oldPath, newPath)) {
                    bashCommand = "# the old and the new data file ('${oldPath}') are the same, no move needed.\n"
                } else {
                    bashCommand = "# new file already exists: '${newPath}'; delete old file\n rm -f '${oldPath}'\n"
                }
            } else {
                bashCommand = """
                              mkdir -p -m 2750 '${newPath.parent}';
                              mv '${oldPath}' \\
                                 '${newPath}';
                              ${getFixGroupCommand(newPath)}\n
                              """.stripIndent()
            }
        } else if (Files.exists(newPath)) {
            bashCommand = "# no old file, and ${newPath} is already at the correct position (apparently put there manually?)\n"
        } else {
            String message = """The file can not be found at either old or new location:
                                oldName: ${oldPath}
                                newName: ${newPath}
                             """.stripIndent()
            if (data.failOnMissingFiles) {
                throw new FileNotFoundException(message)
            } else {
                data.log << '\n' << message
            }
        }
        return bashCommand
    }

    /**
     * Writes list size and entries in given StringBuilder log.
     *
     * @param list which should be logged
     * @param label which should be used
     * @param log StingBuilder to be written into
     */
    void logListEntries(List list, String label, StringBuilder log) {
        log << "\n  ${label} (${list.size()}):"
        list.each { log << "\n    - ${it}" }
    }

    /**
     * Creates bash command to fix the group
     *
     * @param path on which the fix should be performed
     * @return command to fix the group
     */
    String getFixGroupCommand(Path path) {
        return "chgrp -h `stat -c '%G' ${path.parent}` ${path}"
    }

    /**
     * function to rename data files and connect to a new project.
     * It is also checked, that the files and the view by pid links do not exist anymore in the old directory, but exist in
     * the new directory.
     */
    String renameRawSequenceFiles(D data) throws AssertionError {
        assert data.rawSequenceFiles*.id as Set == data.oldRawSequenceFileNameMap.keySet()*.id as Set

        String bashScriptToMoveFiles = ""

        data.rawSequenceFiles.each {
            String oldFilename = it.fileName
            it.project = data.projectSwap.new
            it.fileName = it.vbpFileName = data.rawSequenceFileSwaps.find { swap -> swap.old == it.fileName }.new
            it.seqTrack.workflowArtefact?.producedBy?.project = data.projectSwap.new
            it.seqTrack.workflowArtefact?.producedBy?.save(flush: false)
            if (it.mateNumber == null && it.fileWithdrawn && it.fileType &&
                    it.fileType.type == FileType.Type.SEQUENCE && it.fileType.vbpPath == "/sequence/") {
                data.log << "\n====> set mate number for withdrawn data file"
                assert it.seqTrack.seqType.libraryLayout == SequencingReadType.SINGLE: "sequencing read type is not ${SequencingReadType.SINGLE}"
                it.mateNumber = 1
            }
            it.save(flush: true)
            data.log << "\n    changed ${oldFilename} to ${it.fileName}"

            bashScriptToMoveFiles += createRenameRawSequenceFileCommands(oldFilename, it, data)
        }
        return bashScriptToMoveFiles
    }

    /**
     * creates a map containing for every Datafile of the list the direct and the viewByPid file name as map.
     */
    Map<RawSequenceFile, Map<String, String>> collectFileNamesOfRawSequenceFiles(List<RawSequenceFile> rawSequenceFiles) {
        Map<RawSequenceFile, Map<String, String>> map = [:]
        rawSequenceFiles.each { RawSequenceFile rawSequenceFile ->
            String directFileName = rawSequenceDataWorkFileService.getFilePath(rawSequenceFile)
            String vbpFileName = rawSequenceDataViewFileService.getFilePath(rawSequenceFile)
            map[rawSequenceFile] = [(DIRECT_FILE_NAME): directFileName, (VBP_FILE_NAME): vbpFileName]
            if (rawSequenceFile.seqType.singleCell && rawSequenceFile.seqTrack.singleCellWellLabel) {
                map[rawSequenceFile][WELL_FILE_NAME] = rawSequenceDataAllWellFileService.getFilePath(rawSequenceFile).toString()
                map[rawSequenceFile][WELL_MAPPING_FILE_NAME] = singleCellService.singleCellMappingFile(rawSequenceFile).toString()
                map[rawSequenceFile][WELL_MAPPING_FILE_ENTRY_NAME] = singleCellService.mappingEntry(rawSequenceFile)
            }
        }
        return map
    }

    String createSingeCellScript(RawSequenceFile rawSequenceFile, Map<String, String> oldValues) {
        if (!rawSequenceFile.seqType.singleCell || !rawSequenceFile.seqTrack.singleCellWellLabel) {
            return ''
        }
        String newDirectFileName = rawSequenceDataWorkFileService.getFilePath(rawSequenceFile)
        String newWellFileName = rawSequenceDataAllWellFileService.getFilePath(rawSequenceFile)
        File wellFile = new File(newWellFileName)

        Path mappingFile = singleCellService.singleCellMappingFile(rawSequenceFile)
        String mappingEntry = singleCellService.mappingEntry(rawSequenceFile)

        String oldMappingFile = oldValues[WELL_MAPPING_FILE_NAME]

        return """
               |# Single Cell structure
               |## recreate link
               |rm -f '${oldValues[WELL_FILE_NAME]}'
               |mkdir -p -m 2750 '${wellFile.parent}'
               |ln -sr '${newDirectFileName}' \\\n      '${wellFile}'
               |
               |## remove entry from old mapping file
               |chmod 640 '${oldMappingFile}'
               |sed -i '\\#${oldValues[WELL_MAPPING_FILE_ENTRY_NAME]}#d' ${oldMappingFile}
               |chmod 440 '${oldMappingFile}'
               |
               |## add entry to new mapping file
               |touch '${mappingFile}'
               |chgrp '${rawSequenceFile.project.unixGroup}' '${mappingFile}'
               |chmod 640 '${mappingFile}'
               |echo '${mappingEntry}' >> '${mappingFile}'
               |chmod 440 '${mappingFile}'
               |
               |## delete mapping file, if empty
               |if [ ! -s '${oldMappingFile}' ]
               |then
               |    rm -f '${oldMappingFile}'
               |fi
               |""".stripMargin()
    }

    /**
     * function to get the copy and remove command for one fastqc file
     */
    String copyAndRemoveFastQcFile(String oldFileName, String newFileName, DataSwapData data) {
        Path oldData = Paths.get(oldFileName)
        Path newData = Paths.get(newFileName)
        String mvFastqCommand = generateMaybeMoveBashCommand(oldData, newData, data)

        // also move the fastqc checksums, if they exist
        // missing checksum files are a normal situation (depends on which sequencing center sent it), so no failOnMissingFiles check is needed.
        Path oldDataMd5 = oldData.resolveSibling("${oldData.fileName}.md5sum")
        Path newDataMd5 = newData.resolveSibling("${newData.fileName}.md5sum")
        String mvCheckSumCommand = ''
        if (Files.exists(oldDataMd5) || Files.exists(newDataMd5)) {
            mvCheckSumCommand = generateMaybeMoveBashCommand(
                    oldDataMd5, newDataMd5, data)
        }

        return mvFastqCommand + "\n" + mvCheckSumCommand
    }

    /**
     * Create bash commands for removing analysis files
     *
     * @param data DTO containing all entities necessary to perform a swap
     */
    void createRemoveAnalysisAndAlignmentsCommands(DataSwapData data) {
        data.moveFilesCommands << "\n\n################ delete bam and analysis files ################\n"
        data.dirsToDelete.flatten()*.path.each {
            data.moveFilesCommands << "rm -rf ${it}\n"
        }
    }

    /**
     * Create bash commands for moving data files
     *
     * @param data DTO containing all entities necessary to perform a swap
     */
    void createMoveRawSequenceFilesCommands(D data) {
        data.moveFilesCommands << "\n\n################ move data files ################\n"
        data.moveFilesCommands << renameRawSequenceFiles(data)
    }

    /**
     * Create a warning comment in case the datafile is swapped
     */
    void createCommentForSwappedRawSequenceFiles(DataSwapData data) {
        data.rawSequenceFiles.each { RawSequenceFile rawSequenceFile ->
            if (rawSequenceFile.comment?.comment) {
                commentService.saveComment(rawSequenceFile, rawSequenceFile.comment.comment + "\nAttention: Datafile swapped!")
            } else {
                commentService.saveComment(rawSequenceFile, "Attention: Datafile swapped!")
            }
        }
    }

    /**
     * Checks whether the analysis is still running and throw assert exception when it does.
     *
     * @param data DTO containing all entities necessary to perform a swap
     */
    void checkThatNoAnalysisIsRunning(DataSwapData data) {
        List<RoddyBamFile> roddyBamFiles = RoddyBamFile.createCriteria().list {
            seqTracks {
                inList("id", data.seqTrackList*.id)
            }
        } as List<RoddyBamFile>

        if (roddyBamFiles) {
            List<BamFilePairAnalysis> analysisInstances = roddyBamFiles ?
                    BamFilePairAnalysis.findAllBySampleType1BamFileInListOrSampleType2BamFileInList(roddyBamFiles, roddyBamFiles) : []
            if (analysisInstances) {
                analysisDeletionService.assertThatNoWorkflowsAreRunning(analysisInstances)
            }
        }
    }

    /**
     * Creates groovy script to restartAlignments after swap
     *
     * @param data DTO containing all entities necessary to perform a swap
     */
    void createGroovyConsoleScriptToRestartAlignments(DataSwapData data) {
        Path groovyConsoleScriptToRestartAlignments = fileService.createOrOverwriteScriptOutputFile(
                data.scriptOutputDirectory, "restartAli_${data.bashScriptName}.groovy"
        )
        groovyConsoleScriptToRestartAlignments << ALIGNMENT_SCRIPT_HEADER

        data.seqTrackList.each { SeqTrack seqTrack ->
            groovyConsoleScriptToRestartAlignments << "    ${seqTrack.id},  //${seqTrack}\n"
        }
    }

    /**
     * When an item of the swapList should not change, the 'new' value can be left empty (i.e. empty string: '') for readability.
     *
     * This function fills in empty values of the swapList with their corresponding old value, thus creating full swaps, which enables
     * us to proceed the same way for changing and un-changing entries.
     *
     * @param swapList which is potentially containing omitted new values to complete
     * @param log StingBuilder to be written into
     * @return completed swapList
     * @throws IllegalArgumentException if old values are omitted, so there is no base to build on.
     */
    List<Swap<String>> completeOmittedNewSwapValuesAndLog(List<Swap<String>> swapList, StringBuilder log) throws IllegalArgumentException {
        List<Swap<String>> newSwapList = []
        swapList.each { swap ->
            Swap newSwap
            if (!swap.old) {
                throw new IllegalArgumentException("a swap from ${swap.old} to ${swap.new} cannot be processed")
            }
            // was the value omitted?
            if ((swap.old.size() != 0) && !swap.new) {
                newSwap = new Swap(swap.old, swap.old)
            } else {
                newSwap = new Swap(swap.old, swap.new)
            }
            newSwapList.add(newSwap)
            log << "\n    - ${newSwap.old} --> ${newSwap.new}"
        }
        return newSwapList
    }

    /**
     * creates the complete bash script to move files after database swap
     *
     * @param data DTO containing all entities necessary to perform a swap
     * @return Path to bash script
     */
    void createMoveFilesScript(D data) {
        Path bashScriptToMoveFiles = fileService.createOrOverwriteScriptOutputFile(data.scriptOutputDirectory, "${data.bashScriptName}.sh")
        bashScriptToMoveFiles << data.moveFilesCommands.join()
    }

    /**
     * Creates bash commands to delete empty directories on the file system after SampleSwap and LaneSwap.
     *
     * Also cleanups Individual if it has no other samples left.
     *
     * @param data DTO containing all entities necessary to perform a swap
     */
    void cleanupLeftOverSamples(D data) {
        data.moveFilesCommands << "\n\n"
        data.moveFilesCommands << data.cleanupSampleTypePaths.collect { "rm -rf ${it}" }.join("\n")

        if (!sampleService.getSamplesByIndividual(data.individualSwap.old)) {
            data.moveFilesCommands << "\n\n"
            cleanupLeftOverIndividual(data, true)
        }
    }

    /**
     * Deletes individual data after IndividualSwap, SampleSwap and LaneSwap and
     * creates bash commands to delete empty directories on the file system.
     *
     * It is not necessary to delete the Individual in case of an IndividualSwap, because it will be reused.
     *
     * @param data DTO containing all entities necessary to perform a swap
     * @param cleanupDatabase flag for delete the
     */
    void cleanupLeftOverIndividual(D data, boolean cleanupDatabase = false) {
        if (cleanupDatabase) {
            data.individualSwap.old.delete(flush: true)
        }
        data.moveFilesCommands << data.cleanupIndividualPaths.collect { "rm -rf ${it}" }.join("\n")
        data.moveFilesCommands << "\n"
    }

    /**
     * Creates rename/move commands for one data file and adds them to an given bash script.
     *
     * @param oldFilename which the data file had before it was changed in the database
     * @param rawSequenceFile which should be renamed.
     * @param data DTO containing all entities necessary to perform a swap.
     * @return outPutBashCommands with added bash commands.
     */
    private String createRenameRawSequenceFileCommands(String oldFilename, RawSequenceFile rawSequenceFile, D data) throws FileNotFoundException {
        FileSystem fileSystem = fileSystemService.remoteFileSystem
        String outPutBashCommands = ""
        // fill local variables
        String oldDirectFileName = data.oldRawSequenceFileNameMap[rawSequenceFile][DIRECT_FILE_NAME]
        String oldVbpFileName = data.oldRawSequenceFileNameMap[rawSequenceFile][VBP_FILE_NAME]
        String oldWellName = data.oldRawSequenceFileNameMap[rawSequenceFile][WELL_FILE_NAME]
        Path oldDirectFilePath = fileSystem.getPath(oldDirectFileName)

        // check if old files are already gone and moved
        boolean filesAlreadyMoved = !Files.exists(oldDirectFilePath)

        String bashMoveVbpFile = "rm -f '${oldVbpFileName}';\n"
        String bashMoveDirectFile

        Path newDirectPath = rawSequenceDataWorkFileService.getFilePath(rawSequenceFile)
        Path newVbpPath = rawSequenceDataViewFileService.getFilePath(rawSequenceFile)

        if (Files.exists(newDirectPath)) {
            if (!filesAlreadyMoved && (oldDirectFilePath != newDirectPath)) {
                bashMoveDirectFile = "rm -f '${oldDirectFileName}'"
            } else {
                bashMoveDirectFile = "# ${newDirectPath} is already at the correct position"
            }
        } else {
            if (filesAlreadyMoved) {
                throw new FileNotFoundException("The direct-fastqFiles of raw sequence file (${oldFilename} / ${rawSequenceFile.fileName})" +
                        " of project (${rawSequenceFile.project}) can not be found")
            }
            bashMoveDirectFile = """\n
                                     # ${rawSequenceFile.seqTrack} ${rawSequenceFile}
                                     mkdir -p -m 2750 '${newDirectPath.parent}';""".stripIndent()

            bashMoveDirectFile += """
                                          |mv '${oldDirectFileName}' \\
                                          |   '${newDirectPath}';
                                          |${getFixGroupCommand(newDirectPath)}
                                          |if [ -e '${oldDirectFileName}.md5sum' ]; then
                                          |  mv '${oldDirectFileName}.md5sum' \\
                                          |     '${newDirectPath}.md5sum';
                                          |  ${getFixGroupCommand(newDirectPath.resolveSibling("${newDirectPath.fileName}.md5sum"))}
                                          |fi\n""".stripMargin()
        }
        bashMoveVbpFile += """\
                               |mkdir -p -m 2750 '${newVbpPath.parent}';
                               |ln -sr '${newDirectPath}' \\
                               |      '${newVbpPath}'""".stripMargin()

        outPutBashCommands += "${bashMoveDirectFile}\n${bashMoveVbpFile}\n"
        if (oldWellName) {
            outPutBashCommands += createSingeCellScript(rawSequenceFile, data.oldRawSequenceFileNameMap[rawSequenceFile])
        }
        outPutBashCommands += '\n\n'
        return outPutBashCommands
    }

    /**
     * Iterate over seq tracks gather all directories to delete and make seqTrack as swapped.
     *
     * @param data DTO containing all entities necessary to perform a swap.
     */
    protected void markSeqTracksAsSwappedAndDeleteDependingObjects(D data) {
        data.seqTrackList.each { SeqTrack seqTrack ->
            List<File> dirsToDelete = deletionService.deleteAllProcessingInformationAndResultOfOneSeqTrack(seqTrack, !data.linkedFilesVerified)
            if (dirsToDelete) {
                data.dirsToDelete.addAll(dirsToDelete)
            }
            seqTrack.swapped = true
            seqTrack.save(flush: true)
        }
    }

    /**
     * Gathers the new and the old project by their names and create a new swap with the entities.
     *
     * @param parameters which contain the names of the old and new project.
     * @return Swap contain the entities of the old and new project.
     */
    protected Swap<Project> getProjectSwap(P parameters) {
        Swap<Project> swap =  new Swap<Project>(
                CollectionUtils.exactlyOneElement(Project.findAllByName(parameters.projectNameSwap.old),
                        "old project ${parameters.projectNameSwap.old} not found"),
                CollectionUtils.exactlyOneElement(Project.findAllByName(parameters.projectNameSwap.new),
                        "new project ${parameters.projectNameSwap.new} not found")
        )
        assert swap.old.state != Project.State.ARCHIVED
        assert swap.old.state != Project.State.DELETED
        assert swap.new.state != Project.State.ARCHIVED
        assert swap.new.state != Project.State.DELETED

        return swap
    }

    /**
     * Gathers the new and the old individual by their names and create a new swap with the entities.
     *
     * @param parameters which contain the pids of the old and new individual.
     * @return Swap contain the entities of the old and new individual.
     */
    protected Swap<Individual> getIndividualSwap(P parameters) {
        return new Swap<Individual>(
                CollectionUtils.exactlyOneElement(Individual.findAllByPid(parameters.pidSwap.old),
                        "old individual ${parameters.pidSwap.old} not found"),
                CollectionUtils.exactlyOneElement(Individual.findAllByPid(parameters.pidSwap.new),
                        "new individual ${parameters.pidSwap.new} not found")
        )
    }

    /**
     * Gathers all fastq data files by given given list of seq tracks.
     *
     * @param seqTrackList as parameters for searching for corresponding data files.
     * @param parameters containing the StringBuilder for logging
     * @return found list of fastq data files
     */
    protected List<RawSequenceFile> getRawSequenceFilesBySeqTrackInList(List<SeqTrack> seqTrackList, P parameters) {
        List<RawSequenceFile> rawSequenceFiles = seqTrackList ? RawSequenceFile.findAllBySeqTrackInList(seqTrackList) : []
        logListEntries(rawSequenceFiles, "dataFiles", parameters.log)
        return rawSequenceFiles
    }

    /**
     * Gathers all fastq data filenames by given given list of fastq data files.
     *
     * @param seqTrackList as parameters for searching for corresponding data filenames.
     * @return found list of fastq data filenames
     */
    protected Map<FastqcProcessedFile, String> getFastQcOutputFileNamesByRawSequenceFilesInList(List<RawSequenceFile> rawSequenceFiles) {
        return rawSequenceFiles ? FastqcProcessedFile.findAllBySequenceFileInList(rawSequenceFiles).collectEntries {
            [(it.sequenceFile): fastqcDataFilesService.fastqcOutputPath(it).toString()]
        } : [:]
    }
}
