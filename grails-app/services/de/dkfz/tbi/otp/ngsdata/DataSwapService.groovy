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
import de.dkfz.tbi.otp.utils.DeletionService

import java.nio.file.*

import static org.springframework.util.Assert.*

@SuppressWarnings(['JavaIoPackageAccess', 'Println']) //This class is written for scripts, so it needs the output in stdout
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
        List<MetaDataEntry> metaDataEntries = dataFiles ? MetaDataEntry.findAllByValueAndDataFileInListAndKey(oldValue, dataFiles, sampleIdentifierKeys.first()) : []
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
                "sed -i '/${oldValues[WELL_MAPPING_FILE_ENTRY_NAME]}/d' ${oldMappingFile}",
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
     */
    void moveIndividual(String oldProjectName, String newProjectName, String oldPid, String newPid, Map<String, String> sampleTypeMap,
                        Map<String, String> dataFileMap, String bashScriptName, StringBuilder log, boolean failOnMissingFiles,
                        Path scriptOutputDirectory) {
        log << "\n\nmove ${oldPid} of ${oldProjectName} to ${newPid} of ${newProjectName} "

        completeOmittedNewValuesAndLog(sampleTypeMap, 'samples', log)
        completeOmittedNewValuesAndLog(dataFileMap, 'datafiles', log)

        notNull(oldProjectName, "parameter oldProjectName may not be null")
        notNull(newProjectName, "parameter newProjectName may not be null")
        notNull(oldPid, "parameter oldPid may not be null")
        notNull(newPid, "parameter newPid may not be null")
        notNull(dataFileMap, "parameter dataFileMap may not be null")

        Project oldProject = Project.findByName(oldProjectName)
        notNull(oldProject, "old project ${oldProjectName} not found")
        Project newProject = Project.findByName(newProjectName)
        notNull(newProject, "new project ${newProjectName} not found")

        Individual oldIndividual = Individual.findByPid(oldPid)
        notNull(oldIndividual, "old pid ${oldPid} not found")
        Individual newIndividual = Individual.findByPid(newPid)
        if (oldPid != newPid) {
            isNull(newIndividual, "new pid ${newPid} already exist")
        }
        String processingPathToOldIndividual = dataProcessingFilesService.getOutputDirectory(oldIndividual, DataProcessingFilesService.OutputDirectories.BASE)

        List<Sample> samples = Sample.findAllByIndividual(oldIndividual)
        log << "\n  samples (${samples.size()}): ${samples}"
        notEmpty(samples, "no samples found for ${oldIndividual}")
        isTrue(samples.size() == sampleTypeMap.size())
        samples.each { Sample sample ->
            isTrue(sampleTypeMap.containsKey(sample.sampleType.name), "${sample.sampleType.name} missed in map")
            notNull(SampleType.findByName(sampleTypeMap.get(sample.sampleType.name)), "${sampleTypeMap.get(sample.sampleType.name)} " +
                    "not found in database")
        }

        isTrue(oldIndividual.project == oldProject, "old individual ${oldPid} should be in project ${oldProjectName}, " +
                "but was in ${oldIndividual.project}")

        List<SeqTrack> seqTracks = samples ? SeqTrack.findAllBySampleInList(samples) : []
        log << "\n  seqtracks (${seqTracks.size()}): "
        seqTracks.each { log << "\n    - ${it}" }

        boolean sameLsdf = oldProject.realm == newProject.realm

        List<File> dirsToDelete = []

        throwExceptionInCaseOfExternalMergedBamFileIsAttached(seqTracks)
        throwExceptionInCaseOfSeqTracksAreOnlyLinked(seqTracks)

        // now the changing process(procedure) starts
        if (seqTracks && AlignmentPass.findBySeqTrackInList(seqTracks)) {
            log << "\n -->     found alignments for seqtracks (${AlignmentPass.findAllBySeqTrackInList(seqTracks)*.seqTrack.unique()}): "
        }

        Path groovyConsoleScriptToRestartAlignments = fileService.createOrOverwriteScriptOutputFile(
                scriptOutputDirectory, "restartAli_${bashScriptName}.groovy"
        )
        groovyConsoleScriptToRestartAlignments << ALIGNMENT_SCRIPT_HEADER

        Path bashScriptToMoveFiles = fileService.createOrOverwriteScriptOutputFile(scriptOutputDirectory, "${bashScriptName}.sh")
        bashScriptToMoveFiles << BASH_HEADER

        Path bashScriptToMoveFilesAsOtherUser = fileService.createOrOverwriteScriptOutputFile(scriptOutputDirectory, "${bashScriptName}-otherUser.sh")
        createBashScriptRoddy(seqTracks, dirsToDelete, log, bashScriptToMoveFiles, bashScriptToMoveFilesAsOtherUser)

        seqTracks.each { SeqTrack seqTrack ->
            Map<String, List<File>> dirs = deletionService.deleteAllProcessingInformationAndResultOfOneSeqTrack(seqTrack)
            dirsToDelete.addAll(dirs.get("dirsToDelete"))
            bashScriptToMoveFilesAsOtherUser << "#rm -rf ${dirs.get("dirsToDeleteWithOtherUser").join("\n#rm -rf ")}\n"
            groovyConsoleScriptToRestartAlignments << startAlignmentForSeqTrack(seqTrack)
        }

        List<DataFile> fastqDataFiles = getAndValidateAndShowDataFilesForSeqTracks(seqTracks, dataFileMap, log)
        List<DataFile> bamDataFiles = getAndValidateAndShowAlignmentDataFilesForSeqTracks(seqTracks, dataFileMap, log)
        List<DataFile> dataFiles = [fastqDataFiles, bamDataFiles].flatten()
        Map<DataFile, Map<String, String>> oldDataFileNameMap = collectFileNamesOfDataFiles(dataFiles)
        List<String> oldFastqcFileNames = fastqDataFiles.collect { fastqcDataFilesService.fastqcOutputFile(it) }

        log << "\n  changing ${oldIndividual.project} to ${newProject} for ${oldIndividual}"
        oldIndividual.project = newProject
        oldIndividual.pid = newPid
        oldIndividual.mockPid = newPid
        oldIndividual.mockFullName = newPid
        oldIndividual.save(flush: true)

        bashScriptToMoveFiles << "\n\n################ move data files ################\n"
        bashScriptToMoveFiles << renameDataFiles(dataFiles, newProject, dataFileMap, oldDataFileNameMap, sameLsdf, log)

        samples.each { Sample sample ->
            SampleType newSampleType = SampleType.findByName(sampleTypeMap.get(sample.sampleType.name))
            log << "\n    change ${sample.sampleType.name} to ${newSampleType.name}"
            SampleIdentifier.findAllBySample(sample)*.delete(flush: true)
            sample.sampleType = newSampleType
            sample.save(flush: true)
        }

        bashScriptToMoveFiles << "\n\n\n ################ move fastq files ################ \n"
        samples = Sample.findAllByIndividual(oldIndividual)
        seqTracks = samples ? SeqTrack.findAllBySampleInList(samples) : []
        List<DataFile> newDataFiles = seqTracks ? DataFile.findAllBySeqTrackInList(seqTracks) : []
        List<String> newFastqcFileNames = newDataFiles.collect { fastqcDataFilesService.fastqcOutputFile(it) }

        oldFastqcFileNames.eachWithIndex { oldFastqcFileName, i ->
            bashScriptToMoveFiles << copyAndRemoveFastqcFile(oldFastqcFileName, newFastqcFileNames.get(i), log, failOnMissingFiles)
        }

        bashScriptToMoveFiles << "\n\n################ delete analysis stuff ################\n"
        dirsToDelete.flatten()*.path.each {
            bashScriptToMoveFiles << "#rm -rf ${it}\n"
        }

        bashScriptToMoveFiles << "\n\n\n ################ delete old Individual ################ \n"
        bashScriptToMoveFiles << "# rm -rf ${oldProject.projectSequencingDirectory}/*/view-by-pid/${oldPid}/\n"
        bashScriptToMoveFiles << "# rm -rf ${processingPathToOldIndividual}\n"

        individualService.createComment("Individual swap", [
                individual: oldIndividual,
                project: oldProjectName,
                pid: oldPid,
        ], [
                individual: oldIndividual,
                project: newProjectName,
                pid: newPid,
        ])

        createCommentForSwappedDatafiles(dataFiles)
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
    void moveSample(String oldProjectName, String newProjectName, String oldPid, String newPid, String oldSampleTypeName,
                    String newSampleTypeName, Map<String, String> dataFileMap, String bashScriptName,
                    StringBuilder log, boolean failOnMissingFiles, Path scriptOutputDirectory,
                    boolean linkedFilesVerified = false) throws IOException{
        log << "\n\nmove ${oldPid} ${oldSampleTypeName} of ${oldProjectName} to ${newPid} ${newSampleTypeName} of ${newProjectName} "
        completeOmittedNewValuesAndLog(dataFileMap, 'datafiles', log)

        notNull(oldProjectName, "parameter oldProjectName may not be null")
        notNull(newProjectName, "parameter newProjectName may not be null")
        notNull(oldPid, "parameter oldPid may not be null")
        notNull(newPid, "parameter newPid may not be null")
        notNull(oldSampleTypeName, "parameter oldSampleTypeName may not be null")
        notNull(newSampleTypeName, "parameter newSampleTypeName may not be null")
        notNull(dataFileMap, "parameter dataFileMap may not be null")
        notNull(bashScriptName, "parameter bashScriptName may not be null")

        Project oldProject = Project.findByName(oldProjectName)
        notNull(oldProject, "old project ${oldProjectName} not found")
        Project newProject = Project.findByName(newProjectName)
        notNull(newProject, "new project ${newProjectName} not found")

        Individual oldIndividual = Individual.findByPid(oldPid)
        notNull(oldIndividual, "old pid ${oldPid} not found")
        Individual newIndividual = Individual.findByPid(newPid)
        notNull(newIndividual, "new pid ${newPid} not found")
        isTrue(oldIndividual.project == oldProject, "old individual ${oldPid} should be in project {oldProjectName}, " +
                "but was in ${oldIndividual.project}")
        isTrue(newIndividual.project == newProject, "new individual ${newPid} should be in project {newProjectName}, " +
                "but was in ${newIndividual.project}")

        SampleType oldSampleType = SampleType.findByName(oldSampleTypeName)
        notNull(oldSampleType, "old sample type ${oldSampleTypeName} not found")
        SampleType newSampleType = SampleType.findByName(newSampleTypeName)
        notNull(newSampleType, "new sample type ${newSampleTypeName} not found")

        Sample sample = getSingleSampleForIndividualAndSampleType(oldIndividual, oldSampleType, log)

        boolean sameLsdf = oldProject.realm == newProject.realm

        List<SeqTrack> seqTrackList = getAndShowSeqTracksForSample(sample, log)

        throwExceptionInCaseOfExternalMergedBamFileIsAttached(seqTrackList)
        if (!linkedFilesVerified) {
            throwExceptionInCaseOfSeqTracksAreOnlyLinked(seqTrackList)
        }

        if (seqTrackList && AlignmentPass.findBySeqTrackInList(seqTrackList)) {
            log << "\n -->     found alignments for seqtracks (${AlignmentPass.findBySeqTrackInList(seqTrackList)*.seqTrack.unique()}): "
        }

        List<DataFile> fastqDataFiles = getAndValidateAndShowDataFilesForSeqTracks(seqTrackList, dataFileMap, log)
        List<DataFile> bamDataFiles = getAndValidateAndShowAlignmentDataFilesForSeqTracks(seqTrackList, dataFileMap, log)
        List<DataFile> dataFiles = [fastqDataFiles, bamDataFiles].flatten()
        Map<DataFile, Map<String, String>> oldDataFileNameMap = collectFileNamesOfDataFiles(dataFiles)
        List<String> oldFastqcFileNames = fastqDataFiles.collect { fastqcDataFilesService.fastqcOutputFile(it) }
        List<File> dirsToDelete = []

        // validating ends here, now the changing are started

        Path groovyConsoleScriptToRestartAlignments = fileService.createOrOverwriteScriptOutputFile(scriptOutputDirectory, "restartAli_${bashScriptName}.groovy")
        groovyConsoleScriptToRestartAlignments << ALIGNMENT_SCRIPT_HEADER

        Path bashScriptToMoveFiles = fileService.createOrOverwriteScriptOutputFile(scriptOutputDirectory, "${bashScriptName}.sh")
        bashScriptToMoveFiles << BASH_HEADER

        Path bashScriptToMoveFilesAsOtherUser = fileService.createOrOverwriteScriptOutputFile(scriptOutputDirectory, "${bashScriptName}-otherUser.sh")
        createBashScriptRoddy(seqTrackList, dirsToDelete, log, bashScriptToMoveFiles, bashScriptToMoveFilesAsOtherUser, !linkedFilesVerified)

        seqTrackList.each { SeqTrack seqTrack ->
            Map dirs = deletionService.deleteAllProcessingInformationAndResultOfOneSeqTrack(seqTrack, !linkedFilesVerified)
            dirsToDelete << dirs.get("dirsToDelete")
            bashScriptToMoveFilesAsOtherUser << "#rm -rf ${dirs.get("dirsToDeleteWithOtherUser").join("\n#rm -rf ")}\n"
            groovyConsoleScriptToRestartAlignments << startAlignmentForSeqTrack(seqTrack)
        }

        if (seqTrackList && AlignmentPass.findBySeqTrackInList(seqTrackList)) {
            bashScriptToMoveFiles << "\n\n\n ################ delete old aligned & merged files ################ \n"

            List<AlignmentPass> alignmentPasses = AlignmentPass.findAllBySeqTrackInList(seqTrackList)
            alignmentPasses.each { AlignmentPass alignmentPass ->
                def dirTypeAlignment = DataProcessingFilesService.OutputDirectories.ALIGNMENT
                String baseDirAlignment = dataProcessingFilesService.getOutputDirectory(oldIndividual, dirTypeAlignment)
                String middleDirAlignment = processedAlignmentFileService.getRunLaneDirectory(alignmentPass.seqTrack)
                String oldPathToAlignedFiles = "${baseDirAlignment}/${middleDirAlignment}"
                bashScriptToMoveFiles << "#rm -rf ${oldPathToAlignedFiles}\n"
            }

            def dirTypeMerging = DataProcessingFilesService.OutputDirectories.MERGING
            String baseDirMerging = dataProcessingFilesService.getOutputDirectory(oldIndividual, dirTypeMerging)
            String oldProcessingPathToMergedFiles = "${baseDirMerging}/${oldSampleType.name}"
            bashScriptToMoveFiles << "#rm -rf ${oldProcessingPathToMergedFiles}\n"

            List<ProcessedMergedBamFile> processedMergedBamFiles = ProcessedMergedBamFile.createCriteria().list {
                mergingPass {
                    mergingSet {
                        mergingWorkPackage {
                            eq("sample", sample)
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
        } else {
            // If the seqTracks were not aligned for whatever reason they will be aligned now.
            // !! Check if the seqTracks have to be aligned. If not, comment out this part.
            seqTrackList.each { SeqTrack seqTrack ->
                groovyConsoleScriptToRestartAlignments << startAlignmentForSeqTrack(seqTrack)
            }
        }

        sample.sampleType = newSampleType
        sample.individual = newIndividual
        sample.save(flush: true)

        bashScriptToMoveFiles << "################ move data files ################ \n"
        bashScriptToMoveFiles << renameDataFiles(dataFiles, newProject, dataFileMap, oldDataFileNameMap, sameLsdf, log)

        SampleIdentifier.findAllBySample(sample)*.delete(flush: true)

        List<String> newFastqcFileNames = fastqDataFiles.collect { fastqcDataFilesService.fastqcOutputFile(it) }

        bashScriptToMoveFiles << "\n\n\n ################ move fastq files ################ \n"

        oldFastqcFileNames.eachWithIndex { oldFastqcFileName, i ->
            bashScriptToMoveFiles << copyAndRemoveFastqcFile(oldFastqcFileName, newFastqcFileNames.get(i), log, failOnMissingFiles)
        }

        bashScriptToMoveFiles << "# delete snv stuff\n"
        dirsToDelete.flatten()*.path.each {
            bashScriptToMoveFiles << "#rm -rf ${it}\n"
        }

        individualService.createComment("Sample swap", [
                individual: oldIndividual,
                project: oldProjectName,
                pid: oldPid,
                sampleType: oldSampleTypeName,
        ], [
                individual: newIndividual,
                project: newProjectName,
                pid: newPid,
                sampleType: newSampleTypeName,
        ])

        createCommentForSwappedDatafiles(dataFiles)
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
        }

        if (roddyBamFiles) {
            bashScriptToMoveFilesAsOtherUser << BASH_HEADER

            bashScriptToMoveFilesAsOtherUser << "\n\n\n ################ delete otherUser files ################ \n"
            roddyBamFiles.each { RoddyBamFile roddyBamFile ->
                if (roddyBamFile.isOldStructureUsed()) {
                    bashScriptToMoveFilesAsOtherUser <<
                            "#rm -rf ${roddyBamFile.getFinalExecutionDirectories()*.absolutePath.join("\n#rm -rf ")}\n" +
                            "#rm -rf ${roddyBamFile.getFinalSingleLaneQADirectories().values()*.listFiles().flatten()*.absolutePath.join("\n#rm -rf ")}\n"
                    if (roddyBamFile.isMostRecentBamFile()) {
                        bashScriptToMoveFilesAsOtherUser << "#rm -rf ${roddyBamFile.getFinalMergedQADirectory().listFiles()*.absolutePath.join("\n#rm -rf ")}\n"
                    }
                } else {
                    bashScriptToMoveFilesAsOtherUser <<
                            "#rm -rf ${roddyBamFile.getWorkExecutionDirectories()*.absolutePath.join("\n#rm -rf ")}\n" +
                            "#rm -rf ${roddyBamFile.getWorkMergedQADirectory().absolutePath}\n" +
                            "#rm -rf ${roddyBamFile.getWorkSingleLaneQADirectories().values()*.absolutePath.join("\n#rm -rf ")}\n"
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
        assert externallyProcessedMergedBamFiles.empty : "There are ExternallyProcessedMergedBamFiles attached: ${externallyProcessedMergedBamFiles}"
    }

    /**
     * In case the seqTracks are only linked, the script shall stop
     */
    void throwExceptionInCaseOfSeqTracksAreOnlyLinked(List<SeqTrack> seqTracks) {
        int linkedSeqTracks = seqTracks.findAll { SeqTrack seqTrack ->
            seqTrack.linkedExternally
        }.size()
        assert !linkedSeqTracks : "There are ${linkedSeqTracks} seqTracks only linked"
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
            t.getStackTrace().each { log << "\n    ${it}" }
            println log
        }
    }

    /**
     * Create a warning comment in case the datafile is swapped
     */
    void createCommentForSwappedDatafiles(List<DataFile> datafiles) {
        datafiles.each { DataFile dataFile ->
            if (dataFile.getComment()?.comment) {
                commentService.saveComment(dataFile, dataFile.getComment().comment + "\nAttention: Datafile swapped!")
            } else {
                commentService.saveComment(dataFile, "Attention: Datafile swapped!")
            }
        }
    }


    /**
     * function for a lane swap: Allow to move to another sample (defined by Individual & SampleType),
     * change SeqType, library layout, rename data files using function renameDataFiles.
     *
     * The DB is changed automatically.
     * For the filesystem changes a script is written to ${scriptOutputDirectory} on the server running otp
     */
    //no test written, because a new data swap function are planned
    void swapLane(Map<String, String> inputInformationOTP, Map<String, String> dataFileMap, String bashScriptName,
                  StringBuilder log, boolean failOnMissingFiles, Path scriptOutputDirectory,
                  boolean linkedFilesVerified = false) {
        log << "\nswap from ${inputInformationOTP.oldPid} ${inputInformationOTP.oldSampleTypeName} to " +
                "${inputInformationOTP.newPid} ${inputInformationOTP.newSampleTypeName}\n\n"

        notNull(inputInformationOTP.oldProjectName)
        notNull(inputInformationOTP.newProjectName)
        notNull(inputInformationOTP.oldPid)
        notNull(inputInformationOTP.newPid)
        notNull(inputInformationOTP.oldSampleTypeName)
        notNull(inputInformationOTP.newSampleTypeName)
        notNull(inputInformationOTP.runName)
        notNull(inputInformationOTP.lane)
        notNull(inputInformationOTP.oldSeqTypeName)
        notNull(inputInformationOTP.newSeqTypeName)
        notNull(inputInformationOTP.oldSingleCell)
        notNull(inputInformationOTP.newSingleCell)
        notNull(inputInformationOTP.oldLibraryLayout)
        notNull(inputInformationOTP.newLibraryLayout)
        notNull(bashScriptName)
        notNull(scriptOutputDirectory)

        completeOmittedNewValuesAndLog(dataFileMap, 'datafiles', log)

        Path groovyConsoleScriptToRestartAlignments = fileService.createOrOverwriteScriptOutputFile(
                scriptOutputDirectory, "restartAli_${bashScriptName}.groovy"
        )
        groovyConsoleScriptToRestartAlignments << ALIGNMENT_SCRIPT_HEADER

        Path bashScriptToMoveFiles = fileService.createOrOverwriteScriptOutputFile(scriptOutputDirectory, "${bashScriptName}.sh")
        bashScriptToMoveFiles << BASH_HEADER

        Run run = Run.findByName(inputInformationOTP.runName)
        notNull(run, "The run (${run}) does not exist")

        Project oldProject = Project.findByName(inputInformationOTP.oldProjectName)
        notNull(oldProject, "The old project (${oldProject}) does not exist")
        Project newProject = Project.findByName(inputInformationOTP.newProjectName)
        notNull(newProject, "The new project (${newProject}) does not exist")

        boolean sameLsdf = oldProject.realm == newProject.realm

        Individual oldIndividual = Individual.findByPid(inputInformationOTP.oldPid)
        notNull(oldIndividual, "The old Individual (${oldIndividual}) does not exist")
        Individual newIndividual = Individual.findByPid(inputInformationOTP.newPid)
        notNull(newIndividual, "The new Individual (${newIndividual}) does not exist")

        isTrue(oldIndividual.project == oldProject, "The old individual does not exist in the old project")
        isTrue(newIndividual.project == newProject, "The new individual does not exist in the new project")

        SampleType oldSampleType = SampleType.findByName(inputInformationOTP.oldSampleTypeName)
        notNull(oldSampleType, "The old SampleType (${oldSampleType}) does not exist")
        SampleType newSampleType = SampleType.findByName(inputInformationOTP.newSampleTypeName)
        notNull(newSampleType, "The new SampleType (${newSampleType}) does not exist")

        Sample oldSample = Sample.findByIndividualAndSampleType(oldIndividual, oldSampleType)
        notNull(oldSample, "The old Sample (${oldSample}) does not exist")
        Sample newSample = Sample.findByIndividualAndSampleType(newIndividual, newSampleType)
        if (!inputInformationOTP["sampleNeedsToBeCreated"]) {
            notNull(newSample, "The new Sample (${newIndividual} ${newSampleType}) does not exist")
        } else {
            isNull(newSample, "The new Sample (${newSample}) does exist, but should not")
            newSample = new Sample(individual: newIndividual, sampleType: newSampleType).save(flush: true)
        }

        SeqType oldSeqType = SeqType.findByNameAndLibraryLayoutAndSingleCell(
                inputInformationOTP.oldSeqTypeName, inputInformationOTP.oldLibraryLayout, inputInformationOTP.oldSingleCell)
        notNull(oldSeqType, "The old seqtype ${inputInformationOTP.oldSeqTypeName} ${inputInformationOTP.oldLibraryLayout} does not exist")
        SeqType newSeqType = SeqType.findByNameAndLibraryLayoutAndSingleCell(
                inputInformationOTP.newSeqTypeName, inputInformationOTP.newLibraryLayout, inputInformationOTP.newSingleCell)
        notNull(newSeqType, "The new seqtype ${inputInformationOTP.newSeqTypeName} ${inputInformationOTP.oldLibraryLayout} does not exist")

        List<SeqTrack> seqTracks = SeqTrack.findAllBySampleAndRunAndLaneIdInList(oldSample, run, inputInformationOTP.lane)
        log << "\n${seqTracks.size()} seqTracks found\n"
        isTrue(seqTracks*.seqType.unique().size() == 1)
        isTrue(seqTracks*.seqType.first() == oldSeqType, "expected '${oldSeqType}' but found '${seqTracks*.seqType.first()}'")
        isTrue(seqTracks.size() == inputInformationOTP.lane.size())
        List<File> dirsToDelete = []

        List<String> oldFastqcFileNames = DataFile.findAllBySeqTrackInList(seqTracks).sort { it.id }.collect {
            fastqcDataFilesService.fastqcOutputFile(it)
        }

        Map<DataFile, Map<String, String>> oldPathsPerDataFile = collectFileNamesOfDataFiles(DataFile.findAllBySeqTrackInList(seqTracks))

        throwExceptionInCaseOfExternalMergedBamFileIsAttached(seqTracks)
        if (!linkedFilesVerified) {
            throwExceptionInCaseOfSeqTracksAreOnlyLinked(seqTracks)
        }

        individualService.createComment("Lane swap",
                [
                        individual: oldIndividual,
                        project: oldProject.name,
                        sample: oldSample,
                        pid: inputInformationOTP.oldPid,
                        sampleType: oldSampleType.name,
                        seqType: oldSeqType.name,
                        singleCell: oldSeqType.singleCell,
                        libraryLayout: oldSeqType.libraryLayout,
                ],
                [
                        individual: newIndividual,
                        project: newProject.name,
                        sample: newSample,
                        pid: inputInformationOTP.newPid,
                        sampleType: newSampleType.name,
                        seqType: newSeqType.name,
                        singleCell: newSeqType.singleCell,
                        libraryLayout: newSeqType.libraryLayout,
                ],
                "run: ${run.name}\nlane: ${inputInformationOTP.lane}"
        )



        List<SeqTrack> seqTracksOfOldSample = SeqTrack.findAllBySampleAndSeqType(oldSample, oldSeqType)

        seqTracksOfOldSample.each { SeqTrack seqTrack ->
            groovyConsoleScriptToRestartAlignments << startAlignmentForSeqTrack(seqTrack)
        }

        Path bashScriptToMoveFilesAsOtherUser = fileService.createOrOverwriteScriptOutputFile(scriptOutputDirectory, "${bashScriptName}-otherUser.sh")
        createBashScriptRoddy(seqTracksOfOldSample, dirsToDelete, log, bashScriptToMoveFiles,
                bashScriptToMoveFilesAsOtherUser, !linkedFilesVerified)

        boolean alignmentsProcessed = AlignmentPass.findBySeqTrackInList(seqTracks)
        if (alignmentsProcessed) {
            log << "Alignments found for SeqTracks ${seqTracks}\n\n"

            seqTracksOfOldSample.each { SeqTrack seqTrack ->
                bashScriptToMoveFiles << "\n\n#Delete Alignment- & Merging stuff from ${seqTrack} and retrigger Alignment.\n"
                AlignmentPass.findAllBySeqTrack(seqTrack)
                dirsToDelete << deletionService.deleteAllProcessingInformationAndResultOfOneSeqTrack(seqTrack, !linkedFilesVerified)

                def alignmentDirType = DataProcessingFilesService.OutputDirectories.ALIGNMENT
                String baseDir = dataProcessingFilesService.getOutputDirectory(oldIndividual, alignmentDirType)
                String middleDir = processedAlignmentFileService.getRunLaneDirectory(seqTrack)
                bashScriptToMoveFiles << "# rm -rf '${baseDir}/${middleDir}'\n"

                def mergingDirType = DataProcessingFilesService.OutputDirectories.MERGING
                String mergingResultDir = dataProcessingFilesService.getOutputDirectory(oldIndividual, mergingDirType)
                bashScriptToMoveFiles << "# rm -rf '${mergingResultDir}/${oldSampleType.name}/${oldSeqType.name}/${oldSeqType.libraryLayout}'\n"

                String projectDir = configService.getRootPath()
                String mergedAlignmentDir = mergedAlignmentDataFileService.buildRelativePath(oldSeqType, oldSample)
                bashScriptToMoveFiles << "# rm -rf '${projectDir}/${mergedAlignmentDir}'\n"
            }
            bashScriptToMoveFiles << "# delete analyses stuff\n"
            dirsToDelete.flatten()*.path.each {
                bashScriptToMoveFiles << "#rm -rf ${it}\n"
            }
            isTrue(AlignmentPass.findAllBySeqTrackInList(seqTracks).isEmpty(), "There are alignments for ${seqTracks}, which can not be deleted")
        }

        seqTracks*.sample = newSample
        notNull(seqTracks*.save(flush: true))
        seqTracks = seqTracks.collect {
            if (oldSeqType.hasAntibodyTarget != newSeqType.hasAntibodyTarget) {
                throw new UnsupportedOperationException("Old and new SeqTypes (old: ${oldSeqType}; new: ${newSeqType}) differ in antibody target usage and " +
                        "thus can not be swapped, as we would be missing the antibody target information.")
            }
            it.seqType = newSeqType
            assert it.save(flush: true)
            return it
        }

        bashScriptToMoveFiles << "\n\n#copy and remove fastq files\n"
        bashScriptToMoveFiles << renameDataFiles(DataFile.findAllBySeqTrackInList(seqTracks), newProject, dataFileMap, oldPathsPerDataFile,
                sameLsdf, log)

        List<String> newFastqcFileNames = DataFile.findAllBySeqTrackInList(seqTracks).sort { it.id }.collect {
            fastqcDataFilesService.fastqcOutputFile(it)
        }

        bashScriptToMoveFiles << "\n\n#copy and delete fastqc files\n\n"

        oldFastqcFileNames.eachWithIndex { oldFastqcFileName, i ->
            bashScriptToMoveFiles << copyAndRemoveFastqcFile(oldFastqcFileName, newFastqcFileNames.get(i), log, failOnMissingFiles)
        }

        List<MergingAssignment> mergingAssignments = MergingAssignment.findAllBySeqTrackInList(seqTracks)
        log << "\n${mergingAssignments.size()} MergingAssignment found"
        List<SeqScan> seqScans = mergingAssignments*.seqScan.unique()
        if (seqScans)  {
            List<MergingLog> mergingLogs = MergingLog.findAllBySeqScanInList(seqScans)
            MergingAssignment.findAllBySeqScanInList(seqScans)*.delete()
            if (mergingLogs) {
                MergedAlignmentDataFile.findAllByMergingLogInList(mergingLogs)*.delete()
                mergingLogs*.delete()
            }
            seqScans*.delete()
        }

        createCommentForSwappedDatafiles(DataFile.findAllBySeqTrackInList(seqTracks))

        if (SeqTrack.findAllBySampleAndSeqType(oldSample, oldSeqType).empty) {
            bashScriptToMoveFiles << "\n #There are no seqTracks belonging to the sample ${oldSample} -> delete it on the filesystem\n\n"
            File basePath = oldProject.getProjectSequencingDirectory()
            bashScriptToMoveFiles << "#rm -rf '${basePath}/${oldSeqType.dirName}/view-by-pid/${oldIndividual.pid}/${oldSampleType.dirName}/" +
                    "${oldSeqType.libraryLayoutDirName}'\n"
        }
    }

    /**
     * When an item of the swapMap should not change, the 'new' value can be left empty (i.e. empty string: '') for readability.
     *
     * This function fills in empty values of the swapMap with their corresponding key, thus creating a full map, which enables
     * us to proceed the same way for changing and un-changing entries.
     *
     * Note that this function mutates the swapMap in-place!
     */
    private void completeOmittedNewValuesAndLog(Map<String, String> swapMap, String label, StringBuilder log) {
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
