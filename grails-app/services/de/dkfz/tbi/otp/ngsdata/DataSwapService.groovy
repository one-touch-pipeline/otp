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

/**
 * @deprecated will be replaced with de.dkfz.tbi.otp.dataswap.DataSwapService and this will be deleted in otp-976
 */
@Deprecated
@SuppressWarnings(['JavaIoPackageAccess', 'Println'])
//This class is written for scripts, so it needs the output in stdout
@Transactional
class DataSwapService {

    static final String DIRECT_FILE_NAME = "directFileName"
    static final String VBP_FILE_NAME = "vbpFileName"
    static final String WELL_FILE_NAME = "wellFileName"
    static final String WELL_MAPPING_FILE_NAME = "wellMappingFileName"
    static final String WELL_MAPPING_FILE_ENTRY_NAME = "wellMappingFileEntry"
    static final String MISSING_FILES_TEXT = "The following files are expected, but not found:"
    static final String EXCESS_FILES_TEXT = "The following files are found, but not expected:"
    static final String BASH_HEADER = """\
            #!/bin/bash

            #PLEASE CHECK THE COMMANDS CAREFULLY BEFORE RUNNING THE SCRIPT

            set -e
            set -v

            """.stripIndent()
    static final String ALIGNMENT_SCRIPT_HEADER = "// ids of seqtracks which should be triggered with 'TriggerAlignment.groovy' for alignment\n\n"

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
        String processingPathToOldIndividual = dataProcessingFilesService.getOutputDirectory(
                moveIndividualHelper.oldIndividual,
                DataProcessingFilesService.OutputDirectories.BASE
        )
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
    private String renameDataFiles(
            List<DataFile> dataFiles,
            Project newProject,
            Map<String, String> dataFileMap,
            Map<DataFile, Map<String, String>> oldDataFileNameMap,
            boolean sameLsdf,
            StringBuilder log
    ) {
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
                assert it.seqTrack.seqType.libraryLayout == SequencingReadType.SINGLE: "sequencing read type is not ${SequencingReadType.SINGLE}"
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

    /**
     * create a bash script to delete files from roddy,
     * the script must be executed as other user
     */
    private void createBashScriptRoddy(
            List<SeqTrack> seqTrackList,
            List<File> dirsToDelete,
            StringBuilder log,
            Path bashScriptToMoveFiles,
            Path bashScriptToMoveFilesAsOtherUser,
            boolean enableChecks = true
    ) {
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
     * Helper method to create a groovy script to restart the alignment for all SeqTracks of the old sample.
     * Returns all SeqTracks to restart.
     */
    private List<SeqTrack> createGroovyConsoleScriptToRestartAlignments(
            Path scriptOutputDirectory,
            String bashScriptName,
            Realm realm,
            List<SeqTrack> seqTrackList
    ) {
        Path groovyConsoleScriptToRestartAlignments = fileService.createOrOverwriteScriptOutputFile(
                scriptOutputDirectory, "restartAli_${bashScriptName}.groovy", realm
        )
        groovyConsoleScriptToRestartAlignments << ALIGNMENT_SCRIPT_HEADER

        seqTrackList.each { SeqTrack seqTrack ->
            groovyConsoleScriptToRestartAlignments << "    ${seqTrack.id},  //${seqTrack}\n"
        }

        return seqTrackList
    }

    private String getFixGroupCommand(Path file) {
        "chgrp -h `stat -c '%G' ${file.parent}` ${file}"
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
     * function to get the copy and remove command for one fastqc file
     */
    private String copyAndRemoveFastqcFile(String oldDataFileName, String newDataFileName, StringBuilder log, boolean failOnMissingFiles) {
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

        SequencingReadType oldLibraryLayout
        SequencingReadType newLibraryLayout

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
                assert sampleList.isEmpty(): "The new Sample (${newIndividual} ${newSampleType}) does exist, but should not"
                newSample = new Sample(individual: newIndividual, sampleType: newSampleType).save(flush: true)
            } else {
                newSample = CollectionUtils.exactlyOneElement(sampleList, "The new Sample (${newIndividual} ${newSampleType}) does not exist")
            }

            oldLibraryLayout = SequencingReadType.findByName(extractSingleElement(inputInformationOTP.oldLibraryLayout))
            notNull(oldLibraryLayout, "The old LibraryLayout ${inputInformationOTP.oldLibraryLayout} does not exists")

            newLibraryLayout = SequencingReadType.findByName(extractSingleElement(inputInformationOTP.newLibraryLayout))
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
}
