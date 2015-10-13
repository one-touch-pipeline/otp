package de.dkfz.tbi.otp.ngsdata

import javax.sql.DataSource

import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFileService
import de.dkfz.tbi.otp.dataprocessing.AlignmentPass
import de.dkfz.tbi.otp.dataprocessing.DataProcessingFilesService
import de.dkfz.tbi.otp.dataprocessing.FastqcDataFilesService
import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile
import de.dkfz.tbi.otp.dataprocessing.ProcessedAlignmentFileService
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFileService
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.ngsqc.*

import static org.springframework.util.Assert.*

import groovy.sql.Sql

class SampleSwapService {


    DataSource dataSource

    static final String MISSING_FILES_TEXT = "The following files are expected, but not found:"
    static final String EXCESS_FILES_TEXT = "The following files are found, but not expected:"

    IndividualService individualService
    FastqcDataFilesService fastqcDataFilesService
    LsdfFilesService lsdfFilesService
    DataProcessingFilesService dataProcessingFilesService
    ProcessedAlignmentFileService processedAlignmentFileService
    ProcessedMergedBamFileService processedMergedBamFileService
    SwapHelperService swapHelperService
    ConfigService configService


    final String bashHeader = """\
            #!/bin/bash

            #PLEASE CHECK THE COMMANDS CAREFULLY BEFORE RUNNING THE SCRIPT

            set -e
            set -v

            """.stripIndent()

