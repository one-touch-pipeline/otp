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
import groovy.sql.Sql

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.AnalysisDeletionService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.fileSystemConsistency.ConsistencyStatus
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.utils.CollectionUtils

import javax.sql.DataSource
import java.nio.file.*

import static org.springframework.util.Assert.*

@Transactional
class DataSwapService {
    IndividualService individualService
    CommentService commentService
    FastqcDataFilesService fastqcDataFilesService
    LsdfFilesService lsdfFilesService
    DataProcessingFilesService dataProcessingFilesService
    ProcessedAlignmentFileService processedAlignmentFileService
    ConfigService configService
    SeqTrackService seqTrackService
    AnalysisDeletionService analysisDeletionService
    DataSource dataSource
    MergedAlignmentDataFileService mergedAlignmentDataFileService
    FileService fileService

    static final String MISSING_FILES_TEXT = "The following files are expected, but not found:"
    static final String EXCESS_FILES_TEXT = "The following files are found, but not expected:"

    static final String bashHeader = """\
            #!/bin/bash

            #PLEASE CHECK THE COMMANDS CAREFULLY BEFORE RUNNING THE SCRIPT

            set -e
            set -v

            """.stripIndent()

    static final String alignmentScriptHeader = """\
            // PLEASE CHECK THE COMMANDS CAREFULLY BEFORE RUNNING THE SCRIPT

            // !!Before retriggering the alignments the bashScriptToMoveFiles has to be executed!!

            import de.dkfz.tbi.otp.ngsdata.*
            import de.dkfz.tbi.otp.dataprocessing.*
            import de.dkfz.tbi.otp.ngsqc.*

            """.stripIndent()


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
     * @return The {@link SeqTrack}, possibly with a new type.
     */
    @SuppressWarnings("ParameterReassignment")
    SeqTrack changeSeqType(SeqTrack seqTrack, SeqType newSeqType) {
        assert seqTrack.class == seqTrack.seqType.seqTrackClass
        if (seqTrack.seqType.id != newSeqType.id) {
            if (seqTrack.class != newSeqType.seqTrackClass) {
                if (newSeqType.hasAntibodyTarget) {
                    throw new UnsupportedOperationException("Changing to ${newSeqType} is not supported yet because new need antibody, which is not avilable.")
                }
                Sql sql = new Sql(dataSource)
                assert 1 == sql.executeUpdate("update seq_track set class = ${newSeqType.seqTrackClass.name} " +
                        "where id = ${seqTrack.id} and class = ${seqTrack.class.name};")
                SeqTrack.withSession { session ->
                    session.clear()
                }
                seqTrack = SeqTrack.get(seqTrack.id)
            }
            assert seqTrack.class == newSeqType.seqTrackClass
            seqTrack.seqType = newSeqType
            assert seqTrack.save(flush: true)
        }
        return seqTrack
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
            map[dataFile] = [directFileName: directFileName, vbpFileName: vbpFileName]
        }
        return map
    }

    /**
     * rename all sample identifiers of the given identifier. The new name is the old name added with the text
     * "was changed on" and the current date.
     *
     * @param sample the sample which identifier should be renamed
     * @return void
     */
    void renameSampleIdentifiers(Sample sample, StringBuilder log) {
        List<SampleIdentifier> sampleIdentifiers = SampleIdentifier.findAllBySample(sample)
        log << "\n  sampleIdentifier for sample ${sample} (${sampleIdentifiers.size()}): ${sampleIdentifiers}"
        String postfix = " was changed on ${new Date().format('yyyy-MM-dd')}"
        sampleIdentifiers.each { SampleIdentifier sampleIdentifier ->
            String oldSampleIdentifier = sampleIdentifier.name
            sampleIdentifier.name += postfix
            sampleIdentifier.save(flush: true)
            changeMetadataEntry(sample, MetaDataColumn.SAMPLE_ID.name(), oldSampleIdentifier, sampleIdentifier.name)
        }
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

            String oldDirectFileName = oldDataFileNameMap.find { key, value -> key.id == it.id }.value["directFileName"]
            String oldVbpFileName = oldDataFileNameMap.find { key, value -> key.id == it.id }.value["vbpFileName"]
            File directFile = new File(oldDirectFileName)
            File vbpFile = new File(oldVbpFileName)

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
if [ -e '${oldDirectFileName}.md5sum' ]; then
  mv '${oldDirectFileName}.md5sum' \\
     '${newDirectFileName}.md5sum';
fi
"""
                } else {
                    bashMoveDirectFile += """
cp '${oldDirectFileName}' '${newDirectFileName}';
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
            bashScriptToMoveFiles += "${bashMoveDirectFile}\n${bashMoveVbpFile}\n\n"
        }
        return bashScriptToMoveFiles
    }

    /**
     * The input SeqTrack is passed to the AlignmentDecider
     */
    String startAlignmentForSeqTrack(SeqTrack seqTrack) {
        if (SeqTypeService.getAllAlignableSeqTypes().contains(seqTrack.seqType)) {
            return "// lane: ${seqTrack}\nctx.seqTrackService.decideAndPrepareForAlignment(SeqTrack.get(${seqTrack.id}))\n"
        } else {
            return "// The SeqTrack ${seqTrack} has the seqType ${seqTrack.seqType.name} and will not be aligned\n"
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
        groovyConsoleScriptToRestartAlignments << alignmentScriptHeader

        Path bashScriptToMoveFiles = fileService.createOrOverwriteScriptOutputFile(scriptOutputDirectory, "${bashScriptName}.sh")
        bashScriptToMoveFiles << bashHeader

        Path bashScriptToMoveFilesAsOtherUser = fileService.createOrOverwriteScriptOutputFile(scriptOutputDirectory, "${bashScriptName}-otherUser.sh")
        createBashScriptRoddy(seqTracks, dirsToDelete, log, bashScriptToMoveFiles, bashScriptToMoveFilesAsOtherUser)

        seqTracks.each { SeqTrack seqTrack ->
            Map dirs = deleteAllProcessingInformationAndResultOfOneSeqTrack(seqTrack)
            dirsToDelete << dirs.get("dirsToDelete")
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
            renameSampleIdentifiers(sample, log)
            sample.sampleType = newSampleType
            sample.save(flush: true)
        }

        bashScriptToMoveFiles << "\n\n\n ################ move fastq files ################ \n"
        samples = Sample.findAllByIndividual(oldIndividual)
        seqTracks = samples ? SeqTrack.findAllBySampleInList(samples) : []
        List<DataFile> newDataFiles = seqTracks ? DataFile.findAllBySeqTrackInList(seqTracks) : []
        List<String> newFastqcFileNames = newDataFiles.collect { fastqcDataFilesService.fastqcOutputFile(it) }

        oldFastqcFileNames.eachWithIndex() { oldFastqcFileName, i ->
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
        groovyConsoleScriptToRestartAlignments << alignmentScriptHeader

        Path bashScriptToMoveFiles = fileService.createOrOverwriteScriptOutputFile(scriptOutputDirectory, "${bashScriptName}.sh")
        bashScriptToMoveFiles << bashHeader

        Path bashScriptToMoveFilesAsOtherUser = fileService.createOrOverwriteScriptOutputFile(scriptOutputDirectory, "${bashScriptName}-otherUser.sh")
        createBashScriptRoddy(seqTrackList, dirsToDelete, log, bashScriptToMoveFiles, bashScriptToMoveFilesAsOtherUser, !linkedFilesVerified)

        seqTrackList.each { SeqTrack seqTrack ->
            Map dirs = deleteAllProcessingInformationAndResultOfOneSeqTrack(seqTrack, !linkedFilesVerified)
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

        renameSampleIdentifiers(sample, log)

        List<String> newFastqcFileNames = fastqDataFiles.collect { fastqcDataFilesService.fastqcOutputFile(it) }

        bashScriptToMoveFiles << "\n\n\n ################ move fastq files ################ \n"

        oldFastqcFileNames.eachWithIndex() { oldFastqcFileName, i ->
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

    List<String> deleteIndividual(String pid, boolean check = true) {
        Individual individual = CollectionUtils.exactlyOneElement(
                Individual.findAllByPid(pid), "No individual could be found for PID ${pid}")
        StringBuilder deletionScript = new StringBuilder()
        StringBuilder deletionScriptOtherUser = new StringBuilder()

        List<Sample> samples = Sample.findAllByIndividual(individual)

        List<SeqType> seqTypes = []

        samples.each { Sample sample ->
            List<SeqTrack> seqTracks = SeqTrack.findAllBySample(sample)

            seqTracks.each { SeqTrack seqTrack ->
                seqTrack.dataFiles.each { DataFile dataFile ->
                    deletionScript << "rm -rf ${new File(lsdfFilesService.getFileFinalPath(dataFile)).absolutePath}\n"
                }
                Map<String, List<File>> seqTrackDirsToDelete = deleteSeqTrack(seqTrack, check)

                seqTrackDirsToDelete.get("dirsToDelete").flatten().findAll().each {
                    deletionScript << "rm -rf ${it.absolutePath}\n"
                }
                seqTrackDirsToDelete.get("dirsToDeleteWithOtherUser").flatten().findAll().each {
                    deletionScriptOtherUser << "rm -rf ${it.absolutePath}\n"
                }
                seqTypes.add(seqTrack.seqType)
            }

            SeqScan.findAllBySample(sample)*.delete(flush: true)
            SampleIdentifier.findAllBySample(sample)*.delete(flush: true)
            sample.delete(flush: true)
        }

        seqTypes.unique().each { SeqType seqType ->
            deletionScript << "rm -rf ${individual.getViewByPidPath(seqType).absoluteDataManagementPath}\n"
        }

        individual.delete(flush: true)

        return [deletionScript.toString(), deletionScriptOtherUser.toString()]
    }

    /**
     * function to delete one sample inclusive all dependencies:
     * - seqTracks
     * - dataFiles
     * - fastqcProcessedFiles and the related objects
     * - metaDataEntries
     * - mergingAssignment
     * - seqTracks
     * - seqscan
     * - sample identifiers
     *
     * TODO: test this method
     * Attention: It is assumed that only fastq files exists for this sample.
     * Attention: It is assumed no alignment is done yet
     *
     * @param projectName the name of the project, the patient is connected with
     * @param pid the name of the individual the sample belongs to
     * @param sampleTypeName the name of the sample type the sample belongs to should be deleted
     * @param dataFileList A list of the file name of the datafiles to be deleted
     */
    void deleteSample(String projectName, String pid, String sampleTypeName, List<String> dataFileList, StringBuilder log) {
        log << "\n\ndelete ${pid} ${sampleTypeName} of ${projectName}"

        notNull(projectName, "parameter projectName may not be null")
        notNull(pid, "parameter pid may not be null")
        notNull(sampleTypeName, "parameter sampleTypeName may not be null")

        Project project = Project.findByName(projectName)
        notNull(project, "old project ${projectName} not found")

        Individual individual = Individual.findByPid(pid)
        notNull(individual, "pid ${pid} not found")
        isTrue(individual.project == project, "given project and project of individual are not the same ")

        SampleType sampleType = SampleType.findByName(sampleTypeName)
        notNull(sampleType, "sample type ${sampleTypeName} not found")

        Sample sample = getSingleSampleForIndividualAndSampleType(individual, sampleType, log)

        List<SeqTrack> seqTracks = getAndShowSeqTracksForSample(sample, log)
        assert !seqTracks || !AlignmentPass.findBySeqTrackInList(seqTracks)

        throwExceptionInCaseOfExternalMergedBamFileIsAttached(seqTracks)
        throwExceptionInCaseOfSeqTracksAreOnlyLinked(seqTracks)

        List<DataFile> dataFiles = seqTracks ? DataFile.findAllBySeqTrackInList(seqTracks) : []
        log << "\n  dataFiles (${dataFiles.size()}):"
        dataFiles.each { log << "\n    - ${it}" }
        notEmpty(dataFiles, " no datafiles found for ${sample}")
        isTrue(dataFiles.size() == dataFileList.size(), "size of dataFiles (${dataFiles.size()}) and dataFileList (${dataFileList.size()}) not match")
        dataFiles.each {
            isTrue(dataFileList.contains(it.fileName), "${it.fileName} missed in list")
        }

        // validating ends here, now the changing are started

        //file system changes are already done, so they do not need to be done here
        dataFiles.each {
            //delete first fastqc stuff
            List<FastqcProcessedFile> fastqcProcessedFiles = FastqcProcessedFile.findAllByDataFile(it)

            fastqcProcessedFiles*.delete(flush: true)

            List<MetaDataEntry> metaDataEntries = MetaDataEntry.findAllByDataFile(it)
            metaDataEntries*.delete(flush: true)

            it.delete(flush: true)
            log << "\n    deleted datafile ${it} inclusive fastqc and metadataentries"
        }

        if (seqTracks) {
            MergingAssignment.findAllBySeqTrackInList(seqTracks)*.delete(flush: true)
        }

        seqTracks.each {
            it.delete(flush: true)
            log << "\n    deleted seqtrack ${it}"
        }

        SeqScan.findAllBySample(sample)*.delete(flush: true)
        SampleIdentifier.findAllBySample(sample)*.delete(flush: true)

        sample.delete(flush: true)
        log << "\n    deleted sample ${sample}"
    }

    /**
     * Removes all fastQC information about the dataFile
     *
     * The function should be called inside a transaction (DOMAIN.withTransaction{}) to roll back changes if an exception occurs or a check fails.
     */
    void deleteFastQCInformationFromDataFile(DataFile dataFile) {
        notNull(dataFile, "The input dataFile is null")
        List<FastqcProcessedFile> fastqcProcessedFiles = FastqcProcessedFile.findAllByDataFile(dataFile)

        fastqcProcessedFiles*.delete()
    }


    /**
     * Removes all metadata-entries, which belong to the dataFile
     *
     * The function should be called inside a transaction (DOMAIN.withTransaction{}) to roll back changes if an exception occurs or a check fails.
     */
    void deleteMetaDataEntryForDataFile(DataFile dataFile) {
        notNull(dataFile, "The input dataFiles is null")
        MetaDataEntry.findAllByDataFile(dataFile)*.delete()
    }


    /**
     * Removes all information about the consistency checks of the input dataFile
     *
     * The function should be called inside a transaction (DOMAIN.withTransaction{}) to roll back changes if an exception occurs or a check fails.
     */
    void deleteConsistencyStatusInformationForDataFile(DataFile dataFile) {
        notNull(dataFile, "The input dataFiles is null")
        ConsistencyStatus.findAllByDataFile(dataFile)*.delete()
    }


    /**
     * Removes all QA-Information & the MarkDuplicate-metrics for an AbstractBamFile.
     * As input the AbstractBamFile is chosen, so that the method can be used for ProcessedBamFiles and ProcessedMergedBamFiles.
     *
     * The function should be called inside a transaction (DOMAIN.withTransaction{}) to roll back changes if an exception occurs or a check fails.
     */
    void deleteQualityAssessmentInfoForAbstractBamFile(AbstractBamFile abstractBamFile) {
        notNull(abstractBamFile, "The input AbstractBamFile is null")
        if (abstractBamFile instanceof ProcessedBamFile) {
            List<QualityAssessmentPass> qualityAssessmentPasses = QualityAssessmentPass.findAllByProcessedBamFile(abstractBamFile)

            if (qualityAssessmentPasses) {
                ChromosomeQualityAssessment.findAllByQualityAssessmentPassInList(qualityAssessmentPasses)*.delete()
                OverallQualityAssessment.findAllByQualityAssessmentPassInList(qualityAssessmentPasses)*.delete()
            }

            qualityAssessmentPasses*.delete()
        } else if (abstractBamFile instanceof ProcessedMergedBamFile) {
            List<QualityAssessmentMergedPass> qualityAssessmentMergedPasses = QualityAssessmentMergedPass.findAllByAbstractMergedBamFile(abstractBamFile)

            if (qualityAssessmentMergedPasses) {
                ChromosomeQualityAssessmentMerged.findAllByQualityAssessmentMergedPassInList(qualityAssessmentMergedPasses)*.delete()
                OverallQualityAssessmentMerged.findAllByQualityAssessmentMergedPassInList(qualityAssessmentMergedPasses)*.delete()
            }
            qualityAssessmentMergedPasses*.delete()
        } else if (abstractBamFile instanceof RoddyBamFile) {
            List<QualityAssessmentMergedPass> qualityAssessmentMergedPasses = QualityAssessmentMergedPass.findAllByAbstractMergedBamFile(abstractBamFile)
            if (qualityAssessmentMergedPasses) {
                RoddyQualityAssessment.findAllByQualityAssessmentMergedPassInList(qualityAssessmentMergedPasses)*.delete()
            }
            qualityAssessmentMergedPasses*.delete()
        } else {
            throw new RuntimeException("This BamFile type " + abstractBamFile + " is not supported")
        }

        PicardMarkDuplicatesMetrics.findAllByAbstractBamFile(abstractBamFile)*.delete()
    }

    /**
     * Delete merging related database entries, based on the mergingSetAssignments
     *
     * The function should be called inside a transaction (DOMAIN.withTransaction{}) to roll back changes if an exception occurs or a check fails.
     */
    Map<String, List<File>> deleteMergingRelatedConnectionsOfBamFile(ProcessedBamFile processedBamFile) {
        notNull(processedBamFile, "The input processedBamFile is null in method deleteMergingRelatedConnectionsOfBamFile")
        List<File> dirsToDelete = []
        List<File> dirsToDeleteWithOtherUser = []

        List<MergingSetAssignment> mergingSetAssignments = MergingSetAssignment.findAllByBamFile(processedBamFile)
        List<MergingSet> mergingSets = mergingSetAssignments*.mergingSet
        List<MergingWorkPackage> mergingWorkPackages = mergingSets*.mergingWorkPackage

        if (mergingWorkPackages.empty) {
            println "there is no merging for processedBamFile " + processedBamFile
        } else if (mergingWorkPackages.unique().size() > 1) {
            throw new RuntimeException("There is not one unique mergingWorkPackage for ProcessedBamFile " + processedBamFile)
        } else {
            MergingWorkPackage mergingWorkPackage = mergingWorkPackages.first()
            List<MergingPass> mergingPasses = mergingSets ? MergingPass.findAllByMergingSetInList(mergingSets).unique() : []
            List<ProcessedMergedBamFile> processedMergedBamFiles = mergingPasses ? ProcessedMergedBamFile.findAllByMergingPassInList(mergingPasses) : []

            mergingSetAssignments*.delete(flush: true)

            if (processedMergedBamFiles) {
                BamFilePairAnalysis.findAllBySampleType1BamFileInListOrSampleType2BamFileInList(processedMergedBamFiles, processedMergedBamFiles).each {
                    dirsToDeleteWithOtherUser << AnalysisDeletionService.deleteInstance(it)
                }
            }

            processedMergedBamFiles.each { ProcessedMergedBamFile processedMergedBamFile ->
                deleteQualityAssessmentInfoForAbstractBamFile(processedMergedBamFile)
                MergingSetAssignment.findAllByBamFile(processedMergedBamFile)*.delete(flush: true)

                dirsToDelete << analysisDeletionService.deleteSamplePairsWithoutAnalysisInstances(
                        SamplePair.findAllByMergingWorkPackage1OrMergingWorkPackage2(mergingWorkPackage, mergingWorkPackage))
                processedMergedBamFile.delete(flush: true)
            }

            mergingPasses*.delete(flush: true)

            mergingSets.each { MergingSet mergingSet ->
                // The MergingSet can only be deleted if all corresponding AbstractBamFiles are removed already
                if (!MergingSetAssignment.findByMergingSet(mergingSet)) {
                    mergingSet.delete(flush: true)
                }
            }
            // The MerginWorkPackage can only be deleted if all corresponding MergingSets and AlignmentPasses are removed already
            if (!MergingSet.findByMergingWorkPackage(mergingWorkPackage) && !AlignmentPass.findByWorkPackage(mergingWorkPackage)) {
                mergingWorkPackage.delete(flush: true)
            }
        }
        return ["dirsToDelete": dirsToDelete,
                "dirsToDeleteWithOtherUser": dirsToDeleteWithOtherUser,]
    }

    /**
     * Deletes a dataFile and all corresponding information
     */
    void deleteDataFile(DataFile dataFile) {
        notNull(dataFile, "The dataFile input of method deleteDataFile is null")

        deleteFastQCInformationFromDataFile(dataFile)
        deleteMetaDataEntryForDataFile(dataFile)
        deleteConsistencyStatusInformationForDataFile(dataFile)
        dataFile.delete()
    }

    /**
     * Deletes the datafiles, which represent external bam files, and the connection to the seqTrack (alignmentLog).
     *
     * There are not only fastq files, which are represented by a dataFile, but also bam files.
     * They are not directly connected to a seqTrack, but via an alignmentLog.
     */
    void deleteConnectionFromSeqTrackRepresentingABamFile(SeqTrack seqTrack) {
        notNull(seqTrack, "The input seqTrack of the method deleteConnectionFromSeqTrackRepresentingABamFile is null")

        AlignmentLog.findAllBySeqTrack(seqTrack).each { AlignmentLog alignmentLog ->
            DataFile.findAllByAlignmentLog(alignmentLog).each { deleteDataFile(it) }
            alignmentLog.delete()
        }
    }

    /**
     * Delete all processing information and results in the DB which are connected to one SeqTrack
     *
     * The function should be called inside a transaction (DOMAIN.withTransaction{}) to roll back changes if an exception occurs or a check fails.
     *
     * !! Be aware that the run information, alignmentLog information, mergingLog information and the seqTrack are not deleted.
     * !! If it is not needed to delete this information, this method can be used without pre-work.
     */
    Map<String, List<File>> deleteAllProcessingInformationAndResultOfOneSeqTrack(SeqTrack seqTrack, boolean enableChecks = true) {
        notNull(seqTrack, "The input seqTrack of the method deleteAllProcessingInformationAndResultOfOneSeqTrack is null")
        List<File> dirsToDelete = []
        List<File> dirsToDeleteWithOtherUser = []

        if (enableChecks) {
            throwExceptionInCaseOfSeqTracksAreOnlyLinked([seqTrack])
            throwExceptionInCaseOfExternalMergedBamFileIsAttached([seqTrack])
        }

        List<DataFile> dataFiles = DataFile.findAllBySeqTrack(seqTrack)

        if (dataFiles) {
            ProcessedSaiFile.findAllByDataFileInList(dataFiles)*.delete(flush: true)
        }

        // for ProcessedMergedBamFiles
        AlignmentPass.findAllBySeqTrack(seqTrack).each { AlignmentPass alignmentPass ->
            MergingWorkPackage mergingWorkPackage = alignmentPass.workPackage
            mergingWorkPackage.bamFileInProjectFolder = null
            mergingWorkPackage.save(flush: true)
            ProcessedBamFile.findAllByAlignmentPass(alignmentPass).each { ProcessedBamFile processedBamFile ->
                deleteQualityAssessmentInfoForAbstractBamFile(processedBamFile)
                Map<String, List<File>> processingDirsToDelete = deleteMergingRelatedConnectionsOfBamFile(processedBamFile)
                dirsToDelete << processingDirsToDelete["dirsToDelete"]
                dirsToDeleteWithOtherUser << processingDirsToDelete["dirsToDeleteWithOtherUser"]

                processedBamFile.delete(flush: true)
            }
            alignmentPass.delete(flush: true)
            // The MerginWorkPackage can only be deleted if all corresponding MergingSets and AlignmentPasses are removed already
            if (!MergingSet.findByMergingWorkPackage(mergingWorkPackage) && !AlignmentPass.findByWorkPackage(mergingWorkPackage)) {
                analysisDeletionService.deleteSamplePairsWithoutAnalysisInstances(
                        SamplePair.findAllByMergingWorkPackage1OrMergingWorkPackage2(mergingWorkPackage, mergingWorkPackage))
                mergingWorkPackage.delete(flush: true)
            }
        }

        // for RoddyBamFiles
        MergingWorkPackage mergingWorkPackage = null
        List<RoddyBamFile> bamFiles = RoddyBamFile.createCriteria().list {
            seqTracks {
                eq("id", seqTrack.id)
            }
        }

        if (bamFiles) {
            BamFilePairAnalysis.findAllBySampleType1BamFileInListOrSampleType2BamFileInList(bamFiles, bamFiles).each {
                dirsToDeleteWithOtherUser << AnalysisDeletionService.deleteInstance(it)
            }
        }

        bamFiles.each { RoddyBamFile bamFile ->
            mergingWorkPackage = bamFile.mergingWorkPackage
            mergingWorkPackage.bamFileInProjectFolder = null
            mergingWorkPackage.save(flush: true)
            deleteQualityAssessmentInfoForAbstractBamFile(bamFile)
            dirsToDelete << analysisDeletionService.deleteSamplePairsWithoutAnalysisInstances(
                    SamplePair.findAllByMergingWorkPackage1OrMergingWorkPackage2(mergingWorkPackage, mergingWorkPackage))
            bamFile.delete(flush: true)
            // The MerginWorkPackage can only be deleted if all corresponding RoddyBamFiles are removed already
            if (!RoddyBamFile.findByWorkPackage(mergingWorkPackage)) {
                dirsToDelete << analysisDeletionService.deleteSamplePairsWithoutAnalysisInstances(
                        SamplePair.findAllByMergingWorkPackage1OrMergingWorkPackage2(mergingWorkPackage, mergingWorkPackage))
                mergingWorkPackage.delete(flush: true)
            }
        }
        return ["dirsToDelete": dirsToDelete,
                "dirsToDeleteWithOtherUser": dirsToDeleteWithOtherUser,]
    }

    /**
     * Deletes the SeqScan, corresponding information and its connections to the fastqfiles/seqTracks
     */
    void deleteSeqScanAndCorrespondingInformation(SeqScan seqScan) {
        notNull(seqScan, "The input seqScan of the method deleteSeqScanAndCorrespondingInformation is null")

        List<MergingLog> mergingLogs = MergingLog.findAllBySeqScan(seqScan)
        if (mergingLogs) {
            MergedAlignmentDataFile.findAllByMergingLogInList(mergingLogs)*.delete()
        }
        mergingLogs*.delete()
        seqScan.delete()
    }

    /**
     * Deletes one SeqTrack from the DB
     *
     * The function should be called inside a transaction (DOMAIN.withTransaction{}) to roll back changes if an exception occurs or a check fails.
     *
     * !! Be aware that the run information are not deleted.
     * There is always more than one seqTrack which belongs to one Run, which is why the run is not deleted.
     * !! If it is not needed to delete this information, this method can be used without pre-work.
     */
    Map<String, List<File>> deleteSeqTrack(SeqTrack seqTrack, boolean check = true) {
        notNull(seqTrack, "The input seqTrack of the method deleteSeqTrack is null")

        if (check) {
            throwExceptionInCaseOfExternalMergedBamFileIsAttached([seqTrack])
            throwExceptionInCaseOfSeqTracksAreOnlyLinked([seqTrack])
        }

        Map<String, List<File>> dirsToDelete = deleteAllProcessingInformationAndResultOfOneSeqTrack(seqTrack)
        deleteConnectionFromSeqTrackRepresentingABamFile(seqTrack)
        DataFile.findAllBySeqTrack(seqTrack).each { deleteDataFile(it) }
        MergingAssignment.findAllBySeqTrack(seqTrack)*.delete()

        seqTrack.delete()
        return dirsToDelete
    }

    /**
     * Deletes the given run with all connected data.
     * This includes:
     * - all referenced datafiles using deleteDataFile
     * - all seqtrack using deleteSeqTrack
     * - the run itself
     *
     * The function should be called inside a transaction (DOMAIN.withTransaction{}) to roll back changes if an exception occurs or a check fails.
     */
    List<File> deleteRun(Run run, StringBuilder log) {
        notNull(run, "The input run of the method deleteRun is null")
        log << "\n\nstart deletion of run ${run}"
        List<File> dirsToDelete = []

        log << "\n  delete data files: "
        DataFile.findAllByRun(run).each {
            log << "\n     try to delete: ${it}"
            deleteDataFile(it)
        }

        log << "\n  delete seqTracks:"
        SeqTrack.findAllByRun(run).each {
            log << "\n     try to delete: ${it}"
            dirsToDelete = deleteSeqTrack(it).get("dirsToDelete")
        }

        log << "\n  try to delete run ${run}"
        run.delete()
        log << "\nfinish deletion of run ${run}"
        return dirsToDelete
    }


    /**
     * Deletes the run given by Name.
     * The method fetch the run for the name and call then deleteRun.
     *
     * The function should be called inside a transaction (DOMAIN.withTransaction{}) to roll back changes if an exception occurs or a check fails.
     *
     * @see #deleteRun(de.dkfz.tbi.otp.ngsdata.Run, java.lang.StringBuilder)
     */
    List<File> deleteRunByName(String runName, StringBuilder log) {
        notNull(runName, "The input runName of the method deleteRunByName is null")
        Run run = Run.findByName(runName)
        notNull(run, "No run with name ${runName} could be found in the database")
        List<File> dirsToDelete = deleteRun(run, log)
        log << "the following directories needs to be deleted manually: "
        log << dirsToDelete.flatten()*.path.join('\n')
        log << "And do not forget the other files/directories which belongs to the run"
        return dirsToDelete
    }

    /**
     * Deletes all processed files after the fastqc step from the give project.
     *
     * For the cases where the fastq files are not available in the project folder it has to be checked if the fastq files are still available on midterm.
     * If this is the case the GPCF has to be informed that the must not delete these fastq files during the sample swap.
     * If ExternallyProcessedMergedBamFiles were imported for this project, an exception is thrown to give the opportunity for clarification.
     * If everything was clarified the method can be called with true for "everythingVerified" so that the mentioned checks won't be executed anymore.
     * If the fastq files are not available an error is thrown.
     * If withdrawn data should be ignored, set ignoreWithdrawn "true"
     *
     * If explicitSeqTracks is defined, the defined list of seqTracks will be querried
     *
     * Return a list containing the affected seqTracks
     */
    List<SeqTrack> deleteProcessingFilesOfProject(String projectName, Path scriptOutputDirectory, boolean everythingVerified = false,
                                                  boolean ignoreWithdrawn = false, List<SeqTrack> explicitSeqTracks = []) throws FileNotFoundException {

        Project project = Project.findByName(projectName)
        assert project : "Project does not exist"

        Set<String> dirsToDelete = [] as Set
        Set<String> externalMergedBamFolders = [] as Set

        StringBuilder output = new StringBuilder()

        List<DataFile> dataFiles

        if (explicitSeqTracks.empty) {
            dataFiles = DataFile.createCriteria().list {
                seqTrack {
                    sample {
                        individual {
                            eq('project', project)
                        }
                    }
                }
            }
        } else {
            assert CollectionUtils.exactlyOneElement(explicitSeqTracks*.project.unique()) == project
            dataFiles = explicitSeqTracks ? DataFile.findAllBySeqTrackInList(explicitSeqTracks) : []
        }

        output << "found ${dataFiles.size()} data files for this project\n\n"
        assert !dataFiles.empty : "There are no SeqTracks attached to this project ${projectName}"

        List<DataFile> withdrawnDataFiles = []
        List<DataFile> missingFiles = []
        List<String> filesToClarify = []

        boolean throwException = false

        dataFiles.each {
            if (new File(lsdfFilesService.getFileViewByPidPath(it)).exists()) {
                if (it.seqTrack.linkedExternally && !everythingVerified) {
                    filesToClarify << lsdfFilesService.getFileInitialPath(it)
                    throwException = true
                }
            } else {
                // withdrawn data must have no existing fastq files,
                // to distinguish between missing files and withdrawn files this gets queried
                // an error is thrown as long as ignoreWithdrawn is false
                if (it.fileWithdrawn) {
                    withdrawnDataFiles << it
                    if (!ignoreWithdrawn) {
                        throwException = true
                    }
                } else {
                    throwException = true
                    missingFiles << it
                }
            }
        }

        if (withdrawnDataFiles) {
            output << "The fastq files of the following ${withdrawnDataFiles.size()} data files are withdrawn: \n${withdrawnDataFiles.join("\n")}\n\n"
        }

        if (missingFiles) {
            output << "The fastq files of the following ${missingFiles.size()} data files are missing: \n${missingFiles.join("\n")}\n\n"
        }

        if (filesToClarify) {
            output << "Talk to the sequencing center not to remove the following ${filesToClarify.size()} fastq files until the realignment is finished:" +
                    "\n ${filesToClarify.join("\n")}\n\n"
        }

        dataFiles = dataFiles - withdrawnDataFiles
        List<SeqTrack> seqTracks = dataFiles*.seqTrack.unique()

        // in case there are no dataFiles/seqTracks left this can be ignored.
        if (seqTracks) {
            List<ExternallyProcessedMergedBamFile> externallyProcessedMergedBamFiles = seqTrackService.returnExternallyProcessedMergedBamFiles(seqTracks)
            assert (!externallyProcessedMergedBamFiles || everythingVerified):
                    "There are ${externallyProcessedMergedBamFiles.size()} external merged bam files attached to this project. " +
                            "Clarify if the realignment shall be done anyway."
        }

        if (throwException) {
            println output
            throw new FileNotFoundException()
        }

        output << "delete content in db...\n\n"

        seqTracks.each { SeqTrack seqTrack ->

            File processingDir = new File(dataProcessingFilesService.getOutputDirectory(
                    seqTrack.individual, DataProcessingFilesService.OutputDirectories.MERGING))
            if (processingDir.exists()) {
                dirsToDelete.add(processingDir.path)
            }

            AbstractBamFile latestBamFile = MergingWorkPackage.findBySampleAndSeqType(seqTrack.sample, seqTrack.seqType)?.bamFileInProjectFolder
            if (latestBamFile) {
                File mergingDir = latestBamFile.baseDirectory
                if (mergingDir.exists()) {
                    List<ExternallyProcessedMergedBamFile> files = seqTrackService.returnExternallyProcessedMergedBamFiles([seqTrack])
                    files.each {
                        externalMergedBamFolders.add(it.getNonOtpFolder().getAbsoluteDataManagementPath().path)
                    }
                    mergingDir.listFiles().each {
                        dirsToDelete.add(it.path)
                    }
                }
            }
            deleteAllProcessingInformationAndResultOfOneSeqTrack(seqTrack, false).get("dirsToDelete").flatten().each {
                if (it) {
                    dirsToDelete.add(it.path)
                }
            }
        }

        Path bashScriptToMoveFiles = fileService.createOrOverwriteScriptOutputFile(scriptOutputDirectory, "Delete_${projectName}.sh")
        bashScriptToMoveFiles << bashHeader

        (dirsToDelete - externalMergedBamFolders).each {
            bashScriptToMoveFiles << "rm -rf ${it}\n"
        }

        output << "bash script to remove files on file system created\n\n"

        println output

        return seqTracks
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
            bashScriptToMoveFilesAsOtherUser << bashHeader

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
            ].flatten() as Set

            List<RoddyBamFile> wgbsRoddyBamFiles = roddyBamFiles.findAll { it.seqType.isWgbs() }
            if (wgbsRoddyBamFiles) {
                expectedContent.addAll(wgbsRoddyBamFiles*.finalMetadataTableFile)
                expectedContent.addAll(wgbsRoddyBamFiles*.finalMethylationDirectory)
            }

            Set<File> foundFiles = roddyBamFiles*.baseDirectory.unique()*.listFiles().flatten() as Set
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
                dirsToDelete << deleteAllProcessingInformationAndResultOfOneSeqTrack(seqTrack, enableChecks).get("dirsToDelete")
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
        groovyConsoleScriptToRestartAlignments << alignmentScriptHeader

        Path bashScriptToMoveFiles = fileService.createOrOverwriteScriptOutputFile(scriptOutputDirectory, "${bashScriptName}.sh")
        bashScriptToMoveFiles << bashHeader

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
                dirsToDelete << deleteAllProcessingInformationAndResultOfOneSeqTrack(seqTrack, !linkedFilesVerified)

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
            changeSeqType(it, newSeqType)
        }

        bashScriptToMoveFiles << "\n\n#copy and remove fastq files\n"
        bashScriptToMoveFiles << renameDataFiles(DataFile.findAllBySeqTrackInList(seqTracks), newProject, dataFileMap, oldPathsPerDataFile,
                sameLsdf, log)

        List<String> newFastqcFileNames = DataFile.findAllBySeqTrackInList(seqTracks).sort { it.id }.collect {
            fastqcDataFilesService.fastqcOutputFile(it)
        }

        bashScriptToMoveFiles << "\n\n#copy and delete fastqc files\n\n"

        oldFastqcFileNames.eachWithIndex() { oldFastqcFileName, i ->
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
     * When an item doesn't change, the 'new' value in the input map can be left empty (i.e. empty string: '') for readability.
     *
     * This implied "remains the same" needs to be made explicit for the rest of the script to work.
     *
     * !Gotcha! the swapMap is mutated in-place as a side-effect!
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
