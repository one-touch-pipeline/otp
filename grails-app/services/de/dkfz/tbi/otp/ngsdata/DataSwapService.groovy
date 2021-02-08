/*
 * Copyright 2011-2019 The OTP authors
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
package de.dkfz.tbi.otp.ngsdata

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.AnalysisDeletionService
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.DeletionService

import java.nio.file.*

import static org.springframework.util.Assert.*

@SuppressWarnings(['JavaIoPackageAccess', 'Println'])
//This class is written for scripts, so it needs the output in stdout
@Transactional
class DataSwapService {

    static final String DIRECT_FILE_NAME = "directFileName"
    static final String VBP_FILE_NAME = "vbpFileName"
    static final String WELL_FILE_NAME = "wellFileName"
    static final String WELL_MAPPING_FILE_NAME = "wellMappingFileName"
    static final String WELL_MAPPING_FILE_ENTRY_NAME = "wellMappingFileEntry"

    IndividualService individualService
    CommentService commentService
    FastqcDataFilesService fastqcDataFilesService
    LsdfFilesService lsdfFilesService
    DataProcessingFilesService dataProcessingFilesService
    ProcessedAlignmentFileService processedAlignmentFileService
    ConfigService configService
    SeqTrackService seqTrackService
    MergedAlignmentDataFileService mergedAlignmentDataFileService
    FileService fileService
    DeletionService deletionService
    SingleCellService singleCellService

    static final String MISSING_FILES_TEXT = "The following files are expected, but not found:"
    static final String EXCESS_FILES_TEXT = "The following files are found, but not expected:"

    static final String BASH_HEADER = """\
            #!/bin/bash

            #PLEASE CHECK THE COMMANDS CAREFULLY BEFORE RUNNING THE SCRIPT

            set -e
            set -v

            """.stripIndent()

    static final String ALIGNMENT_SCRIPT_HEADER = "// ids of seqtracks which should be triggered with 'TriggerAlignment.groovy' for alignment\n\n"

    /**
     * Adapts the MetaDataFile copy in the database, when the corresponding values, which are stored in other objects, are changed
     *
     * @param sample to which the MetaDataEntry belongs
     * @param oldValue the old value of the MetaDataEntry
     * @param newValue the new value of the MetaDataEntry
     * @param metaDataKey the key of the MetaDataEntry
     */
    void changeMetadataEntry(Sample sample, String metaDataKey, String oldValue, String newValue) {
        List<SeqTrack> seqtracks = SeqTrack.findAllBySample(sample)
        List<DataFile> dataFiles = seqtracks ? DataFile.findAllBySeqTrackInList(seqtracks) : []
        List<MetaDataKey> sampleIdentifierKeys = MetaDataKey.findAllByName(metaDataKey)
        assert sampleIdentifierKeys.unique().size() == 1
        List<MetaDataEntry> metaDataEntries = dataFiles ? MetaDataEntry.findAllByValueAndDataFileInListAndKey(
                oldValue,
                dataFiles,
                sampleIdentifierKeys.first()
        ) : []
        metaDataEntries.each {
            it.value = newValue
            it.save(flush: true)
        }
    }

    /**
     * get the sample of individual and sample type.
     *
     * @param individual the individual the sample should be belong to
     * @param sampleType the sampleType the sample should be belong to
     * @return the sample for the combination of individual and sampleType
     * @throw Exception if no samples found or more then 1 sample
     */
    Sample getSingleSampleForIndividualAndSampleType(Individual individual, SampleType sampleType, StringBuilder log) {
        List<Sample> samples = Sample.findAllByIndividualAndSampleType(individual, sampleType)
        log << "\n  samples (${samples.size()}): ${samples}"
        notEmpty(samples)
        isTrue(samples.size() == 1)
        Sample sample = samples[0]
        return sample
    }

    /**
     * get the seqtracks for the sample and show them.
     *
     * @param sample the sample the seqTracks should be fetch for
     * @return the seqtracks for the sample
     */
    List<SeqTrack> getAndShowSeqTracksForSample(Sample sample, StringBuilder log) {
        List<SeqTrack> seqTracks = SeqTrack.findAllBySample(sample)
        log << "\n  seqtracks (${seqTracks.size()}): "
        seqTracks.each { log << "\n    - ${it}" }
        return seqTracks
    }

    /**
     * get the dataFiles for the seqTracks, validate and show them.
     *
     * @param seqTracks the seqTracks the dataFiles should be fetch for
     * @param dataFileMap A map of old file name and new file name
     * @return the dataFiles for the seqTracks
     */
    List<DataFile> getAndValidateAndShowDataFilesForSeqTracks(List<SeqTrack> seqTracks, Map<String, String> dataFileMap, StringBuilder log) {
        List<DataFile> dataFiles = seqTracks ? DataFile.findAllBySeqTrackInList(seqTracks) : []
        log << "\n  dataFiles (${dataFiles.size()}):"
        dataFiles.each { log << "\n    - ${it}" }
        notEmpty(dataFiles)
        dataFiles.each {
            isTrue(dataFileMap.containsKey(it.fileName), "${it.fileName} missed in map")
        }
        return dataFiles
    }

    /**
     * get the dataFiles connected about an AlignmentLog to the seqTracks, validate and show them.
     *
     * @param seqTracks the seqTracks the dataFiles connected about alignmentLog should be fetch for
     * @param dataFileMap A map of old file name and new file name
     * @return the dataFiles for the seqTracks
     */
    List<DataFile> getAndValidateAndShowAlignmentDataFilesForSeqTracks(
            List<SeqTrack> seqTracks, Map<String, String> dataFileMap, StringBuilder log) {
        List<AlignmentLog> alignmentsLog = seqTracks ? AlignmentLog.findAllBySeqTrackInList(seqTracks) : []
        if (!alignmentsLog) {
            return []
        }
        List<DataFile> dataFiles = DataFile.findAllByAlignmentLogInList(alignmentsLog)
        log << "\n  alignment dataFiles (${dataFiles.size()}):"
        dataFiles.each { log << "\n    - ${it}" }
        dataFiles.each {
            isTrue(dataFileMap.containsKey(it.fileName), "${it.fileName} missed in map")
        }
        return dataFiles
    }

    /**
     * function to get the copy and remove command for one fastqc file
     */
    String copyAndRemoveFastqcFile(String oldDataFileName, String newDataFileName, StringBuilder log, boolean failOnMissingFiles) {
        Path oldData = Paths.get(oldDataFileName)
        Path newData = Paths.get(newDataFileName)
        String mvFastqCommand = generateMaybeMoveBashCommand(oldData, newData, failOnMissingFiles, log)

        // also move the fastqc checksums, if they exist
        //   missing checksum files are a normal situation (depends on which sequencing center sent it), so no failOnMissingFiles check is needed.
        Path oldDataMd5 = oldData.resolveSibling("${oldData.fileName}.md5sum")
        Path newDataMd5 = newData.resolveSibling("${newData.fileName}.md5sum")
        String mvCheckSumCommand = ''
        if (Files.exists(oldDataMd5) || Files.exists(newDataMd5)) {
            mvCheckSumCommand = generateMaybeMoveBashCommand(
                    oldDataMd5, newDataMd5,
                    failOnMissingFiles, log)
        }

        return mvFastqCommand + "\n" + mvCheckSumCommand
    }

    /**
     * validates a move and returns the bash-command to move an old file to a new location.
     */
    private String generateMaybeMoveBashCommand(Path old, Path neww, boolean failOnMissingFiles, StringBuilder log) {
        String bashCommand = ""
        if (Files.exists(old)) {
            if (Files.exists(neww)) {
                if (Files.isSameFile(old, neww)) {
                    bashCommand = "# the old and the new data file ('${old}') are the same, no move needed.\n"
                } else {
                    bashCommand = "# new file already exists: '${neww}'; delete old file\n# rm -f '${old}'\n"
                }
            } else {
                bashCommand = """
mkdir -p -m 2750 '${neww.parent}';
mv '${old}' \\
   '${neww}';
${getFixGroupCommand(neww)}
\n"""
            }
        } else {
            if (Files.exists(neww)) {
                bashCommand = "# no old file, and ${neww} is already at the correct position (apparently put there manually?)\n"
            } else {
                String message = """The file can not be found at either old or new location:
  oldName: ${old}
  newName: ${neww}
"""
                if (failOnMissingFiles) {
                    throw new RuntimeException(message)
                } else {
                    log << '\n' << message
                }
            }
        }
        return bashCommand
    }

    /**
     * creates a map containing for every Datafile of the list the direct and the viewByPid file name as map.
     */
    Map<DataFile, Map<String, String>> collectFileNamesOfDataFiles(List<DataFile> dataFiles) {
        Map<DataFile, Map<String, String>> map = [:]
        dataFiles.each { DataFile dataFile ->
            String directFileName = lsdfFilesService.getFileFinalPath(dataFile)
            String vbpFileName = lsdfFilesService.getFileViewByPidPath(dataFile)
            map[dataFile] = [(DIRECT_FILE_NAME): directFileName, (VBP_FILE_NAME): vbpFileName]
            if (dataFile.seqType.singleCell && dataFile.seqTrack.singleCellWellLabel) {
                map[dataFile][WELL_FILE_NAME] = lsdfFilesService.getWellAllFileViewByPidPath(dataFile)
                map[dataFile][WELL_MAPPING_FILE_NAME] = singleCellService.singleCellMappingFile(dataFile)
                map[dataFile][WELL_MAPPING_FILE_ENTRY_NAME] = singleCellService.mappingEntry(dataFile)
            }
        }
        return map
    }

    /**
     * function to rename data files and connect to a new project.
     * It is also checked, that the files and the view by pid links do not exist anymore in the old directory, but exist in
     * the new directory.
     *
     * @param dataFiles The datafiles to be renamed
     * @param newProject The new project the files should be connected with
     * @param dataFileMap A map of old file name and new file name
     * @param oldDataFileNameMap A map containg for every datafile the old direct file name and the old vbpFileName
     *        (can be generated by #collectFileNamesOfDataFiles before changing of corresponding objects)
     */
    String renameDataFiles(List<DataFile> dataFiles, Project newProject, Map<String, String> dataFileMap, Map<DataFile,
            Map<String, String>> oldDataFileNameMap, boolean sameLsdf, StringBuilder log) {
        notNull(dataFiles, "parameter dataFiles must not be null")
        notNull(newProject, "parameter newProject must not be null")
        notNull(dataFileMap, "parameter dataFileMap must not be null")
        assert dataFiles*.fileName as Set == dataFileMap.keySet()
        assert dataFiles*.id as Set == oldDataFileNameMap.keySet()*.id as Set

        String bashScriptToMoveFiles = ""

        dataFiles.each {
            boolean filesAlreadyMoved = true
            String bashMoveDirectFile = ""
            String bashMoveVbpFile = ""

            String oldDirectFileName = oldDataFileNameMap[it][DIRECT_FILE_NAME]
            String oldVbpFileName = oldDataFileNameMap[it][VBP_FILE_NAME]
            String oldWellName = oldDataFileNameMap[it][WELL_FILE_NAME]
            File directFile = new File(oldDirectFileName)
            File vbpFile

            if (directFile.exists()) {
                filesAlreadyMoved = false
            }
            bashMoveVbpFile = "rm -f '${oldVbpFileName}';\n"

            String old = it.fileName
            it.project = newProject
            it.fileName = it.vbpFileName = dataFileMap[it.fileName]
            if (it.mateNumber == null && it.fileWithdrawn && it.fileType && it.fileType.type == FileType.Type.SEQUENCE && it.fileType.vbpPath == "/sequence/") {
                log << "\n====> set mate number for withdrawn data file"
                assert it.seqTrack.seqType.libraryLayout == LibraryLayout.SINGLE: "library layout is not ${LibraryLayout.SINGLE}"
                it.mateNumber = 1
            }
            it.save(flush: true)
            log << "\n    changed ${old} to ${it.fileName}"

            String newDirectFileName = lsdfFilesService.getFileFinalPath(it)
            String newVbpFileName = lsdfFilesService.getFileViewByPidPath(it)
            directFile = new File(newDirectFileName)
            vbpFile = new File(newVbpFileName)
            if (!directFile.exists()) {
                if (filesAlreadyMoved) {
                    throw new RuntimeException("The direct-fastqFiles of dataFile (${old} / ${it.fileName}) of project (${it.project}) can not be found")
                }
                bashMoveDirectFile = """\n\n
# ${it.seqTrack} ${it}
mkdir -p -m 2750 '${directFile.parent}';"""
                if (sameLsdf) {
                    bashMoveDirectFile += """
mv '${oldDirectFileName}' \\
   '${newDirectFileName}';
${getFixGroupCommand(directFile.toPath())}
if [ -e '${oldDirectFileName}.md5sum' ]; then
  mv '${oldDirectFileName}.md5sum' \\
     '${newDirectFileName}.md5sum';
  ${getFixGroupCommand(directFile.toPath().resolveSibling("${directFile.name}.md5sum"))}
fi
"""
                } else {
                    bashMoveDirectFile += """
cp '${oldDirectFileName}' '${newDirectFileName}';
${getFixGroupCommand(directFile.toPath())}
echo '${it.md5sum}  ${newDirectFileName}' | md5sum -c
chmod 440 ${newDirectFileName}
# rm -f '${oldDirectFileName}'
"""
                }
            } else {
                if (!filesAlreadyMoved && (oldDirectFileName != newDirectFileName)) {
                    bashMoveDirectFile = "# rm -f '${oldDirectFileName}'"
                } else {
                    bashMoveDirectFile = "# ${newDirectFileName} is already at the correct position"
                }
            }
            bashMoveVbpFile += """mkdir -p -m 2750 '${vbpFile.parent}';
ln -s '${newDirectFileName}' \\
      '${newVbpFileName}'"""

            bashScriptToMoveFiles += "${bashMoveDirectFile}\n${bashMoveVbpFile}\n"
            if (oldWellName) {
                bashScriptToMoveFiles += createSingeCellScript(it, oldDataFileNameMap[it])
            }
            bashScriptToMoveFiles += '\n\n'
        }
        return bashScriptToMoveFiles
    }

    String getFixGroupCommand(Path file) {
        "chgrp -h `stat -c '%G' ${file.parent}` ${file}"
    }

    protected String createSingeCellScript(DataFile dataFile, Map<String, ?> oldValues) {
        if (!dataFile.seqType.singleCell || !dataFile.seqTrack.singleCellWellLabel) {
            return ''
        }
        String newDirectFileName = lsdfFilesService.getFileFinalPath(dataFile)
        String newWellFileName = lsdfFilesService.getWellAllFileViewByPidPath(dataFile)
        File wellFile = new File(newWellFileName)

        Path mappingFile = singleCellService.singleCellMappingFile(dataFile)
        String mappingEntry = singleCellService.mappingEntry(dataFile)

        Path oldMappingFile = oldValues[WELL_MAPPING_FILE_NAME]

        return [
                '',
                '# Single Cell structure',
                '## recreate link',
                "rm -f '${oldValues[WELL_FILE_NAME]}'",
                "mkdir -p -m 2750 '${wellFile.parent}'",
                "ln -s '${newDirectFileName}' \\\n      '${wellFile}'",
                '',
                '## remove entry from old mapping file',
                "sed -i '\\#${oldValues[WELL_MAPPING_FILE_ENTRY_NAME]}#d' ${oldMappingFile}",
                '',
                '## add entry to new mapping file',
                "touch '${mappingFile}'",
                "echo '${mappingEntry}' >> '${mappingFile}'",
                '',
                '## delete mapping file, if empty',
                "if [ ! -s '${oldMappingFile}' ]",
                'then',
                "    rm '${oldMappingFile}'",
                'fi',
                '',
        ].join('\n')
    }

    /**
     * The input SeqTrack is passed to the AlignmentDecider
     */
    String startAlignmentForSeqTrack(SeqTrack seqTrack) {
        "    ${seqTrack.id},  //${seqTrack}\n"
    }

    class MoveIndividualHelper {
        Project oldProject
        Project newProject

        Individual oldIndividual
        Individual newIndividual

        List<Sample> samples
        List<SeqTrack> seqTracks

        List<DataFile> fastqDataFiles
        List<DataFile> bamDataFiles
        List<DataFile> dataFiles
        Map<DataFile, Map<String, String>> oldDataFileNameMap
        List<String> oldFastqcFileNames

        List<File> dirsToDelete = []

        boolean sameLsdf

        MoveIndividualHelper(Map<String, String> inputInformationOTP, Map<String, String> sampleTypeMap, Map<String, String> dataFileMap, StringBuilder log) {
            checkInputInformation(inputInformationOTP)

            oldProject = CollectionUtils.exactlyOneElement(Project.findAllByName(inputInformationOTP.oldProjectName),
                    "old project ${inputInformationOTP.oldProjectName} not found")

            newProject = CollectionUtils.exactlyOneElement(Project.findAllByName(inputInformationOTP.newProjectName),
                    "new project ${inputInformationOTP.newProjectName} not found")

            sameLsdf = oldProject.realm == newProject.realm

            oldIndividual = CollectionUtils.exactlyOneElement(Individual.findAllByPid(inputInformationOTP.oldPid),
                    "old pid ${inputInformationOTP.oldPid} not found")

            newIndividual = CollectionUtils.atMostOneElement(Individual.findAllByPid(inputInformationOTP.newPid))
            if (inputInformationOTP.oldPid != inputInformationOTP.newPid) {
                isNull(newIndividual, "new pid ${inputInformationOTP.newPid} already exist")
            }

            samples = Sample.findAllByIndividual(oldIndividual)
            log << "\n  samples (${samples.size()}): ${samples}"

            notEmpty(samples, "no samples found for ${oldIndividual}")
            isTrue(samples.size() == sampleTypeMap.size(), "Given Sample map different in size than found samples!")
            samples.each { Sample sample ->
                isTrue(sampleTypeMap.containsKey(sample.sampleType.name), "${sample.sampleType.name} missed in map")
                notNull(SampleType.findByName(sampleTypeMap.get(sample.sampleType.name)), "${sampleTypeMap.get(sample.sampleType.name)} " +
                        "not found in database")
            }

            isTrue(oldIndividual.project == oldProject, "old individual ${inputInformationOTP.oldPid} should be in project" +
                    " ${inputInformationOTP.oldProjectName}, but was in ${oldIndividual.project}")

            seqTracks = samples ? SeqTrack.findAllBySampleInList(samples) : []
            log << "\n  seqtracks (${seqTracks.size()}): "
            seqTracks.each { log << "\n    - ${it}" }

            fastqDataFiles = getAndValidateAndShowDataFilesForSeqTracks(seqTracks, dataFileMap, log)
            bamDataFiles = getAndValidateAndShowAlignmentDataFilesForSeqTracks(seqTracks, dataFileMap, log)
            dataFiles = [fastqDataFiles, bamDataFiles].flatten() as List<DataFile>
            oldDataFileNameMap = collectFileNamesOfDataFiles(dataFiles)
            oldFastqcFileNames = fastqDataFiles.collect { fastqcDataFilesService.fastqcOutputFile(it) }
        }

        private void checkInputInformation(Map<String, String> inputInformationOTP) {
            notNull(inputInformationOTP.oldProjectName, "parameter oldProjectName may not be null")

            notNull(inputInformationOTP.newProjectName, "parameter newProjectName may not be null")

            notNull(inputInformationOTP.oldPid, "parameter oldPid may not be null")

            notNull(inputInformationOTP.newPid, "parameter newPid may not be null")
        }
    }

    /**
     * function to move one individual from one project to another, renaming it, change the sample type of connected samples
     * and rename data files using function renameDataFiles.
     *
     * Attention: The method can only handle data files of type fastq.
     * Attention: It is assumed no alignment is done yet
     *
     * @param oldProjectName the name of the project, the patient is currently connected with
     * @param newProjectName the name of the existing project, the patient should be connected with
     * @param oldPid the name of the individual to move and to rename
     * @param newPid the new name of the individual. This name may not used yet. It is used for the three properties pid, mockPid and mockfullname
     * @param sampleTypeMap a map used for changing the sample type of the samples. The sample types are given by name, not
     *                      by Object. The Map have to map all sample types used for this individual. It is allwed to map a
     *                      sample type to itself. The sample type have to be found in the database.
     * @param dataFileMap A map of old file name and new file name. The map have to contain the file name of all datafiles of the individual
     * @param linkedFilesVerified when the source-fastq files have been linked from the sequencing facility, this flag
     *                            asserts that a human has checked that the symlinks still work (i.e. files still exist at the sequencing facility)
     */
    void moveIndividual(
            Map<String, String> inputInformationOTP,
            Map<String, String> sampleTypeMap,
            Map<String, String> dataFileMap,
            String bashScriptName,
            StringBuilder log,
            boolean failOnMissingFiles,
            Path scriptOutputDirectory,
            boolean linkedFilesVerified = false
    ) {
        log << "\n\nmove ${inputInformationOTP.oldPid} of ${inputInformationOTP.oldProjectName} to" +
                " ${inputInformationOTP.newPid} of ${inputInformationOTP.newProjectName} "

        completeOmittedNewValuesAndLog(sampleTypeMap, 'samples', log)
        completeOmittedNewValuesAndLog(dataFileMap, 'datafiles', log)

        notNull(dataFileMap, "parameter dataFileMap may not be null")

        MoveIndividualHelper moveIndividualHelper = new MoveIndividualHelper(inputInformationOTP, sampleTypeMap, dataFileMap, log)

        String processingPathToOldIndividual = dataProcessingFilesService.getOutputDirectory(
                moveIndividualHelper.oldIndividual,
                DataProcessingFilesService.OutputDirectories.BASE
        )

        // now the changing process(procedure) starts
        if (moveIndividualHelper.seqTracks && AlignmentPass.findBySeqTrackInList(moveIndividualHelper.seqTracks)) {
            log << "\n -->     found alignments for seqtracks (${AlignmentPass.findAllBySeqTrackInList(moveIndividualHelper.seqTracks)*.seqTrack.unique()}): "
        }

        Realm realm = configService.defaultRealm

        // RestartAlignmentScript
        createGroovyConsoleScriptToRestartAlignments(scriptOutputDirectory, bashScriptName, realm, moveIndividualHelper.seqTracks)

        // MoveFilesScript will be filled during routine
        Path bashScriptToMoveFiles = fileService.createOrOverwriteScriptOutputFile(scriptOutputDirectory, "${bashScriptName}.sh", realm)
        bashScriptToMoveFiles << BASH_HEADER

        Path bashScriptToMoveFilesAsOtherUser = fileService.createOrOverwriteScriptOutputFile(scriptOutputDirectory,
                "${bashScriptName}-otherUser.sh", realm)
        createBashScriptRoddy(moveIndividualHelper.seqTracks, moveIndividualHelper.dirsToDelete, log, bashScriptToMoveFiles, bashScriptToMoveFilesAsOtherUser,
                !linkedFilesVerified)

        moveIndividualHelper.seqTracks.each { SeqTrack seqTrack ->
            Map<String, List<File>> dirs = deletionService.deleteAllProcessingInformationAndResultOfOneSeqTrack(seqTrack, !linkedFilesVerified)
            moveIndividualHelper.dirsToDelete.addAll(dirs.get("dirsToDelete"))
            bashScriptToMoveFilesAsOtherUser << "#rm -rf ${dirs.get("dirsToDeleteWithOtherUser").join("\n#rm -rf ")}\n"
            // mark as swapped
            seqTrack.swapped = true
            seqTrack.save(flush: true)
        }

        log << "\n  changing ${moveIndividualHelper.oldIndividual.project} to ${moveIndividualHelper.newProject} for ${moveIndividualHelper.oldIndividual}"
        moveIndividualHelper.oldIndividual.project = moveIndividualHelper.newProject
        moveIndividualHelper.oldIndividual.pid = inputInformationOTP.newPid
        moveIndividualHelper.oldIndividual.mockPid = inputInformationOTP.newPid
        moveIndividualHelper.oldIndividual.mockFullName = inputInformationOTP.newPid
        moveIndividualHelper.oldIndividual.save(flush: true)

        bashScriptToMoveFiles << "\n\n################ move data files ################\n"
        bashScriptToMoveFiles << renameDataFiles(
                moveIndividualHelper.dataFiles,
                moveIndividualHelper.newProject,
                dataFileMap,
                moveIndividualHelper.oldDataFileNameMap,
                moveIndividualHelper.sameLsdf,
                log
        )

        moveIndividualHelper.samples.each { Sample sample ->
            SampleType newSampleType = SampleType.findByName(sampleTypeMap.get(sample.sampleType.name))
            log << "\n    change ${sample.sampleType.name} to ${newSampleType.name}"
            SampleIdentifier.findAllBySample(sample)*.delete(flush: true)
            sample.sampleType = newSampleType
            sample.save(flush: true)
        }

        bashScriptToMoveFiles << "\n\n\n ################ move fastq files ################ \n"
        moveIndividualHelper.samples = Sample.findAllByIndividual(moveIndividualHelper.oldIndividual)
        moveIndividualHelper.seqTracks = moveIndividualHelper.samples ? SeqTrack.findAllBySampleInList(moveIndividualHelper.samples) : []
        List<DataFile> newDataFiles = moveIndividualHelper.seqTracks ? DataFile.findAllBySeqTrackInList(moveIndividualHelper.seqTracks) : []
        List<String> newFastqcFileNames = newDataFiles.collect { fastqcDataFilesService.fastqcOutputFile(it) }

        moveIndividualHelper.oldFastqcFileNames.eachWithIndex { oldFastqcFileName, i ->
            bashScriptToMoveFiles << copyAndRemoveFastqcFile(oldFastqcFileName, newFastqcFileNames.get(i), log, failOnMissingFiles)
        }

        bashScriptToMoveFiles << "\n\n################ delete analysis stuff ################\n"
        moveIndividualHelper.dirsToDelete.flatten()*.path.each {
            bashScriptToMoveFiles << "#rm -rf ${it}\n"
        }

        bashScriptToMoveFiles << "\n\n\n ################ delete old Individual ################ \n"
        bashScriptToMoveFiles << "# rm -rf ${moveIndividualHelper.oldProject.projectSequencingDirectory}/*/view-by-pid/${inputInformationOTP.oldPid}/\n"
        bashScriptToMoveFiles << "# rm -rf ${processingPathToOldIndividual}\n"

        // Comment
        individualService.createComment("Individual swap", [
                individual: moveIndividualHelper.oldIndividual,
                project   : moveIndividualHelper.oldProject.name,
                pid       : inputInformationOTP.oldPid,
        ], [
                individual: moveIndividualHelper.oldIndividual,
                project   : moveIndividualHelper.newProject.name,
                pid       : inputInformationOTP.newPid,
        ])

        createCommentForSwappedDatafiles(moveIndividualHelper.dataFiles)
    }

    class MoveSampleHelper {

        Project oldProject
        Project newProject

        Individual oldIndividual
        Individual newIndividual

        SampleType oldSampleType
        SampleType newSampleType

        Sample sample

        List<SeqTrack> seqTrackList

        List<DataFile> fastqDataFiles
        List<DataFile> bamDataFiles
        List<DataFile> dataFiles
        Map<DataFile, Map<String, String>> oldDataFileNameMap
        List<String> oldFastqcFileNames
        List<File> dirsToDelete

        boolean sameLsdf

        MoveSampleHelper(Map<String, String> inputInformationOTP, Map<String, String> dataFileMap, StringBuilder log) {
            checkInputInformation(inputInformationOTP)

            oldProject = CollectionUtils.exactlyOneElement(Project.findAllByName(inputInformationOTP.oldProjectName),
                    "old project ${inputInformationOTP.oldProjectName} not found")
            newProject = CollectionUtils.exactlyOneElement(Project.findAllByName(inputInformationOTP.newProjectName),
                    "new project ${inputInformationOTP.newProjectName} not found")

            sameLsdf = oldProject.realm == newProject.realm

            oldIndividual = CollectionUtils.exactlyOneElement(Individual.findAllByPid(inputInformationOTP.oldPid),
                    "old pid ${inputInformationOTP.oldPid} not found")
            newIndividual = CollectionUtils.exactlyOneElement(Individual.findAllByPid(inputInformationOTP.newPid),
                    "new pid ${inputInformationOTP.newPid} not found")
            isTrue(
                    oldIndividual.project == oldProject,
                    "old individual ${inputInformationOTP.oldPid} should be in project" +
                            " ${inputInformationOTP.oldProjectName}, but was in ${oldIndividual.project}"
            )
            isTrue(
                    newIndividual.project == newProject,
                    "new individual ${inputInformationOTP.newPid} should be in project" +
                            "  ${inputInformationOTP.newProject}, but was in ${newIndividual.project}"
            )

            oldSampleType = CollectionUtils.exactlyOneElement(SampleType.findAllByName(inputInformationOTP.oldSampleTypeName),
                    "old sample type ${inputInformationOTP.oldSampleTypeName} not found")
            newSampleType = CollectionUtils.exactlyOneElement(SampleType.findAllByName(inputInformationOTP.newSampleTypeName),
                    "new sample type ${inputInformationOTP.newSampleTypeName} not found")

            sample = getSingleSampleForIndividualAndSampleType(oldIndividual, oldSampleType, log)

            seqTrackList = getAndShowSeqTracksForSample(sample, log)

            fastqDataFiles = getAndValidateAndShowDataFilesForSeqTracks(seqTrackList, dataFileMap, log)
            bamDataFiles = getAndValidateAndShowAlignmentDataFilesForSeqTracks(seqTrackList, dataFileMap, log)
            dataFiles = [fastqDataFiles, bamDataFiles].flatten() as List<DataFile>
            oldDataFileNameMap = collectFileNamesOfDataFiles(dataFiles)
            oldFastqcFileNames = fastqDataFiles.collect { fastqcDataFilesService.fastqcOutputFile(it) }
            dirsToDelete = []
        }

        private void checkInputInformation(Map<String, String> inputInformationOTP) {
            notNull(inputInformationOTP.oldProjectName, "parameter oldProjectName may not be null")

            notNull(inputInformationOTP.newProjectName, "parameter newProjectName may not be null")

            notNull(inputInformationOTP.oldPid, "parameter oldPid may not be null")

            notNull(inputInformationOTP.newPid, "parameter newPid may not be null")

            notNull(inputInformationOTP.oldSampleTypeName, "parameter oldSampleTypeName may not be null")

            notNull(inputInformationOTP.newSampleTypeName, "parameter newSampleTypeName may not be null")
        }
    }

    /**
     * function to move one sample from one individual to another, renaming it, change the sample type rename data files
     * using function renameDataFiles.
     *
     * Attention: The method can only handle data files of type fastq.
     * Attention: It is assumed no alignment is done yet
     *
     * @param oldProjectName the name of the project, the old patient is connected with
     * @param newProjectName the name of the project, the new patient is connected with
     * @param oldPid the name of the individual the sample currently belongs to
     * @param newPid the name of the individual the sample should be belong to
     * @param oldSampleTypeName the name of the sample type the sample currently belongs to
     * @param newSampleTypeName the name of the existing  sample type the sample should be belong to.
     * @param dataFileMap A map of old file name and new file name. The map have to contain the file name of all datafiles of the individual
     * @param linkedFilesVerified when the source-fastq files have been linked from the sequencing facility, this flag
     * asserts that a human has checked that the symlinks still work (i.e. files still exist at the sequencing facility)
     */
    void moveSample(Map<String, String> inputInformationOTP, Map<String, String> dataFileMap, String bashScriptName,
                    StringBuilder log, boolean failOnMissingFiles, Path scriptOutputDirectory,
                    boolean linkedFilesVerified = false) throws IOException {
        log << "\n\nmove ${inputInformationOTP.oldPid} ${inputInformationOTP.oldSampleTypeName} of ${inputInformationOTP.oldProjectName} " +
                "to ${inputInformationOTP.newPid} ${inputInformationOTP.newSampleTypeName} of ${inputInformationOTP.newProjectName} "

        notNull(dataFileMap, "parameter dataFileMap may not be null")
        notNull(bashScriptName, "parameter bashScriptName may not be null")

        completeOmittedNewValuesAndLog(dataFileMap, 'datafiles', log)
        MoveSampleHelper moveSampleHelper = new MoveSampleHelper(inputInformationOTP, dataFileMap, log)

        throwExceptionInCaseOfExternalMergedBamFileIsAttached(moveSampleHelper.seqTrackList)
        if (!linkedFilesVerified) {
            throwExceptionInCaseOfSeqTracksAreOnlyLinked(moveSampleHelper.seqTrackList)
        }

        if (moveSampleHelper.seqTrackList && AlignmentPass.findBySeqTrackInList(moveSampleHelper.seqTrackList)) {
            log << "\n -->     found alignments for seqtracks (${AlignmentPass.findBySeqTrackInList(moveSampleHelper.seqTrackList)*.seqTrack.unique()}): "
        }

        // validation ends here, now the changing is started
        Realm realm = configService.defaultRealm

        // RestartAlignmentScript
        createGroovyConsoleScriptToRestartAlignments(scriptOutputDirectory, bashScriptName, realm, moveSampleHelper.seqTrackList)

        // MoveFilesScript will be filled during routine
        Path bashScriptToMoveFiles = fileService.createOrOverwriteScriptOutputFile(scriptOutputDirectory, "${bashScriptName}.sh", realm)
        bashScriptToMoveFiles << BASH_HEADER

        Path bashScriptToMoveFilesAsOtherUser = fileService.createOrOverwriteScriptOutputFile(
                scriptOutputDirectory, "${bashScriptName}-otherUser.sh", realm
        )
        createBashScriptRoddy(
                moveSampleHelper.seqTrackList, moveSampleHelper.dirsToDelete, log, bashScriptToMoveFiles, bashScriptToMoveFilesAsOtherUser, !linkedFilesVerified
        )

        moveSampleHelper.seqTrackList.each { SeqTrack seqTrack ->
            Map dirs = deletionService.deleteAllProcessingInformationAndResultOfOneSeqTrack(seqTrack, !linkedFilesVerified)
            dirs.get("dirsToDelete").each {
                moveSampleHelper.dirsToDelete.push(it)
            }
            bashScriptToMoveFilesAsOtherUser << "#rm -rf ${dirs.get("dirsToDeleteWithOtherUser").join("\n#rm -rf ")}\n"

            // mark as swapped
            seqTrack.swapped = true
            seqTrack.save(flush: true)
        }

        if (moveSampleHelper.seqTrackList && AlignmentPass.findBySeqTrackInList(moveSampleHelper.seqTrackList)) {
            bashScriptToMoveFiles << "\n\n\n ################ delete old aligned & merged files ################ \n"

            List<AlignmentPass> alignmentPasses = AlignmentPass.findAllBySeqTrackInList(moveSampleHelper.seqTrackList)
            alignmentPasses.each { AlignmentPass alignmentPass ->
                def dirTypeAlignment = DataProcessingFilesService.OutputDirectories.ALIGNMENT
                String baseDirAlignment = dataProcessingFilesService.getOutputDirectory(moveSampleHelper.oldIndividual, dirTypeAlignment)
                String middleDirAlignment = processedAlignmentFileService.getRunLaneDirectory(alignmentPass.seqTrack)
                String oldPathToAlignedFiles = "${baseDirAlignment}/${middleDirAlignment}"
                bashScriptToMoveFiles << "#rm -rf ${oldPathToAlignedFiles}\n"
            }

            def dirTypeMerging = DataProcessingFilesService.OutputDirectories.MERGING
            String baseDirMerging = dataProcessingFilesService.getOutputDirectory(moveSampleHelper.oldIndividual, dirTypeMerging)
            String oldProcessingPathToMergedFiles = "${baseDirMerging}/${moveSampleHelper.oldSampleType.name}"
            bashScriptToMoveFiles << "#rm -rf ${oldProcessingPathToMergedFiles}\n"

            List<ProcessedMergedBamFile> processedMergedBamFiles = ProcessedMergedBamFile.createCriteria().list {
                mergingPass {
                    mergingSet {
                        mergingWorkPackage {
                            eq("sample", moveSampleHelper.sample)
                        }
                    }
                }
            }
            List<ProcessedMergedBamFile> latestProcessedMergedBamFiles = processedMergedBamFiles.findAll {
                it.mergingPass.isLatestPass() && it.mergingSet.isLatestSet()
            }
            latestProcessedMergedBamFiles.each { ProcessedMergedBamFile latestProcessedMergedBamFile ->
                String oldProjectPathToMergedFiles = latestProcessedMergedBamFile.baseDirectory.absolutePath
                bashScriptToMoveFiles << "#rm -rf ${oldProjectPathToMergedFiles}\n"
            }
        }

        moveSampleHelper.sample.sampleType = moveSampleHelper.newSampleType
        moveSampleHelper.sample.individual = moveSampleHelper.newIndividual
        moveSampleHelper.sample.save(flush: true)

        bashScriptToMoveFiles << "################ move data files ################ \n"
        bashScriptToMoveFiles << renameDataFiles(
                moveSampleHelper.dataFiles,
                moveSampleHelper.newProject,
                dataFileMap,
                moveSampleHelper.oldDataFileNameMap,
                moveSampleHelper.sameLsdf,
                log
        )

        SampleIdentifier.findAllBySample(moveSampleHelper.sample)*.delete(flush: true)

        List<String> newFastqcFileNames = moveSampleHelper.fastqDataFiles.collect { fastqcDataFilesService.fastqcOutputFile(it) }

        bashScriptToMoveFiles << "\n\n\n ################ move fastq files ################ \n"

        moveSampleHelper.oldFastqcFileNames.eachWithIndex { oldFastqcFileName, i ->
            bashScriptToMoveFiles << copyAndRemoveFastqcFile(oldFastqcFileName, newFastqcFileNames.get(i), log, failOnMissingFiles)
        }

        bashScriptToMoveFiles << "# delete snv stuff\n"
        moveSampleHelper.dirsToDelete.flatten()*.path.each {
            bashScriptToMoveFiles << "#rm -rf ${it}\n"
        }

        individualService.createComment("Sample swap", [
                individual: moveSampleHelper.oldIndividual,
                project   : moveSampleHelper.oldProject.name,
                pid       : moveSampleHelper.oldIndividual.pid,
                sampleType: moveSampleHelper.oldSampleType.name,
        ], [
                individual: moveSampleHelper.newIndividual,
                project   : moveSampleHelper.newProject.name,
                pid       : moveSampleHelper.newIndividual.pid,
                sampleType: moveSampleHelper.newSampleType.name,
        ])

        createCommentForSwappedDatafiles(moveSampleHelper.dataFiles)
    }

    /**
     * create a bash script to delete files from roddy,
     * the script must be executed as other user
     */
    void createBashScriptRoddy(List<SeqTrack> seqTrackList, List<File> dirsToDelete, StringBuilder log,
                               Path bashScriptToMoveFiles,
                               Path bashScriptToMoveFilesAsOtherUser, boolean enableChecks = true) {
        List<RoddyBamFile> roddyBamFiles = RoddyBamFile.createCriteria().list {
            seqTracks {
                inList("id", seqTrackList*.id)
            }
        } as List<RoddyBamFile>

        if (roddyBamFiles) {
            bashScriptToMoveFilesAsOtherUser << BASH_HEADER

            bashScriptToMoveFilesAsOtherUser << "\n\n\n ################ delete otherUser files ################ \n"
            roddyBamFiles.each { RoddyBamFile roddyBamFile ->
                if (roddyBamFile.isOldStructureUsed()) {
                    bashScriptToMoveFilesAsOtherUser <<
                            "#rm -rf ${roddyBamFile.finalExecutionDirectories*.absolutePath.join("\n#rm -rf ")}\n" +
                            "#rm -rf ${roddyBamFile.finalSingleLaneQADirectories.values()*.listFiles().flatten()*.absolutePath.join("\n#rm -rf ")}\n"
                    if (roddyBamFile.isMostRecentBamFile()) {
                        bashScriptToMoveFilesAsOtherUser << "#rm -rf ${roddyBamFile.finalMergedQADirectory.listFiles()*.absolutePath.join("\n#rm -rf ")}\n"
                    }
                } else {
                    bashScriptToMoveFilesAsOtherUser <<
                            "#rm -rf ${roddyBamFile.workExecutionDirectories*.absolutePath.join("\n#rm -rf ")}\n" +
                            "#rm -rf ${roddyBamFile.workMergedQADirectory.absolutePath}\n" +
                            "#rm -rf ${roddyBamFile.workSingleLaneQADirectories.values()*.absolutePath.join("\n#rm -rf ")}\n"
                }
            }

            List<BamFilePairAnalysis> analysisInstances = roddyBamFiles ?
                    BamFilePairAnalysis.findAllBySampleType1BamFileInListOrSampleType2BamFileInList(roddyBamFiles, roddyBamFiles) : []
            if (analysisInstances) {
                bashScriptToMoveFilesAsOtherUser << "# delete analysis stuff\n"
                AnalysisDeletionService.assertThatNoWorkflowsAreRunning(analysisInstances)
                analysisInstances.each {
                    bashScriptToMoveFilesAsOtherUser << "#rm -rf ${AnalysisDeletionService.deleteInstance(it)}/*\n"
                }
            }

            Set<File> expectedContent = [
                    roddyBamFiles*.finalBamFile,
                    roddyBamFiles*.finalBaiFile,
                    roddyBamFiles*.finalMd5sumFile,
                    roddyBamFiles*.finalExecutionStoreDirectory,
                    roddyBamFiles*.finalQADirectory,
                    roddyBamFiles.findAll {
                        //files of old structure has no work directory
                        !it.isOldStructureUsed()
                    }*.workDirectory.findAll {
                        //in case of realignment the work dir could already be deleted
                        it.exists()
                    },
            ].flatten() as Set<File>

            List<RoddyBamFile> wgbsRoddyBamFiles = roddyBamFiles.findAll { it.seqType.isWgbs() }
            if (wgbsRoddyBamFiles) {
                expectedContent.addAll(wgbsRoddyBamFiles*.finalMetadataTableFile)
                expectedContent.addAll(wgbsRoddyBamFiles*.finalMethylationDirectory)
            }

            Set<File> foundFiles = roddyBamFiles*.baseDirectory.unique()*.listFiles().flatten() as Set<File>
            if (foundFiles != expectedContent) {
                List<File> missingFiles = (expectedContent - foundFiles).sort()
                List<File> excessFiles = (foundFiles - expectedContent).sort()

                log << "\n\n=====================================================\n"
                if (missingFiles) {
                    log << "\n${MISSING_FILES_TEXT}\n    ${missingFiles.join('\n    ')}"
                }
                if (excessFiles) {
                    log << "\n${EXCESS_FILES_TEXT}\n    ${excessFiles.join('\n    ')}"
                }
                log << "\n=====================================================\n"
            }

            bashScriptToMoveFiles << "#rm -rf ${roddyBamFiles*.baseDirectory.unique().join(" ")}\n"

            seqTrackList.each { SeqTrack seqTrack ->
                dirsToDelete.addAll(deletionService.deleteAllProcessingInformationAndResultOfOneSeqTrack(seqTrack, enableChecks).get("dirsToDelete"))
            }
        }
    }

    /**
     * In case there are ExternallyProcessedMergedBamFile attached to the lanes to swap, the script shall stop
     * @param seqTracks
     */
    void throwExceptionInCaseOfExternalMergedBamFileIsAttached(List<SeqTrack> seqTracks) {
        List<ExternallyProcessedMergedBamFile> externallyProcessedMergedBamFiles = seqTrackService.returnExternallyProcessedMergedBamFiles(seqTracks)
        assert externallyProcessedMergedBamFiles.empty: "There are ExternallyProcessedMergedBamFiles attached: ${externallyProcessedMergedBamFiles}"
    }

    /**
     * In case the seqTracks are only linked, the script shall stop
     */
    void throwExceptionInCaseOfSeqTracksAreOnlyLinked(List<SeqTrack> seqTracks) {
        int linkedSeqTracks = seqTracks.findAll { SeqTrack seqTrack ->
            seqTrack.linkedExternally
        }.size()
        assert !linkedSeqTracks: "There are ${linkedSeqTracks} seqTracks only linked"
    }

    /**
     * A transaction wrapper for a callback.
     * The closure execute the given closure inside a transaction and assert at the end the value of rollback.
     * The transaction ensures, that if an exception occur in the script, all database changes are roll backed.
     * The rollback flag allows to trigger a rollback at the of the transaction to ensure, that nothing is changed.
     * This allows to test the script without changing the database.
     */
    static void transaction(boolean rollback, Closure c, StringBuilder log) {
        try {
            Project.withTransaction {
                c()
                assert !rollback
            }
        } catch (Throwable t) {
            log << "\n\n${t}"
            t.stackTrace.each { log << "\n    ${it}" }
            println log
        }
    }

    /**
     * Create a warning comment in case the datafile is swapped
     */
    void createCommentForSwappedDatafiles(List<DataFile> datafiles) {
        datafiles.each { DataFile dataFile ->
            if (dataFile.comment?.comment) {
                commentService.saveComment(dataFile, dataFile.comment.comment + "\nAttention: Datafile swapped!")
            } else {
                commentService.saveComment(dataFile, "Attention: Datafile swapped!")
            }
        }
    }

    /**
     * Helper class for a LaneSwap. Performs Input checking and data retrieval during object construction
     */
    class SwapLaneHelper {
        Run run

        Project oldProject
        Project newProject

        Individual oldIndividual
        Individual newIndividual

        SampleType oldSampleType
        SampleType newSampleType

        Sample oldSample
        Sample newSample

        SeqType oldSeqType
        SeqType newSeqType

        LibraryLayout oldLibraryLayout
        LibraryLayout newLibraryLayout

        boolean oldSingleCell
        boolean newSingleCell
        boolean sampleNeedsToBeCreated

        List lanes

        List<SeqTrack> seqTracks

        List<String> oldFastqcFileNames

        Map<DataFile, Map<String, String>> oldPathsPerDataFile

        List<File> dirsToDelete = []

        boolean sameLsdf

        SwapLaneHelper(Map<String, List<String>> inputInformationOTP) {
            checkInputInformation(inputInformationOTP)
            run = CollectionUtils.exactlyOneElement(Run.findAllByName(extractSingleElement(inputInformationOTP.runName)),
                    "The run (${inputInformationOTP.runName}) does not exist")

            oldProject = CollectionUtils.exactlyOneElement(Project.findAllByName(extractSingleElement(inputInformationOTP.oldProjectName)),
                    "The old project (${inputInformationOTP.oldProjectName}) does not exist")

            newProject = CollectionUtils.exactlyOneElement(Project.findAllByName(extractSingleElement(inputInformationOTP.newProjectName)),
                    "The new project (${inputInformationOTP.newProjectName}) does not exist")

            sameLsdf = oldProject.realm == newProject.realm

            oldIndividual = CollectionUtils.exactlyOneElement(Individual.findAllByPid(extractSingleElement(inputInformationOTP.oldPid)),
                    "The old Individual (${inputInformationOTP.oldPid}) does not exist")

            newIndividual = CollectionUtils.exactlyOneElement(Individual.findAllByPid(extractSingleElement(inputInformationOTP.newPid)),
                    "The new Individual (${inputInformationOTP.newPid}) does not exist")

            oldSampleType = CollectionUtils.exactlyOneElement(SampleType.findAllByName(extractSingleElement(inputInformationOTP.oldSampleTypeName)),
                    "The old SampleType (${inputInformationOTP.oldSampleTypeName}) does not exist")

            newSampleType = CollectionUtils.exactlyOneElement(SampleType.findAllByName(extractSingleElement(inputInformationOTP.newSampleTypeName)),
                    "The new SampleType (${inputInformationOTP.newSampleTypeName}) does not exist")

            oldSample = CollectionUtils.exactlyOneElement(Sample.findAllByIndividualAndSampleType(this.oldIndividual, this.oldSampleType),
                    "The old Sample (${oldIndividual} ${oldSampleType}) does not exist")

            sampleNeedsToBeCreated = Boolean.parseBoolean(extractSingleElement(inputInformationOTP.sampleNeedsToBeCreated))
            List<Sample> sampleList = Sample.findAllByIndividualAndSampleType(newIndividual, newSampleType)

            if (sampleNeedsToBeCreated) {
                assert sampleList.isEmpty() : "The new Sample (${newIndividual} ${newSampleType}) does exist, but should not"
                newSample = new Sample(individual: newIndividual, sampleType: newSampleType).save(flush: true)
            } else {
                newSample = CollectionUtils.exactlyOneElement(sampleList, "The new Sample (${newIndividual} ${newSampleType}) does not exist")
            }

            oldLibraryLayout = LibraryLayout.findByName(extractSingleElement(inputInformationOTP.oldLibraryLayout))
            notNull(oldLibraryLayout, "The old LibraryLayout ${inputInformationOTP.oldLibraryLayout} does not exists")

            newLibraryLayout = LibraryLayout.findByName(extractSingleElement(inputInformationOTP.newLibraryLayout))
            notNull(oldLibraryLayout, "The new LibraryLayout ${inputInformationOTP.newLibraryLayout} does not exists")

            oldSingleCell = Boolean.parseBoolean(extractSingleElement(inputInformationOTP.oldSingleCell))
            newSingleCell = Boolean.parseBoolean(extractSingleElement(inputInformationOTP.newSingleCell))
            oldSeqType = CollectionUtils.exactlyOneElement(
                    SeqType.findAllByNameAndLibraryLayoutAndSingleCell(
                            extractSingleElement(inputInformationOTP.oldSeqTypeName),
                            oldLibraryLayout,
                            oldSingleCell
                    ),
                    "The old seqtype ${inputInformationOTP.oldSeqTypeName} ${inputInformationOTP.oldLibraryLayout} " +
                            "${inputInformationOTP.oldSingleCell} does not exist"
            )

            newSeqType = CollectionUtils.exactlyOneElement(
                    SeqType.findAllByNameAndLibraryLayoutAndSingleCell(
                            extractSingleElement(inputInformationOTP.newSeqTypeName),
                            newLibraryLayout,
                            newSingleCell
                    ),
                    "The new seqtype ${inputInformationOTP.newSeqTypeName} ${inputInformationOTP.oldLibraryLayout} " +
                            "${inputInformationOTP.newSingleCell} does not exist"
            )

            lanes = inputInformationOTP.lane as List
            seqTracks = SeqTrack.findAllBySampleAndRunAndLaneIdInList(oldSample, run, lanes)
            isTrue(seqTracks*.seqType.unique().size() == 1, "SeqTrack of different SeqTypes found!")
            isTrue(seqTracks*.seqType.first() == oldSeqType, "expected '${oldSeqType}' but found '${seqTracks*.seqType.first()}'")
            isTrue(seqTracks.size() == inputInformationOTP.lane.size(), "Given lane(s) ${inputInformationOTP.lane} and found SeqTracks differ!")

            oldFastqcFileNames = DataFile.findAllBySeqTrackInList(seqTracks).sort { it.id }.collect {
                fastqcDataFilesService.fastqcOutputFile(it)
            }

            oldPathsPerDataFile = collectFileNamesOfDataFiles(DataFile.findAllBySeqTrackInList(seqTracks))
        }

        private void checkInputInformation(Map<String, List<String>> inputInformationOTP) {
            extractSingleElement(inputInformationOTP.oldProjectName)

            extractSingleElement(inputInformationOTP.newProjectName)

            extractSingleElement(inputInformationOTP.oldPid)

            extractSingleElement(inputInformationOTP.newPid)

            extractSingleElement(inputInformationOTP.oldSampleTypeName)

            extractSingleElement(inputInformationOTP.newSampleTypeName)

            extractSingleElement(inputInformationOTP.runName)

            notNull(inputInformationOTP.lane, "lane not set")

            extractSingleElement(inputInformationOTP.oldSeqTypeName)

            extractSingleElement(inputInformationOTP.newSeqTypeName)

            extractSingleElement(inputInformationOTP.oldSingleCell)

            extractSingleElement(inputInformationOTP.newSingleCell)

            extractSingleElement(inputInformationOTP.oldLibraryLayout)

            extractSingleElement(inputInformationOTP.newLibraryLayout)

            extractSingleElement(inputInformationOTP.sampleNeedsToBeCreated)
        }

        private String extractSingleElement(List<String> stringList) {
            return CollectionUtils.exactlyOneElement(stringList)
        }
    }

    /**
     * function for a lane swap: Allow to move to another sample (defined by Individual & SampleType),
     * change SeqType, library layout, rename data files using function renameDataFiles.
     *
     * The DB is changed automatically.
     * For the filesystem changes a script is written to ${scriptOutputDirectory} on the server running otp
     */
    void swapLane(Map<String, List<String>> inputInformationOTP, Map<String, String> dataFileMap, String bashScriptName,
                  StringBuilder log, boolean failOnMissingFiles, Path scriptOutputDirectory,
                  boolean linkedFilesVerified = false) {
        log << "\nswap from ${inputInformationOTP.oldPid} ${inputInformationOTP.oldSampleTypeName} to " +
                "${inputInformationOTP.newPid} ${inputInformationOTP.newSampleTypeName}\n\n"

        // check Input
        notNull(bashScriptName, "bashScriptName not set")
        notNull(scriptOutputDirectory, "scriptOutputDirectory not set")

        // get DatabaseObjects and check findings
        SwapLaneHelper swapLaneHelper = new SwapLaneHelper(inputInformationOTP)
        log << "\n${swapLaneHelper.seqTracks.size()} seqTracks found\n"

        throwExceptionInCaseOfExternalMergedBamFileIsAttached(swapLaneHelper.seqTracks)
        if (!linkedFilesVerified) {
            throwExceptionInCaseOfSeqTracksAreOnlyLinked(swapLaneHelper.seqTracks)
        }

        completeOmittedNewValuesAndLog(dataFileMap, 'datafiles', log)

        // RestartAlignmentScript
        List<SeqTrack> seqTracksOfOldSample = SeqTrack.findAllBySampleAndSeqType(swapLaneHelper.oldSample, swapLaneHelper.oldSeqType)
        createGroovyConsoleScriptToRestartAlignments(
                scriptOutputDirectory,
                bashScriptName,
                configService.defaultRealm,
                seqTracksOfOldSample
        )

        // MoveFilesScript will be filled during routine
        Path bashScriptToMoveFiles = fileService.createOrOverwriteScriptOutputFile(
                scriptOutputDirectory,
                "${bashScriptName}.sh",
                configService.defaultRealm
        )
        bashScriptToMoveFiles << BASH_HEADER

        Path bashScriptToMoveFilesAsOtherUser = fileService.createOrOverwriteScriptOutputFile(
                scriptOutputDirectory,
                "${bashScriptName}-otherUser.sh",
                configService.defaultRealm
        )
        createBashScriptRoddy(
                seqTracksOfOldSample,
                swapLaneHelper.dirsToDelete,
                log, bashScriptToMoveFiles,
                bashScriptToMoveFilesAsOtherUser,
                !linkedFilesVerified
        )

        boolean alignmentsProcessed = AlignmentPass.findBySeqTrackInList(swapLaneHelper.seqTracks)
        if (alignmentsProcessed) {
            log << "Alignments found for SeqTracks ${swapLaneHelper.seqTracks}\n\n"

            seqTracksOfOldSample.each { SeqTrack seqTrack ->
                bashScriptToMoveFiles << "\n\n#Delete Alignment- & Merging stuff from ${seqTrack} and retrigger Alignment.\n"
                AlignmentPass.findAllBySeqTrack(seqTrack)
                swapLaneHelper.dirsToDelete.push(deletionService.deleteAllProcessingInformationAndResultOfOneSeqTrack(seqTrack, !linkedFilesVerified) as File)

                def alignmentDirType = DataProcessingFilesService.OutputDirectories.ALIGNMENT
                String baseDir = dataProcessingFilesService.getOutputDirectory(swapLaneHelper.oldIndividual, alignmentDirType)
                String middleDir = processedAlignmentFileService.getRunLaneDirectory(seqTrack)
                bashScriptToMoveFiles << "# rm -rf '${baseDir}/${middleDir}'\n"

                def mergingDirType = DataProcessingFilesService.OutputDirectories.MERGING
                String mergingResultDir = dataProcessingFilesService.getOutputDirectory(swapLaneHelper.oldIndividual, mergingDirType)
                bashScriptToMoveFiles << "# rm -rf '${mergingResultDir}/${swapLaneHelper.oldSampleType.name}/" +
                        "${swapLaneHelper.oldSeqType.name}/${swapLaneHelper.oldSeqType.libraryLayout}'\n"

                String projectDir = configService.rootPath
                String mergedAlignmentDir = mergedAlignmentDataFileService.buildRelativePath(swapLaneHelper.oldSeqType, swapLaneHelper.oldSample)
                bashScriptToMoveFiles << "# rm -rf '${projectDir}/${mergedAlignmentDir}'\n"
            }
            bashScriptToMoveFiles << "# delete analyses stuff\n"
            swapLaneHelper.dirsToDelete.flatten()*.path.each {
                bashScriptToMoveFiles << "#rm -rf ${it}\n"
            }
            isTrue(
                    AlignmentPass.findAllBySeqTrackInList(swapLaneHelper.seqTracks).isEmpty(),
                    "There are alignments for ${swapLaneHelper.seqTracks}, which can not be deleted"
            )
        }

        // change SeqTracks
        swapLaneHelper.seqTracks = swapLaneHelper.seqTracks.collect {
            if (swapLaneHelper.oldSeqType.hasAntibodyTarget != swapLaneHelper.newSeqType.hasAntibodyTarget) {
                throw new UnsupportedOperationException("Old and new SeqTypes (old: ${swapLaneHelper.oldSeqType};" +
                        " new: ${swapLaneHelper.newSeqType}) differ in antibody target usage and " +
                        "thus can not be swapped, as we would be missing the antibody target information.")
            }
            it.swapped = true
            it.seqType = swapLaneHelper.newSeqType
            it.sample = swapLaneHelper.newSample
            assert it.save(flush: true)
            return it
        }

        bashScriptToMoveFiles << "\n\n#copy and remove fastq files\n"
        bashScriptToMoveFiles << renameDataFiles(
                DataFile.findAllBySeqTrackInList(swapLaneHelper.seqTracks),
                swapLaneHelper.newProject,
                dataFileMap,
                swapLaneHelper.oldPathsPerDataFile,
                swapLaneHelper.sameLsdf,
                log
        )

        // files need to be already renamed at this point
        List<String> newFastqcFileNames = DataFile.findAllBySeqTrackInList(swapLaneHelper.seqTracks).sort { it.id }.collect {
            fastqcDataFilesService.fastqcOutputFile(it)
        }

        bashScriptToMoveFiles << "\n\n#copy and delete fastqc files\n\n"

        swapLaneHelper.oldFastqcFileNames.eachWithIndex { oldFastqcFileName, i ->
            bashScriptToMoveFiles << copyAndRemoveFastqcFile(oldFastqcFileName, newFastqcFileNames.get(i), log, failOnMissingFiles)
        }

        List<MergingAssignment> mergingAssignments = MergingAssignment.findAllBySeqTrackInList(swapLaneHelper.seqTracks)
        log << "\n${mergingAssignments.size()} MergingAssignment found"
        List<SeqScan> seqScans = mergingAssignments*.seqScan.unique()
        if (seqScans) {
            List<MergingLog> mergingLogs = MergingLog.findAllBySeqScanInList(seqScans)
            MergingAssignment.findAllBySeqScanInList(seqScans)*.delete()
            if (mergingLogs) {
                MergedAlignmentDataFile.findAllByMergingLogInList(mergingLogs)*.delete()
                mergingLogs*.delete()
            }
            seqScans*.delete()
        }

        // check if there are any remaining SeqTracks for this sample/seqType combination left
        if (SeqTrack.findAllBySampleAndSeqType(swapLaneHelper.oldSample, swapLaneHelper.oldSeqType).empty) {
            bashScriptToMoveFiles << "\n #There are no seqTracks belonging to the sample ${swapLaneHelper.oldSample} -> delete it on the filesystem\n\n"
            File basePath = swapLaneHelper.oldProject.projectSequencingDirectory
            bashScriptToMoveFiles << "#rm -rf '${basePath}/${swapLaneHelper.oldSeqType.dirName}/" +
                    "view-by-pid/${swapLaneHelper.oldIndividual.pid}/${swapLaneHelper.oldSampleType.dirName}/" +
                    "${swapLaneHelper.oldSeqType.libraryLayoutDirName}'\n"
        }

        // comment
        individualService.createComment("Lane swap",
                [
                        individual   : swapLaneHelper.oldIndividual,
                        project      : swapLaneHelper.oldProject.name,
                        sample       : swapLaneHelper.oldSample,
                        pid          : swapLaneHelper.oldIndividual.pid,
                        sampleType   : swapLaneHelper.oldSampleType.name,
                        seqType      : swapLaneHelper.oldSeqType.name,
                        singleCell   : swapLaneHelper.oldSeqType.singleCell,
                        libraryLayout: swapLaneHelper.oldSeqType.libraryLayout,
                ],
                [
                        individual   : swapLaneHelper.newIndividual,
                        project      : swapLaneHelper.newProject.name,
                        sample       : swapLaneHelper.newSample,
                        pid          : swapLaneHelper.newIndividual.pid,
                        sampleType   : swapLaneHelper.newSampleType.name,
                        seqType      : swapLaneHelper.newSeqType.name,
                        singleCell   : swapLaneHelper.newSeqType.singleCell,
                        libraryLayout: swapLaneHelper.newSeqType.libraryLayout,
                ],
                "run: ${swapLaneHelper.run.name}\nlane: ${inputInformationOTP.lane}"
        )

        createCommentForSwappedDatafiles(DataFile.findAllBySeqTrackInList(swapLaneHelper.seqTracks))
    }

    /**
     * Helper method to create a groovy script to restart the alignment for all SeqTracks of the old sample.
     * Returns all SeqTracks to restart.
     */
    List<SeqTrack> createGroovyConsoleScriptToRestartAlignments(Path scriptOutputDirectory, String bashScriptName, Realm realm, List<SeqTrack> seqTrackList) {
        Path groovyConsoleScriptToRestartAlignments = fileService.createOrOverwriteScriptOutputFile(
                scriptOutputDirectory, "restartAli_${bashScriptName}.groovy", realm
        )
        groovyConsoleScriptToRestartAlignments << ALIGNMENT_SCRIPT_HEADER

        seqTrackList.each { SeqTrack seqTrack ->
            groovyConsoleScriptToRestartAlignments << startAlignmentForSeqTrack(seqTrack)
        }

        return seqTrackList
    }

    /**
     * When an item of the swapMap should not change, the 'new' value can be left empty (i.e. empty string: '') for readability.
     *
     * This function fills in empty values of the swapMap with their corresponding key, thus creating a full map, which enables
     * us to proceed the same way for changing and un-changing entries.
     *
     * Note that this function mutates the swapMap in-place!
     */
    private static void completeOmittedNewValuesAndLog(Map<String, String> swapMap, String label, StringBuilder log) {
        log << "\n  swapping ${label}:"

        swapMap.each { String old, String neww ->
            String newValue
            // was the value omitted?
            if ((old.size() != 0) && !neww) {
                swapMap.put(old, old)
                newValue = old
            } else {
                newValue = neww
            }

            log << "\n    - ${old} --> ${newValue}"
        }
    }
}