    final String alignmentScriptHeader = """\
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
        List<DataFile> dataFiles = DataFile.findAllBySeqTrackInList(SeqTrack.findAllBySample(sample))
        List<MetaDataKey> sampleIdentifierKeys = MetaDataKey.findAllByName(metaDataKey)
        assert sampleIdentifierKeys.unique().size() == 1
        List<MetaDataEntry> metaDataEntries = MetaDataEntry.findAllByValueAndDataFileInListAndKey(oldValue, dataFiles, sampleIdentifierKeys.first())
        metaDataEntries.each {
            it.value = newValue
            it.save()
        }
    }

    /**
     * @return The {@link SeqTrack}, possibly with a new type.
     */
    SeqTrack changeSeqType(SeqTrack seqTrack, SeqType newSeqType) {
        assert seqTrack.class == seqTrack.seqType.seqTrackClass
        if (seqTrack.seqType.id != newSeqType.id) {
            if (seqTrack.class != newSeqType.seqTrackClass) {
                [seqTrack.seqType, newSeqType]*.seqTrackClass.each {
                    if (![SeqTrack, ExomeSeqTrack].contains(it)) {
                        throw new UnsupportedOperationException("Changing the SeqTrack class from or to ${it} is not supported yet.")
                    }
                }
                Sql sql = new Sql(dataSource)
                assert 1 == sql.executeUpdate("update seq_track set class = ${newSeqType.seqTrackClass.name} where id = ${seqTrack.id} and class = ${seqTrack.class.name};")
                SeqTrack.withSession { session ->
                    session.clear()
                }
                seqTrack = SeqTrack.get(seqTrack.id)
            }
            assert seqTrack.class == newSeqType.seqTrackClass
            seqTrack.seqType = newSeqType
            assert seqTrack.save(failOnError: true, flush: true)
        }
        return seqTrack
    }

    /**
     * rename all sample identifiers of the given identifier. The new name is the old name added with the text
     * "was changed on" and the current date.
     *
     * @param sample the sample which identifier should be renamed
     * @return void
     */
    void renameSampleIdentifiers(Sample sample, StringBuilder outputStringBuilder) {
        List<SampleIdentifier> sampleIdentifiers = SampleIdentifier.findAllBySample(sample)
        outputStringBuilder << "\n  sampleIdentifier for sample ${sample} (${sampleIdentifiers.size()}): ${sampleIdentifiers}"
        String postfix = " was changed on ${new Date().format('yyyy-MM-dd')}"
        sampleIdentifiers.each { SampleIdentifier sampleIdentifier ->
            String oldSampleIdentifier = sampleIdentifier.name
            sampleIdentifier.name += postfix
            sampleIdentifier.save()
            changeMetadataEntry(sample, "SAMPLE_ID", oldSampleIdentifier, sampleIdentifier.name)
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
    Sample getSingleSampleForIndividualAndSampleType(Individual individual, SampleType sampleType, StringBuilder outputStringBuilder) {
        List<Sample> samples = Sample.findAllByIndividualAndSampleType(individual, sampleType)
        outputStringBuilder << "\n  samples (${samples.size()}): ${samples}"
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
    List<SeqTrack> getAndShowSeqTracksForSample(Sample sample, StringBuilder outputStringBuilder) {
        List<SeqTrack> seqTracks = SeqTrack.findAllBySample(sample)
        outputStringBuilder << "\n  seqtracks (${seqTracks.size()}): "
        seqTracks.each { outputStringBuilder << "\n    - ${it}" }
        return seqTracks
    }

    /**
     * get the dataFiles for the seqTracks, validate and show them.
     *
     * @param seqTracks the seqTracks the dataFiles should be fetch for
     * @param dataFileMap A map of old file name and new file name
     * @return the dataFiles for the seqTracks
     */
    List<DataFile> getAndValidateAndShowDataFilesForSeqTracks(List<SeqTrack> seqTracks, Map<String, String> dataFileMap, StringBuilder outputStringBuilder) {
        List<DataFile> dataFiles = DataFile.findAllBySeqTrackInList(seqTracks)
        outputStringBuilder << "\n  dataFiles (${dataFiles.size()}):"
        dataFiles.each { outputStringBuilder << "\n    - ${it}" }
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
    List<DataFile> getAndValidateAndShowAlignmentDataFilesForSeqTracks(List<SeqTrack> seqTracks, Map<String, String> dataFileMap, StringBuilder outputStringBuilder) {
        List<AlignmentLog> alignmentsLog = AlignmentLog.findAllBySeqTrackInList(seqTracks)
        if (!alignmentsLog) {
            return []
        }
        List<DataFile> dataFiles = DataFile.findAllByAlignmentLogInList(alignmentsLog)
        outputStringBuilder << "\n  alignment dataFiles (${dataFiles.size()}):"
        dataFiles.each { outputStringBuilder << "\n    - ${it}" }
        dataFiles.each {
            isTrue(dataFileMap.containsKey(it.fileName), "${it.fileName} missed in map")
        }
        return dataFiles
    }

    /**
     * function to get the copy and remove command for one fastqc file
     */
    String copyAndRemoveFastqcFile(String oldDataFileName, String newDataFileName, StringBuilder outputStringBuilder, boolean failOnMissingFiles) {
        File oldDataFile = new File(oldDataFileName)
        File newDataFile = new File(newDataFileName)
        String bashCommand = ""
        if (oldDataFile.exists()) {
            if (newDataFile.exists()) {
                if (oldDataFileName != newDataFileName) {
                    bashCommand = "# rm -f '${oldDataFileName}'"
                } else {
                    bashCommand = "# the old and the new data file ('${oldDataFileName}') are the same\n"
                }
            } else {
                bashCommand = """
mkdir -p -m 2750 '${newDataFile.getParent()}';
mv '${oldDataFileName}' '${newDataFileName}';
\n"""
            }
        } else {
            if (newDataFile.exists()) {
                bashCommand = "# ${newDataFileName} is already at the correct position"
            } else {
                String message = "The fastqcFile (${newDataFileName}) can not be found"
                if(failOnMissingFiles) {
                    throw new RuntimeException(message)
                } else {
                    outputStringBuilder << '\n' << message
                }
            }
        }
        return bashCommand
    }


    /**
     * creates a map containing for every Datafile of the list the direct and the viewByPid file name as map.
     *
     */
    Map<DataFile, Map<String, String>> collectFileNamesOfDataFiles(List<DataFile> dataFiles) {
        Map<DataFile, Map<String, String>> map = [:]
        dataFiles.each { DataFile dataFile ->
            String directFileName = lsdfFilesService.getFileFinalPath(dataFile)
            String vbpFileName = lsdfFilesService.getFileViewByPidPath(dataFile)
            map.putAt(dataFile, [directFileName: directFileName, vbpFileName: vbpFileName])
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
    String renameDataFiles(List<DataFile> dataFiles, Project newProject, Map<String, String> dataFileMap, Map<DataFile, Map<String, String>> oldDataFileNameMap, boolean sameLsdf, StringBuilder outputStringBuilder) {
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

            String oldDirectFileName = oldDataFileNameMap.find {key, value -> key.id == it.id}.value["directFileName"]
            String oldVbpFileName = oldDataFileNameMap.find {key, value -> key.id == it.id}.value["vbpFileName"]
            File directFile = new File(oldDirectFileName)
            File vbpFile = new File(oldVbpFileName)

            if (directFile.exists()) {
                filesAlreadyMoved = false
            }
            bashMoveVbpFile = "rm -f '${oldVbpFileName}';\n"

            String old = it.fileName
            it.project = newProject
            it.fileName = it.vbpFileName = dataFileMap[it.fileName]
            if (it.readNumber == null && it.fileWithdrawn && it.fileType && it.fileType.type == FileType.Type.SEQUENCE && it.fileType.vbpPath == "/sequence/") {
                outputStringBuilder << "\n====> set read number for withdrawn data file"
                it.readNumber = MetaDataService.findOutReadNumberIfSingleEndOrByFileName(it.fileName, it.seqTrack.seqType.libraryLayout == 'SINGLE')
            }
            it.save(flush: true)
            outputStringBuilder << "\n    changed ${old} to ${it.fileName}"

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
mkdir -p -m 2750 '${directFile.getParent()}';"""
                if (sameLsdf) {
                    bashMoveDirectFile += """
mv '${oldDirectFileName}' '${newDirectFileName}';
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
            bashMoveVbpFile += "mkdir -p -m 2750 '${vbpFile.getParent()}'; ln -s '${newDirectFileName}' '${newVbpFileName}'"
            bashScriptToMoveFiles += "${bashMoveDirectFile}\n${bashMoveVbpFile}\n\n"
        }
        return bashScriptToMoveFiles
    }



    /**
     * The input SeqTrack is passed to the AlignmentDecider
     */
    String startAlignmentForSeqTrack(SeqTrack seqTrack) {
        if (seqTrack.seqType.name == SeqTypeNames.WHOLE_GENOME.seqTypeName && seqTrack.seqType.libraryLayout == "PAIRED" && seqTrack.getConfiguredReferenceGenome()) {
            return "//lane: ${seqTrack}\nctx.seqTrackService.decideAndPrepareForAlignment(SeqTrack.get(${seqTrack.id}))\n"
        } else if (seqTrack.seqType.name == SeqTypeNames.EXOME.seqTypeName && seqTrack.seqType.libraryLayout == "PAIRED" && seqTrack.getConfiguredReferenceGenome()) {
            if ((seqTrack instanceof ExomeSeqTrack) && (seqTrack.libraryPreparationKit != null) &&
                    (BedFile.findByReferenceGenomeAndLibraryPreparationKit(seqTrack.getConfiguredReferenceGenome(), seqTrack.libraryPreparationKit))) {
                return "//lane: ${seqTrack}\nctx.seqTrackService.decideAndPrepareForAlignment(SeqTrack.get(${seqTrack.id}))\n"
            } else {
                return "//!!! The BED file or the library preparation kit are missing for seqTrack ${seqTrack.id}: ${seqTrack}\n"
            }
        }
        return "// The SeqTrack ${seqTrack} has the seqType ${seqTrack.seqType.name} and will not be aligned\n"
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
    void moveIndividual(String oldProjectName, String newProjectName, String oldPid, String newPid, Map<String, String> sampleTypeMap, Map<String, String> dataFileMap, String bashScriptName, StringBuilder outputStringBuilder, boolean failOnMissingFiles, String scriptOutputDirectory) {
        outputStringBuilder << "\n\nmove ${oldPid} of ${oldProjectName} to ${newPid} of ${newProjectName} "
        outputStringBuilder << "\n  swap sample: "
        sampleTypeMap.each { a, b ->
            // When the old and the new sample type are the same, it shall be enough to write it only once in the input map.
            // This part is to complete the map, when only the keys are provided.
            if ((a.size() != 0) && (b.size() == 0)) {
                b = a
                sampleTypeMap.put(a, b)
            }
            outputStringBuilder << "\n    - ${a} --> ${b}"
        }
        outputStringBuilder << "\n  swap datafile: "
        dataFileMap.each { a, b ->
            // When the old and the new data files are the same, it shall be enough to write it only once in the input map.
            // This part is to complete the map, when only the keys are provided.
            if ((a.size() != 0) && (b.size() == 0)) {
                b = a
                dataFileMap.put(a, b)
            }
            outputStringBuilder << "\n    - ${a} --> ${b}"
        }

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
        outputStringBuilder << "\n  samples (${samples.size()}): ${samples}"
        notEmpty(samples, "no samples found for ${oldIndividual}")
        isTrue(samples.size() == sampleTypeMap.size())
        samples.each { Sample sample ->
            isTrue(sampleTypeMap.containsKey(sample.sampleType.name), "${sample.sampleType.name} missed in map")
            notNull(SampleType.findByName(sampleTypeMap.get(sample.sampleType.name)), "${sampleTypeMap.get(sample.sampleType.name)} not found in database")
        }

        isTrue(oldIndividual.project == oldProject, "old individual ${oldPid} should be in project {oldProjectName}, but was in ${oldIndividual.project}")

        List<SeqTrack> seqTracks = SeqTrack.findAllBySampleInList(samples)
        outputStringBuilder << "\n  seqtracks (${seqTracks.size()}): "
        seqTracks.each { outputStringBuilder << "\n    - ${it}" }

        boolean sameLsdf = oldProject.realmName == newProject.realmName

        List<File> dirsToDelete = []

        swapHelperService.throwExceptionInCaseOfExternalMergedBamFileIsAttached(seqTracks)

        // now the changing process(procedure) starts
        if (AlignmentPass.findBySeqTrackInList(seqTracks)) {
            outputStringBuilder << "\n -->     found alignments for seqtracks (${AlignmentPass.findAllBySeqTrackInList(seqTracks)*.seqTrack.unique()}): "
        }

        File groovyConsoleScriptToRestartAlignments = createFileSafely("${scriptOutputDirectory}", "restartAli_${bashScriptName}.groovy")
        groovyConsoleScriptToRestartAlignments << alignmentScriptHeader

        File bashScriptToMoveFiles = createFileSafely("${scriptOutputDirectory}", "${bashScriptName}.sh")
        bashScriptToMoveFiles << bashHeader


        createBashScriptRoddy(seqTracks, dirsToDelete, scriptOutputDirectory, outputStringBuilder, bashScriptName, bashScriptToMoveFiles)

        seqTracks.each { SeqTrack seqTrack ->
            dirsToDelete << swapHelperService.deleteAllProcessingInformationAndResultOfOneSeqTrack(seqTrack)
            groovyConsoleScriptToRestartAlignments << startAlignmentForSeqTrack(seqTrack)
        }

        List<DataFile> fastqDataFiles = getAndValidateAndShowDataFilesForSeqTracks(seqTracks, dataFileMap, outputStringBuilder)
        List<DataFile> bamDataFiles = getAndValidateAndShowAlignmentDataFilesForSeqTracks(seqTracks, dataFileMap, outputStringBuilder)
        List<DataFile> dataFiles = [fastqDataFiles, bamDataFiles].flatten()
        Map<DataFile, Map<String, String>> oldDataFileNameMap = collectFileNamesOfDataFiles(dataFiles)
        List<String> oldFastqcFileNames = fastqDataFiles.collect { fastqcDataFilesService.fastqcOutputFile(it) }


        outputStringBuilder << "\n  changing ${oldIndividual.project} to ${newProject} for ${oldIndividual}"
        oldIndividual.project = newProject
        oldIndividual.pid = newPid
        oldIndividual.mockPid = newPid
        oldIndividual.mockFullName = newPid
        oldIndividual.save(flush: true)
        samples.each { Sample sample ->
            SampleType newSampleType = SampleType.findByName(sampleTypeMap.get(sample.sampleType.name))
            outputStringBuilder << "\n    change ${sample.sampleType.name} to ${newSampleType.name}"
            renameSampleIdentifiers(sample, outputStringBuilder)
            sample.sampleType = newSampleType
            sample.save(flush: true)
        }


        bashScriptToMoveFiles << "################ move data files ################ \n"
        bashScriptToMoveFiles << renameDataFiles(dataFiles, newProject, dataFileMap, oldDataFileNameMap, sameLsdf, outputStringBuilder)


        samples = Sample.findAllByIndividual(oldIndividual)
        seqTracks = SeqTrack.findAllBySampleInList(samples)
        List<DataFile> newDataFiles =DataFile.findAllBySeqTrackInList(seqTracks)
        List<String> newFastqcFileNames = newDataFiles.collect { fastqcDataFilesService.fastqcOutputFile(it) }

        bashScriptToMoveFiles << "\n\n\n ################ move fastq files ################ \n"

        oldFastqcFileNames.eachWithIndex() { oldFastqcFileName, i ->
            bashScriptToMoveFiles << copyAndRemoveFastqcFile(oldFastqcFileName, newFastqcFileNames.get(i), outputStringBuilder, failOnMissingFiles)
        }

        bashScriptToMoveFiles << "# delete snv stuff\n"
        dirsToDelete.flatten()*.path.each {
            bashScriptToMoveFiles << "#rm -rf ${it}\n"
        }

        bashScriptToMoveFiles << "\n\n\n ################ delete old Individual ################ \n"
        bashScriptToMoveFiles << "# rm -rf '${configService.getProjectRootPath(oldProject)}/sequencing/*/view-by-pid/${oldPid}/' \n"
        bashScriptToMoveFiles << "# rm -rf '${processingPathToOldIndividual}'\n"

        individualService.createComment("Individual swap", [individual: oldIndividual, project: oldProjectName, pid: oldPid], [individual: oldIndividual, project: newProjectName, pid: newPid])
    }

    /**
     * Once all connections to an individual are deleted or moved and the individual is 'empty' it can be deleted.
     * The mutations, resultDataFiles and StudySamples are empty anyway. Just for completeness I remove them.
     * TODO: test this method
     */
    void deleteIndividual(String pid) {
        Individual individual = Individual.findByPid(pid)
        notNull(individual, "There was no individual found for the pid " + pid)
        swapHelperService.deleteMutationsAndResultDataFilesOfOneIndividual(individual)
        StudySample.findAllByIndividual(individual)*.delete()

        individual.delete()
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
     */
    void moveSample(String oldProjectName, String newProjectName, String oldPid, String newPid, String oldSampleTypeName, String newSampleTypeName, Map<String, String> dataFileMap, String bashScriptName, StringBuilder outputStringBuilder, boolean failOnMissingFiles, String scriptOutputDirectory) throws IOException{
        outputStringBuilder << "\n\nmove ${oldPid} ${oldSampleTypeName} of ${oldProjectName} to ${newPid} ${newSampleTypeName} of ${newProjectName} "
        outputStringBuilder << "\n  swap datafile: "
        dataFileMap.each { a, b ->
            // When the old and the new data files are the same, it shall be enough to write it only once in the input map.
            // This part is to complete the map, when only the keys are provided.
            if ((a.size() != 0) && !b) {
                b = a
                dataFileMap.put(a, b)
            }
            outputStringBuilder << "\n    - ${a} --> ${b}"
        }

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
        isTrue(oldIndividual.project == oldProject, "old individual ${oldPid} should be in project {oldProjectName}, but was in ${oldIndividual.project}")
        isTrue(newIndividual.project == newProject, "new individual ${newPid} should be in project {newProjectName}, but was in ${newIndividual.project}")

        SampleType oldSampleType = SampleType.findByName(oldSampleTypeName)
        notNull(oldSampleType, "old sample type ${oldSampleTypeName} not found")
        SampleType newSampleType = SampleType.findByName(newSampleTypeName)
        notNull(newSampleType, "new sample type ${newSampleTypeName} not found")

        Sample sample = getSingleSampleForIndividualAndSampleType(oldIndividual, oldSampleType, outputStringBuilder)

        boolean sameLsdf = oldProject.realmName == newProject.realmName

        List<SeqTrack> seqTrackList = getAndShowSeqTracksForSample(sample, outputStringBuilder)

        swapHelperService.throwExceptionInCaseOfExternalMergedBamFileIsAttached(seqTrackList)

        if (AlignmentPass.findBySeqTrackInList(seqTrackList)) {
            outputStringBuilder << "\n -->     found alignments for seqtracks (${AlignmentPass.findBySeqTrackInList(seqTrackList)*.seqTrack.unique()}): "
        }

        List<DataFile> fastqDataFiles = getAndValidateAndShowDataFilesForSeqTracks(seqTrackList, dataFileMap, outputStringBuilder)
        List<DataFile> bamDataFiles = getAndValidateAndShowAlignmentDataFilesForSeqTracks(seqTrackList, dataFileMap, outputStringBuilder)
        List<DataFile> dataFiles = [fastqDataFiles, bamDataFiles].flatten()
        Map<DataFile, Map<String, String>> oldDataFileNameMap = collectFileNamesOfDataFiles(dataFiles)
        List<String> oldFastqcFileNames = fastqDataFiles.collect { fastqcDataFilesService.fastqcOutputFile(it) }
        List<File> dirsToDelete = []

        // validating ends here, now the changing are started

        File groovyConsoleScriptToRestartAlignments = createFileSafely("${scriptOutputDirectory}", "restartAli_${bashScriptName}.groovy")
        groovyConsoleScriptToRestartAlignments << alignmentScriptHeader

        File bashScriptToMoveFiles = createFileSafely("${scriptOutputDirectory}", "${bashScriptName}.sh")
        bashScriptToMoveFiles << bashHeader

        createBashScriptRoddy(seqTrackList, dirsToDelete, scriptOutputDirectory, outputStringBuilder, bashScriptName, bashScriptToMoveFiles)

        seqTrackList.each { SeqTrack seqTrack ->
            dirsToDelete << swapHelperService.deleteAllProcessingInformationAndResultOfOneSeqTrack(seqTrack)
            groovyConsoleScriptToRestartAlignments << startAlignmentForSeqTrack(seqTrack)
        }

        if (AlignmentPass.findBySeqTrackInList(seqTrackList)) {
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
                String oldProjectPathToMergedFiles = AbstractMergedBamFileService.destinationDirectory(latestProcessedMergedBamFile)
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
        sample.save(flush: true, failOnError: true)

        renameSampleIdentifiers(sample, outputStringBuilder)


        bashScriptToMoveFiles << "################ move data files ################ \n"
        bashScriptToMoveFiles << renameDataFiles(dataFiles, newProject, dataFileMap, oldDataFileNameMap, sameLsdf, outputStringBuilder)

        List<String> newFastqcFileNames = fastqDataFiles.collect { fastqcDataFilesService.fastqcOutputFile(it) }

        bashScriptToMoveFiles << "\n\n\n ################ move fastq files ################ \n"

        oldFastqcFileNames.eachWithIndex() { oldFastqcFileName, i ->
            bashScriptToMoveFiles << copyAndRemoveFastqcFile(oldFastqcFileName, newFastqcFileNames.get(i), outputStringBuilder, failOnMissingFiles)
        }


        bashScriptToMoveFiles << "# delete snv stuff\n"
        dirsToDelete.flatten()*.path.each {
            bashScriptToMoveFiles << "#rm -rf ${it}\n"
        }

        individualService.createComment("Sample swap", [individual: oldIndividual, project: oldProjectName, pid: oldPid, sampleType: oldSampleTypeName],
                [individual: newIndividual, project: newProjectName, pid: newPid, sampleType: newSampleTypeName])
    }

    /**
     * create a bash script to delete files from roddy,
     * the script must be executed as other user
     */
    private void createBashScriptRoddy(List<SeqTrack> seqTrackList, List<File> dirsToDelete, String scriptOutputDirectory, StringBuilder outputStringBuilder, String bashScriptName, File bashScriptToMoveFiles) {
        List<RoddyBamFile> roddyBamFiles = RoddyBamFile.createCriteria().list {
            seqTracks {
                inList("id", seqTrackList*.id)
            }
        }

        if (roddyBamFiles) {
            File bashScriptToMoveFilesAsOtherUser = createFileSafely("${scriptOutputDirectory}", "${bashScriptName}-otherUser.sh")
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
                            "#rm -rf ${roddyBamFile.getWorkSingleLaneQADirectories().values()*.listFiles().flatten()*.absolutePath.join("\n#rm -rf ")}\n"
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
            Set<File> foundFiles = roddyBamFiles*.baseDirectory.unique()*.listFiles().flatten() as Set
            if (foundFiles != expectedContent) {
                List<File> missingFiles = (expectedContent - foundFiles).sort()
                List<File> excessFiles = (foundFiles - expectedContent).sort()

                outputStringBuilder << "\n\n=====================================================\n"
                if (missingFiles) {
                    outputStringBuilder << "\n${MISSING_FILES_TEXT}\n    ${missingFiles.join('\n    ')}"
                }
                if (excessFiles) {
                    outputStringBuilder << "\n${EXCESS_FILES_TEXT}\n    ${excessFiles.join('\n    ')}"
                }
                outputStringBuilder << "\n=====================================================\n"
            }

            bashScriptToMoveFiles << "#rm -rf ${roddyBamFiles[0].baseDirectory}\n"

            seqTrackList.each { SeqTrack seqTrack ->
                dirsToDelete << swapHelperService.deleteAllProcessingInformationAndResultOfOneSeqTrack(seqTrack)
            }
        }
    }

    /**
     * create a new file
     * if it already exists, it will be overwritten
     */
    def createFileSafely(String parent, String name) {
        File file = new File(parent, name)
        if (file.exists()) {
            assert file.delete()
        }
        assert file.createNewFile()
        return file
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
    void deleteSample(String projectName, String pid, String sampleTypeName, List<String> dataFileList, StringBuilder outputStringBuilder) {
        outputStringBuilder << "\n\ndelete ${pid} ${sampleTypeName} of ${projectName}"

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

        Sample sample = getSingleSampleForIndividualAndSampleType(individual, sampleType, outputStringBuilder)

        List<SeqTrack> seqTracks = getAndShowSeqTracksForSample(sample, outputStringBuilder)
        isNull(AlignmentPass.findBySeqTrackInList(seqTracks))

        swapHelperService.throwExceptionInCaseOfExternalMergedBamFileIsAttached(seqTracks)

        List<DataFile> dataFiles = DataFile.findAllBySeqTrackInList(seqTracks)
        outputStringBuilder << "\n  dataFiles (${dataFiles.size()}):"
        dataFiles.each { outputStringBuilder << "\n    - ${it}" }
        notEmpty(dataFiles, " no datafiles found for ${sample}")
        isTrue(dataFiles.size() == dataFileList.size(), "size of dataFiles (${dataFiles.size()} and dataFileList (${dataFileList.size()} not match")
        dataFiles.each {
            isTrue(dataFileList.contains(it.fileName), "${it.fileName} missed in list")
        }

        // validating ends here, now the changing are started

        //file system changes are already done, so they do not need to be done here
        dataFiles.each {
            //delete first fastqc stuff
            List<FastqcProcessedFile> fastqcProcessedFiles = FastqcProcessedFile.findAllByDataFile(it)

            FastqcBasicStatistics.findAllByFastqcProcessedFileInList(fastqcProcessedFiles)*.delete(flush: true)
            FastqcKmerContent.findAllByFastqcProcessedFileInList(fastqcProcessedFiles)*.delete(flush: true)
            FastqcModuleStatus.findAllByFastqcProcessedFileInList(fastqcProcessedFiles)*.delete(flush: true)
            FastqcOverrepresentedSequences.findAllByFastqcProcessedFileInList(fastqcProcessedFiles)*.delete(flush: true)
            FastqcPerBaseSequenceAnalysis.findAllByFastqcProcessedFileInList(fastqcProcessedFiles)*.delete(flush: true)
            FastqcPerSequenceGCContent.findAllByFastqcProcessedFileInList(fastqcProcessedFiles)*.delete(flush: true)
            FastqcPerSequenceQualityScores.findAllByFastqcProcessedFileInList(fastqcProcessedFiles)*.delete(flush: true)
            FastqcSequenceDuplicationLevels.findAllByFastqcProcessedFileInList(fastqcProcessedFiles)*.delete(flush: true)
            FastqcSequenceLengthDistribution.findAllByFastqcProcessedFileInList(fastqcProcessedFiles)*.delete(flush: true)

            fastqcProcessedFiles*.delete(flush: true)

            List<MetaDataEntry> metaDataEntries = MetaDataEntry.findAllByDataFile(it)
            metaDataEntries*.delete(flush: true)

            it.delete(flush: true)
            outputStringBuilder << "\n    deleted datafile ${it} inclusive fastqc and metadataentries"
        }

        MergingAssignment.findAllBySeqTrackInList(seqTracks)*.delete(flush: true)

        seqTracks.each {
            it.delete(flush: true)
            outputStringBuilder << "\n    deleted seqtrack ${it}"
        }

        SeqScan.findAllBySample(sample)*.delete(flush: true)
        SampleIdentifier.findAllBySample(sample)*.delete(flush: true)

        sample.delete(flush: true)
        outputStringBuilder << "\n    deleted sample ${sample}"
    }
}
